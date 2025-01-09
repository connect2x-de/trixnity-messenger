package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.MessageUserReactions
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionKey
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import de.connect2x.trixnity.messenger.viewmodel.util.getMessageEditHistory
import de.connect2x.trixnity.messenger.viewmodel.util.getMessageReadReceipts
import de.connect2x.trixnity.messenger.viewmodel.util.getMessageUserReactions
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface MessageMetadataViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        eventId: EventId,
        roomId: RoomId,
        onBack: () -> Unit,
    ): MessageMetadataViewModel =
        MessageMetadataViewModelImpl(
            viewModelContext,
            eventId,
            roomId,
            onBack,
        )

    companion object : MessageMetadataViewModelFactory
}

interface MessageMetadataViewModel {
    val eventId: EventId
    val senderInfo: StateFlow<UserInfoElement?>
    val userInteractions: StateFlow<List<MessageUserInteraction>>
    val reactionCounts: StateFlow<Map<ReactionKey, UInt>>
    val edits: StateFlow<List<TimelineElementHolderViewModel>>

    val error: StateFlow<String?>
//    val errorCause: StateFlow<String?>

    //    fun errorDismiss()
    fun back()
}

class MessageMetadataViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val eventId: EventId,
    private val roomId: RoomId,
    private val onBack: () -> Unit,
) : MessageMetadataViewModel, MatrixClientViewModelContext by viewModelContext {
    private val config = get<MatrixMessengerConfiguration>()
    private val initials = get<Initials>()
    private val timeZone = get<TimeZone>()

    private val backCallback = BackCallback {
        onBack()
    }

    init {
        backHandler.register(backCallback)
    }

    override val edits: StateFlow<List<TimelineElementHolderViewModel>> =
        getMessageEditHistory(matrixClient, eventId, roomId)
            .map {
                it.map { timelineEvent ->
                    val roomId = timelineEvent.roomId
                    val eventId = timelineEvent.eventId
                    val sender = timelineEvent.sender
                    val key = timelineEvent.event.unsigned?.transactionId?.asKey(timelineEvent.roomId)
                        ?: eventId.asKey(timelineEvent.roomId)
                    log.trace { "generate timeline element $eventId" }
                    val lifecycleRegistry = LifecycleRegistry()
                    get<TimelineElementHolderViewModelFactory>().create(
                        viewModelContext = childContextWithOwnLifecycle(lifecycleRegistry),
                        key = key,
                        timelineEventFlow = flowOf(timelineEvent),  // TODO: is this correct?
                        roomId = roomId,
                        eventId = eventId,
                        sender = sender,
                        formattedDate = formatDate(
                            Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                                .toLocalDateTime(timeZone)
                        ),
                        formattedTime = formatTime(
                            Instant.fromEpochMilliseconds(timelineEvent.originTimestamp)
                                .toLocalDateTime(timeZone)
                        ),
                        showLoadingIndicatorBefore = flowOf(false),
                        showLoadingIndicatorAfter = flowOf(false),
                        showReplacedEvents = flowOf(true),
                        onMessageReplace = { _, _ -> },
                        onMessageReply = { _, _ -> },
                        onMessageReport = { _, _ -> },
                        onOpenMention = { _, _ -> },
                        onOpenMetadata = {},
                    )
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())

    private fun EventId.asKey(roomId: RoomId? = null) =
        (roomId ?: this@MessageMetadataViewModelImpl.roomId).full + "-" + full

    private fun String.asKey(roomId: RoomId? = null) =
        (roomId ?: this@MessageMetadataViewModelImpl.roomId).full + "-" + this

    @OptIn(ExperimentalCoroutinesApi::class)
    override val senderInfo: StateFlow<UserInfoElement?> =
        matrixClient.room.getTimelineEvent(roomId, eventId) {
            fetchSize = 1
            allowReplaceContent = false
        }.flatMapLatest {
            it?.let { event ->
                matrixClient.user.getById(roomId, it.sender)
                    .filterNotNull().map { roomUser ->
                        roomUser.toUserInfoElement(
                            coroutineScope,
                            matrixClient,
                            initials,
                            config.avatarMaxSize,
                            roomUser.userId,
                        )
                    }
            } ?: flowOf(null)
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val readers: StateFlow<Set<RoomUser>> =
        matrixClient.room.getTimelineEvent(roomId, eventId) {
            fetchSize = 1
            allowReplaceContent = false
        }
            .map { it?.sender }
            .filterNotNull()
            .flatMapLatest { senderUserId ->
                getMessageReadReceipts(matrixClient, senderUserId, roomId, eventId)
            }.stateIn(coroutineScope, whileSubscribedWithTimeout, emptySet())

    val reactions: StateFlow<MessageUserReactions> =
        getMessageUserReactions(matrixClient, roomId, eventId)
            .stateIn(coroutineScope, whileSubscribedWithTimeout, MessageUserReactions())

    override val userInteractions: StateFlow<List<MessageUserInteraction>> =
        combine(readers, reactions) { readers, reactions ->
            (readers + reactions.byUser.keys).toSet().map { roomUser ->
                MessageUserInteraction(
                    userInfo = roomUser.toUserInfoElement(
                        coroutineScope,
                        matrixClient,
                        initials,
                        config.avatarMaxSize,
                        roomUser.userId,
                    ),
                    reactions = reactions.byUser.getOrElse(roomUser) { emptySet() },
                    hasRead = readers.contains(roomUser),
                )
            }
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, emptyList())


    override val reactionCounts: StateFlow<Map<ReactionKey, UInt>> =
        reactions.map { it.byCount }
            .stateIn(coroutineScope, whileSubscribedWithTimeout, emptyMap())

    override val error: StateFlow<String?> = MutableStateFlow(null)
//        get() = TODO("Not yet implemented")

    override fun back() {
        onBack()
    }

}

data class MessageUserInteraction(
    val userInfo: UserInfoElement,
    val reactions: Set<ReactionKey>,
    val hasRead: Boolean,
)

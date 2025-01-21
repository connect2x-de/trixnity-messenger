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
import de.connect2x.trixnity.messenger.viewmodel.util.getMessageReadReceipts
import de.connect2x.trixnity.messenger.viewmodel.util.getMessageUserReactions
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
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
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
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
    val messagePreview: StateFlow<TimelineElementHolderViewModel?>
    val userInteractions: StateFlow<List<MessageUserInteraction>>
    val reactionCounts: StateFlow<Map<ReactionKey, UInt>>
    val error: StateFlow<String?>
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

    override fun back() {
        onBack()
    }

    private val message: StateFlow<TimelineEvent?> =
        matrixClient.room.getTimelineEvent(roomId, eventId)
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val messagePreview: StateFlow<TimelineElementHolderViewModel?> =
        message.map { timelineEvent ->
            if (timelineEvent == null) return@map null
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
                showReplacedEvents = flowOf(false),
                onMessageReplace = { _, _ -> },
                onMessageReply = { _, _ -> },
                onMessageReport = { _, _ -> },
                onOpenMention = { _, _ -> },
                onOpenMetadata = {},
            )
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    private fun EventId.asKey(roomId: RoomId? = null) =
        (roomId ?: this@MessageMetadataViewModelImpl.roomId).full + "-" + full

    private fun String.asKey(roomId: RoomId? = null) =
        (roomId ?: this@MessageMetadataViewModelImpl.roomId).full + "-" + this

    @OptIn(ExperimentalCoroutinesApi::class)
    override val senderInfo: StateFlow<UserInfoElement?> =
        message.flatMapLatest {
            it?.let { event ->
                matrixClient.user.getById(roomId, event.sender)
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
    private val readers: StateFlow<Map<UserId, Flow<RoomUser?>>> =
        message.map { it?.sender }
            .filterNotNull()
            .flatMapLatest { senderUserId ->
                getMessageReadReceipts(matrixClient, senderUserId, roomId, eventId)
            }
            .stateIn(coroutineScope, whileSubscribedWithTimeout, emptyMap())

    private val reactions: StateFlow<MessageUserReactions> =
        getMessageUserReactions(matrixClient, roomId, eventId)
            .stateIn(coroutineScope, whileSubscribedWithTimeout, MessageUserReactions())

    override val userInteractions: StateFlow<List<MessageUserInteraction>> =
        combine(readers, reactions) { readers, reactions ->
            reactions.byUser.map { (userId, userReactions) ->
                userId.toInteraction(
                    userReactions.roomUserFlow,
                    userReactions.reactions,
                    hasRead = { readers.containsKey(it) }
                )
            } + readers.mapNotNull { (userId, roomUserFlow) ->
                if (reactions.byUser.containsKey(userId)) null
                else userId.toInteraction(
                    roomUserFlow,
                    hasRead = { true }
                )
            }
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, emptyList())

    private fun UserId.toInteraction(
        roomUserFlow: Flow<RoomUser?>,
        reactions: Set<ReactionKey>? = null,
        hasRead: (UserId) -> Boolean,
    ): MessageUserInteraction {
        val info: StateFlow<UserInfoElement?> = roomUserFlow.map { roomUser ->
            roomUser.toUserInfoElement(
                coroutineScope,
                matrixClient,
                initials,
                config.avatarMaxSize,
                this,
            )
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)
        return MessageUserInteraction(
            userInfo = info,
            reactions = reactions ?: setOf(),
            hasRead = hasRead(this),
            userId = this,
        )
    }

    override val reactionCounts: StateFlow<Map<ReactionKey, UInt>> =
        reactions.map { it.byCount.mapValues { it.value.count } }
            .stateIn(coroutineScope, whileSubscribedWithTimeout, emptyMap())

    override val error: StateFlow<String?> = MutableStateFlow(null)
}

data class MessageUserInteraction(
    val userId: UserId,
    val userInfo: StateFlow<UserInfoElement?>,
    val reactions: Set<ReactionKey>,
    val hasRead: Boolean,
)

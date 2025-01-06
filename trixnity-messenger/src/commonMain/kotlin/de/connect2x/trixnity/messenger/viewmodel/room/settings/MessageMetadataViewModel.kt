package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionKey
import de.connect2x.trixnity.messenger.viewmodel.util.getMessageEditHistory
import de.connect2x.trixnity.messenger.viewmodel.util.getMessageReadReceipts
import de.connect2x.trixnity.messenger.viewmodel.util.getMessageUserReactions
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.sender
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
    ): MessageMetadataViewModel = MessageMetadataViewModelImpl(
        viewModelContext,
        eventId,
        roomId,
        onBack,
    )

    companion object : MessageMetadataViewModelFactory
}

interface MessageMetadataViewModel {
//    val sender: StateFlow<UserInfoElement?>

    //    val message: TimelineElementViewModel.Message<*>
//    val message: TimelineElementHolderViewModel
    val eventId: EventId

    val readers: StateFlow<List<UserInfoElement>?>

    //    val reactors: StateFlow<Map<String, List<UserInfoElement>>?>
//    val edits: StateFlow<List<TimelineElementViewModel.Message<*>>>
    val edits: StateFlow<List<TimelineEvent>>
    val reactions: StateFlow<Map<UserInfoElement, Set<ReactionKey>>>
    val error: StateFlow<String?>
//    val errorCause: StateFlow<String?>

    //    fun errorDismiss()
    fun back()

    // TODO: add functions to initiate edit/reply/delete/other moderation options?
}

class MessageMetadataViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
//    override val message: TimelineElementViewModel.Message<*>,
//    override val message: TimelineElementHolderViewModel,
    override val eventId: EventId,
    roomId: RoomId,
    private val onBack: () -> Unit,
) : MessageMetadataViewModel, MatrixClientViewModelContext by viewModelContext {
    private val config = get<MatrixMessengerConfiguration>()
    private val initials = get<Initials>()

    private val backCallback = BackCallback {
        onBack()
    }

    init {
        backHandler.register(backCallback)
    }

    //    override val sender: StateFlow<UserInfoElement?> = message.sender
//    override val readers: StateFlow<List<UserInfoElement>?> = message.isReadBy
//    override val reactors: StateFlow<Map<String, List<UserInfoElement>>?> =
//        message.reactions.map { reactions ->
//            reactions.mapValues { (_, value) -> value.map { it.sender } }
//        }
//            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
//
    override val edits: StateFlow<List<TimelineEvent>> =
        getMessageEditHistory(matrixClient, eventId, roomId)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())

//        flow<List<TimelineElementViewModel.Message<*>>> {
////            emit(null)
////            matrixClient
////            message.eventId
//            matrixClient.room.getTimelineEventRelations(roomId, message.eventId, RelationType.Replace).map {
//                it?.mapValues { event ->
////                    event.value
//                }
//            }
//
//

    @OptIn(ExperimentalCoroutinesApi::class)
    override val readers: StateFlow<List<UserInfoElement>?> =
        matrixClient.room.getTimelineEvent(roomId, eventId) {
            fetchSize = 1
            allowReplaceContent = false
        }
            .map { it?.sender }
            .filterNotNull()
            .flatMapLatest { senderUserId ->
                getMessageReadReceipts(matrixClient, senderUserId, roomId, eventId).map { users ->
                    when {
                        users.isEmpty() -> null
                        else -> users.map {
                            it.toUserInfoElement(
                                coroutineScope,
                                matrixClient,
                                initials,
                                config.avatarMaxSize,
                                it.userId,
                            )
                        }
                    }
                }
            }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val reactions: StateFlow<Map<UserInfoElement, Set<ReactionKey>>> =
        getMessageUserReactions(matrixClient, roomId, eventId)
            .map {
                it.mapKeys {
                    it.key.toUserInfoElement(
                        coroutineScope,
                        matrixClient,
                        initials,
                        config.avatarMaxSize,
                        it.key.userId,
                    )
                }
            }
            .stateIn(coroutineScope, whileSubscribedWithTimeout, mapOf())

    override val error: StateFlow<String?> = MutableStateFlow(null)
//        get() = TODO("Not yet implemented")

    override fun back() {
        onBack()
    }

}

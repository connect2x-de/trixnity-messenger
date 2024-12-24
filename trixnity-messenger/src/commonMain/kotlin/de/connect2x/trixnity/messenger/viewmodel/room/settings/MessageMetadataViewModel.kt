package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.messageEdits
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId


private val log = KotlinLogging.logger {}

interface MessageMetadataViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        eventId: EventId,
        roomId: RoomId,
        onBack: () -> Unit,
    ): MessageMetadataViewModel {
        return MessageMetadataViewModelImpl(
            viewModelContext,
            eventId,
            roomId,
            onBack,
        )
    }

    companion object : MessageMetadataViewModelFactory
}

interface MessageMetadataViewModel {
//    val sender: StateFlow<UserInfoElement?>

    //    val message: TimelineElementViewModel.Message<*>
//    val message: TimelineElementHolderViewModel
    val eventId: EventId

    //    val readers: StateFlow<List<UserInfoElement>?>
//    val reactors: StateFlow<Map<String, List<UserInfoElement>>?>
//    val edits: StateFlow<List<TimelineElementViewModel.Message<*>>>
    val edits: StateFlow<List<TimelineEvent>>
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
    private val roomId: RoomId,
    private val onBack: () -> Unit,
) : MessageMetadataViewModel, MatrixClientViewModelContext by viewModelContext {

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
        messageEdits(matrixClient, eventId, roomId)
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

    override val error: StateFlow<String?>
        get() = TODO("Not yet implemented")

    override fun back() {
        onBack()
    }

}

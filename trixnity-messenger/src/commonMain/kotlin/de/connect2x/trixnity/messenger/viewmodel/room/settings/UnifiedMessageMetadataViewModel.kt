package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.room
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.RelationType


private val log = KotlinLogging.logger {}

interface UnifiedMessageMetadataViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
//        message: TimelineElementViewModel.Message<*>,
        message: TimelineElementHolderViewModel,
        roomId: RoomId,
//        addMembersToRoomViewModel: PotentialMembersViewModel,
        onBack: () -> Unit,
    ): UnifiedMessageMetadataViewModel {
        return UnifiedMessageMetadataViewModelImpl(
            viewModelContext,
            message,
            roomId,
//            addMembersToRoomViewModel,
            onBack,
        )
    }

    companion object : UnifiedMessageMetadataViewModelFactory
}

interface UnifiedMessageMetadataViewModel {
    val sender: StateFlow<UserInfoElement?>

    //    val message: TimelineElementViewModel.Message<*>
    val message: TimelineElementHolderViewModel
    val readers: StateFlow<List<UserInfoElement>?>
    val reactors: StateFlow<Map<String, List<UserInfoElement>>?>
    val edits: StateFlow<List<TimelineElementViewModel.Message<*>>>
    val error: StateFlow<String?>
//    val errorCause: StateFlow<String?>

    //    fun errorDismiss()
    fun back()

    // TODO: add functions to initiate edit/reply/delete/other moderation options?
}

class UnifiedMessageMetadataViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
//    override val message: TimelineElementViewModel.Message<*>,
    override val message: TimelineElementHolderViewModel,
    private val roomId: RoomId,
    private val onBack: () -> Unit,
) : UnifiedMessageMetadataViewModel, MatrixClientViewModelContext by viewModelContext {

    private val backCallback = BackCallback {
        onBack()
    }

    init {
        backHandler.register(backCallback)
    }

    override val sender: StateFlow<UserInfoElement?> = message.sender
    override val readers: StateFlow<List<UserInfoElement>?> = message.isReadBy
    override val reactors: StateFlow<Map<String, List<UserInfoElement>>?> =
        message.reactions.map { reactions ->
            reactions.mapValues { (_, value) -> value.map { it.sender } }
        }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val edits: StateFlow<List<TimelineElementViewModel.Message<*>>> =
        flow<List<TimelineElementViewModel.Message<*>>> {
//            emit(null)
//            matrixClient
//            message.eventId
            matrixClient.room.getTimelineEventRelations(roomId, message.eventId, RelationType.Replace).map {
                it?.mapValues { event ->
//                    event.value
                }
            }


        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())

    override val error: StateFlow<String?>
        get() = TODO("Not yet implemented")

    override fun back() {
        onBack()
    }

}

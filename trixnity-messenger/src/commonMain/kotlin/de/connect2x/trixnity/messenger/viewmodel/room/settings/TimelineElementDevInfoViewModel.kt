package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

interface TimelineElementDevInfoViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        eventId: EventId,
        roomId: RoomId,
        timelineElementMetadataViewModel: TimelineElementMetadataViewModel,
        onBack: () -> Unit,
    ): TimelineElementDevInfoViewModel =
        TimelineElementDevInfoViewModelImpl(
            viewModelContext = viewModelContext,
            eventId = eventId,
            roomId = roomId,
            timelineElementMetadataViewModel = timelineElementMetadataViewModel,
            onBack = onBack,
        )

    companion object : TimelineElementDevInfoViewModelFactory
}

interface TimelineElementDevInfoViewModel {
    val eventId: EventId
    val body: StateFlow<String?>
    val formatedBody: StateFlow<String?>
    fun back()
}

class TimelineElementDevInfoViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val eventId: EventId,
    roomId: RoomId,
    private val timelineElementMetadataViewModel: TimelineElementMetadataViewModel,
    private val onBack: () -> Unit,
) : TimelineElementDevInfoViewModel, MatrixClientViewModelContext by viewModelContext {

    override val body: StateFlow<String?> = timelineElementMetadataViewModel.element
        .flatMapLatest {a ->
            a?.element?.map { b ->
                (b as? RoomMessageTimelineElementViewModel)?.body
            }?:flowOf(null)
        }
        .stateIn(coroutineScope, WhileSubscribed(), null)

    override val formatedBody: StateFlow<String?> = timelineElementMetadataViewModel.element
        .flatMapLatest {a ->
            a?.element?.map { b ->
                (b as? RoomMessageTimelineElementViewModel)?.body
            }?:flowOf(null)
        }
        .stateIn(coroutineScope, WhileSubscribed(), null)

    private val backCallback = BackCallback {
        onBack()
    }
    init {
        registerBackCallback(backCallback)
    }
    override fun back() {
        onBack()
    }
}

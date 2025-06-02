package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent

interface EncryptedWaitTimelineElementViewModel : TimelineElementViewModel.Message<EncryptedMessageEventContent>

interface EncryptedWaitTimelineElementViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
    ): EncryptedWaitTimelineElementViewModel? {
        return EncryptedWaitTimelineElementViewModelImpl(viewModelContext)
    }

    companion object : EncryptedWaitTimelineElementViewModelFactory
}

class EncryptedWaitTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
) : EncryptedWaitTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext

package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent

interface EncryptedWaitTimelineElementViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
    ): EncryptedWaitTimelineElementViewModel? {
        return EncryptedWaitTimelineElementViewModelImpl(viewModelContext)
    }

    companion object : EncryptedWaitTimelineElementViewModelFactory
}

interface EncryptedWaitTimelineElementViewModel : TimelineElementViewModel.Message<EncryptedMessageEventContent>

class EncryptedWaitTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
) : MatrixClientViewModelContext by viewModelContext, EncryptedWaitTimelineElementViewModel

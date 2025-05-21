package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext

interface EncryptedWaitTimelineElementViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
    ): MessageTimelineElementViewModel.EncryptedWait? {
        return EncryptedWaitTimelineElementViewModelImpl(viewModelContext)
    }

    companion object : EncryptedWaitTimelineElementViewModelFactory
}

class EncryptedWaitTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
) : MatrixClientViewModelContext by viewModelContext, MessageTimelineElementViewModel.EncryptedWait

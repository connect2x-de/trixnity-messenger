package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import net.folivo.trixnity.client.store.TimelineEvent.TimelineEventContentError
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent
import org.koin.core.component.get

interface EncryptedErrorTimelineElementViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        error: Throwable,
    ): EncryptedErrorTimelineElementViewModel? {
        return EncryptedErrorTimelineElementViewModelImpl(
            viewModelContext,
            error,
        )
    }

    companion object : EncryptedErrorTimelineElementViewModelFactory
}

interface EncryptedErrorTimelineElementViewModel : TimelineElementViewModel.Message<EncryptedMessageEventContent>

class EncryptedErrorTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    error: Throwable,
) : EncryptedErrorTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    private val i18n = get<I18n>()
    val error = when (error) {
        TimelineEventContentError.DecryptionAlgorithmNotSupported -> i18n.timelineElementDecryptionErrorAlgorithmNotSupported()
        is TimelineEventContentError.DecryptionError -> i18n.timelineElementDecryptionErrorGeneric(error.error.message)
        TimelineEventContentError.DecryptionTimeout -> i18n.timelineElementDecryptionErrorTimeout()
        TimelineEventContentError.NoContent -> i18n.timelineElementDecryptionErrorNoContent()
        else -> i18n.timelineElementDecryptionErrorGeneric(error.message)
    }
}

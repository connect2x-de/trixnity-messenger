package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.store.TimelineEvent

private val log = KotlinLogging.logger { }

interface EncryptedMessageViewModelFactory {
    fun newEncryptedMessageViewModel(
        viewModelContext: MatrixClientViewModelContext,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: StateFlow<Boolean>,
        sender: StateFlow<String>,
        invitation: Flow<String?>,
        timelineEventFlow: Flow<TimelineEvent?>,
    ): EncryptedMessageViewModel {
        return EncryptedMessageViewModelImpl(
            viewModelContext,
            formattedDate,
            showDateAbove,
            formattedTime,
            isByMe,
            showChatBubbleEdge,
            showBigGap,
            showSender,
            sender,
            invitation,
            timelineEventFlow
        )
    }
}

interface EncryptedMessageViewModel : RoomMessageViewModel {
    val waitForDecryption: StateFlow<Boolean>
}

open class EncryptedMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    override val formattedTime: String?,
    override val isByMe: Boolean,
    override val showChatBubbleEdge: Boolean,
    override val showBigGap: Boolean,
    override val showSender: StateFlow<Boolean>,
    override val sender: StateFlow<String>,
    override val invitation: Flow<String?>,
    timelineEventFlow: Flow<TimelineEvent?>,
) : MatrixClientViewModelContext by viewModelContext, EncryptedMessageViewModel {

    override val waitForDecryption: StateFlow<Boolean> =
        timelineEventFlow
            .onEach { timelineEvent ->
                timelineEvent?.content?.onFailure { exception ->
                    log.error(exception) { "Cannot decrypt event (${timelineEvent.eventId})." }
                }
            }
            .map { it?.content == null }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)
}
package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId

private val log = KotlinLogging.logger { }

interface EncryptedMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: Flow<Boolean>,
        sender: Flow<UserInfoElement>,
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

    companion object : EncryptedMessageViewModelFactory
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
    showSender: Flow<Boolean>,
    sender: Flow<UserInfoElement>,
    invitation: Flow<String?>,
    timelineEventFlow: Flow<TimelineEvent?>,
) : MatrixClientViewModelContext by viewModelContext, EncryptedMessageViewModel {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement(""))
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

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
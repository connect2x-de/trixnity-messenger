package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.unsigned
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.UnknownEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.originTimestampOrNull
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface RedactedMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: RedactedEventContent,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: Flow<Boolean>,
        sender: Flow<UserInfoElement>,
        invitation: Flow<String?>,
        selectedRoomId: RoomId,
        redactedBy: UserId?
    ): RedactedMessageViewModel {
        return RedactedMessageViewModelImpl(
            viewModelContext,
            timelineEvent,
            content,
            formattedDate,
            showDateAbove,
            formattedTime,
            isByMe,
            showChatBubbleEdge,
            showBigGap,
            selectedRoomId,
            showSender,
            sender,
            invitation,
            redactedBy
        )
    }

    companion object : RedactedMessageViewModelFactory
}

interface RedactedMessageViewModel : RoomMessageViewModel {
    val formattedMessage: StateFlow<String>
    val redactedAtDateTime: String?
}

open class RedactedMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    content: RedactedEventContent,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    override val formattedTime: String?,
    override val isByMe: Boolean,
    override val showChatBubbleEdge: Boolean,
    override val showBigGap: Boolean,
    selectedRoomId: RoomId,
    showSender: Flow<Boolean>,
    sender: Flow<UserInfoElement>,
    invitation: Flow<String?>,
    redactedBy: UserId?,
) : RedactedMessageViewModel, MatrixClientViewModelContext by viewModelContext {
    private val timeZone = get<TimeZone>()

    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement("", UserId("")))
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    override val formattedMessage = when (redactedBy) {
            null -> MutableStateFlow(i18n.eventMessageRedactedByUnknown())
            matrixClient.userId -> MutableStateFlow(i18n.eventMessageRedactedByMe())
            else -> matrixClient.user.getById(selectedRoomId, redactedBy).map {
                i18n.eventMessageRedacted(it?.name ?: redactedBy.full)
            }.stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(),
                i18n.eventMessageRedacted(i18n.commonUnknown())
            )
        }


    override val redactedAtDateTime: String? =
        timelineEvent?.unsigned?.redactedBecause?.originTimestampOrNull?.let {
            val localDateTime = Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone)
            "${formatDate(localDateTime)}, ${formatTime(localDateTime)}"
        }

}

class PreviewRedactedMessageViewModel() : RedactedMessageViewModel {
    override val formattedMessage: StateFlow<String> = MutableStateFlow("deleted by Martin")
    override val isByMe: Boolean = false
    override val showChatBubbleEdge: Boolean = false
    override val showBigGap: Boolean = false
    override val showSender: StateFlow<Boolean> = MutableStateFlow(true)
    override val sender: StateFlow<UserInfoElement> = MutableStateFlow(UserInfoElement("Martin", UserId("martin:matrix.org")))
    override val formattedTime: String? = null
    override val invitation: StateFlow<String?> = MutableStateFlow(null)
    override val formattedDate: String = "23.12.21"
    override val showDateAbove: Boolean = false
    override val redactedAtDateTime: String = "25.12.21, 13:18"
}

package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.RichRepliesComputations
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.isReplacing
import net.folivo.trixnity.client.store.unsigned
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent.MegolmEncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.Location
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.Unknown
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.VerificationRequest
import net.folivo.trixnity.core.model.events.m.room.TopicEventContent
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import org.koin.core.component.get


private val log = KotlinLogging.logger { }

interface TimelineEventSubViewmodelFactory {
    fun createEventSubViewmodel(
        viewModelContext: MatrixClientViewModelContext,
        timelineEventFlow: Flow<TimelineEvent?>,
        selectedRoomId: RoomId,
        content: RoomEventContent,
        previousRoomEvent: TimelineEvent?,
        sender: Flow<UserInfoElement>,
        invitation: Flow<String?>,
        isDirect: StateFlow<Boolean>,
        onOpenModal: OpenModalCallback,
        onOpenMention: OpenMentionCallback,
    ): Flow<BaseTimelineElementViewModel>
}

class DefaultTimelineEventSubViewmodelFactory : TimelineEventSubViewmodelFactory {

    override fun createEventSubViewmodel(
        viewModelContext: MatrixClientViewModelContext,
        timelineEventFlow: Flow<TimelineEvent?>,
        selectedRoomId: RoomId,
        content: RoomEventContent,
        previousRoomEvent: TimelineEvent?,
        sender: Flow<UserInfoElement>,
        invitation: Flow<String?>,
        isDirect: StateFlow<Boolean>,
        onOpenModal: OpenModalCallback,
        onOpenMention: OpenMentionCallback,
    ): Flow<BaseTimelineElementViewModel> = timelineEventFlow.filterNotNull().map { timelineEvent ->
        val event = timelineEvent.event
        val isByMe = TimelineElementHolderViewModelHelper.isByMe(viewModelContext, timelineEvent)
        val (isPreviousBySomeoneElse, isPreviousOfOtherDay) = TimelineElementHolderViewModelHelper
            .isPreviousBySomeoneElseOrOtherDay(previousRoomEvent, event)
        val showChatBubbleEdge = isPreviousBySomeoneElse || isPreviousOfOtherDay
        val showSender = isDirect.map {
            it.not() && isByMe.not() && showChatBubbleEdge
            // we can safely stateIn here since viewModels are cached
        }
        val showDateAbove: Boolean = isPreviousOfOtherDay
        val receivedDateTime = TimelineElementHolderViewModelHelper.localDateTimeOf(event)
        if (timelineEvent.isReplacing) return@map TimelineElementHolderViewModelHelper
            .createNullTimelineElementViewModel(viewModelContext, invitation)
        else when (content) {
            is RoomMessageEventContent -> createRoomMessageEventSubViewmodel(
                viewModelContext,
                content,
                event,
                timelineEvent,
                selectedRoomId,
                sender,
                showSender,
                receivedDateTime,
                showDateAbove,
                isByMe,
                showChatBubbleEdge,
                invitation,
                onOpenMention,
                onOpenModal,
            )

            is RedactedEventContent -> {
                log.trace { "Create redacted text message view model: ${event.id}" }
                val redactedBy = timelineEvent.unsigned?.redactedBecause?.sender

                viewModelContext.get<RedactedMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    sender = sender,
                    showSender = MutableStateFlow(false),
                    formattedTime = formatTime(receivedDateTime),
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    isByMe = isByMe,
                    selectedRoomId = selectedRoomId,
                    showChatBubbleEdge = showChatBubbleEdge,
                    showBigGap = showChatBubbleEdge,
                    invitation = invitation,
                    redactedBy = redactedBy,
                )
            }

            is AvatarEventContent -> {
                log.trace { "Create avatar change status view model: ${event.id}" }
                viewModelContext.get<RoomAvatarChangeStatusViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    invitation = invitation,
                    sender = sender,
                    isDirectFlow = isDirect,
                )
            }

            is HistoryVisibilityEventContent -> {
                log.trace { "Create history visibility change status view model: ${event.id}" }
                viewModelContext.get<HistoryVisibilityChangeStatusViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    invitation = invitation,
                    sender = sender,
                    isDirectFlow = isDirect,
                )
            }

            is MegolmEncryptedMessageEventContent -> {
                log.trace { "Create encrypted message view model: ${event.id}" }
                viewModelContext.get<EncryptedMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEventFlow = timelineEventFlow,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    formattedTime = formatTime(receivedDateTime),
                    isByMe = isByMe,
                    showChatBubbleEdge = showChatBubbleEdge,
                    showBigGap = showChatBubbleEdge,
                    showSender = showSender,
                    sender = sender,
                    invitation = invitation,
                )
            }

            is MemberEventContent -> {
                log.trace { "Create member status view model: ${event.id}" }
                viewModelContext.get<MemberStatusViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEventFlow = timelineEventFlow,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    invitation = invitation,
                    sender = sender,
                    isDirectFlow = isDirect,
                )
            }

            is CreateEventContent -> {
                log.trace { "Create room created status view model: ${event.id}" }
                viewModelContext.get<RoomCreatedStatusViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    invitation = invitation,
                    sender = sender,
                    isDirectFlow = isDirect,
                )
            }

            is EncryptionEventContent -> {
                log.trace { "Create room encryption view model: ${event.id}" }
                viewModelContext.get<RoomEncryptionEnabledViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    invitation = invitation,
                    sender = sender,
                    isDirectFlow = isDirect
                )
            }

            is NameEventContent -> {
                log.trace { "Create room name change status view model: ${event.id}" }
                viewModelContext.get<RoomNameChangeStatusViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    invitation = invitation,
                    sender = sender,
                    isDirectFlow = isDirect,
                )
            }

            is TopicEventContent -> {
                log.trace { "Create room topic change status view model: ${event.id}" }
                viewModelContext.get<RoomTopicChangeStatusViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    invitation = invitation,
                    sender = sender,
                    isDirectFlow = isDirect,
                )
            }

            else -> {
                log.error { "Unable to resolve sub viewmodel for event: ${event.id}" }
                TimelineElementHolderViewModelHelper.createNullTimelineElementViewModel(viewModelContext, invitation)
            }
        }
    }

    fun createRoomMessageEventSubViewmodel(
        viewModelContext: MatrixClientViewModelContext,
        content: RoomMessageEventContent,
        event: ClientEvent.RoomEvent<*>,
        timelineEvent: TimelineEvent,
        selectedRoomId: RoomId,
        sender: Flow<UserInfoElement>,
        showSender: Flow<Boolean>,
        receivedDateTime: LocalDateTime,
        showDateAbove: Boolean,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        invitation: Flow<String?>,
        onOpenMention: OpenMentionCallback,
        onOpenModal: OpenModalCallback,
    ): TimelineElementWithTimestampViewModel {
        val richRepliesComputations = viewModelContext.get<RichRepliesComputations>()
        return when (content) {
            is TextBased.Notice -> {
                log.trace { "Create notice message view model: ${event.id}" }
                viewModelContext.get<NoticeMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    fallbackMessage = content.body,
                    referencedMessage = richRepliesComputations.getReferencedMessage(
                        viewModelContext.matrixClient,
                        content.relatesTo,
                        selectedRoomId
                    ),
                    message = content.bodyWithoutFallback,
                    formattedBody = content.formattedBody,
                    sender = sender,
                    showSender = showSender,
                    formattedTime = formatTime(receivedDateTime),
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    isByMe = isByMe,
                    showChatBubbleEdge = showChatBubbleEdge,
                    showBigGap = showChatBubbleEdge,
                    invitation = invitation,
                    roomId = selectedRoomId,
                    onOpenMention = onOpenMention,
                )
            }

            is TextBased.Emote -> {
                log.trace { "Create emote message view model: ${event.id}" }
                viewModelContext.get<EmoteMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    fallbackMessage = content.body,
                    referencedMessage = richRepliesComputations.getReferencedMessage(
                        viewModelContext.matrixClient,
                        content.relatesTo,
                        selectedRoomId
                    ),
                    message = content.bodyWithoutFallback,
                    formattedBody = content.formattedBody,
                    sender = sender,
                    showSender = showSender,
                    formattedTime = formatTime(receivedDateTime),
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    isByMe = isByMe,
                    showChatBubbleEdge = showChatBubbleEdge,
                    showBigGap = showChatBubbleEdge,
                    invitation = invitation,
                    roomId = selectedRoomId,
                    onOpenMention = onOpenMention,
                )
            }


            is TextBased.Text -> {
                log.trace { "Create text message view model: ${event.id}" }
                viewModelContext.get<TextMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    fallbackMessage = content.body,
                    referencedMessage = richRepliesComputations.getReferencedMessage(
                        viewModelContext.matrixClient,
                        content.relatesTo,
                        selectedRoomId
                    ),
                    message = content.bodyWithoutFallback,
                    formattedBody = content.formattedBody,
                    sender = sender,
                    showSender = showSender,
                    formattedTime = formatTime(receivedDateTime),
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    isByMe = isByMe,
                    showChatBubbleEdge = showChatBubbleEdge,
                    showBigGap = showChatBubbleEdge,
                    invitation = invitation,
                    roomId = selectedRoomId,
                    onOpenMention = onOpenMention,
                )
            }

            is FileBased.Image -> {
                log.trace { "Create image message view model: ${event.id}" }
                viewModelContext.get<ImageMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    formattedTime = formatTime(receivedDateTime),
                    isByMe = isByMe,
                    showChatBubbleEdge = showChatBubbleEdge,
                    showBigGap = showChatBubbleEdge,
                    showSender = showSender,
                    sender = sender,
                    invitation = invitation,
                    onOpenModal = onOpenModal,
                    mediaUploadProgress = MutableStateFlow(null),
                )
            }

            is FileBased.Video -> {
                log.trace { "Create video message view model: ${event.id}" }
                viewModelContext.get<VideoMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    formattedTime = formatTime(receivedDateTime),
                    isByMe = isByMe,
                    showChatBubbleEdge = showChatBubbleEdge,
                    showBigGap = showChatBubbleEdge,
                    showSender = showSender,
                    sender = sender,
                    invitation = invitation,
                    onOpenModal = onOpenModal,
                )
            }

            is FileBased.Audio -> {
                log.trace { "Create audio message view model: ${event.id}" }
                viewModelContext.get<AudioMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    sender = sender,
                    showSender = showSender,
                    formattedTime = formatTime(receivedDateTime),
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    isByMe = isByMe,
                    showChatBubbleEdge = showChatBubbleEdge,
                    showBigGap = showChatBubbleEdge,
                    invitation = invitation,
                    onOpenModal = onOpenModal,
                )
            }

            is FileBased.File -> {
                log.trace { "Create file message view model: ${event.id}" }
                viewModelContext.get<FileMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    formattedTime = formatTime(receivedDateTime),
                    isByMe = isByMe,
                    showChatBubbleEdge = showChatBubbleEdge,
                    showBigGap = showChatBubbleEdge,
                    showSender = showSender,
                    sender = sender,
                    invitation = invitation,
                )
            }

            is Location -> {
                log.trace { "Create location message view model: ${event.id}" }
                viewModelContext.get<LocationMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    formattedTime = formatTime(receivedDateTime),
                    isByMe = isByMe,
                    showChatBubbleEdge = showChatBubbleEdge,
                    showBigGap = showChatBubbleEdge,
                    showSender = showSender,
                    sender = sender,
                    invitation = invitation,
                )
            }

            is VerificationRequest -> {
                log.trace { "Create user verification view model: ${event.id}" }
                viewModelContext.get<UserVerificationViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    invitation = invitation,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    formattedTime = formatTime(receivedDateTime),
                    userInfoFlow = sender,
                    selectedRoomId = selectedRoomId,
                    timelineEventId = timelineEvent.eventId,
                )
            }

            is Unknown -> {
                log.warn { "created fallback view model: ${event.id}" }
                viewModelContext.get<FallbackMessageViewModelFactory>().create(
                    viewModelContext = viewModelContext,
                    timelineEvent = timelineEvent,
                    content = content,
                    fallbackMessage = content.body,
                    referencedMessage = richRepliesComputations.getReferencedMessage(
                        viewModelContext.matrixClient,
                        content.relatesTo,
                        selectedRoomId
                    ),
                    message = content.bodyWithoutFallback,
                    formattedBody = content.formattedBody,
                    sender = sender,
                    showSender = showSender,
                    formattedTime = formatTime(receivedDateTime),
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    isByMe = isByMe,
                    showChatBubbleEdge = showChatBubbleEdge,
                    showBigGap = showChatBubbleEdge,
                    invitation = invitation,
                    roomId = selectedRoomId,
                    onOpenMention = onOpenMention,
                )
            }
        }
    }
}

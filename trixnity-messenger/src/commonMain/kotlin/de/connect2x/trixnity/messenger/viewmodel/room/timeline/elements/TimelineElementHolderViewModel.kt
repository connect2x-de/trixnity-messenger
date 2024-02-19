package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.RichRepliesComputations
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import de.connect2x.trixnity.messenger.viewmodel.util.isDifferentDay
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.isReplaced
import net.folivo.trixnity.client.store.isReplacing
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent.MegolmEncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.*
import net.folivo.trixnity.core.model.events.m.room.bodyWithoutFallback
import net.folivo.trixnity.core.model.events.m.room.getFormattedBody
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

interface TimelineElementHolderViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        key: String,
        timelineEventFlow: Flow<TimelineEvent?>,
        selectedRoomId: RoomId,
        eventId: EventId,
        canLoadMoreBefore: Flow<Boolean>,
        canLoadMoreAfter: Flow<Boolean>,
        isDirect: StateFlow<Boolean>,
        isReadFlow: Flow<Boolean>,
        shouldShowUnreadMarkerFlow: Flow<Boolean>,
        readBy: Flow<List<String>>,
        onMessageEdited: (EventId) -> Unit,
        onMessageRepliedTo: (EventId) -> Unit,
        onMessageReportTo: (EventId) -> Unit,
        onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
    ): TimelineElementHolderViewModel {
        return TimelineElementHolderViewModelImpl(
            viewModelContext,
            key,
            timelineEventFlow,
            selectedRoomId,
            eventId,
            canLoadMoreBefore,
            canLoadMoreAfter,
            isDirect,
            isReadFlow,
            readBy,
            shouldShowUnreadMarkerFlow,
            onMessageEdited,
            onMessageRepliedTo,
            onOpenModal = onOpenModal,
            onMessageReportTo = onMessageReportTo
        )
    }

    companion object : TimelineElementHolderViewModelFactory
}

interface TimelineElementHolderViewModel : BaseTimelineElementHolderViewModel {
    val eventId: EventId

    val shouldShowUnreadMarkerFlow: StateFlow<Boolean>
    val showLoadingIndicatorBefore: StateFlow<Boolean>
    val showLoadingIndicatorAfter: StateFlow<Boolean>

    val isDirect: StateFlow<Boolean>
    val isRead: StateFlow<Boolean>

    val isReplaced: StateFlow<Boolean>
    val canBeEdited: StateFlow<Boolean>
    val canBeRedacted: StateFlow<Boolean>
    val redactionInProgress: StateFlow<Boolean>
    val redactionError: StateFlow<String?>
    val canBeRepliedTo: StateFlow<Boolean>
    val highlight: StateFlow<Boolean>
    val canBeReported: StateFlow<Boolean>
    val reportToMessageInProgress: MutableSharedFlow<Boolean>

    fun edit()
    fun endEdit()
    fun redact()
    fun replyTo()
    fun endReplyTo()
    fun reportTo()
    fun endReportTo()

    /** returns no Flow! -> for current value, recompute every time needed (Flow computation would be expensive) */
    suspend fun isReadBy(): String
}

@OptIn(ExperimentalCoroutinesApi::class)
open class TimelineElementHolderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val key: String,
    protected val timelineEventFlow: Flow<TimelineEvent?>,
    protected val selectedRoomId: RoomId,
    override val eventId: EventId,
    canLoadMoreBefore: Flow<Boolean>,
    canLoadMoreAfter: Flow<Boolean>,
    override val isDirect: StateFlow<Boolean>,
    isReadFlow: Flow<Boolean>,
    private val readBy: Flow<List<String>>,
    shouldShowUnreadMarkerFlow: Flow<Boolean>,
    private val onMessageEdited: (EventId) -> Unit,
    private val onMessageRepliedTo: (EventId) -> Unit,
    private val onMessageReportTo: (EventId) -> Unit,
    private val onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
) : TimelineElementHolderViewModel, MatrixClientViewModelContext by viewModelContext {
    private val timelineElementRules = get<TimelineElementRules>()
    private val richRepliesComputations = get<RichRepliesComputations>()
    private val initials = get<Initials>()

    override val showLoadingIndicatorBefore =
        canLoadMoreBefore.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val showLoadingIndicatorAfter =
        canLoadMoreAfter.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val shouldShowUnreadMarkerFlow: StateFlow<Boolean> =
        shouldShowUnreadMarkerFlow.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val isRead: StateFlow<Boolean> =
        isReadFlow.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val isReplaced: StateFlow<Boolean> =
        timelineEventFlow.map { it?.isReplaced == true }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    private val _editInProgress = MutableStateFlow(false)
    private val _reportInProgress = MutableStateFlow(false)
    private val _redactionInProgress = MutableStateFlow(false)
    override val redactionInProgress: StateFlow<Boolean> = _redactionInProgress.asStateFlow()
    private val _redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val redactionError: StateFlow<String?> = _redactionError.asStateFlow()
    override val canBeRepliedTo: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<RoomMessageEventContent>(selectedRoomId)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val canBeReported: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<RoomMessageEventContent>(selectedRoomId)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)


    private val _replyToInProgress = MutableStateFlow(false)

    override val reportToMessageInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)

        override val highlight: StateFlow<Boolean> =
        combine(_editInProgress, _replyToInProgress) { editInProgress, replyToInProgress ->
            editInProgress || replyToInProgress
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    // since this is a rather expensive operation do not compute as a flow
    override suspend fun isReadBy(): String {
        val read = readBy.map { it.joinToString(limit = 10) }.first()
        return if (read.isNotBlank()) {
            i18n.timelineElementReadBy(read)
        } else {
            ""
        }
    }

    override val timelineElementViewModel = combine(
        timelineEventFlow.filterNotNull(),
        timelineEventFlow.flatMapLatest {
            it?.let { timelineEvent ->
                findPreviousVisibleTimelineEvent(timelineEvent)
            } ?: flowOf(null)
        },
    ) { timelineEvent, previousTimelineEvent ->
        val sender = timelineEventFlow.flatMapLatest {
            it?.let { timelineEvent ->
                matrixClient.user.getById(selectedRoomId, timelineEvent.event.sender)
                    .map { user ->
                        UserInfoElement(
                            name = user?.name ?: timelineEvent.event.sender.full,
                            initials = user?.name?.let(initials::compute),
                            image = user?.avatarUrl?.let { avatarUrl ->
                                matrixClient.media.getThumbnail(
                                    avatarUrl,
                                    avatarSize().toLong(),
                                    avatarSize().toLong()
                                ).fold(
                                    onSuccess = { it },
                                    onFailure = {
                                        log.error(it) { "Cannot load avatar image for user '${user.name}'." }
                                        null
                                    }
                                )?.toByteArray()
                            }
                        )
                    }
            } ?: flowOf(UserInfoElement(i18n.commonUnknown()))
        }

        val invitation = timelineEventFlow
            .mapNotNull { it ->
                if (it != null && it.previousEventId == null) it else null
            }
            .flatMapLatest { firstTimelineEvent ->
                findInviterId(firstTimelineEvent).flatMapLatest { inviterId ->
                    getInviterDisplayName(inviterId)
                }
            }

        subViewModel(
            timelineEvent = timelineEvent,
            previousRoomEvent = previousTimelineEvent,
            sender = sender,
            invitation = invitation,
        )
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private suspend fun findPreviousVisibleTimelineEvent(timelineEvent: TimelineEvent): Flow<TimelineEvent?>? {
        val previousTimelineEventOrNull = matrixClient.room.getPreviousTimelineEvent(timelineEvent)
        return previousTimelineEventOrNull?.first() //do NOT  wait until the previous event has been decrypted
            ?.let { previousTimelineEvent ->
                if (timelineElementRules.areVisible.any { it.isInstance(previousTimelineEvent.event.content) }) {
                    previousTimelineEventOrNull
                } else {
                    findPreviousVisibleTimelineEvent(previousTimelineEvent)
                }
            }
    }

    private fun findInviterId(
        timelineEvent: TimelineEvent?,
    ): Flow<UserId?> {
        return timelineEvent?.let { te ->
            val event = te.event
            val content = event.content
            if (event is StateEvent &&
                event.stateKey == matrixClient.userId.full &&
                content is MemberEventContent &&
                content.membership == Membership.INVITE
            ) {
                flowOf(event.sender)
            } else {
                matrixClient.room.getNextTimelineEvent(te)
                    ?.flatMapLatest { nextTimelineEvent ->
                        findInviterId(nextTimelineEvent)
                    }
                    ?: flowOf(null)
            }
        } ?: flowOf(null)
    }

    private fun getInviterDisplayName(
        inviterId: UserId?,
    ): Flow<String?> {
        return inviterId?.let {
            matrixClient.user.getById(selectedRoomId, inviterId)
                .map { user -> user?.name ?: inviterId.full }
        }
            ?.map { inviter -> i18n.invitationFrom(inviter) }
            ?: flowOf(null)
    }

    private fun subViewModel(
        timelineEvent: TimelineEvent,
        previousRoomEvent: TimelineEvent?,
        sender: Flow<UserInfoElement>,
        invitation: Flow<String?>,
    ): BaseTimelineElementViewModel {
        val event = timelineEvent.event
        val receivedDateTime = localDateTimeOf(event)
        val isByMe = isByMe(timelineEvent)

        val (isPreviousBySomeoneElse, isPreviousOfOtherDay) = isPreviousBySomeoneElseOrOtherDay(
            previousRoomEvent,
            event,
        )

        val showChatBubbleEdge = isPreviousBySomeoneElse || isPreviousOfOtherDay
        val showSender = isDirect.map {
            it.not() && isByMe.not() && showChatBubbleEdge
            // we can safely stateIn here since viewModels are cached
        }
        val showDateAbove: Boolean = isPreviousOfOtherDay

        val content = timelineEvent.content?.fold(
            onSuccess = { it },
            onFailure = {
                log.error(it) { "cannot decrypt message event" }
                event.content
            }
        ) ?: event.content

        if (timelineEvent.isReplacing) {
            return createNullTimelineElementViewModel(invitation)
        }

        return when (content) {
            is RoomMessageEventContent -> {
                when (content) {
                    is TextBased -> {
                        log.trace { "Create text message view model: ${event.id}" }
                        get<TextMessageViewModelFactory>().create(
                            viewModelContext = this,
                            timelineEvent = timelineEvent,
                            content = content,
                            fallbackMessage = content.body,
                            referencedMessage = richRepliesComputations.getReferencedMessage(
                                matrixClient,
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
                        )
                    }

                    is FileBased.Image -> {
                        log.trace { "Create image message view model: ${event.id}" }
                        get<ImageMessageViewModelFactory>().create(
                            viewModelContext = this,
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
                        get<VideoMessageViewModelFactory>().create(
                            viewModelContext = this,
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
                        get<AudioMessageViewModelFactory>().create(
                            viewModelContext = this,
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
                        get<FileMessageViewModelFactory>().create(
                            viewModelContext = this,
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
                        get<UserVerificationViewModelFactory>().create(
                            viewModelContext = this,
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
                        get<FallbackMessageViewModelFactory>().create(
                            viewModelContext = this,
                            timelineEvent = timelineEvent,
                            content = content,
                            fallbackMessage = content.body,
                            referencedMessage = richRepliesComputations.getReferencedMessage(
                                matrixClient,
                                content.relatesTo,
                                selectedRoomId
                            ),
                            message = content.bodyWithoutFallback,
                            formattedBody = content.getFormattedBody(),
                            sender = sender,
                            showSender = showSender,
                            formattedTime = formatTime(receivedDateTime),
                            formattedDate = formatDate(receivedDateTime),
                            showDateAbove = showDateAbove,
                            isByMe = isByMe,
                            showChatBubbleEdge = showChatBubbleEdge,
                            showBigGap = showChatBubbleEdge,
                            invitation = invitation,
                        )
                    }
                }
            }

            is RedactedEventContent -> {
                log.trace { "Create redacted text message view model: ${event.id}" }
                get<RedactedMessageViewModelFactory>().create(
                    viewModelContext = this,
                    timelineEvent = timelineEvent,
                    content = content,
                    sender = sender,
                    showSender = MutableStateFlow(false),
                    formattedTime = formatTime(receivedDateTime),
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    isByMe = isByMe,
                    showChatBubbleEdge = showChatBubbleEdge,
                    showBigGap = showChatBubbleEdge,
                    invitation = invitation,
                )
            }

            is MegolmEncryptedMessageEventContent -> {
                log.trace { "Create encrypted message view model: ${event.id}" }
                get<EncryptedMessageViewModelFactory>().create(
                    viewModelContext = this,
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
                get<MemberStatusViewModelFactory>().create(
                    viewModelContext = this,
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
                get<RoomCreatedStatusViewModelFactory>().create(
                    viewModelContext = this,
                    timelineEvent = timelineEvent,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    invitation = invitation,
                    sender = sender,
                    isDirectFlow = isDirect,
                )
            }

            is NameEventContent -> {
                log.trace { "Create room name change status view model: ${event.id}" }
                get<RoomNameChangeStatusViewModelFactory>().create(
                    viewModelContext = this,
                    timelineEvent = timelineEvent,
                    content = content,
                    formattedDate = formatDate(receivedDateTime),
                    showDateAbove = showDateAbove,
                    invitation = invitation,
                    sender = sender,
                    isDirectFlow = isDirect,
                )
            }

            else -> createNullTimelineElementViewModel(invitation)
        }
    }

    private fun createNullTimelineElementViewModel(
        invitation: Flow<String?>,
    ) =
        NullTimelineElementViewModel(
            viewModelContext = this,
            formattedDate = "",
            showDateAbove = false,
            invitation = invitation,
        )

    private fun isByMe(timelineEvent: TimelineEvent): Boolean {
        return timelineEvent.event.sender == matrixClient.userId
    }

    private fun localDateTimeOf(event: RoomEvent<*>): LocalDateTime {
        val timestamp = event.originTimestamp
        requireNotNull(timestamp) // should not happen as only RoomEvents and StateEvents are possible
        return Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.of(timezone()))
    }

    private fun isPreviousBySomeoneElseOrOtherDay(
        previousTimelineEvent: TimelineEvent?,
        event: RoomEvent<*>,
    ): Pair<Boolean, Boolean> {
        return previousTimelineEvent?.let {
            val (_, datesAreDifferent) = compareDates(event, previousTimelineEvent)
            Pair(
                event.sender != previousTimelineEvent.event.sender || previousTimelineEvent.event is StateEvent,
                datesAreDifferent,
            )
        } ?: Pair(true, true) // first message is treated like it is different to the previous one
    }

    private fun compareDates(
        event: RoomEvent<*>,
        previousTimelineEvent: TimelineEvent
    ): Pair<LocalDateTime, Boolean> {
        val thisLocalDateTime = localDateTimeOf(event)
        val previousLocalDateTime = localDateTimeOf(previousTimelineEvent.event)
        return Pair(thisLocalDateTime, thisLocalDateTime.isDifferentDay(previousLocalDateTime))
    }

    override val canBeEdited: StateFlow<Boolean> = timelineEventFlow
        .filterNotNull()
        .map {
            it.event.sender == matrixClient.userId && it.content?.getOrNull() is TextBased
        }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override fun edit() {
        _editInProgress.value = true
        coroutineScope.launch {
            timelineEventFlow.first()?.eventId?.let { onMessageEdited(it) }
        }
    }

    override fun endEdit() {
        _editInProgress.value = false
    }

    override val canBeRedacted: StateFlow<Boolean> = channelFlow {
        timelineEventFlow
            .filterNotNull()
            .flatMapLatest { timelineEvent ->
                matrixClient.user.canRedactEvent(
                    timelineEvent.roomId,
                    timelineEvent.eventId
                )
            }
            .collectLatest { send(it) }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override fun redact() {
        if (redactionInProgress.value.not()) {
            coroutineScope.launch {
                timelineEventFlow.first()?.let { timelineEvent ->
                    if (matrixClient.user.canRedactEvent(
                            timelineEvent.roomId,
                            timelineEvent.eventId
                        ).first()
                    ) {
                        launch {
                            _redactionInProgress.value = true
                            _redactionError.value = null
                            matrixClient.api.room.redactEvent(
                                selectedRoomId,
                                timelineEvent.eventId,
                                txnId = uuid4().toString()
                            ).onSuccess {
                                log.debug { "successfully redacted event ${timelineEvent.eventId}" }
                            }.onFailure {
                                log.error(it) { "could not redact event ${timelineEvent.eventId}" }
                                _redactionError.value = i18n.timelineElementRedactError()
                            }.also {
                                _redactionInProgress.value = false
                            }
                        }
                    } else {
                        log.warn { "try to redact timeline event $eventId, but is no room message or it is not by this user" }

                    }
                } ?: log.warn { "try to redact a timeline event, but it is 'null'" }
            }
        } else {
            log.warn { "try to redact timeline event $eventId, but is already marked for redaction" }
        }
    }

    override fun replyTo() {
        _replyToInProgress.value = true
        coroutineScope.launch {
            timelineEventFlow.first()?.event?.let {
                if (it is MessageEvent<*>) onMessageRepliedTo(it.id)
                else log.warn { "Try to reply to non-message event is not allowed." }
            }
        }
    }

    override fun endReplyTo() {
        _replyToInProgress.value = false
    }

    override fun reportTo() {
        log.trace { "Calling reportToMessage function" }
        reportToMessageInProgress.value = true
        coroutineScope.launch {
            timelineEventFlow.first()?.event?.let {
                if (it is MessageEvent<*>) onMessageReportTo(it.id)
                else log.warn { "Try to report to non-message event is not allowed." }
            }
        }
    }

    override fun endReportTo() {
        log.trace { "Calling endReportTo function" }
        reportToMessageInProgress.value = false

    }

    override fun toString(): String {
        return "TimelineElementViewModel(showLoadingIndicator=${showLoadingIndicatorBefore.value}, shouldShowUnreadMarker=${shouldShowUnreadMarkerFlow.value}, isDirect=${isDirect.value})"
    }

}

class PreviewTimelineElementViewModel1 : TimelineElementHolderViewModel {
    override val eventId: EventId = EventId("\$1:localhost")
    override val key: String = eventId.full
    override val timelineElementViewModel: StateFlow<BaseTimelineElementViewModel?> =
        MutableStateFlow(object : TextBasedViewModel {
            override val fallbackMessage: String = "Hello everyone!"
            override val referencedMessage: MutableStateFlow<ReferencedMessage?> = MutableStateFlow(null)
            override val message: String = "Hello everyone!"
            override val formattedBody: String = "Hello <b>everyone!</b>"
            override val isByMe: Boolean = false
            override val showChatBubbleEdge: Boolean = true
            override val showBigGap: Boolean = true
            override val showSender: MutableStateFlow<Boolean> = MutableStateFlow(true)
            override val sender: MutableStateFlow<UserInfoElement> = MutableStateFlow(UserInfoElement("Benedict"))
            override val formattedTime: String = "11:04"
            override val invitation: MutableStateFlow<String?> = MutableStateFlow(null)
            override val formattedDate: String = "23.11.22"
            override val showDateAbove: Boolean = true
        }
        )
    override val shouldShowUnreadMarkerFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorAfter: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isDirect: StateFlow<Boolean> = MutableStateFlow(false)
    override val isRead: StateFlow<Boolean> = MutableStateFlow(false)
    override val isReplaced: StateFlow<Boolean> = MutableStateFlow(false)
    override val canBeEdited: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeRedacted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val canBeRepliedTo: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canBeReported: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val highlight: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reportToMessageInProgress: MutableSharedFlow<Boolean>  = MutableSharedFlow()
    override fun edit() {
    }

    override fun endEdit() {
    }

    override fun redact() {
    }

    override fun replyTo() {
    }

    override fun endReplyTo() {
    }

    override fun reportTo() {
    }

    override fun endReportTo() {
    }

    override suspend fun isReadBy(): String = "Bob, Alice"
}

class PreviewTimelineElementViewModel2 : TimelineElementHolderViewModel {
    override val eventId: EventId = EventId("\$2:localhost")
    override val key: String = eventId.full
    override val timelineElementViewModel: MutableStateFlow<BaseTimelineElementViewModel?> =
        MutableStateFlow(null)
    override val shouldShowUnreadMarkerFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorAfter: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isDirect: StateFlow<Boolean> = MutableStateFlow(false)
    override val isRead: StateFlow<Boolean> = MutableStateFlow(false)
    override val isReplaced: StateFlow<Boolean> = MutableStateFlow(false)
    override val canBeEdited: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeRedacted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val canBeRepliedTo: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canBeReported: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val highlight: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reportToMessageInProgress: MutableSharedFlow<Boolean>  = MutableSharedFlow()
    override fun edit() {
    }

    override fun endEdit() {
    }

    override fun redact() {
    }

    override fun replyTo() {
    }

    override fun endReplyTo() {
    }

    override fun reportTo() {
    }

    override fun endReportTo() {
    }

    override suspend fun isReadBy(): String = "Bob, Alice"

    init {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            delay(3.seconds)
            timelineElementViewModel.value = object : TextBasedViewModel {
                override val fallbackMessage: String = "I have good news."
                override val referencedMessage: MutableStateFlow<ReferencedMessage?> =
                    MutableStateFlow(null)
                override val message: String = "I have good news."
                override val formattedBody: String = "<b>I</b> have good news."
                override val isByMe: Boolean = false
                override val showChatBubbleEdge: Boolean = false
                override val showBigGap: Boolean = false
                override val showSender: MutableStateFlow<Boolean> = MutableStateFlow(false)
                override val sender: MutableStateFlow<UserInfoElement> =
                    MutableStateFlow(UserInfoElement("Benedict"))
                override val formattedTime: String = "11:05"
                override val invitation: MutableStateFlow<String?> = MutableStateFlow(null)
                override val formattedDate: String = "23.11.22"
                override val showDateAbove: Boolean = false
            }
        }
    }
}
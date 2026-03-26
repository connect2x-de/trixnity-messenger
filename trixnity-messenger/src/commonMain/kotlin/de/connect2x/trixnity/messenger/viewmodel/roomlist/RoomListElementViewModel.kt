package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.client.notification
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.client.store.RoomOutboxMessage
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.MarkedUnreadEventContent
import de.connect2x.trixnity.core.model.events.m.Presence
import de.connect2x.trixnity.core.model.events.m.RelatesTo
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationDoneEventContent
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationStep
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.Location
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.Unknown
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.VerificationRequest
import de.connect2x.trixnity.core.model.events.m.room.bodyWithoutFallback
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.LeaveRoom
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomInviter
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.RoomPresence
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.formatTimestamp
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import de.connect2x.trixnity.messenger.viewmodel.util.typingInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.koin.core.component.get
import kotlin.time.Clock
import kotlin.time.Instant

interface RoomListElementViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId,
        onRoomSelected: () -> Unit,
        onCloseRoom: () -> Unit
    ): RoomListElementViewModel = RoomListElementViewModelImpl(viewModelContext, roomId, onRoomSelected, onCloseRoom)

    companion object : RoomListElementViewModelFactory
}

interface RoomListElementViewModel {
    val account: UserId
    val roomId: RoomId
    val isLoaded: StateFlow<Boolean>
    val error: StateFlow<String?>
    val isDirect: StateFlow<Boolean?>
    val isInvite: StateFlow<Boolean?>
    val isLeave: StateFlow<Boolean?>
    val isKnock: StateFlow<Boolean?>
    val inviterUserInfo: StateFlow<UserInfoElement?>
    val isEncrypted: StateFlow<Boolean?>
    val isPublic: StateFlow<Boolean?>
    val roomName: StateFlow<String?>
    val roomImageInitials: StateFlow<String?>
    val roomImage: StateFlow<ByteArray?>
    val lastMessage: StateFlow<String?>
    val usersTyping: StateFlow<String?>
    val time: StateFlow<String?>
    val isUnread: StateFlow<Boolean?>
    val notificationCount: StateFlow<String?>
    val presence: StateFlow<Presence?>
    val accountColor: StateFlow<Long?>
    val rejectInvitationInProgress: StateFlow<Boolean>

    fun unknock()
    fun acceptInvitation()
    fun rejectInvitation()
    fun rejectInvitationAndBlockInviter()
    fun forgetRoom()
    fun clearError()
    fun markUnread()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
open class RoomListElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val roomId: RoomId,
    private val onRoomSelected: () -> Unit,
    private val onRoomClose: () -> Unit
) : MatrixClientViewModelContext by viewModelContext, RoomListElementViewModel {
    private val roomInviter = get<RoomInviter>()
    private val roomPresence = get<RoomPresence>()
    private val roomNameCalculations = get<RoomName>()
    private val initials = get<Initials>()
    private val clock = get<Clock>()
    private val timeZone = get<TimeZone>()
    private val userBlocking = get<UserBlocking>()
    private val leaveRoom = get<LeaveRoom>()

    private val roomFlow = matrixClient.room.getById(roomId).filterNotNull()
        .shareIn(coroutineScope, WhileSubscribed(), 1)

    override val accountColor: StateFlow<Long?> =
        get<MatrixMessengerSettingsHolder>().map {
            if (it.base.accounts.size > 1) {
                it.base.accounts[userId]?.base?.displayColor
            } else null
        }.stateIn(coroutineScope, WhileSubscribed(), null)
    override val account: UserId = matrixClient.userId
    override val error = MutableStateFlow<String?>(null)
    override val isDirect: StateFlow<Boolean?> =
        roomFlow.map { it.isDirect }
            .stateIn(coroutineScope, WhileSubscribed(), null)
    override val isInvite: StateFlow<Boolean?> =
        roomFlow.map { it.membership == Membership.INVITE }
            .stateIn(coroutineScope, WhileSubscribed(), null)
    override val isKnock: StateFlow<Boolean?> =
        roomFlow.map { it.membership == Membership.KNOCK }
            .stateIn(coroutineScope, WhileSubscribed(), null)
    override val isLeave: StateFlow<Boolean?> =
        roomFlow.map { it.membership == Membership.LEAVE }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    private val maxMediaSizeInMemory = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory

    override val inviterUserInfo: StateFlow<UserInfoElement?> =
        combine(isInvite.filterNotNull(), roomFlow) { isInvite, room ->
            if (isInvite) {
                room.roomId
            } else {
                null
            }
        }.flatMapLatest { roomId ->
            if (roomId != null) {
                roomInviter.getInviter(matrixClient, roomId)?.let { inviterUserId ->
                    matrixClient.user.getById(roomId, inviterUserId)
                        .filterNotNull()
                        .map {
                            it.toUserInfoElement(
                                coroutineScope,
                                matrixClient,
                                initials,
                                inviterUserId,
                                maxMediaSizeInMemory,
                            )
                        }
                } ?: flowOf(null)
            } else {
                flowOf(null)
            }
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val isEncrypted: StateFlow<Boolean?> =
        roomFlow.map { it.encrypted }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val isPublic: StateFlow<Boolean?> =
        combine(
            matrixClient.room.getState<HistoryVisibilityEventContent>(roomId).map {
                it?.content?.historyVisibility == HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE
            },
            matrixClient.room.getState<JoinRulesEventContent>(roomId).map {
                it?.content?.joinRule == JoinRulesEventContent.JoinRule.Public
            }
        ) { a, b -> a || b }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val roomName: StateFlow<String?> =
        roomNameCalculations.getRoomName(roomId, matrixClient).map { it }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val roomImageInitials: StateFlow<String?> =
        roomNameCalculations.getRoomName(roomId, matrixClient, formatted = false)
            .map { initials.compute(it) }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val roomImage: StateFlow<ByteArray?> =
        roomFlow.map { room ->
            room.avatarUrl?.let { avatarUrl ->
                matrixClient.media.getThumbnail(avatarUrl, avatarSize().toLong(), avatarSize().toLong())
                    .fold(
                        onSuccess = {
                            it.toByteArray(coroutineScope, maxSize = maxMediaSizeInMemory)
                        },
                        onFailure = {
                            log.error(it) { "Cannot load user avatar." }
                            null
                        }
                    )
            }
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    private val lastRelevantTimelineEventMessage =
        roomFlow.flatMapLatest { room ->
            val lastRelevantEventId = room.lastRelevantEventId
            if (lastRelevantEventId != null)
                matrixClient.room.getTimelineEvent(
                    roomId = roomId,
                    eventId = lastRelevantEventId,
                )
            else flowOf(null)
        }.distinctUntilChanged()
            .flatMapLatest { lastTimelineEvent ->
                if (lastTimelineEvent != null) {
                    combine(
                        matrixClient.user.getById(roomId, lastTimelineEvent.event.sender),
                        matrixClient.room.getById(roomId).map { it?.isDirect == true }
                            .distinctUntilChanged(),
                        matrixClient.room.getDraftMessage(roomId)
                    ) { lastTimelineEventSender, isDirect, draftMessage ->
                        val draftMessageContent = getDraftMessageContent(draftMessage)
                        val message = timelineEventTypeDescription(lastTimelineEvent)
                        val isByMe = matrixClient.userId == lastTimelineEvent.event.sender
                        val sender = if (isByMe) {
                            i18n.roomListYou()
                        } else {
                            lastTimelineEventSender?.name ?: lastTimelineEvent.event.sender.full
                        }
                        if (draftMessageContent != null) "${i18n.roomListDraft()}: $draftMessageContent"
                        else if (isDirect && isByMe.not()) message
                        else "${sender}: $message"
                    }
                } else flowOf("")
            }.shareIn(coroutineScope, WhileSubscribed(), 1)

    override val lastMessage: StateFlow<String?> =
        combine(lastRelevantTimelineEventMessage, isInvite.filterNotNull()) { message, isInvite ->
            if (isInvite) {
                val inviter = roomNameCalculations.getInviterName(roomId, matrixClient)
                i18n.roomListInvitationFrom(inviter)
            } else {
                message
            }
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val usersTyping: StateFlow<String?> = matrixClient.room.usersTyping.map { map ->
        map[roomId]?.let { typingInfo(matrixClient, roomId, i18n, it) }
    }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val time: StateFlow<String?> =
        roomFlow.map { room ->
            room.lastRelevantEventTimestamp?.let {
                formatTimestamp(it, clock, timeZone)
            } ?: matrixClient.room.getState<CreateEventContent>(roomId).first()
                ?.originTimestamp?.let { Instant.fromEpochMilliseconds(it) }
                ?.let { formatTimestamp(it, clock, timeZone) }
            ?: ""
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val isUnread: StateFlow<Boolean?> =
        matrixClient.notification.isUnread(roomId)
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val notificationCount: StateFlow<String?> =
        matrixClient.notification.getCount(roomId).map { count ->
            when {
                count == 0 -> null
                count > 99 -> "99+"
                else -> count.toString()
            }
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val presence: StateFlow<Presence?> =
        roomPresence.invoke(matrixClient, roomId)
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val isLoaded: StateFlow<Boolean> =
        combine(
            roomName,
            isInvite,
            lastMessage
        ) { roomName, isInvite, lastMessage -> roomName != null && isInvite != null && lastMessage != null }
            .stateIn(coroutineScope, WhileSubscribed(), false)

    override val rejectInvitationInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun unknock() {
        coroutineScope.launch {
            if (matrixClient.syncState.value != SyncState.RUNNING) {
                log.debug { "try to reject room invitation while not connected" }
                error.value = i18n.roomListKnockOffline()
                return@launch
            }

            leaveRoom(matrixClient, roomId, forget = false)
                .onSuccess { log.info { "successfully rejected invitation" } }
                .onFailure {
                    log.error(it) { "Cannot reject invitation" }
                    error.value = i18n.roomListKnockError()
                }
        }
    }

    override fun acceptInvitation() {
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                log.debug { "try to join room while not connected" }
                error.value = i18n.roomListInvitationOffline()
            } else {
                log.debug { "try to join room $roomId" }
                matrixClient.api.room.joinRoom(roomId).fold(
                    onSuccess = {
                        onRoomSelected()
                    },
                    onFailure = {
                        log.error(it) { "Cannot join room." }
                        error.value = i18n.roomListInvitationError()
                    }
                )
            }
        }
    }

    override fun rejectInvitation() {
        coroutineScope.launch {
            rejectInvitationInProgress.value = true
            rejectInvitationSuspend(
                onSuccess = { rejectInvitationInProgress.value = false },
                onFailure = { rejectInvitationInProgress.value = false }
            )
        }
    }

    override fun rejectInvitationAndBlockInviter() {
        coroutineScope.launch {
            log.debug { "Rejecting the invitation to $roomId and block inviter" }
            roomInviter.getInviter(matrixClient, roomId)?.let { inviter ->
                log.debug { "Inviter to block: ${inviter.full}" }
                rejectInvitationInProgress.value = true
                userBlocking.blockUser(
                    matrixClient = matrixClient,
                    userToBlock = inviter,
                    onSuccess = {
                        log.debug { "Blocked user $inviter" }
                        rejectInvitationSuspend(
                            onSuccess = { rejectInvitationInProgress.value = false },
                            onFailure = { rejectInvitationInProgress.value = false }
                        )
                    },
                    onFailure = {
                        log.error { "Cannot block user $inviter" }
                        error.value = i18n.blockUserError(inviter.full)
                        rejectInvitationInProgress.value = false
                    }
                )
            }
        }
    }

    override fun forgetRoom() {
        coroutineScope.launch {
            if (matrixClient.syncState.value != SyncState.RUNNING) {
                error.value = i18n.forgetRoomErrorOffline()
                return@launch
            }

            leaveRoom(matrixClient, roomId)
                .onSuccess { log.info { "successfully forgot room" } }
                .onFailure { log.error(it) { "failed to forget room" } }
            onRoomClose()
        }
    }

    override fun clearError() {
        error.value = null
    }

    override fun markUnread() {
        coroutineScope.launch {
            matrixClient.api.room.setAccountData(MarkedUnreadEventContent(true), roomId, userId).fold(
                onSuccess = {
                    log.info { "Marked room $roomId as unread" }
                },
                onFailure = {
                    log.error(it) { "Couldn't mark room $roomId as unread" }
                }
            )
        }
    }

    private suspend fun rejectInvitationSuspend(
        onSuccess: suspend () -> Unit = {},
        onFailure: (Throwable) -> Unit = {}
    ) {
        if (matrixClient.syncState.value == SyncState.ERROR) {
            log.debug { "Try to reject room invitation while not connected" }
            error.value = i18n.roomListInvitationOffline()
            onFailure(IllegalStateException("Sync failed, cannot reject invitation while not connected"))
            return
        }
        leaveRoom(matrixClient, roomId)
            .onSuccess {
                log.info { "Successfully rejected invitation" }
                onSuccess()
            }
            .onFailure {
                log.error(it) { "Failed to reject invitation" }
                onFailure(it)
            }
    }

    private fun getDraftMessageContent(draft: RoomOutboxMessage<*>?): String? {
        return when (draft?.content) {
            is TextBased.Text -> {
                val content = (draft.content as TextBased.Text).body
                val relatesTo = draft.content.relatesTo
                if (relatesTo is RelatesTo.Replace) {
                    (relatesTo.newContent as? TextBased.Text)?.body?.ifEmpty { null }
                } else {
                    content.ifEmpty { null }
                }
            }

            else -> null
        }
    }

    private fun timelineEventTypeDescription(event: TimelineEvent): String =
        event.content?.getOrNull().let { content ->
            when (content) {
                is FileBased.Image -> i18n.roomListContentImage()
                is FileBased.Video -> i18n.roomListContentVideo()
                is FileBased.Audio -> i18n.roomListContentAudio()
                is FileBased.File -> i18n.roomListContentFile()
                is VerificationRequest -> i18n.roomListContentVerificationRequest(content.to.toString())
                is VerificationDoneEventContent -> i18n.roomListContentVerificationCompleted()
                is VerificationCancelEventContent -> i18n.roomListContentVerificationCancelled()
                is VerificationStep -> i18n.roomListContentVerificationInProgress()
                is TextBased,
                is Location,
                is Unknown -> content.bodyWithoutFallback

                else -> ""
            }
        }
}

class PreviewRoomListElementViewModel1 : RoomListElementViewModel {
    @Suppress("DEPRECATION") // TODO: Adjust this for Room version 12 eventually
    private val roomId1 = RoomId("1")
    override val account: UserId = UserId("user", "server")
    override val roomId: RoomId = roomId1
    override val isLoaded: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isDirect: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val isLeave: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isInvite: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isKnock: StateFlow<Boolean?> = MutableStateFlow(false)
    override val inviterUserInfo: StateFlow<UserInfoElement?> = MutableStateFlow(null)
    override val isEncrypted: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val isPublic: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val roomName: MutableStateFlow<String?> = MutableStateFlow("Benedict")
    override val roomImageInitials: MutableStateFlow<String?> = MutableStateFlow("B")
    override val roomImage: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
    override val lastMessage: MutableStateFlow<String?> = MutableStateFlow("Gute Entscheidung!")
    override val usersTyping: MutableStateFlow<String?> = MutableStateFlow(null)
    override val time: MutableStateFlow<String?> = MutableStateFlow("20:46")
    override val isUnread: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val notificationCount: MutableStateFlow<String?> = MutableStateFlow("99+")
    override val presence: MutableStateFlow<Presence?> = MutableStateFlow(Presence.ONLINE)
    override val accountColor: StateFlow<Long?> = MutableStateFlow(null)
    override val rejectInvitationInProgress: StateFlow<Boolean> = MutableStateFlow(false)

    override fun unknock() {}
    override fun acceptInvitation() {}
    override fun rejectInvitation() {}
    override fun forgetRoom() {}
    override fun rejectInvitationAndBlockInviter() {}
    override fun clearError() {}
    override fun markUnread() {}
}

class PreviewRoomListElementViewModel2 : RoomListElementViewModel {
    @Suppress("DEPRECATION") // TODO: Adjust this for Room version 12 eventually
    private val roomId2 = RoomId("2")
    override val account: UserId = UserId("0", "server")
    override val roomId: RoomId = roomId2
    override val isLoaded: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isDirect: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isLeave: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isInvite: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isKnock: StateFlow<Boolean?> = MutableStateFlow(false)
    override val isEncrypted: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val inviterUserInfo: StateFlow<UserInfoElement?> = MutableStateFlow(null)
    override val isPublic: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val roomName: MutableStateFlow<String?> = MutableStateFlow("Allgemein")
    override val roomImageInitials: MutableStateFlow<String?> = MutableStateFlow("A")
    override val roomImage: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
    override val lastMessage: MutableStateFlow<String?> =
        MutableStateFlow("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
    override val usersTyping: MutableStateFlow<String?> = MutableStateFlow("Martin schreibt...")
    override val time: MutableStateFlow<String?> = MutableStateFlow("24.12.19")
    override val isUnread: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val notificationCount: MutableStateFlow<String?> = MutableStateFlow("2")
    override val presence: MutableStateFlow<Presence?> = MutableStateFlow(Presence.ONLINE)
    override val accountColor: StateFlow<Long?> = MutableStateFlow(null)
    override val rejectInvitationInProgress: StateFlow<Boolean> = MutableStateFlow(false)

    override fun unknock() {}
    override fun acceptInvitation() {}
    override fun rejectInvitation() {}
    override fun forgetRoom() {}
    override fun rejectInvitationAndBlockInviter() {}
    override fun clearError() {}
    override fun markUnread() {}
}

class PreviewRoomListElementViewModel3 : RoomListElementViewModel {
    @Suppress("DEPRECATION") // TODO: Adjust this for Room version 12 eventually
    private val roomId3 = RoomId("3")
    override val account: UserId = UserId("1", "server")
    override val roomId: RoomId = roomId3
    override val isLoaded: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isDirect: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val isLeave: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isInvite: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isKnock: StateFlow<Boolean?> = MutableStateFlow(false)
    override val isEncrypted: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val isPublic: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val inviterUserInfo: StateFlow<UserInfoElement?> = MutableStateFlow(null)
    override val roomName: MutableStateFlow<String?> = MutableStateFlow("Martin")
    override val roomImageInitials: MutableStateFlow<String?> = MutableStateFlow("M")
    override val roomImage: MutableStateFlow<ByteArray?> = MutableStateFlow(previewImageByteArray())
    override val lastMessage: MutableStateFlow<String?> =
        MutableStateFlow("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
    override val usersTyping: MutableStateFlow<String?> = MutableStateFlow(null)
    override val time: MutableStateFlow<String?> = MutableStateFlow("12.12.19")
    override val isUnread: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val notificationCount: MutableStateFlow<String?> = MutableStateFlow(null)
    override val presence: MutableStateFlow<Presence?> = MutableStateFlow(Presence.ONLINE)
    override val accountColor: StateFlow<Long?> = MutableStateFlow(null)
    override val rejectInvitationInProgress: StateFlow<Boolean> = MutableStateFlow(false)

    override fun unknock() {}
    override fun acceptInvitation() {}
    override fun rejectInvitation() {}
    override fun forgetRoom() {}
    override fun rejectInvitationAndBlockInviter() {}
    override fun clearError() {}
    override fun markUnread() {}
}

class PreviewRoomListElementViewModel4 : RoomListElementViewModel {
    @Suppress("DEPRECATION") // TODO: Adjust this for Room version 12 eventually
    private val roomId3 = RoomId("4")
    override val account: UserId = UserId("1", "server")
    override val roomId: RoomId = roomId3
    override val isLoaded: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isLeave: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isDirect: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val isInvite: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isKnock: StateFlow<Boolean?> = MutableStateFlow(false)
    override val inviterUserInfo: StateFlow<UserInfoElement?> = MutableStateFlow(null)
    override val isEncrypted: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val isPublic: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val roomName: MutableStateFlow<String?> = MutableStateFlow("Martin")
    override val roomImageInitials: MutableStateFlow<String?> = MutableStateFlow("M")
    override val roomImage: MutableStateFlow<ByteArray?> = MutableStateFlow(previewImageByteArray())
    override val lastMessage: MutableStateFlow<String?> =
        MutableStateFlow("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
    override val usersTyping: MutableStateFlow<String?> = MutableStateFlow(null)
    override val time: MutableStateFlow<String?> = MutableStateFlow("12.12.19")
    override val isUnread: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val notificationCount: MutableStateFlow<String?> = MutableStateFlow(null)
    override val presence: MutableStateFlow<Presence?> = MutableStateFlow(Presence.OFFLINE)
    override val accountColor: StateFlow<Long?> = MutableStateFlow(null)
    override val rejectInvitationInProgress: StateFlow<Boolean> = MutableStateFlow(false)

    override fun unknock() {}
    override fun acceptInvitation() {}
    override fun rejectInvitation() {}
    override fun forgetRoom() {}
    override fun rejectInvitationAndBlockInviter() {}
    override fun clearError() {}
    override fun markUnread() {}
}

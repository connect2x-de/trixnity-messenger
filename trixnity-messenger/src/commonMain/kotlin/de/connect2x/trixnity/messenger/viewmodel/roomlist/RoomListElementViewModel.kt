package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RelevantTimelineEvents
import de.connect2x.trixnity.messenger.viewmodel.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.room.*
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.*
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface RoomListElementViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId,
        onRoomSelected: () -> Unit,
    ): RoomListElementViewModel {
        return RoomListElementViewModelImpl(
            viewModelContext, roomId, onRoomSelected,
        )
    }

    companion object : RoomListElementViewModelFactory
}

interface RoomListElementViewModel {
    val account: UserId
    val roomId: RoomId
    val error: StateFlow<String?>
    val isDirect: StateFlow<Boolean?>
    val isInvite: StateFlow<Boolean?>
    val isEncrypted: StateFlow<Boolean?>
    val isPublic: StateFlow<Boolean?>
    val roomName: StateFlow<String?>
    val roomImageInitials: StateFlow<String?>
    val roomImage: StateFlow<ByteArray?>
    val lastMessage: StateFlow<String?>
    val time: StateFlow<String?>
    val unreadMessages: StateFlow<String?>
    val presence: StateFlow<Presence?>
    val accountColor: StateFlow<Long?>
    fun acceptInvitation()
    fun rejectInvitation()
    fun rejectInvitationAndBlockInviter()
    fun clearError()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
open class RoomListElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val roomId: RoomId,
    private val onRoomSelected: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, RoomListElementViewModel {
    private val roomInviter = get<RoomInviter>()
    private val userPresence = get<UserPresence>()
    private val roomNameCalculations = get<RoomName>()
    private val initials = get<Initials>()
    private val clock = get<Clock>()
    private val relevantTimelineEvents = get<RelevantTimelineEvents>()
    private val userBlocking = get<UserBlocking>()

    private val roomFlow = matrixClient.room.getById(roomId).filterNotNull()
        .shareIn(coroutineScope, WhileSubscribed(), 1)

    override val accountColor: StateFlow<Long?> =
        get<MatrixMessengerSettingsHolder>().map {
            if (it.accounts.size > 1) {
                it.accounts[userId]?.displayColor
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
    override val isEncrypted: StateFlow<Boolean?> =
        roomFlow.map { it.encrypted }
            .stateIn(coroutineScope, WhileSubscribed(), null)
    override val isPublic: StateFlow<Boolean?> =
        matrixClient.room.getState<JoinRulesEventContent>(roomId).map {
            it?.content?.joinRule == JoinRulesEventContent.JoinRule.Public
        }.stateIn(coroutineScope, WhileSubscribed(), null)
    override val roomName: StateFlow<String?> =
        roomNameCalculations.getRoomName(roomId, matrixClient).map { it }
            .stateIn(coroutineScope, WhileSubscribed(), null)
    override val roomImageInitials: StateFlow<String?> =
        roomName.map { it?.let { initials.compute(it) } }
            .stateIn(coroutineScope, WhileSubscribed(), null)
    override val roomImage: StateFlow<ByteArray?> =
        roomFlow.map { room ->
            room.avatarUrl?.let { avatarUrl ->
                matrixClient.media.getThumbnail(avatarUrl, avatarSize().toLong(), avatarSize().toLong())
                    .fold(
                        onSuccess = { it.toByteArray() },
                        onFailure = {
                            log.error(it) { "Cannot load user avatar." }
                            null
                        }
                    )
            }
        }.stateIn(coroutineScope, WhileSubscribed(), null)
    private val lastTimelineEventAndMessage =
        matrixClient.room.getLastTimelineEvent(roomId).flatMapLatest { lastTimelineEventFlow ->
            lastTimelineEventFlow
                ?.flatMapLatest { lastTimelineEvent ->
                    getRelevantLastTimelineEvent(lastTimelineEvent)
                }
                ?.flatMapLatest { lastTimelineEvent ->
                    if (lastTimelineEvent != null) {
                        combine(
                            matrixClient.user.getById(roomId, lastTimelineEvent.event.sender),
                            matrixClient.room.getById(roomId).map { it?.isDirect == true }
                                .distinctUntilChanged(),
                        ) { lastTimelineEventSender, isDirect ->
                            val message = lastTimelineEventType(lastTimelineEvent)
                            val isByMe = matrixClient.userId == lastTimelineEvent.event.sender
                            val sender = if (isByMe) {
                                i18n.roomListYou()
                            } else {
                                lastTimelineEventSender?.name ?: lastTimelineEvent.event.sender.full
                            }
                            if (isDirect && isByMe.not()) Pair(lastTimelineEvent, message)
                            else Pair(lastTimelineEvent, "${sender}: $message")
                        }
                    } else flowOf(Pair(null, ""))
                } ?: flowOf(Pair(null, ""))
        }.shareIn(coroutineScope, WhileSubscribed(), 1)
    override val lastMessage: StateFlow<String?> =
        combine(lastTimelineEventAndMessage, isInvite.filterNotNull()) { (_, message), isInvite ->
            if (isInvite) {
                val inviter = roomNameCalculations.getInviterName(roomId, matrixClient)
                i18n.roomListInvitationFrom(inviter)
            } else {
                message
            }
        }.stateIn(coroutineScope, WhileSubscribed(), null)
    override val time: StateFlow<String?> =
        lastTimelineEventAndMessage.map { (lastTimelineEvent) ->
            lastTimelineEvent?.let {
                formatTimestamp(Instant.fromEpochMilliseconds(it.event.originTimestamp), clock)
            }
                ?: matrixClient.room.getState<CreateEventContent>(roomId).first()
                    ?.originTimestamp?.let { Instant.fromEpochMilliseconds(it) }
                    ?.let { formatTimestamp(it, clock) }
                ?: ""
        }.stateIn(coroutineScope, WhileSubscribed(), null)
    override val unreadMessages: StateFlow<String?> =
        combine(roomFlow, isInvite.filterNotNull()) { room, isInvite ->
            when {
                isInvite -> "1"
                room.unreadMessageCount == 0L -> null
                room.unreadMessageCount > 99 -> "99+"
                else -> room.unreadMessageCount.toString()
            }
        }.stateIn(coroutineScope, WhileSubscribed(), null)
    override val presence: StateFlow<Presence?> =
        userPresence.presentEventContentFlow(matrixClient, roomId)
            .map { it?.presence }
            .stateIn(coroutineScope, WhileSubscribed(), null)

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
            rejectInvitationSuspend()
        }
    }

    override fun rejectInvitationAndBlockInviter() {
        coroutineScope.launch {
            log.debug { "reject the invitation to ${roomId}and block inviter" }
            roomInviter.getInviter(matrixClient, roomId)?.let { inviter ->
                log.debug { "inviter to block: ${inviter.full}" }
                userBlocking.blockUser(matrixClient, inviter, onSuccess = {
                    log.debug { "blocked user $inviter" }
                    rejectInvitationSuspend()
                }) {
                    log.error { "cannot block user $inviter" }
                    error.value = i18n.blockUserError(inviter.full)
                }
            }
        }
    }

    override fun clearError() {
        error.value = null
    }

    private suspend fun rejectInvitationSuspend() {
        if (matrixClient.syncState.value == SyncState.ERROR) {
            log.debug { "try to reject room invitation while not connected" }
            error.value = i18n.roomListInvitationOffline()
        } else {
            matrixClient.api.room.leaveRoom(roomId).fold(
                onSuccess = { log.info { "successfully rejected invitation" } },
                onFailure = {
                    log.error(it) { "Cannot reject invitation" }
                    error.value = i18n.roomListInvitationError()
                }
            )
        }
    }

    private fun lastTimelineEventType(lastTimelineEvent: TimelineEvent): String =
        if (lastTimelineEvent.content?.isSuccess == true) {
            val content = lastTimelineEvent.content?.getOrNull()
            if (content is RoomMessageEventContent)
                when (content) {

                    is FileBased.Image -> i18n.roomListContentImage()
                    is FileBased.Video -> i18n.roomListContentVideo()
                    is FileBased.Audio -> i18n.roomListContentAudio()
                    is FileBased.File -> i18n.roomListContentFile()
                    is TextBased,
                    is VerificationRequest,
                    is Unknown -> content.bodyWithoutFallback
                }
            else ""
        } else ""

    private fun getRelevantLastTimelineEvent(
        timelineEvent: TimelineEvent?,
        eventsToSearch: Int = 100,
    ): Flow<TimelineEvent?> {
        return if (relevantTimelineEvents.isRelevantTimelineEvent(timelineEvent) && timelineEvent?.event !is StateEvent)
            return flowOf(timelineEvent)
        else {
            if (eventsToSearch > 0) {
                timelineEvent?.let { matrixClient.room.getPreviousTimelineEvent(it) }
                    ?.flatMapLatest { getRelevantLastTimelineEvent(it, eventsToSearch - 1) }
                    ?: flowOf(null)
            } else flowOf(null)
        }
    }

}


class PreviewRoomListElementViewModel1 : RoomListElementViewModel {
    private val roomId1 = RoomId("1", "localhost")
    override val account: UserId = UserId("user", "server")
    override val roomId: RoomId = roomId1
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isDirect: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val isInvite: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isEncrypted: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val isPublic: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val roomName: MutableStateFlow<String?> = MutableStateFlow("Benedict")
    override val roomImageInitials: MutableStateFlow<String?> = MutableStateFlow("B")
    override val roomImage: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
    override val lastMessage: MutableStateFlow<String?> = MutableStateFlow("Gute Entscheidung!")
    override val time: MutableStateFlow<String?> = MutableStateFlow("20:46")
    override val unreadMessages: MutableStateFlow<String?> = MutableStateFlow("99+")
    override val presence: MutableStateFlow<Presence?> = MutableStateFlow(Presence.ONLINE)
    override val accountColor: StateFlow<Long?> = MutableStateFlow(null)
    override fun acceptInvitation() {}
    override fun rejectInvitation() {}
    override fun rejectInvitationAndBlockInviter() {}
    override fun clearError() {}
}

class PreviewRoomListElementViewModel2 : RoomListElementViewModel {
    private val roomId2 = RoomId("2", "localhost")
    override val account: UserId = UserId("0", "server")
    override val roomId: RoomId = roomId2
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isDirect: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isInvite: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isEncrypted: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isPublic: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val roomName: MutableStateFlow<String?> = MutableStateFlow("Allgemein")
    override val roomImageInitials: MutableStateFlow<String?> = MutableStateFlow("A")
    override val roomImage: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
    override val lastMessage: MutableStateFlow<String?> =
        MutableStateFlow("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
    override val time: MutableStateFlow<String?> = MutableStateFlow("24.12.19")
    override val unreadMessages: MutableStateFlow<String?> = MutableStateFlow("2")
    override val presence: MutableStateFlow<Presence?> = MutableStateFlow(Presence.ONLINE)
    override val accountColor: StateFlow<Long?> = MutableStateFlow(null)
    override fun acceptInvitation() {}
    override fun rejectInvitation() {}
    override fun rejectInvitationAndBlockInviter() {}
    override fun clearError() {}
}

class PreviewRoomListElementViewModel3 : RoomListElementViewModel {
    private val roomId3 = RoomId("3", "localhost")
    override val account: UserId = UserId("1", "server")
    override val roomId: RoomId = roomId3
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isDirect: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val isInvite: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isEncrypted: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val isPublic: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val roomName: MutableStateFlow<String?> = MutableStateFlow("Martin")
    override val roomImageInitials: MutableStateFlow<String?> = MutableStateFlow("M")
    override val roomImage: MutableStateFlow<ByteArray?> = MutableStateFlow(previewImageByteArray())
    override val lastMessage: MutableStateFlow<String?> =
        MutableStateFlow("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
    override val time: MutableStateFlow<String?> = MutableStateFlow("12.12.19")
    override val unreadMessages: MutableStateFlow<String?> = MutableStateFlow(null)
    override val presence: MutableStateFlow<Presence?> = MutableStateFlow(Presence.ONLINE)
    override val accountColor: StateFlow<Long?> = MutableStateFlow(null)
    override fun acceptInvitation() {}
    override fun rejectInvitation() {}
    override fun rejectInvitationAndBlockInviter() {}
    override fun clearError() {}
}

class PreviewRoomListElementViewModel4 : RoomListElementViewModel {
    private val roomId3 = RoomId("4", "localhost")
    override val account: UserId = UserId("1", "server")
    override val roomId: RoomId = roomId3
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isDirect: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val isInvite: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isEncrypted: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val isPublic: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val roomName: MutableStateFlow<String?> = MutableStateFlow("Martin")
    override val roomImageInitials: MutableStateFlow<String?> = MutableStateFlow("M")
    override val roomImage: MutableStateFlow<ByteArray?> = MutableStateFlow(previewImageByteArray())
    override val lastMessage: MutableStateFlow<String?> =
        MutableStateFlow("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
    override val time: MutableStateFlow<String?> = MutableStateFlow("12.12.19")
    override val unreadMessages: MutableStateFlow<String?> = MutableStateFlow(null)
    override val presence: MutableStateFlow<Presence?> = MutableStateFlow(Presence.OFFLINE)
    override val accountColor: StateFlow<Long?> = MutableStateFlow(null)
    override fun acceptInvitation() {}
    override fun rejectInvitation() {}
    override fun rejectInvitationAndBlockInviter() {}
    override fun clearError() {}
}
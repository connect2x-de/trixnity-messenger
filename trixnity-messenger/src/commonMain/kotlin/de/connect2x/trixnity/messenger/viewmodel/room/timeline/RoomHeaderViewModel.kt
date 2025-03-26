package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.DirectRoom
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.RoomTopic
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import de.connect2x.trixnity.messenger.viewmodel.util.UserPresence
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import de.connect2x.trixnity.messenger.viewmodel.util.typingInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.key.UserTrustLevel
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface RoomHeaderViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        onBack: () -> Unit,
        onVerifyUser: () -> Unit,
        onOpenRoomSettings: () -> Unit,
        onOpenUserProfile: (UserId) -> Unit,
    ): RoomHeaderViewModel =
        RoomHeaderViewModelImpl(
            viewModelContext = viewModelContext,
            selectedRoomId = selectedRoomId,
            onBack = onBack,
            onVerifyUser = onVerifyUser,
            onOpenRoomSettings = onOpenRoomSettings,
            onOpenUserProfile = onOpenUserProfile,
        )

    companion object : RoomHeaderViewModelFactory
}

data class RoomHeaderInfo(
    val roomName: String,
    var roomTopic: String,
    val roomImageInitials: String,
    val roomImage: ByteArray?,
    val presence: Presence?,
    val isEncrypted: Boolean,
    val isPublic: Boolean,
    val isLeave: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as RoomHeaderInfo
        if (roomName != other.roomName) return false
        if (roomTopic != other.roomTopic) return false
        if (roomImageInitials != other.roomImageInitials) return false
        if (roomImage != null) {
            if (other.roomImage == null) return false
            if (!roomImage.contentEquals(other.roomImage)) return false
        } else if (other.roomImage != null) return false
        if (presence != other.presence) return false
        if (isEncrypted != other.isEncrypted) return false
        if (isPublic != other.isPublic) return false
        if (isLeave != other.isLeave) return false
        return true
    }

    override fun hashCode(): Int {
        var result = roomName.hashCode()
        result = 31 * result + roomTopic.hashCode()
        result = 31 * result + roomImageInitials.hashCode()
        result = 31 * result + (roomImage?.contentHashCode() ?: 0)
        result = 31 * result + (presence?.hashCode() ?: 0)
        result = 31 * result + isEncrypted.hashCode()
        result = 31 * result + isPublic.hashCode()
        result = 31 * result + isLeave.hashCode()
        return result
    }
}

interface RoomHeaderViewModel {
    val error: StateFlow<String?>
    val roomHeaderInfo: StateFlow<RoomHeaderInfo>
    val usersTyping: StateFlow<String?>
    val userTrustLevel: StateFlow<UserTrustLevel?>
    val canVerifyUser: StateFlow<Boolean>

    /**
     * Is true if this is a direct room and only 2 users are present and not already blocked.
     */
    val canBlockUser: StateFlow<Boolean>

    /**
     * Is true if this is a direct room and only 2 users are present and other user is blocked.
     */
    val canUnblockUser: StateFlow<Boolean>
    val isUserBlocked: StateFlow<Boolean>

    val isDirectChat: StateFlow<Boolean>

    fun blockUser()
    fun unblockUser()
    fun verifyUser()
    fun openRoomSettings()
    fun openUserProfile()
    fun back()
}

@OptIn(ExperimentalCoroutinesApi::class)
open class RoomHeaderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val onBack: () -> Unit,
    private val onVerifyUser: () -> Unit,
    private val onOpenRoomSettings: () -> Unit,
    private val onOpenUserProfile: (UserId) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, RoomHeaderViewModel {
    private val directRoom = get<DirectRoom>()
    private val userPresence = get<UserPresence>()
    private val roomName = get<RoomName>()
    private val roomTopic = get<RoomTopic>()
    private val initials = get<Initials>()
    private val userBlocking = get<UserBlocking>()
    private val maxAvatarSize = get<MatrixMessengerConfiguration>().avatarMaxSize

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    override val roomHeaderInfo: StateFlow<RoomHeaderInfo> =
        combine(
            matrixClient.room.getById(selectedRoomId),
            roomName.getRoomName(selectedRoomId, matrixClient),
            roomTopic.getRoomTopic(selectedRoomId, matrixClient),
            userPresence.presentEventContentFlow(matrixClient, selectedRoomId),
            matrixClient.room.getState<JoinRulesEventContent>(selectedRoomId)
        ) { room, roomNameElement, roomTopicElement, userPresence, joinRules ->
            val roomImage = room?.avatarUrl?.let { avatarUrl ->
                matrixClient.media.getThumbnail(
                    avatarUrl,
                    avatarSize().toLong(),
                    avatarSize().toLong()
                ).fold(
                    onSuccess = {
                        it.limitedByteArrayOrNull(
                            maxAvatarSize
                        ) { log.error { "Room avatar for room $selectedRoomId exceeds max preview limits, so it's not displayed" } }
                    },
                    onFailure = { exc ->
                        if (exc !is CancellationException) {
                            log.error(exc) { "Cannot load avatar image for room '${roomNameElement}'." }
                        }
                        null
                    }
                )
            }
            RoomHeaderInfo(
                roomName = roomNameElement,
                roomTopic = roomTopicElement,
                roomImageInitials = initials.compute(roomNameElement),
                roomImage = roomImage,
                presence = userPresence?.presence,
                isEncrypted = room?.encrypted == true,
                isPublic = joinRules?.content?.joinRule == JoinRulesEventContent.JoinRule.Public,
                isLeave = room?.membership?.let { it == Membership.LEAVE }?: false
            )
        }.stateIn(
            coroutineScope,
            WhileSubscribed(),
            RoomHeaderInfo(
                roomName = "",
                roomTopic = "",
                roomImageInitials = initials.compute(selectedRoomId.full),
                roomImage = null,
                presence = Presence.OFFLINE,
                isEncrypted = false,
                isPublic = true,
                isLeave = false
            )
        )

    override val userTrustLevel: StateFlow<UserTrustLevel?> =
        directRoom.getUsers(matrixClient, selectedRoomId).flatMapLatest {
            it.firstOrNull()?.let { userId ->
                matrixClient.key.getTrustLevel(userId)
            } ?: flowOf(null)
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val canVerifyUser: StateFlow<Boolean> =
        combine(
            directRoom.getUsers(matrixClient, selectedRoomId),
            userTrustLevel
        ) { otherUsers, userTrustLevel ->
            val otherUserVerified =
                userTrustLevel is UserTrustLevel.CrossSigned && userTrustLevel.verified
            otherUsers.size == 1 && otherUserVerified.not()
        }.stateIn(coroutineScope, WhileSubscribed(), false)

    override val usersTyping = matrixClient.room.usersTyping.map { map ->
        map[selectedRoomId]?.let { typingInfo(matrixClient, selectedRoomId, i18n, it) }
    }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val canBlockUser: StateFlow<Boolean> = combine(
        directRoom.getUsers(matrixClient, selectedRoomId),
        matrixClient.user.getAccountData<IgnoredUserListEventContent>(),
    ) { otherUsers, ignoredUserListEventContent ->
        otherUsers.size == 1 && (ignoredUserListEventContent?.ignoredUsers?.containsKey(otherUsers[0])?.not() ?: false)
    }.stateIn(coroutineScope, WhileSubscribed(), false)

    override val canUnblockUser: StateFlow<Boolean> = combine(
        directRoom.getUsers(matrixClient, selectedRoomId),
        matrixClient.user.getAccountData<IgnoredUserListEventContent>(),
    ) { otherUsers, ignoredUserListEventContent ->
        otherUsers.size == 1 && (ignoredUserListEventContent?.ignoredUsers?.containsKey(otherUsers[0]))
                ?: false
    }.stateIn(coroutineScope, WhileSubscribed(), false)

    override val isUserBlocked: StateFlow<Boolean> =
        directRoom.getUsers(matrixClient, selectedRoomId).flatMapLatest { userIds ->
            if (userIds.size == 1) userBlocking.isUserBlocked(matrixClient, userIds[0]) else flowOf(false)
        }.stateIn(coroutineScope, WhileSubscribed(), false)

    private val otherDirectUser = directRoom.getUsers(matrixClient, selectedRoomId).mapLatest { userIds ->
        if (userIds.size == 1) userIds[0] else null
    }.stateIn(coroutineScope, WhileSubscribed(), null)

    override val isDirectChat = otherDirectUser.mapLatest { it != null }
        .stateIn(coroutineScope, WhileSubscribed(), false)

    override fun openUserProfile() {
        otherDirectUser.value?.also { userId ->
            onOpenUserProfile(userId)
        }
    }

    override fun verifyUser() {
        onVerifyUser()
    }

    override fun blockUser() {
        coroutineScope.launch {
            if (canBlockUser.value) {
                val otherUsers = directRoom.getUsers(matrixClient, selectedRoomId).first()
                if (otherUsers.size == 1) {
                    userBlocking.blockUser(matrixClient, otherUsers[0]) {
                        error.value = i18n.blockUserError(otherUsers[0].full)
                    }
                }
            }
        }
    }

    override fun unblockUser() {
        coroutineScope.launch {
            if (canUnblockUser.value) {
                val otherUsers = directRoom.getUsers(matrixClient, selectedRoomId).first()
                if (otherUsers.size == 1) {
                    userBlocking.unblockUser(matrixClient, otherUsers[0]) {
                        error.value = i18n.blockUserError(otherUsers[0].full)
                    }
                }
            }
        }
    }

    override fun openRoomSettings() {
        onOpenRoomSettings()
    }

    override fun back() {
        onBack()
    }
}

class PreviewRoomHeaderViewModel : RoomHeaderViewModel {
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val roomHeaderInfo: MutableStateFlow<RoomHeaderInfo> = MutableStateFlow(
        RoomHeaderInfo(
            roomName = "Dev Channel",
            roomTopic = "The channel that devs!",
            roomImageInitials = "DC",
            roomImage = null,
            presence = null,
            isEncrypted = false,
            isPublic = true,
            isLeave = false
        )
    )
    override val usersTyping: MutableStateFlow<String?> = MutableStateFlow("is typing...")
    override val userTrustLevel: MutableStateFlow<UserTrustLevel?> = MutableStateFlow(null)
    override val canVerifyUser: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canBlockUser: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canUnblockUser: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isUserBlocked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isDirectChat: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override fun verifyUser() {}
    override fun blockUser() {}
    override fun unblockUser() {}
    override fun openRoomSettings() {}
    override fun openUserProfile() {}
    override fun back() {}
}

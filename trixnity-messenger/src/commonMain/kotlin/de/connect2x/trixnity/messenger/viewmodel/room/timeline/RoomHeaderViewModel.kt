package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.RoomHeaderElement
import de.connect2x.trixnity.messenger.viewmodel.RoomName
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.key.UserTrustLevel
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.TypingEventContent
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface RoomHeaderViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        isBackButtonVisible: MutableStateFlow<Boolean>,
        onBack: () -> Unit,
        onVerifyUser: () -> Unit,
        onShowRoomSettings: () -> Unit,
    ): RoomHeaderViewModel {
        return RoomHeaderViewModelImpl(
            viewModelContext,
            selectedRoomId,
            isBackButtonVisible,
            onBack,
            onVerifyUser,
            onShowRoomSettings,
        )
    }

    companion object : RoomHeaderViewModelFactory
}

interface RoomHeaderViewModel {
    val error: StateFlow<String?>
    val isBackButtonVisible: StateFlow<Boolean>
    val roomHeaderElement: StateFlow<RoomHeaderElement>
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

    fun blockUser()
    fun unblockUser()
    fun verifyUser()
    fun showRoomSettings()
    fun goBack()
}

@OptIn(ExperimentalCoroutinesApi::class)
open class RoomHeaderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    override val isBackButtonVisible: MutableStateFlow<Boolean>,
    private val onBack: () -> Unit,
    private val onVerifyUser: () -> Unit,
    private val onShowRoomSettings: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, RoomHeaderViewModel {

    override val roomHeaderElement: StateFlow<RoomHeaderElement>
    override val userTrustLevel: StateFlow<UserTrustLevel?>
    override val canVerifyUser: StateFlow<Boolean>
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    private val directRoom = get<DirectRoom>()
    private val userPresence = get<UserPresence>()
    private val roomName = get<RoomName>()
    private val initials = get<Initials>()
    private val userBlocking = get<UserBlocking>()

    override val usersTyping = matrixClient.room.usersTyping.map { map ->
        map[selectedRoomId]?.let { typingInfo(it) }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val canBlockUser: StateFlow<Boolean> = combine(
        directRoom.getUsers(matrixClient, selectedRoomId),
        matrixClient.user.getAccountData<IgnoredUserListEventContent>(),
    ) { otherUsers, ignoredUserListEventContent ->
        otherUsers.size == 1 && (ignoredUserListEventContent?.ignoredUsers?.containsKey(otherUsers[0])?.not())
                ?: false
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val canUnblockUser: StateFlow<Boolean> = combine(
        directRoom.getUsers(matrixClient, selectedRoomId),
        matrixClient.user.getAccountData<IgnoredUserListEventContent>(),
    ) { otherUsers, ignoredUserListEventContent ->
        otherUsers.size == 1 && (ignoredUserListEventContent?.ignoredUsers?.containsKey(otherUsers[0]))
                ?: false
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val isUserBlocked: StateFlow<Boolean> =
        directRoom.getUsers(matrixClient, selectedRoomId).flatMapLatest { userIds ->
            if (userIds.size == 1) userBlocking.isUserBlocked(matrixClient, userIds[0]) else flowOf(false)
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    init {
        roomHeaderElement =
            combine(
                matrixClient.room.getById(selectedRoomId),
                roomName.getRoomNameElement(selectedRoomId, matrixClient),
                userPresence.presentEventContentFlow(matrixClient, selectedRoomId)
            ) { room, roomNameElement, userPresence ->
                val roomImage = room?.avatarUrl?.let { avatarUrl ->
                    matrixClient.media.getThumbnail(
                        avatarUrl,
                        avatarSize().toLong(),
                        avatarSize().toLong()
                    ).fold(
                        onSuccess = { it },
                        onFailure = {
                            log.error(it) { "Cannot load avatar image for room '${roomNameElement.roomName}'." }
                            null
                        }
                    )
                }?.toByteArray()
                RoomHeaderElement(
                    roomNameElement.roomName,
                    initials.compute(roomNameElement.roomName),
                    roomImage,
                    userPresence?.presence,
                )
            }.stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(),
                RoomHeaderElement(
                    "",
                    initials.compute(selectedRoomId.full),
                    null,
                    Presence.OFFLINE
                )
            )

        userTrustLevel =
            directRoom.getUsers(matrixClient, selectedRoomId).flatMapLatest {
                it.firstOrNull()?.let { userId ->
                    matrixClient.key.getTrustLevel(userId)
                } ?: flowOf(null)
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

        canVerifyUser =
            combine(
                directRoom.getUsers(matrixClient, selectedRoomId),
                userTrustLevel
            ) { otherUsers, userTrustLevel ->
                val otherUserVerified =
                    userTrustLevel is UserTrustLevel.CrossSigned && userTrustLevel.verified
                otherUsers.size == 1 && otherUserVerified.not()
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
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

    override fun showRoomSettings() {
        onShowRoomSettings()
    }

    override fun goBack() {
        onBack()
    }

    private suspend fun typingInfo(eventContent: TypingEventContent): String? {
        val usersTyping = eventContent.users.filterNot { it == matrixClient.userId }
        return when {
            usersTyping.isEmpty() -> null
            usersTyping.size == 1 -> {
                val username = usersTyping[0].let {
                    matrixClient.user.getById(selectedRoomId, it).first()?.name
                        ?: it.full
                }
                val isDirect =
                    matrixClient.room.getById(selectedRoomId).first()?.isDirect ?: false
                if (isDirect)
                    i18n.roomHeaderTypingSingleDirect()
                else
                    i18n.roomHeaderTypingSingle(username)
            }

            usersTyping.size < 4 -> {
                val usernames = usersTyping.map {
                    matrixClient.user.getById(selectedRoomId, it).first()?.name
                        ?: it.full
                }

                i18n.roomHeaderTypingMultiple(
                    i18n.commonAnd(
                        usernames.take(usernames.size - 1).joinToString(),
                        usernames.last()
                    )
                )
            }

            else -> {
                val usernames = usersTyping.map {
                    matrixClient.user.getById(selectedRoomId, it).first()?.name
                        ?: it.full
                }
                i18n.roomHeaderTypingMultipleMore(usernames.take(2).joinToString())
            }
        }
    }
}

class PreviewRoomHeaderViewModel : RoomHeaderViewModel {
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isBackButtonVisible: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val roomHeaderElement: MutableStateFlow<RoomHeaderElement> = MutableStateFlow(
        RoomHeaderElement(
            roomName = "Dev Channel",
            roomImageInitials = "DC",
            roomImage = null,
            presence = null
        )
    )
    override val usersTyping: MutableStateFlow<String?> = MutableStateFlow("is typing...")
    override val userTrustLevel: MutableStateFlow<UserTrustLevel?> = MutableStateFlow(null)
    override val canVerifyUser: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canBlockUser: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canUnblockUser: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isUserBlocked: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun verifyUser() {}
    override fun blockUser() {}
    override fun unblockUser() {}
    override fun showRoomSettings() {}
    override fun goBack() {}

}

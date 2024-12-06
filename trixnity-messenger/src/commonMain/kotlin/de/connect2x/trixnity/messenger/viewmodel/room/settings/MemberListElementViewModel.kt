package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModel.*
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.key.UserTrustLevel
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.store.originalName
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.core.component.get

interface MemberListElementViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomUser: RoomUser,
        selectedRoomId: RoomId,
        onShowUserProfile: (UserId) -> Unit
    ): MemberListElementViewModelImpl {
        return MemberListElementViewModelImpl(
            viewModelContext = viewModelContext,
            roomUser = roomUser,
            selectedRoomId = selectedRoomId,
            onShowUserProfile = onShowUserProfile
        )
    }

    companion object : MemberListElementViewModelFactory
}

interface MemberListElementViewModel {
    val userId: UserId
    val member: StateFlow<UserInfoElement?>
    val userTrustLevel: StateFlow<UserTrustLevel?>
    val membership: StateFlow<Membership?>
    val iHavePowerToUnbanUser: StateFlow<Boolean>
    val role: StateFlow<Role>
    val powerLevel: StateFlow<Long>
    val showRole: StateFlow<Boolean>
    val showPowerLevel: StateFlow<Boolean>
    val isUserBlocked: StateFlow<Boolean>
    val presence: StateFlow<Presence>

    fun showUserProfile()
}

@OptIn(ExperimentalCoroutinesApi::class)
class MemberListElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomUser: RoomUser,
    private val selectedRoomId: RoomId,
    private val onShowUserProfile: (UserId) -> Unit
) : MatrixClientViewModelContext by viewModelContext, MemberListElementViewModel {
    override val userId = roomUser.userId
    override val member: StateFlow<UserInfoElement?>
    override val membership: StateFlow<Membership?> = matrixClient.user.getById(selectedRoomId, userId)
        .mapLatest { it?.membership }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val initials = get<Initials>()
    private val userBlocking = get<UserBlocking>()

    private val roomUserOriginalName = MutableStateFlow<String?>(null)

    override val userTrustLevel: StateFlow<UserTrustLevel?> = matrixClient.key.getTrustLevel(userId)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val role = MutableStateFlow(Role.USER)
    override val showRole = MutableStateFlow(false)
    override val powerLevel = matrixClient.user.getPowerLevel(selectedRoomId, userId)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), 0)
    override val showPowerLevel = MutableStateFlow(false)

    override val iHavePowerToUnbanUser: StateFlow<Boolean> = matrixClient.user.canUnbanUser(selectedRoomId, userId)
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override val isUserBlocked: StateFlow<Boolean> = userBlocking.isUserBlocked(matrixClient, userId)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val presence = matrixClient.user.userPresence.map { it[userId]?.presence ?: Presence.OFFLINE }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Presence.OFFLINE)

    init {
        coroutineScope.launch {
            matrixClient.user.getPowerLevel(selectedRoomId, userId).collect { powerLevel ->
                role.value = getPowerRole(powerLevel)
                showRole.value = role.value != Role.USER
                showPowerLevel.value = role.value.getMinPowerLevel() != powerLevel
            }
        }

        // TODO this is not reactive! For example instead of RoomUser, just the userId should be passed from the list.
        member = channelFlow {
            roomUserOriginalName.value = roomUser.originalName
            send(
                UserInfoElement(
                    roomUser.name,
                    roomUser.userId,
                    initials.compute(roomUser.name),
                    getImage(
                        matrixClient,
                        roomUser
                    ),
                )
            )
        }.buffer(0).stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    }

    override fun showUserProfile() {
        onShowUserProfile(userId)
    }

    private suspend fun getImage(matrixClient: MatrixClient, user: RoomUser): Flow<ByteArray>? {
        return user.avatarUrl?.let { url ->
            matrixClient.media.getThumbnail(url, avatarSize().toLong(), avatarSize().toLong()).fold(
                onSuccess = { it },
                onFailure = { null }
            )
        }
    }
    private fun getPowerRole(powerLevel: Long): Role {
        return when {
            powerLevel >= Role.ADMIN.getMinPowerLevel() -> Role.ADMIN
            powerLevel >= Role.MODERATOR.getMinPowerLevel() -> Role.MODERATOR
            else -> Role.USER
        }
    }
}


package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.MemberElement
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.ADMIN
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.MODERATOR
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.USER
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

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
    val member: StateFlow<MemberElement?>
    val userTrustLevel: StateFlow<UserTrustLevel?>
    // val memberOptionsOpen: StateFlow<Boolean>
    // val error: StateFlow<String?>
    // val membershipReason: StateFlow<String?>
    val membership: StateFlow<Membership?>
    // val kickUserWarningOpen: StateFlow<Boolean>
    // val kickUserWarningMessage: StateFlow<String>
    // val kickUserWarningTitle: StateFlow<String>
    // val iHavePowerToKickUser: StateFlow<Boolean>
    // val banUserWarningOpen: StateFlow<Boolean>
    // val iHavePowerToBanUser: StateFlow<Boolean>
    // val unbanUserWarningOpen: StateFlow<Boolean>
    val iHavePowerToUnbanUser: StateFlow<Boolean>
    val role: StateFlow<Role>
    val powerLevel: StateFlow<Long>
    val showRole: StateFlow<Boolean>
    val showPowerLevel: StateFlow<Boolean>
    // val changePowerLevelViewModel: ChangePowerLevelViewModel
    val isUserBlocked: StateFlow<Boolean>
    // val blockingInProgress: StateFlow<Boolean>
    val presence: StateFlow<Presence>

    // fun openMemberOptions()
    // fun closeMemberOptions()
    // fun openKickUserWarning()
    // fun closeKickUserWarning()
    // fun kickUser(userId: UserId)
    // fun openBanUserWarning()
    // fun closeBanUserWarning()
    // fun banUser(reason: String?)
    // fun openUnbanUserWarning()
    // fun closeUnbanUserWarning()
    // fun unbanUser(reason: String?)
    // fun blockUser()
    // fun unblockUser()
    fun showUserProfile()

    enum class Role {
        USER {
            override fun getMinPowerLevel() = 0L
        },
        MODERATOR {
            override fun getMinPowerLevel() = 50L
        },
        ADMIN {
            override fun getMinPowerLevel() = 100L
        };

        abstract fun getMinPowerLevel(): Long
    }

    data class MemberElement(
        val image: ByteArray?,
        val displayName: String,
        val userId: String,
        val initials: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as MemberElement

            if (image != null) {
                if (other.image == null) return false
                if (!image.contentEquals(other.image)) return false
            } else if (other.image != null) return false
            if (displayName != other.displayName) return false
            if (userId != other.userId) return false
            if (initials != other.initials) return false

            return true
        }

        override fun hashCode(): Int {
            var result = image?.contentHashCode() ?: 0
            result = 31 * result + displayName.hashCode()
            result = 31 * result + userId.hashCode()
            result = 31 * result + initials.hashCode()
            return result
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MemberListElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomUser: RoomUser,
    // override val error: MutableStateFlow<String?>,
    private val selectedRoomId: RoomId,
    private val onShowUserProfile: (UserId) -> Unit
) : MatrixClientViewModelContext by viewModelContext, MemberListElementViewModel {
    override val userId = roomUser.userId
    override val member: StateFlow<MemberElement?>
    override val membership: StateFlow<Membership?> = matrixClient.user.getById(selectedRoomId, userId)
        .mapLatest { it?.membership }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val initials = get<Initials>()
    private val userBlocking = get<UserBlocking>()

    private val roomUserOriginalName = MutableStateFlow<String?>(null)

    override val userTrustLevel: StateFlow<UserTrustLevel?> = matrixClient.key.getTrustLevel(userId)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val role = MutableStateFlow(USER)
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
                showRole.value = !(role.value == USER)
                showPowerLevel.value = role.value.getMinPowerLevel() != powerLevel
            }
        }

        // TODO this is not reactive! For example instead of RoomUser, just the userId should be passed from the list.
        member = channelFlow {
            roomUserOriginalName.value = roomUser.originalName
            send(
                MemberElement(
                    getImage(
                        matrixClient,
                        roomUser
                    ),
                    roomUser.name,
                    roomUser.userId.full,
                    initials.compute(roomUser.name),
                )
            )
        }.buffer(0).stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    }

    override fun showUserProfile() {
        onShowUserProfile(userId)
    }

    private suspend fun getImage(matrixClient: MatrixClient, user: RoomUser): ByteArray? {
        return user.avatarUrl?.let { url ->
            matrixClient.media.getThumbnail(url, avatarSize().toLong(), avatarSize().toLong()).fold(
                onSuccess = { it.toByteArray() },
                onFailure = { null }
            )
        }
    }
    private fun getPowerRole(powerLevel: Long): Role {
        return when {
            powerLevel >= ADMIN.getMinPowerLevel() -> ADMIN
            powerLevel >= MODERATOR.getMinPowerLevel() -> MODERATOR
            else -> USER
        }
    }
}


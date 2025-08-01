package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModel.Role
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
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.store.originalName
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.PowerLevel
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.crypto.key.UserTrustLevel
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

interface MemberListElementViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomUser: RoomUser,
        selectedRoomId: RoomId,
        onOpenUserProfile: (UserId) -> Unit
    ): MemberListElementViewModel {
        return MemberListElementViewModelImpl(
            viewModelContext = viewModelContext,
            roomUser = roomUser,
            selectedRoomId = selectedRoomId,
            onOpenUserProfile = onOpenUserProfile,
        )
    }

    companion object : MemberListElementViewModelFactory
}

interface MemberListElementViewModel {
    val memberUserId: UserId
    val member: StateFlow<MemberElement?>
    val userTrustLevel: StateFlow<UserTrustLevel?>
    val membership: StateFlow<Membership?>
    val iHavePowerToUnbanUser: StateFlow<Boolean>
    val role: StateFlow<Role>
    val powerLevel: StateFlow<PowerLevel?>
    val showRole: StateFlow<Boolean>
    val showPowerLevel: StateFlow<Boolean>
    val iHavePowerToBlockUser: Boolean
    val isUserBlocked: StateFlow<Boolean>
    val presence: StateFlow<Presence>

    fun openUserProfile()

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
    private val selectedRoomId: RoomId,
    private val onOpenUserProfile: (UserId) -> Unit
) : MatrixClientViewModelContext by viewModelContext, MemberListElementViewModel {
    override val memberUserId = roomUser.userId
    override val member: StateFlow<MemberListElementViewModel.MemberElement?>
    override val membership: StateFlow<Membership?> = matrixClient.user.getById(selectedRoomId, memberUserId)
        .mapLatest { it?.membership }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val initials = get<Initials>()
    private val userBlocking = get<UserBlocking>()

    private val roomUserOriginalName = MutableStateFlow<String?>(null)

    override val userTrustLevel: StateFlow<UserTrustLevel?> = matrixClient.key.getTrustLevel(memberUserId)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val role = MutableStateFlow(Role.USER)
    override val showRole = MutableStateFlow(false)
    override val powerLevel = matrixClient.user.getPowerLevel(selectedRoomId, memberUserId)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val showPowerLevel = MutableStateFlow(false)

    override val iHavePowerToUnbanUser: StateFlow<Boolean> =
        matrixClient.user.canUnbanUser(selectedRoomId, memberUserId)
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override val iHavePowerToBlockUser: Boolean = matrixClient.userId != roomUser.userId
    override val isUserBlocked: StateFlow<Boolean> = userBlocking.isUserBlocked(matrixClient, memberUserId)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val presence = matrixClient.user.getPresence(memberUserId)
        .map { it?.presence ?: Presence.OFFLINE }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Presence.OFFLINE)

    private val maxMediaSizeInMemory = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory

    init {
        coroutineScope.launch {
            matrixClient.user.getPowerLevel(selectedRoomId, memberUserId).collect { powerLevel ->
                role.value = getPowerRole(powerLevel)
                showRole.value = role.value != Role.USER
                showPowerLevel.value = role.value.getMinPowerLevel() != powerLevel
            }
        }

        // TODO this is not reactive! For example instead of RoomUser, just the userId should be passed from the list.
        member = channelFlow {
            roomUserOriginalName.value = roomUser.originalName
            send(
                MemberListElementViewModel.MemberElement(
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

    override fun openUserProfile() {
        onOpenUserProfile(memberUserId)
    }


    private suspend fun getImage(matrixClient: MatrixClient, user: RoomUser): ByteArray? {
        return user.avatarUrl?.let { url ->
            matrixClient.media.getThumbnail(url, avatarSize().toLong(), avatarSize().toLong()).fold(
                onSuccess = {
                    it.toByteArray(coroutineScope, maxSize = maxMediaSizeInMemory)
                },
                onFailure = { null }
            )
        }
    }

    private fun getPowerRole(powerLevel: PowerLevel): Role {
        return when {
            powerLevel >= Role.ADMIN.getMinPowerLevel() -> Role.ADMIN
            powerLevel >= Role.MODERATOR.getMinPowerLevel() -> Role.MODERATOR
            else -> Role.USER
        }
    }
}


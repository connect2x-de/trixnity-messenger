package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.*
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.store.originalName
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

interface MemberListElementViewModelFactory {
    fun newMemberListElementViewModel(
        viewModelContext: MatrixClientViewModelContext,
        roomUser: RoomUser,
        error: MutableStateFlow<String?>,
        selectedRoomId: RoomId
    ): MemberListElementViewModelImpl {
        return MemberListElementViewModelImpl(
            viewModelContext = viewModelContext,
            roomUser = roomUser,
            error = error,
            selectedRoomId = selectedRoomId
        )
    }
}

interface MemberListElementViewModel {
    val userId: UserId
    val member: StateFlow<MemberElement?>
    val memberOptionsOpen: StateFlow<Boolean>
    val error: StateFlow<String?>
    val kickUserWarningOpen: StateFlow<Boolean>
    val kickUserWarningMessage: StateFlow<String>
    val kickUserWarningTitle: StateFlow<String>
    val iHavePowerToKickUser: StateFlow<Boolean>
    val role: StateFlow<Role>
    val powerLevel: StateFlow<Int>
    val showRole: StateFlow<Boolean>
    val showPowerLevel: StateFlow<Boolean>
    val changePowerLevelViewModel: ChangePowerLevelViewModel

    fun openMemberOptions()
    fun closeMemberOptions()
    fun openKickUserWarning()
    fun closeKickUserWarning()
    fun kickUser(userId: UserId)

    enum class Role {
        USER {
            override fun getMinPowerLevel() = 0
        },
        MODERATOR {
            override fun getMinPowerLevel() = 50
        },
        ADMIN {
            override fun getMinPowerLevel() = 100
        };

        abstract fun getMinPowerLevel(): Int
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

open class MemberListElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomUser: RoomUser,
    override val error: MutableStateFlow<String?>,
    private val selectedRoomId: RoomId
) : MatrixClientViewModelContext by viewModelContext, MemberListElementViewModel {

    override val memberOptionsOpen = MutableStateFlow(false)
    override val userId = roomUser.userId

    override val kickUserWarningOpen = MutableStateFlow(false)
    override val kickUserWarningMessage = MutableStateFlow("")
    override val kickUserWarningTitle = MutableStateFlow("")

    private val initials = get<Initials>()
    private val isDirect: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val roomUserOriginalName = MutableStateFlow<String?>(null)

    override val member: StateFlow<MemberListElementViewModel.MemberElement?>
    override val role = MutableStateFlow(USER)
    override val showRole = MutableStateFlow(false)
    override val powerLevel = matrixClient.user.getPowerLevel(selectedRoomId, userId)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), 0)
    override val showPowerLevel = MutableStateFlow(false)

    override val iHavePowerToKickUser = matrixClient.user.canKickUser(selectedRoomId, userId)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val changePowerLevelViewModel: ChangePowerLevelViewModel =
        get<ChangePowerLevelViewModelFactory>()
            .newChangePowerLevelViewModelFactory(
                viewModelContext = viewModelContext.childContext("changePowerLevel-${roomUser.userId.full}"),
                powerLevel = powerLevel,
                roomUser = roomUser,
                error = error,
                selectedRoomId = selectedRoomId,
                closeMemberOptions = ::closeMemberOptions
            )

    init {
        coroutineScope.launch {
            val roomFlow = matrixClient.room.getById(selectedRoomId)
            combine(roomFlow, roomUserOriginalName) { room, roomUserOriginalName ->
                isDirect.value = room?.isDirect ?: false

                val displayName =
                    (roomUserOriginalName ?: userId.full) + " (" + userId.full + ")"

                if (isDirect.value) {
                    kickUserWarningTitle.value = i18n.settingsRoomMemberListKickUserWarningTitleChat(displayName)
                    kickUserWarningMessage.value = i18n.settingsRoomMemberListKickUserWarningMessageChat()

                } else {
                    kickUserWarningTitle.value = i18n.settingsRoomMemberListKickUserWarningTitleGroup(displayName)
                    kickUserWarningMessage.value = i18n.settingsRoomMemberListKickUserWarningMessageGroup()
                }
            }.collect()
        }

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

    private suspend fun getImage(matrixClient: MatrixClient, user: RoomUser): ByteArray? {
        return user.avatarUrl?.let { url ->
            matrixClient.media.getThumbnail(url, avatarSize().toLong(), avatarSize().toLong()).fold(
                onSuccess = { it.toByteArray() },
                onFailure = { null }
            )
        }
    }

    override fun openMemberOptions() {
        memberOptionsOpen.value = true
    }

    override fun closeMemberOptions() {
        memberOptionsOpen.value = false
    }

    override fun openKickUserWarning() {
        kickUserWarningOpen.value = true
    }

    override fun closeKickUserWarning() {
        kickUserWarningOpen.value = false
    }

    override fun kickUser(userId: UserId) {
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                error.value = i18n.settingsRoomMemberListKickUserErrorOffline()
            } else {
                matrixClient.api.rooms.kickUser(
                    selectedRoomId,
                    userId = userId,
                    null,
                    null
                ).fold(
                    onSuccess = {
                        closeMemberOptions()
                    },
                    onFailure = {
                        if (it is CancellationException) {
                            return@launch
                        }
                        log.error(it) { "cannot kick user $userId from $selectedRoomId" }
                        error.value = i18n.settingsRoomMemberListKickUserError()
                    }
                )
            }
        }
    }

    private fun getPowerRole(powerLevel: Int): Role {
        return when {
            powerLevel >= ADMIN.getMinPowerLevel() -> ADMIN
            powerLevel >= MODERATOR.getMinPowerLevel() -> MODERATOR
            else -> USER
        }
    }
}


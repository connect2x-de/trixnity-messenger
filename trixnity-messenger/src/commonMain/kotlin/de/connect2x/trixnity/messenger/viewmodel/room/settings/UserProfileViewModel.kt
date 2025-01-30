package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModel.*
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.key.UserTrustLevel
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.store.originalName
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.clientserverapi.model.rooms.CreateRoom
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.InitialStateEvent
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

interface UserProfileViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        userId: UserId,
        selectedRoomId: RoomId,
        goToRoom: (UserId, RoomId) -> Unit,
        onBack: () -> Unit
    ): UserProfileViewModel {
        return UserProfileViewModelImpl(
            viewModelContext = viewModelContext,
            userId = userId,
            selectedRoomId = selectedRoomId,
            goToRoom = goToRoom,
            onBack = onBack
        )
    }

    companion object : UserProfileViewModelFactory
}

interface UserProfileViewModel {
    val userId: UserId
    val isMyself: Boolean
    val isDirect: StateFlow<Boolean>
    val userInfo: StateFlow<UserInfoElement?>
    val userTrustLevel: StateFlow<UserTrustLevel?>
    val error: StateFlow<String?>
    val membershipReason: StateFlow<String?>
    val membership: StateFlow<Membership?>
    val kickUserReason: MutableStateFlow<String>
    val kickUserWarningOpen: StateFlow<Boolean>
    val kickUserWarningMessage: StateFlow<String>
    val kickUserWarningTitle: StateFlow<String>
    val iHavePowerToKickUser: StateFlow<Boolean>
    val banUserWarningOpen: StateFlow<Boolean>
    val membershipChanging: StateFlow<Boolean>
    val iHavePowerToBanUser: StateFlow<Boolean>
    val banReason: MutableStateFlow<String>
    val unbanUserWarningOpen: StateFlow<Boolean>
    val iHavePowerToUnbanUser: StateFlow<Boolean>
    val unbanReason: MutableStateFlow<String>
    val role: StateFlow<Role>
    val powerLevel: StateFlow<Long>
    val showRole: StateFlow<Boolean>
    val showPowerLevel: StateFlow<Boolean>
    val changePowerLevelViewModel: ChangePowerLevelViewModel
    val isUserBlocked: StateFlow<Boolean>
    val blockingInProgress: StateFlow<Boolean>
    val presence: StateFlow<Presence>
    val openingChat: StateFlow<Boolean>
    val verifying: StateFlow<Boolean>
    val canOpenChat: StateFlow<Boolean>

    fun openKickUserWarning()
    fun closeKickUserWarning()
    fun kickUser()
    fun openBanUserWarning()
    fun closeBanUserWarning()
    fun banUser()
    fun openUnbanUserWarning()
    fun closeUnbanUserWarning()
    fun unbanUser()
    fun blockUser()
    fun unblockUser()

    fun back()
    fun errorDismiss()

    fun openChat()
    fun startVerification()
}

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val userId: UserId,
    private val selectedRoomId: RoomId,
    private val goToRoom: (UserId, RoomId) -> Unit,
    private val onBack: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, UserProfileViewModel {
    override val isMyself = userId == matrixClient.userId

    private val roomUser = matrixClient.user.getById(selectedRoomId, userId)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val canOpenChat =
            matrixClient.user.getAccountData<DirectEventContent>().map { directEvent ->
                val isDirectChatWithOtherUser = directEvent?.mappings?.get(userId)?.contains(selectedRoomId) ?: false
                log.debug { "Checking whether chat can be opened: is direct chat with other user: $isDirectChatWithOtherUser" }
                !isDirectChatWithOtherUser
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    override val kickUserWarningOpen = MutableStateFlow(false)
    override val kickUserWarningMessage = MutableStateFlow("")
    override val kickUserWarningTitle = MutableStateFlow("")

    override val banUserWarningOpen = MutableStateFlow(false)
    override val unbanUserWarningOpen = MutableStateFlow(false)

    override val membershipReason: StateFlow<String?> = roomUser
        .mapLatest { it?.event?.content?.reason }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val membership: StateFlow<Membership?> = roomUser
        .mapLatest { it?.membership }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val initials = get<Initials>()
    private val userBlocking = get<UserBlocking>()

    override val isDirect: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val roomUserOriginalName = MutableStateFlow<String?>(null)

    val isUserInRoom = matrixClient.user.getById(selectedRoomId, userId)
        .map { it != null }
        .shareIn(coroutineScope, SharingStarted.WhileSubscribed())

    override val userTrustLevel: StateFlow<UserTrustLevel?> = matrixClient.key.getTrustLevel(userId)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val userInfo: StateFlow<UserInfoElement?>
    override val role = MutableStateFlow(Role.USER)
    override val showRole = MutableStateFlow(false)
    override val powerLevel = matrixClient.user.getPowerLevel(selectedRoomId, matrixClient.userId)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), 0)
    override val showPowerLevel = MutableStateFlow(false)

    private val _membershipChanging = MutableStateFlow(false)
    override val membershipChanging: StateFlow<Boolean> = _membershipChanging

    override val kickUserReason: MutableStateFlow<String> = MutableStateFlow("")

    override val iHavePowerToKickUser =
       combine(
           isUserInRoom,
           matrixClient.user.canKickUser(selectedRoomId, userId)
       ) { inRoom, canKick -> inRoom && canKick }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override val iHavePowerToBanUser: StateFlow<Boolean> =
       combine(
           isUserInRoom,
           matrixClient.user.canBanUser(selectedRoomId, userId)
       ) { inRoom, canBan -> inRoom && canBan }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override val banReason: MutableStateFlow<String> = MutableStateFlow("")

    override val iHavePowerToUnbanUser: StateFlow<Boolean> =
        combine(
            isUserInRoom,
            matrixClient.user.canUnbanUser(selectedRoomId, userId),
        ) { inRoom, canUnBan -> inRoom && canUnBan }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override val unbanReason: MutableStateFlow<String> = MutableStateFlow("")

    override val isUserBlocked: StateFlow<Boolean> = userBlocking.isUserBlocked(matrixClient, userId)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val blockingInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val changePowerLevelViewModel: ChangePowerLevelViewModel =
        get<ChangePowerLevelViewModelFactory>()
            .create(
                viewModelContext = viewModelContext.childContext("changePowerLevel-${userId.full}"),
                powerLevel = powerLevel,
                targetUser = userId,
                error = error,
                selectedRoomId = selectedRoomId
            )
    override val presence = matrixClient.user.userPresence.map { it[userId]?.presence ?: Presence.OFFLINE }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Presence.OFFLINE)


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
                showRole.value = role.value != Role.USER
                showPowerLevel.value = role.value.getMinPowerLevel() != powerLevel
            }
        }

        userInfo = roomUser.mapNotNull {
            it?.let { roomUser ->
                roomUserOriginalName.value = roomUser.originalName

                UserInfoElement(
                    roomUser.name,
                    roomUser.userId,
                    initials.compute(roomUser.name),
                    getImageLazy(
                        matrixClient,
                        roomUser
                    ),
                )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    }

    private fun getImageLazy(matrixClient: MatrixClient, user: RoomUser) = flow {
        emit(getImage(matrixClient, user))
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private suspend fun getImage(matrixClient: MatrixClient, user: RoomUser): ByteArray? {
        val maxAvatarSize = get<MatrixMessengerConfiguration>().avatarMaxSize
        return user.avatarUrl?.let { url ->
            matrixClient.media.getThumbnail(url, avatarSize().toLong(), avatarSize().toLong()).fold(
                onSuccess = {
                    it.limitedByteArrayOrNull(maxAvatarSize) {
                        log.error { "User avatar for user ${user.userId} exceeds max preview size, so it is not displayed" }
                    }
                },
                onFailure = { null }
            )
        }
    }

    init {
        backHandler.register(BackCallback { onBack() })
    }

    override fun back() {
        onBack()
    }

    override fun errorDismiss() {
        error.value = null
    }

    override fun openKickUserWarning() {
        kickUserWarningOpen.value = true
    }

    override fun closeKickUserWarning() {
        kickUserWarningOpen.value = false
    }

    override fun openBanUserWarning() {
        banUserWarningOpen.value = true
    }

    override fun closeBanUserWarning() {
        banUserWarningOpen.value = false
    }

    override fun openUnbanUserWarning() {
        unbanUserWarningOpen.value = true
    }

    override fun closeUnbanUserWarning() {
        unbanUserWarningOpen.value = false
    }

    override fun kickUser() {
        coroutineScope.launch {
            if (_membershipChanging.getAndUpdate { true }) {
                error.value = i18n.userProfileMembershipChanging()
                return@launch
            }

            if (matrixClient.syncState.value == SyncState.ERROR) {
                error.value = i18n.settingsRoomMemberListKickUserErrorOffline()
            } else {
                matrixClient.api.room.kickUser(
                    selectedRoomId,
                    userId,
                    kickUserReason.value.ifBlank { null },
                    null
                ).fold(
                    onSuccess = {
                        kickUserReason.value = ""
                        error.value = null
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
        }.invokeOnCompletion { _membershipChanging.value = false }
    }

    override fun banUser() {
        coroutineScope.launch {
            if (_membershipChanging.getAndUpdate { true }) {
                error.value = i18n.userProfileMembershipChanging()
                return@launch
            }

            if (matrixClient.syncState.value == SyncState.ERROR) {
                error.value = i18n.settingsRoomMemberBanUserErrorOffline()
                return@launch
            }

            if (!iHavePowerToBanUser.value) {
                error.value = i18n.settingsRoomMemberBanUserErrorNotPossible()
                return@launch
            }

            matrixClient.api.room.banUser(
                roomId = selectedRoomId,
                userId = userId,
                reason = banReason.value.ifBlank { null },
            ).fold(
                onSuccess = {
                    banReason.value = ""
                    error.value = null
                },
                onFailure = {
                    if (it is CancellationException) {
                        return@launch
                    }

                    log.error(it) { "cannot ban user $userId from $selectedRoomId: ${it.message}" }
                    error.value = i18n.settingsRoomMemberBanUserError()
                }
            )
        }.invokeOnCompletion {
            _membershipChanging.value = false
        }
    }

    override fun unbanUser() {
        val roomUserId = userId
        coroutineScope.launch {
            if (_membershipChanging.getAndUpdate { true }) {
                error.value = i18n.userProfileMembershipChanging()
                return@launch
            }

            if (matrixClient.syncState.value == SyncState.ERROR) {
                error.value = i18n.settingsRoomMemberUnbanUserErrorOffline()
                return@launch
            }

            if (!iHavePowerToUnbanUser.value) {
                log.error { "cannot unban user $roomUserId from $selectedRoomId: User is not able to unban this member" }
                error.value = i18n.settingsRoomMemberUnbanUserErrorNotPossible()
                return@launch
            }

            matrixClient.api.room.unbanUser(
                roomId = selectedRoomId,
                userId = roomUserId,
                reason = unbanReason.value.ifBlank { null },
            ).fold(
                onSuccess = {
                    unbanReason.value = ""
                    error.value = null
                },
                onFailure = {
                    if (it is CancellationException) {
                        return@launch
                    }

                    log.error(it) { "cannot unban user $roomUserId from $selectedRoomId: ${it.message}" }
                    error.value = i18n.settingsRoomMemberUnbanUserError()
                }
            )
        }.invokeOnCompletion { _membershipChanging.value = false }
    }

    override fun blockUser() {
        coroutineScope.launch {
            try {
                blockingInProgress.value = true
                userBlocking.blockUser(matrixClient, userId) {
                    error.value = i18n.blockUserError(userId.full)
                }
            } finally {
                blockingInProgress.value = false
            }
        }
    }

    override fun unblockUser() {
        coroutineScope.launch {
            try {
                blockingInProgress.value = true
                userBlocking.unblockUser(matrixClient, userId) {
                    error.value = i18n.settingsUnblockUserError(userId.full)
                }
            } finally {
                blockingInProgress.value = false
            }
        }
    }

    private fun getPowerRole(powerLevel: Long): Role {
        return when {
            powerLevel >= Role.ADMIN.getMinPowerLevel() -> Role.ADMIN
            powerLevel >= Role.MODERATOR.getMinPowerLevel() -> Role.MODERATOR
            else -> Role.USER
        }
    }


    override val openingChat = MutableStateFlow(false)

    override fun openChat() {
        if (isMyself) {
            log.warn { "cannot open chat with yourself" }
            return
        }
        if (openingChat.compareAndSet(expect = false, update = true)) {
            coroutineScope.launch {
                getOrCreateRoom()?.also { roomId ->
                    goToRoom(matrixClient.userId, roomId)
                } ?: run {
                    error.value = i18n.createNewChatError()
                }
            }.invokeOnCompletion {
                openingChat.update { false }
            }
        }
    }

    override val verifying = MutableStateFlow(false)

    override fun startVerification() {
        if (isMyself) {
            log.warn { "cannot verify yourself" }
            return
        }
        if (verifying.compareAndSet(expect = false, update = true)) {
            coroutineScope.launch {
                val req = matrixClient.verification.createUserVerificationRequest(userId)
                    .fold(
                        onSuccess = { it },
                        onFailure = {
                            log.error(it) { "cannot verify user $userId" }
                            error.value = i18n.userVerificationNoMatch() // TODO
                            return@launch
                        }
                    )

                req.state.first(::isVerificationStateFinished)
            }.invokeOnCompletion {
                verifying.update { false }
            }
        }

    }

    private fun isVerificationStateFinished(verificationState: ActiveVerificationState) = when (verificationState) {
        ActiveVerificationState.Done,
        is ActiveVerificationState.Cancel,
        ActiveVerificationState.AcceptedByOtherDevice,
        ActiveVerificationState.Undefined -> true

        else -> false
    }

    private suspend fun getOrCreateRoom() =
        getExistingChat() ?: createNewChat()

    private suspend fun getExistingChat(): RoomId? =
        matrixClient.user.getAccountData<DirectEventContent>().firstOrNull()
            ?.mappings
            ?.get(userId)
            ?.firstNotNullOfOrNull { roomId -> roomId.takeIf { isUserNotLeft(it, userId) } }

    private suspend fun isUserNotLeft(roomId: RoomId, userId: UserId) =
        matrixClient.user.getById(roomId, userId).firstOrNull()
            ?.membership.let { membership ->
                membership == Membership.JOIN || membership == Membership.INVITE || membership == Membership.KNOCK
            }

    private suspend fun createNewChat(): RoomId? =
        matrixClient.api.room.createRoom(
            isDirect = true,
            invite = if (userId == matrixClient.userId) setOf() else setOf(userId),
            initialState = listOf(
                InitialStateEvent(EncryptionEventContent(), ""),
            ),
            preset = CreateRoom.Request.Preset.TRUSTED_PRIVATE
        ).fold(
            onSuccess = { it },
            onFailure = { log.error(it) { "could not create new direct chat" }; null }
        )
}


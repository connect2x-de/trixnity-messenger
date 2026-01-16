package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.LeaveRoom
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get

interface RoomSettingsViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        onCloseRoom: () -> Unit,
        onOpenAddMembers: () -> Unit,
        onOpenExportRoom: () -> Unit,
        onCloseRoomSettings: () -> Unit,
        onOpenUserProfile: (UserId) -> Unit,
        onOpenAvatarCutter: OpenAvatarCutterCallback,
        onOpenPowerLevel: () -> Unit,
        onOpenMention: OpenMentionCallback,
    ): RoomSettingsViewModel = RoomSettingsViewModelImpl(
        viewModelContext = viewModelContext,
        selectedRoomId = selectedRoomId,
        onOpenAddMembers = onOpenAddMembers,
        onOpenExportRoom = onOpenExportRoom,
        onCloseRoomSettings = onCloseRoomSettings,
        onOpenAvatarCutter = onOpenAvatarCutter,
        onCloseRoom = onCloseRoom,
        onOpenUserProfile = onOpenUserProfile,
        onOpenPowerLevel = onOpenPowerLevel,
        onOpenMention = onOpenMention,
    )

    companion object : RoomSettingsViewModelFactory
}

interface RoomSettingsViewModel {
    val roomId: RoomId
    val error: StateFlow<String?>
    val changeRoomAvatarViewModel: ChangeRoomAvatarViewModel
    val roomSettingsNameViewModel: RoomSettingsNameViewModel
    val roomSettingsTopicViewModel: RoomSettingsTopicViewModel
    val roomSettingsNotificationsViewModel: RoomSettingsNotificationsViewModel
    val roomSettingsHistoryVisibilityViewModel: RoomSettingsHistoryVisibilityViewModel
    val roomSettingsJoinRulesViewModel: RoomSettingsJoinRulesViewModel
    val roomSettingsSecurityViewModel: RoomSettingsSecurityViewModel
    val roomSettingsAliasViewModel: RoomSettingsAliasViewModel
    val memberListViewModel: MemberListViewModel
    val hasPowerToInvite: StateFlow<Boolean>
    val isDirect: StateFlow<Boolean>
    val isLeave: StateFlow<Boolean>
    val isEncrypted: StateFlow<Boolean>

    // Messages
    val leaveRoomSettingEntryText: StateFlow<String>
    val leaveRoomWarningOpen: StateFlow<Boolean>
    val leaveRoomWarningTitle: StateFlow<String>
    val leaveRoomWarningMessage: StateFlow<String>
    val leaveRoomWarningConfirmButtonText: StateFlow<String>

    fun openAddMembersView()
    fun openExportRoomView()
    fun openPowerLevelView()
    fun leaveRoom()
    fun forgetRoom()
    fun openLeaveRoomWarningDialog()
    fun closeLeaveRoomWarningDialog()
    fun close()
    fun openUserProfile(userId: UserId)
}

class RoomSettingsViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val onOpenAddMembers: () -> Unit,
    private val onOpenExportRoom: () -> Unit,
    private val onCloseRoomSettings: () -> Unit,
    private val onCloseRoom: () -> Unit,
    private val onOpenAvatarCutter: OpenAvatarCutterCallback,
    private val onOpenUserProfile: (UserId) -> Unit,
    private val onOpenPowerLevel: () -> Unit,
    private val onOpenMention: OpenMentionCallback,
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsViewModel {
    private val leaveRoom: LeaveRoom = get()

    private val backCallback = BackCallback {
        close()
    }

    init {
        registerBackCallback(backCallback)
    }

    override val roomId: RoomId
        get() = selectedRoomId

    override val error = MutableStateFlow<String?>(null)

    override val changeRoomAvatarViewModel: ChangeRoomAvatarViewModel by lazy {
        get<ChangeRoomAvatarViewModelFactory>()
            .create(viewModelContext, selectedRoomId, onOpenAvatarCutter)
    }

    override val roomSettingsNameViewModel by lazy {
        get<RoomSettingsNameViewModelFactory>()
            .create(viewModelContext, selectedRoomId)
    }
    override val roomSettingsTopicViewModel by lazy {
        get<RoomSettingsTopicViewModelFactory>()
            .create(viewModelContext, selectedRoomId, onOpenMention)
    }

    override val roomSettingsNotificationsViewModel: RoomSettingsNotificationsViewModel by lazy {
        get<RoomSettingsNotificationsViewModelFactory>()
            .create(viewModelContext, selectedRoomId, error)
    }

    override val roomSettingsHistoryVisibilityViewModel: RoomSettingsHistoryVisibilityViewModel by lazy {
        get<RoomSettingsHistoryVisibilityViewModelFactory>()
            .create(viewModelContext, selectedRoomId, error)
    }

    override val roomSettingsJoinRulesViewModel: RoomSettingsJoinRulesViewModel by lazy {
        get<RoomSettingsJoinRulesViewModelFactory>()
            .create(viewModelContext, selectedRoomId, error)
    }

    override val roomSettingsSecurityViewModel: RoomSettingsSecurityViewModel by lazy {
        get<RoomSettingsSecurityViewModelFactory>()
            .create(viewModelContext, selectedRoomId, error)
    }

    override val roomSettingsAliasViewModel: RoomSettingsAliasViewModel by lazy {
        get<RoomSettingsAliasViewModelFactory>()
            .create(viewModelContext, selectedRoomId, isDirect, error, error, error)
    }

    override val leaveRoomSettingEntryText = MutableStateFlow("")
    override val leaveRoomWarningTitle = MutableStateFlow("")
    override val leaveRoomWarningMessage = MutableStateFlow("")
    override val leaveRoomWarningConfirmButtonText = MutableStateFlow("")
    override val leaveRoomWarningOpen = MutableStateFlow(false)
    override val isDirect: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isEncrypted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isLeave: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val memberListViewModel: MemberListViewModel =
        get<MemberListViewModelFactory>().create(
            viewModelContext = childContext("memberList-${selectedRoomId}"),
            selectedRoomId = selectedRoomId,
            onOpenUserProfile = ::openUserProfile,
            error = error,
        )

    override val hasPowerToInvite: StateFlow<Boolean> =
        matrixClient.user.canInvite(selectedRoomId)
            .stateIn(coroutineScope, WhileSubscribed(), false)

    init {
        coroutineScope.launch {
            matrixClient.room.getById(selectedRoomId).collect {
                isDirect.value = it?.isDirect ?: false
                isEncrypted.value = it?.encrypted ?: false
                isLeave.value = it?.membership?.let { membership -> membership == Membership.LEAVE } ?: false

                if (isLeave.value) {
                    if (isDirect.value) {
                        leaveRoomSettingEntryText.value = i18n.settingsRoomForgetRoomMessageChat()
                        leaveRoomWarningTitle.value = i18n.settingsRoomForgetRoomWarningTitleChat()
                        leaveRoomWarningMessage.value = i18n.settingsRoomLeaveRoomWarningMessageChat()
                        leaveRoomWarningConfirmButtonText.value = i18n.settingsRoomForgetRoomWarningConfirmButtonChat()
                    } else {
                        leaveRoomSettingEntryText.value = i18n.settingsRoomForgetRoomMessageGroup()
                        leaveRoomWarningTitle.value = i18n.settingsRoomForgetRoomWarningTitleGroup()
                        leaveRoomWarningMessage.value = i18n.settingsRoomLeaveRoomWarningMessageGroup()
                        leaveRoomWarningConfirmButtonText.value = i18n.settingsRoomForgetRoomWarningConfirmButtonGroup()
                    }
                } else {
                    if (isDirect.value) {
                        leaveRoomSettingEntryText.value = i18n.settingsRoomLeaveRoomMessageChat()
                        leaveRoomWarningTitle.value = i18n.settingsRoomLeaveRoomWarningTitleChat()
                        leaveRoomWarningMessage.value = i18n.settingsRoomLeaveRoomWarningMessageChat()
                        leaveRoomWarningConfirmButtonText.value = i18n.settingsRoomLeaveRoomWarningConfirmButtonChat()

                    } else {
                        leaveRoomSettingEntryText.value = i18n.settingsRoomLeaveRoomMessageGroup()
                        leaveRoomWarningTitle.value = i18n.settingsRoomLeaveRoomWarningTitleGroup()
                        leaveRoomWarningMessage.value = i18n.settingsRoomLeaveRoomWarningMessageGroup()
                        leaveRoomWarningConfirmButtonText.value = i18n.settingsRoomLeaveRoomWarningConfirmButtonGroup()
                    }
                }
            }
        }
    }

    override fun leaveRoom() {
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                error.value = i18n.settingsRoomLeaveRoomErrorOffline()
                return@launch
            }

            leaveRoom(matrixClient, selectedRoomId, forget = false)
                .onSuccess { log.info { "successfully left room" } }
                .onFailure {
                    if (it is CancellationException) {
                        return@launch
                    }
                    log.error(it) { "cannot leave room $selectedRoomId" }
                    val groupOrChat =
                        if (isDirect.value) i18n.eventChangeChatGenitive()
                        else i18n.eventChangeGroupGenitive()
                    error.value = i18n.settingsRoomLeaveRoomError(groupOrChat)
                }
        }
    }

    override fun forgetRoom() {
        coroutineScope.launch {
            if (matrixClient.syncState.value != SyncState.RUNNING) {
                error.value = i18n.forgetRoomErrorOffline()
                return@launch
            }

            leaveRoom(matrixClient, selectedRoomId)
                .onSuccess { log.info { "successfully forgot room" } }
                .onFailure { log.error(it) { "failed to forget room" } }
            onCloseRoom()
        }
    }

    override fun openLeaveRoomWarningDialog() {
        leaveRoomWarningOpen.value = true
    }

    override fun closeLeaveRoomWarningDialog() {
        leaveRoomWarningOpen.value = false
    }

    override fun close() {
        onCloseRoomSettings()
    }

    override fun openAddMembersView() {
        onOpenAddMembers()
    }

    override fun openExportRoomView() {
        onOpenExportRoom()
    }

    override fun openUserProfile(userId: UserId) {
        onOpenUserProfile(userId)
    }

    override fun openPowerLevelView() {
        onOpenPowerLevel()
    }
}

class PreviewRoomSettingsViewModel : RoomSettingsViewModel {
    override val roomId = RoomId("")
    override val roomSettingsNameViewModel = PreviewRoomSettingsNameViewModel()
    override val roomSettingsTopicViewModel = PreviewRoomSettingsTopicViewModel()
    override val roomSettingsNotificationsViewModel = PreviewRoomSettingsNotificationsViewModel()
    override val roomSettingsAliasViewModel = PreviewRoomSettingsAliasViewModel()
    override val roomSettingsHistoryVisibilityViewModel = PreviewRoomSettingsHistoryVisibilityViewModel()
    override val roomSettingsJoinRulesViewModel = PreviewRoomSettingsJoinRulesViewModel()
    override val roomSettingsSecurityViewModel = PreviewRoomSettingsSecurityViewModel()
    override val error = MutableStateFlow(null)
    override val changeRoomAvatarViewModel = PreviewChangeAvatarViewModel()
    override val leaveRoomSettingEntryText = MutableStateFlow("leave room")
    override val leaveRoomWarningOpen = MutableStateFlow(false)
    override val leaveRoomWarningTitle = MutableStateFlow("leave room warning title")
    override val leaveRoomWarningMessage = MutableStateFlow("leave room warning message")
    override val leaveRoomWarningConfirmButtonText = MutableStateFlow("confirm")
    override val memberListViewModel = PreviewMemberListViewModel()
    override val hasPowerToInvite = MutableStateFlow(true)
    override val isDirect = MutableStateFlow(true)
    override val isEncrypted = MutableStateFlow(false)
    override val isLeave = MutableStateFlow(false)
    override fun openAddMembersView() {}
    override fun openExportRoomView() {}
    override fun openPowerLevelView() {}
    override fun openUserProfile(userId: UserId) {}
    override fun leaveRoom() {}
    override fun forgetRoom() {}
    override fun openLeaveRoomWarningDialog() {}
    override fun closeLeaveRoomWarningDialog() {}
    override fun close() {}
}

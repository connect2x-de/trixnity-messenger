package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get


private val log = KotlinLogging.logger { }

interface RoomSettingsViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        onBack: () -> Unit,
        onShowAddMembers: () -> Unit,
        onShowExportRoom: () -> Unit,
        onCloseRoomSettings: () -> Unit,
    ): RoomSettingsViewModel {
        return RoomSettingsViewModelImpl(
            viewModelContext = viewModelContext,
            selectedRoomId = selectedRoomId,
            onShowAddMembers = onShowAddMembers,
            onShowExportRoom = onShowExportRoom,
            onCloseRoomSettings = onCloseRoomSettings,
            onBack = onBack,
        )
    }

    companion object : RoomSettingsViewModelFactory
}

interface RoomSettingsViewModel {
    val error: StateFlow<String?>
    val roomSettingsNameViewModel: RoomSettingsNameViewModel
    val roomSettingsTopicViewModel: RoomSettingsTopicViewModel
    val roomSettingsNotificationsViewModel: RoomSettingsNotificationsViewModel
    val leaveRoomSettingEntryText: StateFlow<String>
    val leaveRoomWarningOpen: StateFlow<Boolean>
    val leaveRoomWarningTitle: StateFlow<String>
    val leaveRoomWarningMessage: StateFlow<String>
    val leaveRoomWarningConfirmButtonText: StateFlow<String>
    val memberListViewModel: MemberListViewModel
    val hasPowerToInvite: StateFlow<Boolean>

    fun openAddMembersView()
    fun openExportRoomView()
    fun leaveRoom()
    fun openLeaveRoomWarningDialog()
    fun closeLeaveRoomWarningDialog()
    fun close()
}

open class RoomSettingsViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val onShowAddMembers: () -> Unit,
    private val onShowExportRoom: () -> Unit,
    private val onCloseRoomSettings: () -> Unit,
    private val onBack: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsViewModel {
    override val error = MutableStateFlow<String?>(null)
    override val roomSettingsNameViewModel by lazy {
        get<RoomSettingsNameViewModelFactory>()
            .create(viewModelContext, selectedRoomId)
    }
    override val roomSettingsTopicViewModel by lazy {
        get<RoomSettingsTopicViewModelFactory>()
            .create(viewModelContext, selectedRoomId)
    }

    override val roomSettingsNotificationsViewModel: RoomSettingsNotificationsViewModel by lazy {
        get<RoomSettingsNotificationsViewModelFactory>()
            .create(viewModelContext, selectedRoomId, error)
    }

    override val leaveRoomSettingEntryText = MutableStateFlow("")
    override val leaveRoomWarningTitle = MutableStateFlow("")
    override val leaveRoomWarningMessage = MutableStateFlow("")
    override val leaveRoomWarningConfirmButtonText = MutableStateFlow("")

    override val leaveRoomWarningOpen = MutableStateFlow(false)

    protected val isDirect: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val memberListViewModel: MemberListViewModel =
        get<MemberListViewModelFactory>().create(
            viewModelContext = childContext("memberList-${selectedRoomId}"),
            selectedRoomId = selectedRoomId, error = error
        )

    override val hasPowerToInvite: StateFlow<Boolean> =
        matrixClient.user.canInvite(selectedRoomId).stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    init {
        coroutineScope.launch {
            matrixClient.room.getById(selectedRoomId).collect {
                isDirect.value = it?.isDirect ?: false
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

    override fun leaveRoom() {
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                error.value = i18n.settingsRoomLeaveRoomErrorOffline()
            } else {
                matrixClient.api.room.leaveRoom(selectedRoomId).fold(
                    onSuccess = { onBack() },
                    onFailure = {
                        if (it is CancellationException) {
                            return@launch
                        }
                        log.error(it) { "cannot leave room $selectedRoomId" }
                        val groupOrChat =
                            if (isDirect.value) i18n.eventChangeChatGenitive()
                            else i18n.eventChangeGroupGenitive()
                        error.value =
                            i18n.settingsRoomLeaveRoomError(groupOrChat)
                    }
                )
            }
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
        onShowAddMembers()
    }

    override fun openExportRoomView() {
        onShowExportRoom()
    }
}

class PreviewRoomSettingsViewModel : RoomSettingsViewModel {
    override val roomSettingsNameViewModel: RoomSettingsNameViewModel = PreviewRoomSettingsNameViewModel()
    override val roomSettingsTopicViewModel: RoomSettingsTopicViewModel = PreviewRoomSettingsTopicViewModel()
    override val roomSettingsNotificationsViewModel: RoomSettingsNotificationsViewModel =
        PreviewRoomSettingsNotificationsViewModel()
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val leaveRoomSettingEntryText: MutableStateFlow<String> = MutableStateFlow("leave room")
    override val leaveRoomWarningOpen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val leaveRoomWarningTitle: MutableStateFlow<String> = MutableStateFlow("leave room warning title")
    override val leaveRoomWarningMessage: MutableStateFlow<String> = MutableStateFlow("leave room warning message")
    override val leaveRoomWarningConfirmButtonText: MutableStateFlow<String> = MutableStateFlow("confirm")
    override val memberListViewModel: MemberListViewModel = PreviewMemberListViewModel()
    override val hasPowerToInvite: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override fun openAddMembersView() {
    }

    override fun openExportRoomView() {
    }

    override fun leaveRoom() {
    }

    override fun openLeaveRoomWarningDialog() {
    }

    override fun closeLeaveRoomWarningDialog() {
    }

    override fun close() {
    }
}

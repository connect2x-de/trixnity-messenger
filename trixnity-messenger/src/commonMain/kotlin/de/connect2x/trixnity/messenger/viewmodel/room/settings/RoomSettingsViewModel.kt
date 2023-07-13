package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.util.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.clientserverapi.model.push.SetPushRule
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import net.folivo.trixnity.core.model.events.m.room.get
import net.folivo.trixnity.core.model.push.PushAction
import net.folivo.trixnity.core.model.push.PushCondition
import net.folivo.trixnity.core.model.push.PushRuleKind
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface RoomSettingsViewModelFactory {
    fun newRoomSettingsViewModel(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        onBack: () -> Unit,
        onShowAddMembers: () -> Unit,
        onCloseRoomSettings: () -> Unit,
    ): RoomSettingsViewModel {
        return RoomSettingsViewModelImpl(
            viewModelContext = viewModelContext,
            selectedRoomId = selectedRoomId,
            onShowAddMembers = onShowAddMembers,
            onCloseRoomSettings = onCloseRoomSettings,
            onBack = onBack,
        )
    }
}

interface RoomSettingsViewModel {
    val roomName: StateFlow<String?>
    val canChangeRoomName: StateFlow<Boolean>
    val roomNotificationLevels: Map<NotificationLevels, NotificationLevel>
    val selectedRoomNotificationsLevel: StateFlow<NotificationLevel>
    val isNotificationsLevelLoading: StateFlow<Boolean>
    val error: StateFlow<String?>
    val leaveRoomSettingEntryText: StateFlow<String>
    val leaveRoomWarningOpen: StateFlow<Boolean>
    val leaveRoomWarningTitle: StateFlow<String>
    val leaveRoomWarningMessage: StateFlow<String>
    val leaveRoomWarningConfirmButtonText: StateFlow<String>
    val memberListViewModel: MemberListViewModel
    val hasPowerToInvite: StateFlow<Boolean>

    fun changeRoomNameTo(roomName: String)
    fun openAddMembersView()
    fun changeSelectedRoomNotificationsLevel(newLevel: NotificationLevel)
    fun leaveRoom()
    fun openLeaveRoomWarningDialog()
    fun closeLeaveRoomWarningDialog()

    fun close()
}

open class RoomSettingsViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val onShowAddMembers: () -> Unit,
    private val onCloseRoomSettings: () -> Unit,
    private val onBack: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsViewModel {

    override val roomName: StateFlow<String?> = matrixClient.room.getById(selectedRoomId).map { it?.name?.explicitName }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val canChangeRoomName: StateFlow<Boolean> =
        combine(
            matrixClient.room.getState<PowerLevelsEventContent>(selectedRoomId, stateKey = ""),
            matrixClient.user.getPowerLevel(selectedRoomId, matrixClient.userId)
        ) { powerLevelEventContentEvent, userPowerLevel ->
            val nameEventContentPowerLevelNecessary =
                powerLevelEventContentEvent?.content?.events?.get(NameEventContent::class)
            val defaultPowerLevelNecessary = powerLevelEventContentEvent?.content?.stateDefault ?: 50
            log.trace { "$userPowerLevel >= $nameEventContentPowerLevelNecessary & $defaultPowerLevelNecessary" }
            if (nameEventContentPowerLevelNecessary != null) {
                userPowerLevel >= nameEventContentPowerLevelNecessary
            } else {
                userPowerLevel >= defaultPowerLevelNecessary
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val roomNotificationLevels = mapOf(
        NotificationLevels.ALL to NotificationLevel(i18n, NotificationLevels.ALL),
        NotificationLevels.MENTIONS to NotificationLevel(i18n, NotificationLevels.MENTIONS),
        NotificationLevels.SILENT to NotificationLevel(i18n, NotificationLevels.SILENT)
    )
    override val selectedRoomNotificationsLevel: StateFlow<NotificationLevel>
    override val isNotificationsLevelLoading = MutableStateFlow(false)
    override val error = MutableStateFlow<String?>(null)

    override val leaveRoomSettingEntryText = MutableStateFlow("")
    override val leaveRoomWarningTitle = MutableStateFlow("")
    override val leaveRoomWarningMessage = MutableStateFlow("")
    override val leaveRoomWarningConfirmButtonText = MutableStateFlow("")

    override val leaveRoomWarningOpen = MutableStateFlow(false)

    private val isDirect: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val memberListViewModel: MemberListViewModel =
        get<MemberListViewModelFactory>().newMemberListViewModel(
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

        selectedRoomNotificationsLevel =
            matrixClient.user.getAccountData<PushRulesEventContent>().map { prs ->
                prs?.let { pushRules ->
                    val notifyMeInMentions = pushRules.global?.get(PushRuleKind.CONTENT)?.any { pushRule ->
                        pushRule.enabled && pushRule.ruleId == ".m.rule.contains_user_name" &&
                                pushRule.actions.any { pushAction -> pushAction == PushAction.Notify }
                    } ?: false

                    val dontNotifyRoom =
                        pushRules.global?.get(PushRuleKind.ROOM)
                            ?.find { pushRule -> pushRule.ruleId == selectedRoomId.full && pushRule.enabled }
                            ?.actions?.any { it == PushAction.DontNotify } == true
                    val dontNotifyOverride =
                        pushRules.global?.get(PushRuleKind.OVERRIDE)?.find { pushRule ->
                            pushRule.conditions?.any { pushCondition ->
                                pushCondition is PushCondition.EventMatch &&
                                        pushCondition.key == "room_id" &&
                                        pushCondition.pattern == selectedRoomId.full
                            } ?: false
                                    && pushRule.conditions?.size == 1
                                    && pushRule.enabled
                        }?.actions?.any { it == PushAction.DontNotify } == true

                    if (dontNotifyOverride || dontNotifyRoom && notifyMeInMentions.not()) {
                        roomNotificationLevels.getValue(NotificationLevels.SILENT)
                    } else if (dontNotifyRoom && notifyMeInMentions) {
                        roomNotificationLevels.getValue(NotificationLevels.MENTIONS)
                    } else {
                        roomNotificationLevels.getValue(NotificationLevels.ALL)
                    }
                } ?: roomNotificationLevels.getValue(NotificationLevels.ALL)
            }.stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(),
                roomNotificationLevels.getValue(NotificationLevels.ALL)
            )
    }

    override fun changeRoomNameTo(roomName: String) {
        if (canChangeRoomName.value) {
            coroutineScope.launch {
                matrixClient.api.rooms.sendStateEvent(selectedRoomId, NameEventContent(roomName), stateKey = "")
                    .onFailure {
                        log.error(it) { "cannot change the room name to '$roomName'" }
                        error.value = i18n.settingsRoomChangeNameError()
                    }
                    .onSuccess {
                        log.debug { "changed room name to '$roomName'" }
                        error.value = null
                    }
            }
        }
    }

    override fun changeSelectedRoomNotificationsLevel(newLevel: NotificationLevel) {
        coroutineScope.launch {
            error.value = null
            isNotificationsLevelLoading.value = true

            when (selectedRoomNotificationsLevel.value.key) {
                NotificationLevels.SILENT -> when (newLevel.key) {
                    NotificationLevels.SILENT -> {}
                    NotificationLevels.ALL -> {
                        deletePush(PushRuleKind.OVERRIDE)
                    }

                    NotificationLevels.MENTIONS -> {
                        deletePush(PushRuleKind.OVERRIDE)
                        setPush(PushRuleKind.ROOM)
                    }
                }

                NotificationLevels.MENTIONS -> when (newLevel.key) {
                    NotificationLevels.SILENT -> {
                        deletePush(PushRuleKind.ROOM)
                        setPush(PushRuleKind.OVERRIDE)
                    }

                    NotificationLevels.ALL -> {
                        deletePush(PushRuleKind.ROOM)
                    }

                    NotificationLevels.MENTIONS -> {}
                }

                NotificationLevels.ALL -> when (newLevel.key) {
                    NotificationLevels.SILENT -> {
                        setPush(PushRuleKind.OVERRIDE)
                    }

                    NotificationLevels.ALL -> {}
                    NotificationLevels.MENTIONS -> {
                        setPush(PushRuleKind.ROOM)
                    }
                }
            }

            isNotificationsLevelLoading.value = false
        }
    }

    private suspend fun deletePush(kind: PushRuleKind) {
        matrixClient.api.push.deletePushRule(
            "global",
            kind,
            selectedRoomId.full,
        ).onFailure { exception ->
            log.error(exception) { "cannot delete push notification rule: $kind (${selectedRoomId.full})" }
            error.value = i18n.settingsRoomNotificationsError()
        }
    }

    private suspend fun setPush(kind: PushRuleKind) {
        matrixClient.api.push.setPushRule(
            "global",
            kind,
            selectedRoomId.full,
            SetPushRule.Request(
                actions = setOf(PushAction.DontNotify),
                conditions =
                if (kind == PushRuleKind.OVERRIDE)
                    setOf(PushCondition.EventMatch(key = "room_id", pattern = selectedRoomId.full))
                else
                    setOf()
            ),
        ).onFailure { exception ->
            log.error(exception) { "Cannot add push notification rule: $kind (${selectedRoomId.full})" }
            error.value = i18n.settingsRoomNotificationsError()
        }
    }

    override fun leaveRoom() {
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                error.value = i18n.settingsRoomLeaveRoomErrorOffline()
            } else {
                matrixClient.api.rooms.leaveRoom(selectedRoomId).fold(
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
}


enum class NotificationLevels(val key: String) {
    ALL("ALL"),
    MENTIONS("MENTIONS"),
    SILENT("SILENT")
}

class NotificationLevel(i18n: I18n, val key: NotificationLevels) {
    val name = MutableStateFlow("")
    val explanation = MutableStateFlow("")

    init {
        name.value = when (key) {
            NotificationLevels.ALL -> i18n.settingsRoomNotificationsAll()
            NotificationLevels.MENTIONS -> i18n.settingsRoomNotificationsMentions()
            NotificationLevels.SILENT -> i18n.settingsRoomNotificationsSilent()
        }

        explanation.value = when (key) {
            NotificationLevels.ALL -> i18n.settingsRoomNotificationsAllExplanation()
            NotificationLevels.MENTIONS -> i18n.settingsRoomNotificationsMentionsExplanation()
            NotificationLevels.SILENT -> i18n.settingsRoomNotificationsSilentExplanation()
        }
    }
}


package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.clientserverapi.model.push.SetPushRule
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.push.PushAction
import net.folivo.trixnity.core.model.push.PushCondition
import net.folivo.trixnity.core.model.push.PushRuleKind

private val log = KotlinLogging.logger { }

interface RoomSettingsNotificationsViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        error: MutableStateFlow<String?>,
    ): RoomSettingsNotificationsViewModel {
        return RoomSettingsNotificationsViewModelImpl(
            viewModelContext,
            selectedRoomId,
            error,
        )
    }

    companion object : RoomSettingsNotificationsViewModelFactory
}

interface RoomSettingsNotificationsViewModel {
    val roomNotificationLevels: Map<NotificationLevels, NotificationLevel>
    val selectedRoomNotificationsLevel: StateFlow<NotificationLevel>
    val isNotificationsLevelLoading: StateFlow<Boolean>
    fun changeSelectedRoomNotificationsLevel(newLevel: NotificationLevel)
}

open class RoomSettingsNotificationsViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val error: MutableStateFlow<String?>,
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsNotificationsViewModel {
    override val roomNotificationLevels = mapOf(
        NotificationLevels.ALL to NotificationLevelImpl(i18n, NotificationLevels.ALL),
        NotificationLevels.MENTIONS to NotificationLevelImpl(i18n, NotificationLevels.MENTIONS),
        NotificationLevels.SILENT to NotificationLevelImpl(i18n, NotificationLevels.SILENT),
        NotificationLevels.DEFAULT to NotificationLevelImpl(i18n, NotificationLevels.DEFAULT),
    )
    override val selectedRoomNotificationsLevel: StateFlow<NotificationLevel> =
        matrixClient.user.getAccountData<PushRulesEventContent>().map { prs ->
            prs?.let { pushRules ->
                val roomActions =
                    pushRules.global?.room
                        ?.filter { it.enabled }
                        ?.find { pushRule -> pushRule.roomId == selectedRoomId }
                        ?.actions
                        ?.filter { it !is PushAction.Unknown }

                val overrideActions =
                    pushRules.global?.override
                        ?.filter { it.enabled }
                        ?.find { pushRule ->
                            pushRule.conditions?.any { pushCondition ->
                                pushCondition == PushCondition.EventMatch(
                                    key = "room_id",
                                    pattern = selectedRoomId.full
                                )
                            } ?: false
                                    && pushRule.conditions?.size == 1
                        }?.actions
                        ?.filter { it !is PushAction.Unknown }

                val level = when {
                    overrideActions == null && roomActions != null && roomActions.isNotEmpty() ->
                        NotificationLevels.ALL

                    overrideActions == null && roomActions != null && roomActions.isEmpty() ->
                        NotificationLevels.MENTIONS

                    roomActions == null && overrideActions != null && overrideActions.isEmpty() ->
                        NotificationLevels.SILENT

                    else -> NotificationLevels.DEFAULT
                }
                roomNotificationLevels.getValue(level)
            } ?: roomNotificationLevels.getValue(NotificationLevels.DEFAULT)
        }.stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(),
            roomNotificationLevels.getValue(NotificationLevels.ALL)
        )
    override val isNotificationsLevelLoading = MutableStateFlow(false)
    
    override fun changeSelectedRoomNotificationsLevel(newLevel: NotificationLevel) {
        coroutineScope.launch {
            error.value = null
            isNotificationsLevelLoading.value = true

            when (newLevel.key) {
                NotificationLevels.DEFAULT -> {
                    deleteRoomPush()
                    deleteOverridePush()
                }

                NotificationLevels.ALL -> {
                    deleteOverridePush()
                    setRoomPush(true)
                }

                NotificationLevels.MENTIONS -> {
                    deleteOverridePush()
                    setRoomPush(false)
                }

                NotificationLevels.SILENT -> {
                    deleteRoomPush()
                    setOverridePush()
                }

            }

            isNotificationsLevelLoading.value = false
        }
    }

    private suspend fun setRoomPush(notify: Boolean) {
        matrixClient.api.push.setPushRule(
            "global",
            PushRuleKind.ROOM,
            selectedRoomId.full,
            SetPushRule.Request(
                conditions = setOf(),
                actions = if (notify) setOf(PushAction.Notify, PushAction.SetSoundTweak("default")) else setOf(),
            ),
        ).onFailure { exception ->
            log.error(exception) { "Cannot add room push notification rule: (${selectedRoomId.full})" }
            error.value = i18n.settingsRoomNotificationsError()
        }
    }

    private suspend fun setOverridePush() {
        matrixClient.api.push.setPushRule(
            "global",
            PushRuleKind.OVERRIDE,
            selectedRoomId.full,
            SetPushRule.Request(
                conditions = setOf(PushCondition.EventMatch(key = "room_id", pattern = selectedRoomId.full)),
                actions = setOf(),
            ),
        ).onFailure { exception ->
            log.error(exception) { "Cannot add override push notification rule: (${selectedRoomId.full})" }
            error.value = i18n.settingsRoomNotificationsError()
        }
    }

    private suspend fun deleteRoomPush() {
        matrixClient.api.push.deletePushRule(
            "global",
            PushRuleKind.ROOM,
            selectedRoomId.full,
        ).onFailure { exception ->
            // we could just prevent calling the function at all, when rule already deleted
            if (exception is MatrixServerException && exception.statusCode == HttpStatusCode.NotFound) return
            log.error(exception) { "cannot delete room push notification rule: (${selectedRoomId.full})" }
            error.value = i18n.settingsRoomNotificationsError()
        }
    }

    private suspend fun deleteOverridePush() {
        matrixClient.api.push.deletePushRule(
            "global",
            PushRuleKind.OVERRIDE,
            selectedRoomId.full,
        ).onFailure { exception ->
            // we could just prevent calling the function at all, when rule already deleted
            if (exception is MatrixServerException && exception.statusCode == HttpStatusCode.NotFound) return
            log.error(exception) { "cannot delete override push notification rule: (${selectedRoomId.full})" }
            error.value = i18n.settingsRoomNotificationsError()
        }
    }
}


enum class NotificationLevels(val key: String) {
    DEFAULT("DEFAULT"),
    ALL("ALL"),
    MENTIONS("MENTIONS"),
    SILENT("SILENT"),
}

interface NotificationLevel {
    val key: NotificationLevels
    val name: MutableStateFlow<String>
    val explanation: MutableStateFlow<String>
}

class NotificationLevelImpl(i18n: I18n, override val key: NotificationLevels) : NotificationLevel {
    override val name = MutableStateFlow("")
    override val explanation = MutableStateFlow("")

    init {
        name.value = when (key) {
            NotificationLevels.ALL -> i18n.settingsRoomNotificationsAll()
            NotificationLevels.MENTIONS -> i18n.settingsRoomNotificationsMentions()
            NotificationLevels.SILENT -> i18n.settingsRoomNotificationsSilent()
            NotificationLevels.DEFAULT -> i18n.settingsRoomNotificationsDefault()
        }

        explanation.value = when (key) {
            NotificationLevels.ALL -> i18n.settingsRoomNotificationsAllExplanation()
            NotificationLevels.MENTIONS -> i18n.settingsRoomNotificationsMentionsExplanation()
            NotificationLevels.SILENT -> i18n.settingsRoomNotificationsSilentExplanation()
            NotificationLevels.DEFAULT -> i18n.settingsRoomNotificationsDefaultExplanation()
        }
    }
}

class PreviewRoomSettingsNotificationsViewModel : RoomSettingsNotificationsViewModel {
    class NotificationLevelAll : NotificationLevel {
        override val key: NotificationLevels = NotificationLevels.ALL
        override val name: MutableStateFlow<String> = MutableStateFlow("all")
        override val explanation: MutableStateFlow<String> = MutableStateFlow("everything")
    }

    class NotificationLevelMentions : NotificationLevel {
        override val key: NotificationLevels = NotificationLevels.MENTIONS
        override val name: MutableStateFlow<String> = MutableStateFlow("mentions")
        override val explanation: MutableStateFlow<String> = MutableStateFlow("something")
    }

    class NotificationLevelSilent : NotificationLevel {
        override val key: NotificationLevels = NotificationLevels.SILENT
        override val name: MutableStateFlow<String> = MutableStateFlow("silent")
        override val explanation: MutableStateFlow<String> = MutableStateFlow("nothing")
    }

    override val roomNotificationLevels: Map<NotificationLevels, NotificationLevel> = mapOf(
        NotificationLevels.ALL to NotificationLevelAll(),
        NotificationLevels.MENTIONS to NotificationLevelMentions(),
        NotificationLevels.SILENT to NotificationLevelSilent(),
        NotificationLevels.DEFAULT to NotificationLevelMentions(),
    )

    override val selectedRoomNotificationsLevel: MutableStateFlow<NotificationLevel> =
        MutableStateFlow(NotificationLevelSilent())

    override val isNotificationsLevelLoading: MutableStateFlow<Boolean> =
        MutableStateFlow(false)

    override fun changeSelectedRoomNotificationsLevel(newLevel: NotificationLevel) {}
}

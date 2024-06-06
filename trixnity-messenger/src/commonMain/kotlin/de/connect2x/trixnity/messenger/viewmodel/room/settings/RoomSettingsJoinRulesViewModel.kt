package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

interface RoomSettingsJoinRulesViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        error: MutableStateFlow<String?>
    ): RoomSettingsJoinRulesViewModel {
        return RoomSettingsJoinRulesViewModelImpl(selectedRoomId, viewModelContext, error)
    }

    companion object : RoomSettingsJoinRulesViewModelFactory
}

interface RoomSettingsJoinRulesViewModel {
    val availableRoomJoinStates: StateFlow<List<JoinRulesEventContent.JoinRule>?>
    val joinRule: StateFlow<JoinRulesEventContent.JoinRule>
    val canChangeJoinRule: StateFlow<Boolean>
    val isJoinRuleChanging: StateFlow<Boolean>

    fun changeJoinRule(newJoinRule: JoinRulesEventContent.JoinRule)
}

class RoomSettingsJoinRulesViewModelImpl(
    private val selectedRoom: RoomId,
    private val viewModelContext: MatrixClientViewModelContext,
    private val error: MutableStateFlow<String?>
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsJoinRulesViewModel {

    override val availableRoomJoinStates: StateFlow<List<JoinRulesEventContent.JoinRule>?> =
        flowOf(
            listOf(
                JoinRulesEventContent.JoinRule.Public,
                JoinRulesEventContent.JoinRule.Invite,
                JoinRulesEventContent.JoinRule.Knock,
                JoinRulesEventContent.JoinRule.Restricted,
                JoinRulesEventContent.JoinRule.KnockRestricted
            )
        ).stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val joinRule: StateFlow<JoinRulesEventContent.JoinRule> =
        matrixClient.room.getState<JoinRulesEventContent>(selectedRoom)
            .map { it?.content?.joinRule ?: JoinRulesEventContent.JoinRule.Public }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), JoinRulesEventContent.JoinRule.Public)

    override val canChangeJoinRule: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<JoinRulesEventContent>(selectedRoom)
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override val isJoinRuleChanging = MutableStateFlow(false)

    override fun changeJoinRule(newJoinRule: JoinRulesEventContent.JoinRule) {
        log.debug { "change RoomJoin for $selectedRoom to $newJoinRule" }

        if (canChangeJoinRule.value && !isJoinRuleChanging.value) {
            isJoinRuleChanging.value = true
            coroutineScope.launch {
                matrixClient.api.room.sendStateEvent(selectedRoom, JoinRulesEventContent(newJoinRule))
                    .onFailure {
                        log.error(it) { "Failed to change room join rules: ${it.message}" }
                        error.value = i18n.settingsRoomJoinRulesChangeError()
                        isJoinRuleChanging.value = false
                    }
                    .onSuccess {
                        error.value = null
                        withTimeoutOrNull(5.seconds) {
                            matrixClient.room.getState<JoinRulesEventContent>(selectedRoom)
                                .first { it?.content?.joinRule == newJoinRule }
                        }
                        isJoinRuleChanging.value = false
                    }
            }
        } else {
            log.error { "Insufficient power level to change room join rules" }
            error.value = i18n.settingsRoomJoinRulesInsufficientPowerLevel()
            isJoinRuleChanging.value = false

        }
    }

}

class PreviewRoomSettingsJoinRulesViewModel : RoomSettingsJoinRulesViewModel {
    override val availableRoomJoinStates: StateFlow<List<JoinRulesEventContent.JoinRule>?> =
        MutableStateFlow(
            listOf(
                JoinRulesEventContent.JoinRule.Public,
                JoinRulesEventContent.JoinRule.Private,
                JoinRulesEventContent.JoinRule.Invite,
                JoinRulesEventContent.JoinRule.Knock,
                JoinRulesEventContent.JoinRule.KnockRestricted,
                JoinRulesEventContent.JoinRule.Restricted
            )
        )
    override val canChangeJoinRule: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isJoinRuleChanging: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val joinRule: MutableStateFlow<JoinRulesEventContent.JoinRule> =
        MutableStateFlow(JoinRulesEventContent.JoinRule.Public)

    override fun changeJoinRule(newJoinRule: JoinRulesEventContent.JoinRule) {
        joinRule.value = newJoinRule
    }

}

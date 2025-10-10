package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.PowerLevel
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.EventType
import net.folivo.trixnity.core.model.events.StateEventContent
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent

interface PowerlevelViewModelFactory {
    companion object : PowerlevelViewModelFactory

    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId,
        onBack: () -> Unit,
    ): PowerlevelViewModel = PowerlevelViewModelImpl(
        viewModelContext = viewModelContext,
        roomId = roomId,
        onBack = onBack,
    )
}

interface PowerlevelViewModel {
    val error: StateFlow<String?>
    fun errorDismiss()

    fun back()

    val powerLevels: StateFlow<PowerLevels?>
    fun setPowerLevels(powerLevels: PowerLevels)

    val canChangePowerLevels: StateFlow<Boolean>
}

@OptIn(ExperimentalCoroutinesApi::class)
class PowerlevelViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val roomId: RoomId,
    private val onBack: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, PowerlevelViewModel {
    override fun back() = onBack()

    val state = matrixClient.room.getState(roomId, PowerLevelsEventContent::class).mapLatest { it?.content }
        .stateIn(coroutineScope, WhileSubscribed(), null)

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override fun errorDismiss() {
        error.value = null
    }

    private val userPowerLevel = matrixClient.user.getPowerLevel(roomId, matrixClient.userId)
    private val userPowerLevelLong = userPowerLevel.mapLatest {
        when (it) {
            is PowerLevel.Creator -> Long.MAX_VALUE
            is PowerLevel.User -> it.level
        }
    }
    override val canChangePowerLevels = combine(
        matrixClient.user.canSendEvent(roomId, PowerLevelsEventContent::class),
        userPowerLevelLong,
    ) { canSend, powerLevel -> canSend && powerLevel > 0 }.stateIn(coroutineScope, WhileSubscribed(), false)

    override val powerLevels: StateFlow<PowerLevels?> = combine(
        state, userPowerLevelLong
    ) { state, userPowerLevel ->
        if (state == null) return@combine null
        PowerLevels(
            ban = CurrentMax(current = state.ban, max = userPowerLevel),
            eventsDefault = CurrentMax(current = state.eventsDefault, max = userPowerLevel),
            invite = CurrentMax(current = state.invite, max = userPowerLevel),
            kick = CurrentMax(current = state.kick, max = userPowerLevel),
            redact = CurrentMax(current = state.redact, max = userPowerLevel),
            stateDefault = CurrentMax(current = state.stateDefault, max = userPowerLevel),
            usersDefault = CurrentMax(current = state.usersDefault, max = userPowerLevel),
            events = state.events.mapValues { (event, pl) ->
                CurrentMax(current = pl, max = maxPowerLevel(event, state, userPowerLevel))
            },
            content = state,
            // notifications = TODO()
        )
    }.stateIn(coroutineScope, WhileSubscribed(), null)

    override fun setPowerLevels(powerLevels: PowerLevels) {
        coroutineScope.launch {
            error.value = matrixClient.api.room.sendStateEvent(roomId, powerLevels.toContent()).exceptionOrNull()?.let {
                when (it) {
                    is MatrixServerException -> it.errorResponse.error
                    else -> it.message
                }
            }
        }
    }

    private fun maxPowerLevel(event: EventType, state: PowerLevelsEventContent, powerLevel: Long): Long? {
        if (powerLevel == Long.MAX_VALUE) return powerLevel
        if (state.events.containsKey(event)) {
            return if (state.events.getValue(event) > powerLevel) null
            else state.events.getValue(event)
        }
        val klass = event.kClass ?: return null
        return when (klass) {
            is StateEventContent -> if (state.stateDefault > powerLevel) null else state.stateDefault
            else -> null
        }
    }
}

data class CurrentMax(
    val current: Long,
    val max: Long?,
)

data class PowerLevels(
    val ban: CurrentMax,
    val events: Map<EventType, CurrentMax>,
    val eventsDefault: CurrentMax,
    val invite: CurrentMax,
    val kick: CurrentMax,
    val redact: CurrentMax,
    val stateDefault: CurrentMax,
    val usersDefault: CurrentMax,
//    val notifications: Map<String, CurrMax>,
    private val content: PowerLevelsEventContent,
) {
    fun toContent(): PowerLevelsEventContent = PowerLevelsEventContent(
        ban = ban.current,
        invite = invite.current,
        kick = kick.current,
        redact = redact.current,
        usersDefault = usersDefault.current,
        eventsDefault = eventsDefault.current,
        stateDefault = stateDefault.current,
        events = events.mapValues { it.value.current },

        users = content.users,
        notifications = content.notifications,
        externalUrl = content.externalUrl,
    )
}



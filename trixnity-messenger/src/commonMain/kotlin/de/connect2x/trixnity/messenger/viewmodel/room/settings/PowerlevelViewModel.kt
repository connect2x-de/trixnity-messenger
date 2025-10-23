package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.scopedMapLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
import net.folivo.trixnity.core.serialization.events.EventContentSerializerMapping
import net.folivo.trixnity.core.serialization.events.EventContentSerializerMappings

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

    val knownEvents: StateFlow<Set<EventContentSerializerMapping<*>>>

    fun setPowerLevels()
    fun resetPowerLevels()

    val canChangePowerLevels: StateFlow<Boolean>
    fun newEvent(type: EventType)

    val ban: Value
    val eventsDefault: Value
    val invite: Value
    val kick: Value
    val redact: Value
    val stateDefault: Value
    val usersDefault: Value
    val inputError: StateFlow<Boolean>
    val isAnyInputModified: StateFlow<Boolean>
    val events: StateFlow<Map<EventType, Value>>
}

class PowerlevelViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val roomId: RoomId,
    private val onBack: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, PowerlevelViewModel {
    override fun back() = onBack()

    private val i18n = viewModelContext.i18n
    private val defaultPowerLevelsEventContent = PowerLevelsEventContent()

    private val state = matrixClient.room.getState(roomId, PowerLevelsEventContent::class)
        .map { it?.content }
        .stateIn(coroutineScope, WhileSubscribed(), null)

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override fun errorDismiss() {
        error.value = null
    }

    private val userPowerLevelLong = matrixClient.user.getPowerLevel(roomId, matrixClient.userId).map {
        when (it) {
            is PowerLevel.Creator -> Long.MAX_VALUE
            is PowerLevel.User -> it.level
        }
    }.stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.usersDefault)

    override val canChangePowerLevels = combine(
        matrixClient.user.canSendEvent(roomId, PowerLevelsEventContent::class),
        userPowerLevelLong,
    ) { canSend, powerLevel ->
        canSend && powerLevel > 0
    }.stateIn(coroutineScope, WhileSubscribed(), false)

    private val addedEvents: MutableStateFlow<Map<EventType, Long>> = MutableStateFlow(emptyMap())

    init {
        coroutineScope.launch {
            state.collect { addedEvents.value = emptyMap() }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun setPowerLevels() {
        coroutineScope.launch {
            error.value = when (val c = content()) {
                // this means the user tried to submit while a text field had an invalid state
                null -> i18n.powerLevelWronglyConfiguredError()
                else -> matrixClient.api.room.sendStateEvent(roomId, c).exceptionOrNull()?.let {
                    when (it) {
                        is MatrixServerException -> it.errorResponse.error
                        else -> it.message ?: i18n.commonUnknown() // some internal Exception occurred
                    }
                }
            }
        }
    }

    override fun resetPowerLevels() {
        addedEvents.value = emptyMap()

        ban.reset()
        eventsDefault.reset()
        invite.reset()
        kick.reset()
        redact.reset()
        stateDefault.reset()
        usersDefault.reset()
        events.value.forEach { (_, v) -> v.reset() }
    }

    override fun newEvent(type: EventType) {
        coroutineScope.launch {
            val content = state.value ?: defaultPowerLevelsEventContent
            addedEvents.value += type to ((content.events + addedEvents.value)[type] ?: content.stateDefault)
        }
    }

    override val knownEvents = flow<Set<EventContentSerializerMapping<*>>> {
        emit(matrixClient.di.get<EventContentSerializerMappings>().message)
    }.stateIn(coroutineScope, WhileSubscribed(), setOf())

    override val ban = Value(
        coroutineScope,
        old = state.map {
            it?.ban ?: defaultPowerLevelsEventContent.ban
        }.stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.ban),
        max = userPowerLevelLong,
    )
    override val eventsDefault = Value(
        coroutineScope,
        old = state.map { it?.eventsDefault ?: defaultPowerLevelsEventContent.eventsDefault }
            .stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.eventsDefault),
        max = userPowerLevelLong,
    )
    override val invite = Value(
        coroutineScope,
        old = state.map { it?.invite ?: defaultPowerLevelsEventContent.invite }
            .stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.invite),
        max = userPowerLevelLong,
    )
    override val kick = Value(
        coroutineScope,
        old = state.map { it?.kick ?: defaultPowerLevelsEventContent.kick }
            .stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.kick),
        max = userPowerLevelLong,
    )
    override val redact = Value(
        coroutineScope,
        old = state.map { it?.redact ?: defaultPowerLevelsEventContent.redact }
            .stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.redact),
        max = userPowerLevelLong,
    )
    override val stateDefault = Value(
        coroutineScope,
        old = state.map { it?.stateDefault ?: defaultPowerLevelsEventContent.stateDefault }
            .stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.stateDefault),
        max = userPowerLevelLong,
    )
    override val usersDefault = Value(
        coroutineScope,
        old = state.map { it?.usersDefault ?: defaultPowerLevelsEventContent.usersDefault }
            .stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.usersDefault),
        max = userPowerLevelLong,
    )

    override val events = combine(state, addedEvents, userPowerLevelLong) { state, addedEvents, userPowerLevelLong ->
        Triple(state, addedEvents, userPowerLevelLong)
    }.scopedMapLatest { (state, addedEvents, userPowerLevelLong) ->
        if (state == null) return@scopedMapLatest mapOf()
        val allEvents = state.events + addedEvents
        allEvents.mapValues { (event, pl) ->
            Value(
                scope = this,
                old = flow {
                    emit(pl)
                }.stateIn(this, WhileSubscribed(), allEvents[event] ?: state.stateDefault),
                max = flow {
                    emit(maxPowerLevel(event, allEvents, state.stateDefault, userPowerLevelLong))
                }.stateIn(this, WhileSubscribed(), null)
            )
        }
    }.stateIn(coroutineScope, WhileSubscribed(), mapOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val eventsInputError: StateFlow<Boolean> = events.map {
        combine(it.values.map { it.error }) { it.any { it } }
    }.flattenConcat().stateIn(coroutineScope, WhileSubscribed(), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val eventsIsModified: StateFlow<Boolean> = events.map {
        combine(it.values.map { it.isModified }) { it.any { it } }
    }.flattenConcat().stateIn(coroutineScope, WhileSubscribed(), false)

    override val inputError = combine(
        listOf(
            ban.error,
            eventsDefault.error,
            invite.error,
            kick.error,
            redact.error,
            stateDefault.error,
            usersDefault.error,
            eventsInputError,
        )
    ) { it.any { it } }.stateIn(coroutineScope, WhileSubscribed(), false)

    override val isAnyInputModified = combine(
        listOf(
            ban.isModified,
            eventsDefault.isModified,
            invite.isModified,
            kick.isModified,
            redact.isModified,
            stateDefault.isModified,
            usersDefault.isModified,
            eventsIsModified,
            addedEvents.map { it.isNotEmpty() }, // adding an event counts as modification
        )
    ) { it.any { it } }.stateIn(coroutineScope, WhileSubscribed(), false)

    // This function only returns null if a text field does not contain a number
    private fun content(): PowerLevelsEventContent? {
        return state.value?.copy(
            ban = ban.modifiedValue() ?: return null,
            eventsDefault = eventsDefault.modifiedValue() ?: return null,
            invite = invite.modifiedValue() ?: return null,
            kick = kick.modifiedValue() ?: return null,
            redact = redact.modifiedValue() ?: return null,
            stateDefault = stateDefault.modifiedValue() ?: return null,
            usersDefault = usersDefault.modifiedValue() ?: return null,
            events = events.value.mapValues { (_, v) -> v.modifiedValue() ?: return null },
        )
    }
}

data class Value(
    private val scope: CoroutineScope,
    val old: StateFlow<Long>,
    val max: StateFlow<Long?>, // null means the value cannot be modified by the user, possibly due top insufficient permissions
    val input: TextFieldViewModel = TextFieldViewModelImpl(maxLength = 50, old.value.toString()),
) {
    init {
        scope.launch {
            old.collect {
                input.update(it.toString())
            }
        }
    }

    fun reset() {
        input.update(old.value.toString())
    }

    val isValidLong = input.map { it.text.toLongOrNull() != null }.stateIn(scope, WhileSubscribed(), true)

    val isUnderMaxPowerLevel = combine(input, max, old) { input, max, old ->
        when (val l = input.text.toLongOrNull()) {
            null -> false
            else -> max != null && l < max
        }
    }.stateIn(scope, WhileSubscribed(), true)

    val isModified = combine(input, old) { input, old ->
        input.text.toLongOrNull()?.let { it != old } ?: true
    }.stateIn(scope, WhileSubscribed(), false)

    val error =
        combine(isModified, isValidLong, isUnderMaxPowerLevel) { isModified, isValidLong, isUnderMaxPowerLevel ->
            isModified && (!isValidLong || !isUnderMaxPowerLevel)
        }.stateIn(scope, WhileSubscribed(), false)

    // null if [input.text] is not a valid number
    fun modifiedValue(): Long? = when {
        isModified.value -> input.value.text.toLongOrNull()
        else -> old.value
    }
}


private fun maxPowerLevel(event: EventType, events: Map<EventType, Long>, stateDefault: Long, powerLevel: Long): Long? {
    if (powerLevel == Long.MAX_VALUE) return powerLevel
    if (events.containsKey(event)) {
        return if (events.getValue(event) > powerLevel) null
        else events.getValue(event)
    }
    val klass = event.kClass ?: return null
    return when (klass) {
        is StateEventContent -> if (stateDefault > powerLevel) null else stateDefault
        else -> null
    }
}

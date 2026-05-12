package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.i18n.I18n
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.client.user.PowerLevel
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.EventType
import de.connect2x.trixnity.core.model.events.m.room.PowerLevelsEventContent
import de.connect2x.trixnity.core.serialization.events.EventContentSerializerMappings
import kotlin.collections.plus

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


    fun setPowerLevels()
    fun resetAll()

    val canChangePowerLevels: StateFlow<Boolean>

    val inputError: StateFlow<Boolean>
    val isAnyInputModified: StateFlow<Boolean>

    val availableUnsetEvents: StateFlow<Set<String>>
    val newEventInput: TextFieldViewModel
    val newEventError: StateFlow<String?>
    fun newEventCreate()

    val ban: Value
    val eventsDefault: Value
    val invite: Value
    val kick: Value
    val redact: Value
    val stateDefault: Value
    val usersDefault: Value
    val events: StateFlow<Map<String, Value>>

    interface Value {
        val input: TextFieldViewModel
        val isModified: StateFlow<Boolean>
        val canChange: StateFlow<Boolean>
        val error: StateFlow<String?>

        fun resetInput()

        val canBeRemoved: Boolean
        fun remove()
    }
}

class PowerlevelViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val roomId: RoomId,
    private val onBack: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, PowerlevelViewModel {
    override fun back() = onBack()

    private val defaultPowerLevelsEventContent = PowerLevelsEventContent()

    private val state = matrixClient.room.getState(roomId, PowerLevelsEventContent::class).map { it?.content }
        .stateIn(coroutineScope, WhileSubscribed(), null)

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override fun errorDismiss() {
        error.value = null
    }

    private val maxPowerLevel = matrixClient.user.getPowerLevel(roomId, matrixClient.userId).map {
        when (it) {
            is PowerLevel.Creator -> Long.MAX_VALUE
            is PowerLevel.User -> it.level
        }
    }.stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.usersDefault)

    override val canChangePowerLevels = combine(
        matrixClient.user.canSendEvent(roomId, PowerLevelsEventContent::class),
        maxPowerLevel,
    ) { canSend, max ->
        canSend && max > 0
    }.stateIn(coroutineScope, WhileSubscribed(), false)

    private val addedEvents: MutableStateFlow<Map<String, Long>> = MutableStateFlow(emptyMap())
    private val removedEvents: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    init {
        coroutineScope.launch {
            state.collect {
                addedEvents.value = emptyMap()
                removedEvents.value = emptySet()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun setPowerLevels() {
        coroutineScope.launch {
            val c = getModifiedContent()
            if (c == state.value)
                return@launch
            error.value = when (c) {
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

    override fun resetAll() {
        addedEvents.value = emptyMap()
        removedEvents.value = emptySet()

        ban.resetInput()
        eventsDefault.resetInput()
        invite.resetInput()
        kick.resetInput()
        redact.resetInput()
        stateDefault.resetInput()
        usersDefault.resetInput()
        events.value.forEach { (_, v) -> v.resetInput() }
    }

    override val ban = ValueImpl(
        coroutineScope, i18n,
        old = state.map {
            it?.ban ?: defaultPowerLevelsEventContent.ban
        }.stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.ban),
        max = maxPowerLevel,
    )
    override val eventsDefault = ValueImpl(
        coroutineScope, i18n,
        old = state.map { it?.eventsDefault ?: defaultPowerLevelsEventContent.eventsDefault }
            .stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.eventsDefault),
        max = maxPowerLevel,
    )
    override val invite = ValueImpl(
        coroutineScope, i18n,
        old = state.map { it?.invite ?: defaultPowerLevelsEventContent.invite }
            .stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.invite),
        max = maxPowerLevel,
    )
    override val kick = ValueImpl(
        coroutineScope, i18n,
        old = state.map { it?.kick ?: defaultPowerLevelsEventContent.kick }
            .stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.kick),
        max = maxPowerLevel,
    )
    override val redact = ValueImpl(
        coroutineScope, i18n,
        old = state.map { it?.redact ?: defaultPowerLevelsEventContent.redact }
            .stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.redact),
        max = maxPowerLevel,
    )
    override val stateDefault = ValueImpl(
        coroutineScope, i18n,
        old = state.map { it?.stateDefault ?: defaultPowerLevelsEventContent.stateDefault }
            .stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.stateDefault),
        max = maxPowerLevel,
    )
    override val usersDefault = ValueImpl(
        coroutineScope, i18n,
        old = state.map { it?.usersDefault ?: defaultPowerLevelsEventContent.usersDefault }
            .stateIn(coroutineScope, WhileSubscribed(), defaultPowerLevelsEventContent.usersDefault),
        max = maxPowerLevel,
    )

    override val events =
        combine(state, addedEvents, removedEvents, maxPowerLevel) { state, addedEvents, removedEvents, maxPowerLevel ->
            Tuple4(state, addedEvents, removedEvents, maxPowerLevel)
        }.scopedMapLatest { (state, addedEvents, removedEvents, maxPowerLevel) ->
            if (state == null) return@scopedMapLatest mapOf()
            val allEvents = state.events.mapKeys { it.key.name } + addedEvents - removedEvents
            allEvents.mapValues { (event, pl) ->
                ValueImpl(
                    scope = this,
                    i18n = i18n,
                    old = MutableStateFlow(pl).asStateFlow(),
                    max = MutableStateFlow(maxPowerLevel).asStateFlow(),
                    onRemove = { removeEvent(event) }
                )
            }
        }.stateIn(coroutineScope, WhileSubscribed(), mapOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val eventsInputError: StateFlow<String?> = events.flatMapLatest {
        if (it.isEmpty()) flowOf(null)
        else combine(it.values.map { it.error }) { if (it.any { it != null }) "" else null }
    }.stateIn(coroutineScope, WhileSubscribed(), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val eventsIsModified: StateFlow<Boolean> = events.flatMapLatest {
        if (it.isEmpty()) flowOf(false)
        else combine(it.values.map { it.isModified }) { it.any { it } }
    }.stateIn(coroutineScope, WhileSubscribed(), false)

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
    ) {
        it.any { it != null }
    }.stateIn(coroutineScope, WhileSubscribed(), false)

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
            removedEvents.map { it.isNotEmpty() },
        )
    ) { it.any { it } }.stateIn(coroutineScope, WhileSubscribed(), false)

    override val availableUnsetEvents: StateFlow<Set<String>> = events.map { events ->
        val mappings = matrixClient.di.get<EventContentSerializerMappings>()
        val messageEvents = mappings.message.map { it.type }
        val stateEvents = mappings.state.map { it.type }
        (messageEvents + stateEvents).filter { !events.keys.contains(it) }.toSet()
    }.stateIn(coroutineScope, WhileSubscribed(), emptySet())

    // This function only returns null if a text field does not contain a number
    private fun getModifiedContent(): PowerLevelsEventContent? {
        return state.value?.copy(
            ban = ban.modifiedValue() ?: return null,
            eventsDefault = eventsDefault.modifiedValue() ?: return null,
            invite = invite.modifiedValue() ?: return null,
            kick = kick.modifiedValue() ?: return null,
            redact = redact.modifiedValue() ?: return null,
            stateDefault = stateDefault.modifiedValue() ?: return null,
            usersDefault = usersDefault.modifiedValue() ?: return null,
            events = events.value.map { (k, v) -> EventType(null, k) to (v.modifiedValue() ?: return null) }.toMap(),
        )
    }

    override val newEventInput = TextFieldViewModelImpl(maxLength = 255)

    override val newEventError = combine(newEventInput, events) { input, events ->
        events[input.text]?.let { i18n.newEventAlreadyExistsErr() }
    }.stateIn(coroutineScope, WhileSubscribed(), null)

    override fun newEventCreate() {
        if (newEventError.value != null) return
        val eventType = newEventInput.value.text
        if (removedEvents.value.contains(eventType)) {
            removedEvents.value -= eventType
        } else {
            val content = state.value ?: defaultPowerLevelsEventContent
            addedEvents.value += eventType to ((content.events + addedEvents.value)[eventType] ?: content.stateDefault)
        }
        newEventInput.update("")
    }

    private fun removeEvent(event: String) {
        if (addedEvents.value.contains(event))
            addedEvents.value -= event
        else
            removedEvents.value += event
    }

    data class ValueImpl(
        private val scope: CoroutineScope,
        private val i18n: I18n,
        private val old: StateFlow<Long>,
        private val max: StateFlow<Long?>, // null means the value cannot be modified by the user, possibly due top insufficient permissions
        override val input: TextFieldViewModel = TextFieldViewModelImpl(maxLength = 50, old.value.toString()),
        private val onRemove: (() -> Unit)? = null,
    ) : PowerlevelViewModel.Value {
        init {
            scope.launch {
                old.collect {
                    input.update(it.toString())
                }
            }
        }

        override fun resetInput() {
            input.update(old.value.toString())
        }

        private val isValidLong = input.map { it.text.toLongOrNull() != null }.stateIn(scope, WhileSubscribed(), true)

        // we cannot change the power level if the current value is higher than our own power level
        override val canChange = combine(old, max) { old, max ->
            max != null && old <= max
        }.stateIn(scope, WhileSubscribed(), true)

        private val isUnderMaxPowerLevel = combine(input, max) { input, max ->
            when (val l = input.text.toLongOrNull()) {
                null -> false
                else -> max != null && l < max
            }
        }.stateIn(scope, WhileSubscribed(), true)

        override val isModified = combine(input, old) { input, old ->
            input.text.toLongOrNull()?.let { it != old } ?: true
        }.stateIn(scope, WhileSubscribed(), false)

        override val error = combine(
            isModified, isValidLong, isUnderMaxPowerLevel, max, old
        ) { isModified, isValidLong, isUnderMaxPowerLevel, max, old ->
            when {
                !isModified -> null // don't show error messages on unmodified entries
                !isValidLong -> i18n.powerLevelInputErrNotANumber()
                !isUnderMaxPowerLevel -> i18n.powerLevelInputErrAboveAllowedPowerLevel(max ?: old)
                else -> null
            }
        }.stateIn(scope, WhileSubscribed(), null)

        // null if [input.text] is not a valid number
        fun modifiedValue(): Long? = when {
            isModified.value -> input.value.text.toLongOrNull()
            else -> old.value
        }

        override val canBeRemoved = onRemove != null
        override fun remove() {
            onRemove?.invoke()
        }
    }
}

private data class Tuple4<out T1, out T2, out T3, out T4>(
    val value1: T1,
    val value2: T2,
    val value3: T3,
    val value4: T4,
)

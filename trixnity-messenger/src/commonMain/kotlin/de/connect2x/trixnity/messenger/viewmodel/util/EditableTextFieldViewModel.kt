package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.viewmodel.util.EditableTextFieldState.Edit
import de.connect2x.trixnity.messenger.viewmodel.util.EditableTextFieldState.Read
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


private val log = KotlinLogging.logger {}

sealed interface EditableTextFieldState : StateFlow<String> {
    class Read(value: StateFlow<String>) : EditableTextFieldState, StateFlow<String> by value
    class Edit(value: MutableStateFlow<String>) : EditableTextFieldState, MutableStateFlow<String> by value

    fun setEdit(value: String) =
        if (this is Edit) this.value = value
        else log.warn { "Can not set value in Read mode!" }

    fun isEditing(): Boolean = this is Edit
}

interface EditableTextFieldViewModel {
    val state: StateFlow<EditableTextFieldState>

    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>
    fun startEdit()
    fun cancelEdit()
    fun applyEdit()
}

class EditableTextFieldViewModelImpl(
    serverValue: Flow<String?>,
    private val coroutineScope: CoroutineScope,
    private val onApplyChange: suspend (String) -> Result<*>,
) : EditableTextFieldViewModel {
    private val serverStateValue = serverValue
        .map { it ?: "" }
        .stateIn(coroutineScope, Eagerly, "")

    private val _state = MutableStateFlow<EditableTextFieldState>(Read(serverStateValue))
    override val state = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    override val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error = _error.asStateFlow()

    init {
        coroutineScope.launch {
            serverStateValue.collect {
                if (isLoading.value) {
                    _state.value = Read(serverStateValue)
                    _isLoading.value = false
                }
                _error.value = null
            }
        }
    }

    override fun startEdit() {
        _state.value = Edit(MutableStateFlow(serverStateValue.value))
        _error.value = null
    }

    override fun cancelEdit() {
        _state.value = Read(serverStateValue)
        _error.value = null
    }

    override fun applyEdit() {
        val oldValue = serverStateValue.value
        val newValue = (_state.value as? Edit)?.value?.trim()
        if (oldValue != newValue && newValue != null && isLoading.value.not()) {
            coroutineScope.launch {
                _isLoading.value = true
                onApplyChange(newValue)
                    .onFailure {
                        _isLoading.value = false
                        _state.value = Edit(MutableStateFlow(newValue))
                        _error.value = it.message
                    }.onSuccess {
                        _error.value = null
                    }
            }
        } else cancelEdit()
    }
}

class PreviewEditableTextFieldViewModel : EditableTextFieldViewModel {
    override val state: StateFlow<EditableTextFieldState> =
        MutableStateFlow(Read(MutableStateFlow("value")))
    override val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
    override val error: StateFlow<String?> = MutableStateFlow("error")

    override fun startEdit() {}
    override fun cancelEdit() {}
    override fun applyEdit() {}
}

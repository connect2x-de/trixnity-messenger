package de.connect2x.trixnity.messenger.viewmodel

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val log = KotlinLogging.logger { }

/**
 * Interface for a backing text field, managing the state of text and its selection.
 */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
interface TextFieldViewModel : StateFlow<TextFieldViewModel.State> {
    /**
     * Represents the state of the text field, including the text content and selection range.
     *
     * @property text The current text in the text field.
     * @property selection The range of the current selection, or `null` if no selection exists.
     * @property epoch The current epoch of the State. A higher epoch means, that the state one is likely newer than the other.
     *           This is useful to prevent unnecessary updates in the UI (depending on the implementation).
     */
    class State(
        val text: String,
        selection: IntRange?,
        val epoch: ULong,
    ) {
        val selection: IntRange? = selection?.coerceIn(text)

        private fun IntRange.coerceIn(text: String): IntRange =
            if (text.isEmpty()) IntRange(0, 0)
            else {
                val firstWithinLimits = first.coerceIn(0..text.length)
                val lastWithinLimits = last.coerceIn(firstWithinLimits..text.length)
                IntRange(firstWithinLimits, lastWithinLimits)
            }

        operator fun component1() = text
        operator fun component2() = selection
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as State

            if (epoch != other.epoch) return false
            if (text != other.text) return false
            if (selection != other.selection) return false

            return true
        }

        override fun hashCode(): Int {
            var result = epoch.hashCode()
            result = 31 * result + text.hashCode()
            result = 31 * result + (selection?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "State(epoch=$epoch, text='$text', selection=$selection)"
        }
    }

    /**
     * Use this if you are interested in [State.text] only. Otherwise, use [value].
     */
    val text: Flow<String>

    /**
     * Use this if you are interested in [State.text] only. Otherwise, use [value].
     */
    val textValue: String

    /**
     * Use this if you are interested in [State.selection] only. Otherwise, use [value].
     */
    val selection: Flow<IntRange?>

    /**
     * Use this if you are interested in [State.selection] only. Otherwise, use [value].
     */
    val selectionValue: IntRange?

    /**
     * The maximum allowed characters in the text field. Everything above the limit will be cut.
     */
    val maxLength: Int

    /**
     * Update the state.
     */
    fun update(text: String, selection: IntRange? = null, epoch: ULong? = null)
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
open class TextFieldViewModelImpl private constructor(
    private val delegate: MutableStateFlow<TextFieldViewModel.State>,
    maxLength: Int,
) : TextFieldViewModel, StateFlow<TextFieldViewModel.State> by delegate.asStateFlow() {
    constructor(
        maxLength: Int,
        initialText: String = "",
        initialSelection: IntRange? = null,
    ) : this(MutableStateFlow(TextFieldViewModel.State(initialText, initialSelection, 1UL)), maxLength)

    override val text: Flow<String>
        get() = map { it.text }.distinctUntilChanged()
    override val textValue: String
        get() = value.text
    override val selection: Flow<IntRange?>
        get() = map { it.selection }.distinctUntilChanged()
    override val selectionValue: IntRange?
        get() = value.selection
    override val maxLength: Int = maxLength

    override fun update(text: String, selection: IntRange?, epoch: ULong?) {
        delegate.update {
            if (epoch == null || epoch > it.epoch) {
                println("+++ ${text.length}")
                TextFieldViewModel.State(
                    epoch = it.epoch + 1u,
                    text = text.take(maxLength),
                    selection = selection?.let {
                        selection.first.coerceIn(0..maxLength)..selection.last.coerceIn(0..maxLength)
                    },
                )
            } else {
                log.trace { "skip update, because epoch $epoch > ${it.epoch}" }
                it
            }
        }
    }
}

interface ApprovableTextFieldViewModel : TextFieldViewModel {
    val isEdit: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>
    fun startEdit()
    fun cancelEdit()
    fun approveEdit()
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
class ApprovableTextFieldViewModelImpl(
    serverValue: Flow<String?>,
    maxLength: Int,
    private val coroutineScope: CoroutineScope,
    private val onApplyChange: suspend (String) -> Result<*>,
) : TextFieldViewModelImpl(maxLength), ApprovableTextFieldViewModel {
    private val serverStateValue = serverValue
        .map { it ?: "" }
        .stateIn(coroutineScope, Eagerly, "")

    private val _isEditing = MutableStateFlow(false)
    override val isEdit: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    override val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error = _error.asStateFlow()

    init {
        coroutineScope.launch {
            serverStateValue.collect {
                if (isLoading.value || isEdit.value.not()) {
                    forceSetText(it)
                    _isLoading.value = false
                }
                _error.value = null
            }
        }
    }

    override fun startEdit() {
        _isEditing.value = true
        _error.value = null
    }

    override fun cancelEdit() {
        _isEditing.value = false
        _error.value = null
        forceSetText(serverStateValue.value)
    }

    override fun approveEdit() {
        val newValue = value.text
        if (newValue != serverStateValue.value && _isLoading.getAndUpdate { true }.not()) {
            coroutineScope.launch {
                _isLoading.value = true
                onApplyChange(newValue)
                    .onFailure {
                        _isLoading.value = false
                        _error.value = it.message
                    }.onSuccess {
                        _isEditing.value = false
                        _error.value = null
                    }
            }
        }
    }

    private fun forceSetText(text: String) {
        super.update(text, null, null)
    }

    override fun update(text: String, selection: IntRange?, epoch: ULong?) {
        if (_isEditing.value) {
            super.update(text, selection, epoch)
        } else {
            log.trace { "prevent update of value because isEditing is false" }
        }
    }
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
class PreviewApprovableTextFieldViewModel : ApprovableTextFieldViewModel, StateFlow<TextFieldViewModel.State> by
MutableStateFlow(TextFieldViewModel.State("", null, 0UL)) {
    override val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
    override val error: StateFlow<String?> = MutableStateFlow("error")
    override val isEdit: StateFlow<Boolean> = MutableStateFlow(true)

    override fun startEdit() {}
    override fun cancelEdit() {}
    override fun approveEdit() {}
    override val text: Flow<String> = flowOf("")
    override var textValue: String = ""
    override val selection: Flow<IntRange?> = flowOf(null)
    override var selectionValue: IntRange? = null
    override val maxLength: Int = 100
    override fun update(text: String, selection: IntRange?, epoch: ULong?) {
    }
}

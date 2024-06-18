package de.connect2x.trixnity.messenger.viewmodel.uia

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaActionConfirmationViewModelPreview.PreviewMode.ERROR
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaActionConfirmationViewModelPreview.PreviewMode.NORMAL
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.io.async.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.core.MatrixServerException
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface UiaActionConfirmationViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        message: String?,
        action: suspend () -> Result<UIA<*>>,
        onNext: (UIA<*>) -> Unit,
        onCancel: () -> Unit,
        onError: (MatrixServerException) -> Unit,
    ): UiaActionConfirmationViewModel {
        return UiaActionConfirmationViewModelImpl(
            viewModelContext,
            message,
            action,
            onNext,
            onCancel,
            onError,
        )
    }

    companion object : UiaActionConfirmationViewModelFactory
}

interface UiaActionConfirmationViewModel {
    val confirmationMessage: String?
    val isPerformingAction: StateFlow<Boolean>
    val error: StateFlow<String?>
    fun next()
    fun cancel()
}

class UiaActionConfirmationViewModelImpl(
    viewModelContext: ViewModelContext,
    override val confirmationMessage: String?,
    private val action: suspend () -> Result<UIA<*>>,
    private val onNext: (UIA<*>) -> Unit,
    private val onCancel: () -> Unit,
    private val onError: (MatrixServerException) -> Unit,
) : ViewModelContext by viewModelContext, UiaActionConfirmationViewModel {
    private val i18n = get<I18n>()
    override val isPerformingAction = MutableStateFlow(false)
    override val error = MutableStateFlow<String?>(null)

    init {
        if (confirmationMessage == null) next()
    }

    override fun next() {
        if (isPerformingAction.getAndUpdate { true }.not()) {
            coroutineScope.launch {
                error.value = null
                action()
                    .onSuccess {
                        if (it is UIA.Error) {
                            log.error { "error during action confirmation: ${it.errorResponse.error}" }
                            error.value = i18n.uiaGenericError(it.errorResponse.error)
                        } else {
                            onNext(it)
                        }
                    }
                    .onFailure { e ->
                        log.error { "error during action confirmation: $e" }
                        if (e is MatrixServerException) onError(e)
                        else error.value = i18n.uiaGenericError(e.message)
                    }
            }.invokeOnCompletion {
                isPerformingAction.value = false
            }
        }
    }

    override fun cancel() {
        onCancel()
    }
}

class UiaActionConfirmationViewModelPreview(mode: PreviewMode = NORMAL) : UiaActionConfirmationViewModel {
    override val confirmationMessage =
        "Are you sure you want to proceed with dropping the table?"
    override val isPerformingAction = MutableStateFlow(false)
    override val error = MutableStateFlow(if (mode == ERROR) "Something wrong!" else null)
    override fun next() {}
    override fun cancel() {}
    enum class PreviewMode {
        NORMAL, ERROR,
    }
}

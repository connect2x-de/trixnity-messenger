package de.connect2x.trixnity.messenger.viewmodel.uia

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepDummyViewModelPreview.PreviewMode.BLANK
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepDummyViewModelPreview.PreviewMode.ERROR
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepDummyViewModelPreview.PreviewMode.SUBMITTING
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.io.async.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationRequest
import net.folivo.trixnity.core.MatrixServerException
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface UiaStepDummyViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        uiaStep: UIA.Step<*>,
        onNext: (UIA<*>) -> Unit,
        onCancel: () -> Unit,
        onError: (MatrixServerException) -> Unit,
    ): UiaStepDummyViewModel {
        return UiaStepDummyInputViewModelImpl(
            viewModelContext,
            uiaStep,
            onNext,
            onCancel,
            onError,
        )
    }

    companion object : UiaStepDummyViewModelFactory
}

interface UiaStepDummyViewModel {
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>

    /**
     * Is called automatically, when the view model appears for the first time.
     */
    fun next()
    fun cancel()
}

class UiaStepDummyInputViewModelImpl(
    viewModelContext: ViewModelContext,
    private val uiaStep: UIA.Step<*>,
    private val onNext: (UIA<*>) -> Unit,
    private val onCancel: () -> Unit,
    private val onError: (MatrixServerException) -> Unit,
) : ViewModelContext by viewModelContext, UiaStepDummyViewModel {
    private val i18n = get<I18n>()
    override val error = MutableStateFlow<String?>(null)
    override val isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        next()
    }

    override fun next() {
        if (isLoading.getAndUpdate { true }.not()) {
            coroutineScope.launch {
                error.value = null
                val authRequest = AuthenticationRequest.Dummy
                uiaStep.authenticate(authRequest)
                    .onSuccess {
                        if (it is UIA.Error) {
                            val errorResponse = it.errorResponse
                            log.error { "error during dummy auth: ${errorResponse.error}" }
                            error.value = i18n.uiaGenericError(errorResponse.error)
                        } else {
                            onNext(it)
                        }
                    }
                    .onFailure { e ->
                        log.error { "error during dummy auth: $e" }
                        if (e is MatrixServerException) onError(e)
                        else error.value = i18n.uiaGenericError(e.message)
                    }
            }.invokeOnCompletion {
                isLoading.value = false
            }
        }
    }

    override fun cancel() {
        onCancel()
    }
}

class UiaStepDummyViewModelPreview(mode: PreviewMode = BLANK) : UiaStepDummyViewModel {
    override val error = MutableStateFlow(if (mode == ERROR) "Error!" else null)
    override val isLoading = MutableStateFlow(mode == SUBMITTING)
    override fun next() {}
    override fun cancel() {}
    enum class PreviewMode {
        BLANK, SUBMITTING, ERROR,
    }
}

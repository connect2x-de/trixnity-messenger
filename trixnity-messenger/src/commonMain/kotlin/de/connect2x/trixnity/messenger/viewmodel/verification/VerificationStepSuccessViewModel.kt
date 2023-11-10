package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


interface VerificationStepSuccessViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        fromDeviceId: String?,
        onVerificationSuccessOk: () -> Unit,
    ): VerificationStepSuccessViewModel {
        return VerificationStepSuccessViewModelImpl(
            viewModelContext, fromDeviceId, onVerificationSuccessOk
        )
    }

    companion object : VerificationStepSuccessViewModelFactory
}

interface VerificationStepSuccessViewModel {
    val deviceName: MutableStateFlow<String?>
    fun ok()
}

open class VerificationStepSuccessViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    fromDeviceId: String?,
    private val onVerificationSuccessOk: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, VerificationStepSuccessViewModel {

    override val deviceName = MutableStateFlow(fromDeviceId)

    init {
        coroutineScope.launch {
            deviceName.value =
                fromDeviceId?.let {
                    matrixClient.api.devices.getDevice(fromDeviceId).fold(
                        onSuccess = { it.displayName ?: fromDeviceId }, onFailure = { fromDeviceId }
                    )
                }
        }
    }

    override fun ok() {
        onVerificationSuccessOk()
    }

}

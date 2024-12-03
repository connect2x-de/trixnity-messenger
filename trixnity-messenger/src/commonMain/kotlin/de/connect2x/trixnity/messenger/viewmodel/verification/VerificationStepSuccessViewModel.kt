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
    val verifiedDeviceName: MutableStateFlow<String>
    val verifyingDeviceName: MutableStateFlow<String?>
    fun ok()
}

open class VerificationStepSuccessViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    fromDeviceId: String?,
    private val onVerificationSuccessOk: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, VerificationStepSuccessViewModel {

    private val deviceId = matrixClient.deviceId

    override val verifiedDeviceName: MutableStateFlow<String> = MutableStateFlow(deviceId)
    override val verifyingDeviceName = MutableStateFlow(fromDeviceId)

    init {
        coroutineScope.launch {
            verifiedDeviceName.value = matrixClient.api.device.getDevice(deviceId).fold(
                onSuccess = { it.displayName ?: deviceId }, onFailure = { deviceId }
            )
            verifyingDeviceName.value =
                fromDeviceId?.let {
                    matrixClient.api.device.getDevice(fromDeviceId).fold(
                        onSuccess = { it.displayName ?: fromDeviceId }, onFailure = { fromDeviceId }
                    )
                }
        }
    }

    override fun ok() {
        onVerificationSuccessOk()
    }

}

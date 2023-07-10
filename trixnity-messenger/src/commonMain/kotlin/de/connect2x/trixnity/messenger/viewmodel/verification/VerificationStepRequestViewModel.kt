package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.UserId


interface VerificationStepRequestViewModelFactory {
    fun newVerificationStepRequestViewModel(
        viewModelContext: MatrixClientViewModelContext,
        onRequestAccept: () -> Unit,
        theirUserId: UserId?,
        fromDeviceId: String,
    ): VerificationStepRequestViewModel {
        return VerificationStepRequestViewModelImpl(
            viewModelContext, onRequestAccept, theirUserId, fromDeviceId
        )
    }
}

interface VerificationStepRequestViewModel {
    val theirDisplayName: MutableStateFlow<String?>
    val deviceDisplayName: MutableStateFlow<String>

    fun next()
}

open class VerificationStepRequestViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onRequestAccept: () -> Unit,
    theirUserId: UserId?,
    fromDeviceId: String,
) : MatrixClientViewModelContext by viewModelContext, VerificationStepRequestViewModel {

    override val theirDisplayName = MutableStateFlow<String?>(null)
    override val deviceDisplayName = MutableStateFlow(fromDeviceId)

    init {
        coroutineScope.launch {
            theirDisplayName.value =
                if (matrixClient.userId == theirUserId) null
                else theirUserId?.let {
                    matrixClient.api.users.getDisplayName(theirUserId).fold(
                        onSuccess = { it }, onFailure = { theirUserId.full }
                    )
                }
                    ?: theirUserId?.full
            deviceDisplayName.value =
                matrixClient.api.devices.getDevice(fromDeviceId).fold(
                    onSuccess = { it.displayName ?: fromDeviceId }, onFailure = { fromDeviceId }
                )
        }
    }

    override fun next() {
        onRequestAccept()
    }
}

package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.UserId


interface VerificationStepRequestViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onRequestAccept: () -> Unit,
        theirUserId: UserId?,
        fromDeviceId: String,
    ): VerificationStepRequestViewModel = VerificationStepRequestViewModelImpl(
        viewModelContext, onRequestAccept, theirUserId, fromDeviceId,
    )

    companion object : VerificationStepRequestViewModelFactory
}

interface VerificationStepRequestViewModel {
    val ourUserId: UserId
    val ourDisplayName: StateFlow<String>
    val ourDeviceDisplayName: StateFlow<String>
    val theirUserId: UserId?
    val theirDisplayName: StateFlow<String?>
    val theirDeviceDisplayName: StateFlow<String>
    val isFromOwnAccount: Boolean

    fun next()
}

open class VerificationStepRequestViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onRequestAccept: () -> Unit,
    override val theirUserId: UserId?,
    theirDeviceId: String,
) : MatrixClientViewModelContext by viewModelContext, VerificationStepRequestViewModel {


    override val ourUserId: UserId = userId

    override val ourDisplayName: StateFlow<String> =
        matrixClient.displayName.map { it ?: userId.full }
            .stateIn(coroutineScope, WhileSubscribed(), userId.full)

    override val theirDisplayName: StateFlow<String?> =
        flow {
            emit(theirUserId?.let { userId ->
                matrixClient.api.user.getDisplayName(userId).fold({ it }, { theirUserId?.full })
            })
        }
            .stateIn(coroutineScope, WhileSubscribed(), theirUserId?.full)

    override val ourDeviceDisplayName: StateFlow<String> =
        flow {
            emit(matrixClient.api.device.getDevice(matrixClient.deviceId).fold({ it }, { null }))
        }
            .map { it?.displayName ?: matrixClient.deviceId }
            .stateIn(coroutineScope, WhileSubscribed(), matrixClient.deviceId)

    override val theirDeviceDisplayName: StateFlow<String> =
        flow {
            emit(matrixClient.api.device.getDevice(theirDeviceId).fold({ it }, { null }))
        }
            .map { it?.displayName ?: theirDeviceId }
            .stateIn(coroutineScope, WhileSubscribed(), theirDeviceId)

    override val isFromOwnAccount: Boolean =
        ourUserId == theirUserId

    override fun next() {
        onRequestAccept()
    }
}

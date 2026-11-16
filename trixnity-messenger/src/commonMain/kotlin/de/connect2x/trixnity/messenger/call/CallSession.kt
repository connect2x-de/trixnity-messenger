package de.connect2x.trixnity.messenger.call

import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.rtc.RtcSlotId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@OptIn(MSC4143::class)
@TrixnityMessengerCallApi
@Serializable
data class CallTarget(val userId: UserId, val roomId: RoomId, val slotId: RtcSlotId)

@TrixnityMessengerCallApi
data class CallSessionState(
    val roomInfo: RoomInfoElement? = null,
    val media: CallMediaState = CallMediaState(),
    val error: String? = null,
)

@TrixnityMessengerCallApi
interface CallSession : AutoCloseable {
    val target: CallTarget
    val state: StateFlow<CallSessionState>
    val permissionRequest: StateFlow<CallPermissionKind?>

    fun onPermissionRequestResult(kind: CallPermissionKind, result: Boolean)

    fun start(onEnded: () -> Unit)

    fun toggleMicrophone()

    fun toggleCamera()
}

@TrixnityMessengerCallApi
internal class CallSessionImpl(
    parentScope: CoroutineScope,
    override val target: CallTarget,
    private val mediaFactory: CallMediaFactory,
    private val createSignalling: (CoroutineScope, () -> Unit) -> CallSignalling,
    private val matrixClients: MatrixClients,
    private val i18n: I18n,
) : CallSession {
    private val coroutineScope =
        CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))
    override val state = MutableStateFlow(CallSessionState())
    override val permissionRequest = MutableStateFlow<CallPermissionKind?>(null)
    private val media = MutableStateFlow<CallMedia?>(null)

    override fun start(onEnded: () -> Unit) {
        coroutineScope.launch {
            launch {
                matrixClients.first { target.userId !in it }
                this@CallSessionImpl.coroutineScope.cancel()
            }
            try {
                coroutineScope {
                    createSignalling(this) { this@CallSessionImpl.coroutineScope.cancel() }
                        .use { signalling ->
                            val activeMedia = mediaFactory.create(this, signalling, ::requestPermission)
                            media.value = activeMedia
                            combine(signalling.roomInfo, signalling.error, activeMedia.state) { room, error, media ->
                                    CallSessionState(room, media, error)
                                }
                                .collect { state.value = it }
                        }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                media.value = null
                state.value = CallSessionState(error = i18n.callConnectError())
                awaitCancellation()
            } finally {
                permissionRequest.value = null
                media.value = null
                onEnded()
            }
        }
    }

    private fun requestPermission(kind: CallPermissionKind) {
        if (!coroutineScope.isActive) return
        check(permissionRequest.compareAndSet(null, kind)) { "A call permission request is already pending" }
    }

    override fun onPermissionRequestResult(kind: CallPermissionKind, result: Boolean) {
        if (!coroutineScope.isActive || !permissionRequest.compareAndSet(kind, null)) return
        media.value?.onPermissionRequestResult(kind, result)
    }

    override fun toggleMicrophone() {
        media.value?.toggleMicrophone()
    }

    override fun toggleCamera() {
        media.value?.toggleCamera()
    }

    override fun close() {
        permissionRequest.value = null
        coroutineScope.cancel()
    }
}

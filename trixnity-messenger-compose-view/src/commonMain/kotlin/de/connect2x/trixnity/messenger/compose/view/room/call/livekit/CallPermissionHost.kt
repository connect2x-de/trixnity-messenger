package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallPermissionKind
import de.connect2x.trixnity.messenger.call.CallService
import de.connect2x.trixnity.messenger.call.CallSession
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get

@TrixnityMessengerCallApi
internal fun interface PermissionController {
    fun requestPermission(permissionKind: CallPermissionKind)
}

@TrixnityMessengerCallApi
@Composable
internal expect fun rememberPermissionController(onResult: (Boolean) -> Unit): PermissionController

@TrixnityMessengerCallApi
@Composable
internal fun CallPermissionHost() {
    val service = DI.get<CallService>()
    val session by service.session.collectAsState()
    val pending = session?.permissionRequest?.collectAsState()?.value
    var launchedRequest by remember { mutableStateOf<Pair<CallSession, CallPermissionKind>?>(null) }
    if (pending == null && launchedRequest == null) return
    val controller = rememberPermissionController { granted ->
        launchedRequest?.let { (owner, kind) -> owner.onPermissionRequestResult(kind, granted) }
        launchedRequest = null
    }
    LaunchedEffect(session, pending, launchedRequest) {
        val owner = service.session.value
        val kind = owner?.permissionRequest?.value
        if (owner != null && kind != null && launchedRequest == null) {
            launchedRequest = owner to kind
            controller.requestPermission(kind)
        }
    }
}

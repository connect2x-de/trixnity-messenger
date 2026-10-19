package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
internal actual fun rememberPermissionController(onResult: (PermissionKind, Boolean) -> Unit): PermissionController {
    val currentOnResult = rememberUpdatedState(onResult)
    return remember {
        DummyPermissionController { permissionKind, granted -> currentOnResult.value(permissionKind, granted) }
    }
}

private class DummyPermissionController(private val onResult: (PermissionKind, Boolean) -> Unit) :
    PermissionController {
    override fun requestPermission(permissionKind: PermissionKind) {
        onResult(permissionKind, true)
    }
}

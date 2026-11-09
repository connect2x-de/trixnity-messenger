package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import android.Manifest
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import java.util.UUID

@Composable
internal actual fun rememberPermissionController(onResult: (PermissionKind, Boolean) -> Unit): PermissionController {
    val currentOnResult = rememberUpdatedState(onResult)
    val registry = checkNotNull(LocalActivityResultRegistryOwner.current).activityResultRegistry
    val key = rememberSaveable { UUID.randomUUID().toString() }
    val controller =
        remember(registry, key) {
            AndroidPermissionController(registry = registry, key = key) { permissionKind, granted ->
                currentOnResult.value(permissionKind, granted)
            }
        }

    return controller
}

private class AndroidPermissionController(
    private val registry: ActivityResultRegistry,
    private val key: String,
    private val onResult: (PermissionKind, Boolean) -> Unit,
) : PermissionController, RememberObserver {

    private var camera: ActivityResultLauncher<String>? = null
    private var microphone: ActivityResultLauncher<String>? = null

    override fun onRemembered() {
        camera =
            registry.register("$key:camera", ActivityResultContracts.RequestPermission()) {
                onResult(PermissionKind.CAMERA, it)
            }

        microphone =
            registry.register("$key:microphone", ActivityResultContracts.RequestPermission()) {
                onResult(PermissionKind.MICROPHONE, it)
            }
    }

    override fun requestPermission(permissionKind: PermissionKind) {
        when (permissionKind) {
            PermissionKind.CAMERA -> checkNotNull(camera).launch(Manifest.permission.CAMERA)

            PermissionKind.MICROPHONE -> checkNotNull(microphone).launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onForgotten() {
        camera?.unregister()
        microphone?.unregister()
        camera = null
        microphone = null
    }

    override fun onAbandoned() = Unit
}

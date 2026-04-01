package de.connect2x.trixnity.messenger.util

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import de.connect2x.lognity.api.logger.Logger
import kotlin.uuid.Uuid

private val log = Logger("de.connect2x.trixnity.messenger.util.RequestPermissionActivityResult")

private fun <I, O> registerActivityResultIgnoreLifecycle(
    activity: ComponentActivity,
    contract: ActivityResultContract<I, O>,
    callback: ActivityResultCallback<O>
): ActivityResultLauncher<I> {
    val unused = Uuid.random().toString()
    return activity.activityResultRegistry.register(unused, contract, callback)
}

/**
 * Use this because the usual API can only be used before lifecycle is STARTED.
 *
 * Pitfall: When calling this, you must call [ActivityResultLauncher.unregister] on the returned
 * [ActivityResultLauncher] when the launcher is no longer needed to release any values that
 * might be captured in the registered callback. (see [ActivityResultRegistry.register])
 */
fun requestPermissionActivityResult(
    activity: ComponentActivity,
    permission: String,
    permissionSettingNameUi: String,
): ActivityResultLauncher<String> {
    return registerActivityResultIgnoreLifecycle(
        activity,
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            log.debug { "Permission granted" }
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    permission
                )
            ) {
                log.debug { "Permission denied but not permanently" }
            } else {
                log.debug { "Permission denied permanently. Redirecting to Android app settings..." }
                activity.runOnUiThread {
                    // TODO: translate
                    Toast.makeText(activity, "Go to Permissions and allow $permissionSettingNameUi", Toast.LENGTH_LONG).show()
                }
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.packageName, null)
                )
                activity.startActivity(intent)
            }
        }
    }
}

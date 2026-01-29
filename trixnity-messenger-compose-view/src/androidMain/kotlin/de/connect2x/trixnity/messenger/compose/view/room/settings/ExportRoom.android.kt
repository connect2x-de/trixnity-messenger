package de.connect2x.trixnity.messenger.compose.view.room.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.export.Destination
import de.connect2x.trixnity.messenger.export.FileBasedExportRoomProperties
import okio.Path.Companion.toPath
import java.io.IOException

private val log: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.room.settings.ExportRoomKt")

@Composable
internal actual fun SelectExportDestination(
    properties: FileBasedExportRoomProperties?,
    result: (Destination?) -> Unit,
) {
    val appName = DI.get<MatrixMessengerConfiguration>().appName
    val i18n = DI.get<I18nView>()
    val context = LocalContext.current

    val initialDirectory = remember {
        getAbsoluteDirectory(
            if (Build.VERSION.SDK_INT < 29) {
                val permission = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (permission == PackageManager.PERMISSION_DENIED) {
                    throw IOException("Insufficient permissions to save files.")
                }
                Uri.fromFile(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).resolve(appName)
                )
            } else MediaStore.Downloads.EXTERNAL_CONTENT_URI
        )
    }

    LaunchedEffect(Unit) {
        log.debug { "set initial directory to $initialDirectory" }
        result(initialDirectory.toPath())
    }

    Text(i18n.exportRoomTargetDirectoryAndroid(), style = MaterialTheme.typography.bodyMedium)
}

private fun getAbsoluteDirectory(uri: Uri?): String {
    val uriPath: String? = uri?.path
    val default = Environment.DIRECTORY_DOWNLOADS
    if (uriPath == null) log.warn { "invalid directory! defaulting to: $default" }
    val directoryKey = uriPath?.split("/")?.also {
        if (it.lastOrNull()?.contains('.') == true) log.warn { "directory uri might be a file: $uri" }
    }?.getOrNull(1) ?: ""
    val mappedDestination = directoryMap.entries.firstOrNull { entry ->
        directoryKey.startsWith(entry.key)
    }?.value ?: default
    return "${Environment.getExternalStorageDirectory()}/${mappedDestination}"
}

private val directoryMap by lazy {
    mapOf(
        "music" to Environment.DIRECTORY_MUSIC,
        "podcasts" to Environment.DIRECTORY_PODCASTS,
        "ringtones" to Environment.DIRECTORY_RINGTONES,
        "alarms" to Environment.DIRECTORY_ALARMS,
        "notifications" to Environment.DIRECTORY_NOTIFICATIONS,
        "pictures" to Environment.DIRECTORY_PICTURES,
        "movies" to Environment.DIRECTORY_MOVIES,
        "downloads" to Environment.DIRECTORY_DOWNLOADS,
        "dcim" to Environment.DIRECTORY_DCIM,
        "documents" to Environment.DIRECTORY_DOCUMENTS,
    )
}

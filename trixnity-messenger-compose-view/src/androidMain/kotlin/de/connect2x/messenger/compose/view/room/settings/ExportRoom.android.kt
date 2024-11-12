package de.connect2x.messenger.compose.view.room.settings

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
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.files.FilePathHelper
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.export.Destination
import de.connect2x.trixnity.messenger.export.FileBasedExportRoomProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import okio.Path.Companion.toPath
import java.io.IOException

private val log = KotlinLogging.logger { }

@Composable
internal actual fun SelectExportDestination(
    properties: FileBasedExportRoomProperties?,
    result: (Destination?) -> Unit
) {
    val appName = DI.get<MatrixMessengerConfiguration>().appName
    val i18n = DI.get<I18nView>()
    val context = LocalContext.current

    val filePathHelper = remember { FilePathHelper(context) }
    val initialDirectory = remember {
        filePathHelper.getAbsolutePath(
            if (Build.VERSION.SDK_INT < 29) {
                val permission = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (permission == PackageManager.PERMISSION_DENIED) {
                    throw IOException("Insufficient permissions to save files.")
                }
                Uri.fromFile(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).resolve(appName)
                )
            } else {
                log.debug { "downloads folder: ${MediaStore.Downloads.EXTERNAL_CONTENT_URI}" }
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }
        )
    }

    LaunchedEffect(Unit) {
        log.debug { "set initial directory to $initialDirectory" }
        result(initialDirectory.toPath())
    }

    Text(i18n.exportRoomTargetDirectoryAndroid(), style = MaterialTheme.typography.bodyMedium)
}


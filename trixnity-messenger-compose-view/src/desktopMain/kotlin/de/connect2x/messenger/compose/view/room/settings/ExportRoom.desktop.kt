package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.export.Destination
import de.connect2x.trixnity.messenger.export.FileBasedExportRoomProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import okio.Path.Companion.toPath

private val log = KotlinLogging.logger { }

@Composable
internal actual fun SelectExportDestination(
    properties: FileBasedExportRoomProperties?,
    result: (Destination?) -> Unit
) {
    val appName = DI.get<MatrixMessengerConfiguration>().appName
    val i18n = DI.get<I18nView>()
    LaunchedEffect(Unit) {
        result(initialDirectory(appName))
    }
    // Due to compose life cycles the launcher needs to be set up even if launch() is skipped.
    val launcher = rememberDirectoryPickerLauncher(
        title = i18n.fileDialogTitleLoad(),
        initialDirectory = initialDirectory(appName).toString()
    ) { file ->
        log.debug { "selected file: $file" }
        file?.let {
            file.path?.toPath()
                ?.let { result(it) }
                ?: run { log.error { "can't resolve path for selected file: $file" } }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            properties?.destination?.toString() ?: "",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1.0f, fill = true)
        )
        Spacer(Modifier.size(20.dp))
        Tooltip(
            tooltip = { Text(i18n.commonFile()) }
        ) {
            ThemedIconButton(
                style = MaterialTheme.components.commonIconButton,
                onClick = { launcher.launch() },
            ) {
                Icon(Icons.Default.Folder, i18n.commonFile())
            }
        }
    }
}

private fun initialDirectory(appName: String) =
    System.getProperty("user.home").toPath().resolve("Downloads").resolve(appName)

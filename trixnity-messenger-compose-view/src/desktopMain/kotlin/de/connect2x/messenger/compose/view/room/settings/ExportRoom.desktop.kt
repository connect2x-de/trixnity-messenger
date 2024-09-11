package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.export.Destination
import de.connect2x.trixnity.messenger.export.FileBasedExportRoomProperties
import okio.Path.Companion.toPath

@Composable
internal actual fun SelectDirectory(properties: FileBasedExportRoomProperties?, result: (Destination?) -> Unit) {
    val i18n = DI.get<I18nView>()
    var showDirPicker by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        result(System.getProperty("user.home").toPath())
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            properties?.destination?.toString() ?: "",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1.0f, fill = true)
        )
        Spacer(Modifier.size(20.dp))
        IconButton(onClick = { showDirPicker = true }, modifier = Modifier.buttonPointerModifier()) {
            Icon(Icons.Default.Folder, i18n.commonFile())
        }
    }

    DirectoryPicker(
        show = showDirPicker,
        initialDirectory = System.getProperty("user.home"),
    ) { path ->
        result(path?.toPath())
        showDirPicker = false
    }
}

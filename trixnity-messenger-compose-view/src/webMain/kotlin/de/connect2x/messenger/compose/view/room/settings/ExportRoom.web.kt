package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.export.Destination
import de.connect2x.trixnity.messenger.export.FileBasedExportRoomProperties

@Composable
internal actual fun SelectExportDestination(
    properties: FileBasedExportRoomProperties?,
    result: (Destination?) -> Unit
) {
    // TODO
}

package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.export.Destination
import de.connect2x.trixnity.messenger.export.FileBasedExportRoomProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

private val log = KotlinLogging.logger {}

@Composable
internal actual fun SelectExportDestination(
    properties: FileBasedExportRoomProperties?,
    result: (Destination?) -> Unit
) {
    // TODO this does save it in the app bundle and not locally on the phone so the user has access to it!
    val documentsPath = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String
    if (documentsPath == null) {
        result(null)
        return
    }
    log.debug { "saving export to ${documentsPath.toPath()}" }
    result(documentsPath.toPath())
}

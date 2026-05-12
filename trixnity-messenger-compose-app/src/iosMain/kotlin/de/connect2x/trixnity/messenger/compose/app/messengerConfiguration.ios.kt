package de.connect2x.trixnity.messenger.compose.app

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.util.RootPath
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

private val log = Logger("de.connect2x.trixnity.messenger.compose.app.messengerConfiguration")

internal actual fun getDevRootPath(): RootPath? = RootPath(
    (NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    )[0] as String)
        .toPath()
).also {
    log.debug { "Root DEV path: $it" }
}

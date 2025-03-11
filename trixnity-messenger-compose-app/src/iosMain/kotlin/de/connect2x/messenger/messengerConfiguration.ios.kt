package de.connect2x.messenger

import de.connect2x.trixnity.messenger.util.RootPath
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

internal actual fun getDevRootPath(): RootPath? = RootPath(
    (NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    )[0] as String)
        .toPath()
) // TODO

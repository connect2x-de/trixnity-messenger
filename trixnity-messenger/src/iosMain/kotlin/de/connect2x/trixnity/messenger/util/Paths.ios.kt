package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.PathsKt")

actual fun platformPathsModule(): Module = module {
    single { FileSystem.SYSTEM }
    single<RootPath> {
        RootPath(
            (NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            )[0] as String)
                .toPath()
        ).also {
            log.debug { "root directory: ${it.path}" }
        }
    }
}

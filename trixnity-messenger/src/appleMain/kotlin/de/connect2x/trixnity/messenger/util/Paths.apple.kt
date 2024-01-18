package de.connect2x.trixnity.messenger.util

import arrow.resilience.common.platform
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun platformPathsModule(): Module = module {
    single<Paths> {
        object : Paths {
            override val fileSystem: FileSystem = FileSystem.SYSTEM
            override val rootPath: Path
                get() = (
                        NSSearchPathForDirectoriesInDomains(
                            NSDocumentDirectory,
                            NSUserDomainMask,
                            true
                        )[0] as String)
                    .toPath()
        }
    }
}
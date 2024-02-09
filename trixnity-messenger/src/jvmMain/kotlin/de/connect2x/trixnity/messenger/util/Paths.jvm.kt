package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformPathsModule(): Module = module {
    single { FileSystem.SYSTEM }
    single<RootPath> {
        val config = get<MatrixMessengerConfiguration>()
        val path = getAppPath(config.appName)
        FileSystem.SYSTEM.createDirectories(path)
        RootPath(path)
    }
}

fun getAppPath(appName: String) =
    System.getProperty("ROOT_PATH")?.toPath()
        ?: when (getOs()) {
            OS.MAC_OS -> {
                System.getenv("HOME").toPath()
                    .resolve("Library")
                    .resolve("Application Support")
                    .resolve(appName)
            }

            OS.WINDOWS -> {
                System.getenv("LOCALAPPDATA").toPath()
                    .resolve(appName)
            }

            OS.LINUX -> {
                System.getenv("HOME").toPath()
                    .resolve(".$appName")
            }
        }
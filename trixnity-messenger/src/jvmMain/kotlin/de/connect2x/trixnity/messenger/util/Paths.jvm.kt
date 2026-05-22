package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformPathsModule(): Module = module {
    single { FileSystem.SYSTEM }
    single<RootPath> {
        val config = get<MatrixMessengerBaseConfiguration>()
        val path = getAppPath(config.appId)
        FileSystem.SYSTEM.createDirectories(path)
        RootPath(path)
    }
}

fun getAppPath(appId: String) =
    System.getenv("TRIXNITY_MESSENGER_ROOT_PATH")?.toPath()
        ?: when (getOs()) {
            OS.MAC_OS -> {
                System.getenv("HOME").toPath().resolve("Library").resolve("Application Support").resolve(appId)
            }

            OS.WINDOWS -> {
                System.getenv("LOCALAPPDATA").toPath().resolve(appId)
            }

            OS.LINUX -> {
                val dataHome =
                    System.getenv("XDG_DATA_HOME")?.toPath()
                        ?: System.getenv("HOME").toPath().resolve(".local").resolve("share")

                dataHome.resolve(appId)
            }
        }

package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformPathsModule(): Module = module {
    single<Paths> {
        val config = get<MatrixMessengerConfiguration>()
        object : Paths {
            override val fileSystem: FileSystem = FileSystem.SYSTEM
            override val rootPath: Path
                get() {
                    val path = System.getProperty("ROOT_PATH")?.toPath() ?: getAppPath(config.appName)
                    FileSystem.SYSTEM.createDirectories(path)
                    return path
                }
        }
    }
}

private fun getAppPath(appName: String) = when (getOs()) {
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
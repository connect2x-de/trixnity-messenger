package de.connect2x.trixnity.messenger.util

import android.content.Context
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformPathsModule(): Module = module {
    single<Paths> {
        val context = get<Context>()
        object : Paths {
            override val fileSystem: FileSystem = FileSystem.SYSTEM
            override val rootPath: Path = context.filesDir.toOkioPath()
        }
    }
}
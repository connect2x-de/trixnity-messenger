package de.connect2x.trixnity.messenger.util

import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformPathsModule(): Module = module {
    single { FileSystem.SYSTEM }
    single<RootPath> {
        val contextGetter = get<ContextGetter>()
        RootPath(contextGetter().filesDir.toOkioPath())
    }
}

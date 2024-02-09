package de.connect2x.trixnity.messenger.util

import android.content.Context
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformPathsModule(): Module = module {
    single { FileSystem.SYSTEM }
    single<RootPath> {
        val context = get<Context>()
        RootPath(context.filesDir.toOkioPath())
    }
}
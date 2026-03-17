@file:OptIn(ExperimentalWasmJsInterop::class)

package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.warn
import js.objects.Object
import js.objects.unsafeJso
import js.string.JsStrings.toKotlinString
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import web.fs.getDirectoryHandle
import web.fs.removeEntry
import web.idb.databases
import web.idb.indexedDB
import web.navigator.navigator
import web.storage.getDirectory
import web.storage.localStorage
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toList

private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.PathsKt")

actual fun platformPathsModule(): Module = module {
    single<RootPath> {
        // there is no actual FileSystem in Browsers, but we can name the databases and the keys of local storage entries like a path
        RootPath("/vfs".toPath())
    }
}

suspend fun Path.deleteVirtualFileSystemData() {
    val path = toString()

    val databaseNames = indexedDB.databases().toList().mapNotNull { it.name }
    databaseNames.forEach { databaseName ->
        if (databaseName.startsWith(path)) {
            indexedDB.deleteDatabase(databaseName)
        }
    }

    val localStorageKeys = Object.keys(localStorage).toList().map { it.toKotlinString() }
    localStorageKeys.forEach { localStorageKey ->
        if (localStorageKey.startsWith(path)) localStorage.removeItem(localStorageKey)
    }

    try {
        var opfsDirectory = navigator.storage.getDirectory()
        for (segment in segments.dropLast(1)) {
            opfsDirectory = opfsDirectory.getDirectoryHandle(segment, unsafeJso { create = true })
        }
        opfsDirectory.removeEntry(segments.last(), unsafeJso { recursive = true })
    } catch (error: Throwable) {
        log.warn(error) { "deleting OPFS directories failed" }
    }
}

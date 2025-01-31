package de.connect2x.trixnity.messenger.util

import io.github.oshai.kotlinlogging.KotlinLogging
import js.objects.Object
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import web.fs.FileSystemGetDirectoryOptions
import web.fs.FileSystemRemoveOptions
import web.idb.indexedDB
import web.navigator.navigator
import web.storage.localStorage

private val log = KotlinLogging.logger {}

actual fun platformPathsModule(): Module = module {
    single<RootPath> {
        // there is no actual FileSystem in Browsers, but we can name the databases and the keys of local storage entries like a path
        RootPath("/vfs".toPath())
    }
}

suspend fun Path.deleteVirtualFileSystemData() {
    val path = toString()

    val databaseNames = indexedDB.databases().mapNotNull { it.name }
    databaseNames.forEach { databaseName ->
        if (databaseName.startsWith(path)) {
            indexedDB.deleteDatabase(databaseName)
        }
    }

    val localStorageKeys = Object.keys(localStorage)
    localStorageKeys.forEach { localStorageKey ->
        if (localStorageKey.startsWith(path)) localStorage.removeItem(localStorageKey)
    }

    try {
        var opfsDirectory = navigator.storage.getDirectory()
        for (segment in segments.dropLast(1)) {
            opfsDirectory = opfsDirectory.getDirectoryHandle(segment, FileSystemGetDirectoryOptions(create = true))
        }
        opfsDirectory.removeEntry(segments.last(), FileSystemRemoveOptions(recursive = true))
    } catch (error: Throwable) {
        log.warn(error) { "deleting OPFS directories failed" }
    }
}

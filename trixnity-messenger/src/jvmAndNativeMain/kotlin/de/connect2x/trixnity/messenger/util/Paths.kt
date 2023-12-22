package de.connect2x.trixnity.messenger.util

import okio.FileSystem
import okio.Path
import org.koin.core.module.Module

interface Paths {
    val fileSystem: FileSystem
    val rootPath: Path
}

expect fun platformPathsModule(): Module
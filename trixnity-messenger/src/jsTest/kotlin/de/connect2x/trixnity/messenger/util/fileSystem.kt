package de.connect2x.trixnity.messenger.util

import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem

actual val fileSystem: FileSystem = FakeFileSystem()

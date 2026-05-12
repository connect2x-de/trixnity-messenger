package de.connect2x.trixnity.messenger.compose.view.files

import io.github.vinceglb.filekit.PlatformFile
import web.file.File

internal actual fun realFile(platformFile: PlatformFile): File = platformFile.file.unsafeCast<File>()

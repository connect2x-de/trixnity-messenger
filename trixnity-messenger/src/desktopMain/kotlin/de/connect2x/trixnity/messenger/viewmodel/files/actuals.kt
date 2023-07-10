package de.connect2x.trixnity.messenger.viewmodel.files

import de.connect2x.trixnity.messenger.MessengerConfig
import okio.FileSystem
import java.nio.file.Files
import kotlin.io.path.absolutePathString

actual fun saveVideo(videoByteArray: ByteArray): String {
    val appName = MessengerConfig.instance.appName
    val tempFile = Files.createTempFile("$appName-", "")
    Files.write(tempFile, videoByteArray)
    return tempFile.absolutePathString()
}

actual fun getFileSystem(): FileSystem = FileSystem.SYSTEM
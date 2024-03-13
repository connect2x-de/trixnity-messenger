package de.connect2x.trixnity.messenger.util

import okio.FileSystem
import platform.Foundation.NSDownloadsDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask


actual fun fileBaseArchiveSink(fileName: String, resultContent: String){
    val path =   (NSSearchPathForDirectoriesInDomains(
        NSDownloadsDirectory,
        NSUserDomainMask,
        true
    )[0] as String) + "/$fileName"



}

package de.connect2x.trixnity.messenger

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.*
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import platform.Foundation.*

private val log = KotlinLogging.logger { }

actual suspend fun createRepositoriesModule(context: Any?, accountName: String): Module {
    return createRealmRepositoriesModule()
}

internal actual suspend fun createMediaStore(context: Any?, accountName: String): MediaStore {
    val mediaStore = OkioMediaStore(NSBundle.mainBundle.bundlePath.toPath().resolve("media"))
    log.debug { "media store location: $mediaStore" }
    return mediaStore
}

actual fun deleteDatabase(accountName: String) {

}

actual fun closeApp() {

}

actual fun getVersion(): String {
    log.debug { "read version file" }
    val version = NSBundle.mainBundle.pathForResource("version", "txt") ?: return "0.0.1"
    log.debug { "version.txt location: $version" }
    return memScoped {
        val errorPtr = alloc<ObjCObjectVar<NSError?>>()

        NSString.stringWithContentsOfFile(
            version,
            encoding = NSUTF8StringEncoding,
            error = errorPtr.ptr
        )?.trim() ?: run {
            log.error { "Couldn't load resource: $version. Error: ${errorPtr.value?.localizedDescription} - ${errorPtr.value}" }
            return "0.0.1"
        }
    }
}

actual fun getLicenses(): String {
    return "Licenses"
}

actual fun isNetworkAvailable(): Boolean {
    return true
}

actual fun deviceDisplayName(): String {
    return "iOS"
}

actual fun getLogContent(): String {
    return ""
}

actual fun sendLogToDevs(emailAddress: String, subject: String, content: String) {

}
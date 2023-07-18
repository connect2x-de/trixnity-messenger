package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.cleanAccountName
import de.connect2x.trixnity.messenger.util.getAccountName
import de.connect2x.trixnity.messenger.util.getSecret
import de.connect2x.trixnity.messenger.util.setSecret
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.realm.kotlin.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import okio.Path.Companion.toOkioPath
import org.koin.core.module.Module
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.system.exitProcess


private val log = KotlinLogging.logger { }

private val accountMutex = Mutex()

actual suspend fun createRepositoriesModule(accountName: String): Module = withContext(Dispatchers.IO) {
    val messengerConfig = MessengerConfig.instance
    val dbFolder = getDbFolderPath(accountName).absolutePathString()
    log.debug { messengerConfig }

    if (messengerConfig.encryptDb) {
        val secretsName =
            "${messengerConfig.packageName}.${messengerConfig.appName}.${accountName.cleanAccountName()}.db"
        val password = getSecret(secretsName)
            ?: createPassword().also {
                setSecret(secretsName, it)
            }

        createRealmRepositoriesModule {
            directory(dbFolder)
            encryptionKey(password.toByteArray())
        }
    } else {
        createRealmRepositoriesModule {
            directory(dbFolder)
        }
    }
}

internal actual suspend fun createMediaStore(context: Any?, accountName: String): MediaStore =
    withContext(Dispatchers.IO) {
        OkioMediaStore(getAppFolder(accountName).resolve("media").toOkioPath())
    }

private fun createPassword(): String {
    val secureRandom = SecureRandom()
    val alphabet = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return generateSequence { alphabet[secureRandom.nextInt(alphabet.size)] }
        .take(Realm.ENCRYPTION_KEY_LENGTH).joinToString("")
}

private suspend fun getDbFolderPath(accountName: String): Path =
    getAppFolder(accountName).resolve("database")

actual suspend fun getAccountNames(): List<String> = withContext(Dispatchers.IO) {
    accountMutex.withLock {
        getAppFolder(accountName = null)
            .listDirectoryEntries()
            .filter { it.isDirectory() }
            .map {
                log.debug { "account encoded: ${it.name}, decoded: ${it.name.getAccountName()}" }
                it.name.getAccountName()
            }
    }
}

fun getAppFolder(accountName: String?): Path  {
    val appName = MessengerConfig.instance.appName
    val cleanAccountName = accountName?.cleanAccountName()
    log.debug { "get app folder for $appName and account $accountName (decoded: $cleanAccountName)" }
    val path = when (getOs()) {
        OS.MAC_OS -> {
            Path.of(System.getenv("HOME")).resolve("Library").resolve("Application Support")
                .resolve(appName).let {
                    if (cleanAccountName == null) it
                    else it.resolve(cleanAccountName)
                }
        }

        OS.WINDOWS -> {
            Path.of(System.getenv("LOCALAPPDATA")).resolve(appName).let {
                if (cleanAccountName == null) it
                else it.resolve(cleanAccountName)
            }
        }

        OS.LINUX -> {
            Path.of(System.getenv("HOME")).resolve(".$appName").let {
                if (cleanAccountName == null) it
                else it.resolve(cleanAccountName)
            }
        }
    }

    Files.createDirectories(path)
    return path
}

actual suspend fun deleteDatabase(accountName: String) {
    withContext(Dispatchers.IO) {
        accountMutex.withLock {
            getDbFolderPath(accountName).toFile().deleteRecursively()
        }
    }
}

actual suspend fun deleteAccountDataLocally(accountName: String) {
    withContext(Dispatchers.IO) {
        accountMutex.withLock {
            val result = getAppFolder(accountName).toFile().deleteRecursively()
            log.debug { "deleted account folder for account $accountName: $result" }
        }
    }
}

actual fun closeApp() {
    exitProcess(0)
}

actual fun getVersion(): String {
    val resourcesDir = File(System.getProperty("compose.application.resources.dir"))
    val properties = Properties()
    properties.load(resourcesDir.resolve("version.properties").inputStream())
    return properties.getProperty("version")
}

actual fun getLicenses(): String {
    val resourcesDir = File(System.getProperty("compose.application.resources.dir"))
    val licensesLines = resourcesDir.resolve("licenses.txt").readLines()
    val cutFromHere = licensesLines.indexOf("Unknown")
    return licensesLines.subList(0, cutFromHere - 1).joinToString(System.lineSeparator())
}

actual fun isNetworkAvailable(): Boolean {
    return true
}

actual fun deviceDisplayName(): String {
    return "${MessengerConfig.instance.appName} (${getOs().value})"
}

actual suspend fun getLogContent(): String {
    return Files.readString(getAppFolder(accountName = null).resolve("timmy.log"))
}

actual fun sendLogToDevs(emailAddress: String, subject: String, content: String) {
    val desktop = Desktop.getDesktop()
    val mailURIStr = String.format(
        "mailto:%s?subject=%s&body=%s",
        emailAddress,
        subject.encodeURLParameter(),
        content.encodeURLParameter()
    )
    val mailURI = URI(mailURIStr);
    desktop.mail(mailURI)
}

enum class OS(val value: String) {
    WINDOWS("Windows"), MAC_OS("macOS"), LINUX("Linux")
}

fun getOs(): OS {
    val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
    return when {
        os.contains("mac", ignoreCase = true) || os.contains("darwin", ignoreCase = true) -> OS.MAC_OS
        os.contains("win", ignoreCase = true) -> OS.WINDOWS
        os.contains("linux", ignoreCase = true) -> OS.LINUX
        else -> throw RuntimeException("os $os is not supported")
    }
}
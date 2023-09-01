package de.connect2x.trixnity.messenger

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import de.connect2x.trixnity.messenger.util.cleanAccountName
import de.connect2x.trixnity.messenger.util.getAccountName
import de.connect2x.trixnity.messenger.util.getSecret
import de.connect2x.trixnity.messenger.util.setSecret
import io.github.oshai.kotlinlogging.KotlinLogging
import io.realm.kotlin.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import okio.Path.Companion.toOkioPath
import org.koin.core.context.GlobalContext
import org.koin.core.module.Module
import java.io.File
import java.security.SecureRandom

private val log = KotlinLogging.logger {}

private val accountMutex = Mutex()

actual suspend fun createRepositoriesModule(accountName: String): Module = withContext(Dispatchers.IO) {
    val messengerConfig = MessengerConfig.instance
    val dbFolder = getDbPath(accountName).absolutePath
    log.debug { "createRepositoriesModule with config: $messengerConfig" }

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

internal actual suspend fun createMediaStore(accountName: String): MediaStore =
    withContext(Dispatchers.IO) {
        OkioMediaStore(getAccountPath(accountName).resolve("media").toOkioPath())
    }

private fun createPassword(): String {
    val secureRandom = SecureRandom()
    val alphabet = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return generateSequence { alphabet[secureRandom.nextInt(alphabet.size)] }
        .take(Realm.ENCRYPTION_KEY_LENGTH).joinToString("")
}

actual suspend fun getAccountNames(): List<String> = withContext(Dispatchers.IO) {
    accountMutex.withLock {
        getAppFolder()
            .list { file, _ -> file.isDirectory }
            ?.map { it.getAccountName() }
            ?: emptyList()
    }
}

actual suspend fun deleteDatabase(accountName: String) {
    withContext(Dispatchers.IO) {
        accountMutex.withLock {
            getDbPath(accountName).deleteRecursively()
        }
    }
}

actual suspend fun deleteAccountDataLocally(accountName: String) {
    withContext(Dispatchers.IO) {
        accountMutex.withLock {
            getAccountPath(accountName).deleteRecursively()
        }
    }
}

private fun getAppFolder() = getContext().filesDir.resolve(MessengerConfig.instance.appName)
private fun getAccountPath(accountName: String) = getAppFolder().resolve(accountName.cleanAccountName())
private fun getDbPath(accountName: String) = getAccountPath(accountName).resolve("database")

actual fun closeApp() {
    getContext().findActivity()?.finishAndRemoveTask()
}

@SuppressLint("MissingPermission")
actual fun isNetworkAvailable(): Boolean {
    val connectivityManager =
        getContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return networkCapabilities != null &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

actual fun deviceDisplayName(): String {
    return "${MessengerConfig.instance.appName.firstOrNull()} (Android)"
}

actual suspend fun getLogContent(): String {
    val logFile = getContext().filesDir.resolve("timmy.log")
    return logFile.absolutePath
}

actual fun sendLogToDevs(emailAddress: String, subject: String, content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(
                getContext(), "de.connect2x.timmy.provider", File(content)
            )
        )
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }

    // @see https://stackoverflow.com/a/22309656   to restrict to only email
    val restrictIntent = Intent(Intent.ACTION_SENDTO)
    val data = Uri.parse("mailto:?to=$emailAddress")
    restrictIntent.data = data
    intent.selector = restrictIntent

    startActivity(getContext(), Intent.createChooser(intent, "E-Mail"), null)
}

fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun getContext() = GlobalContext.get().get<Context>()
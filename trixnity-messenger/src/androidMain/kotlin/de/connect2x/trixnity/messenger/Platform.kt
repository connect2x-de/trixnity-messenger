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
import de.connect2x.trixnity.messenger.util.getSecret
import de.connect2x.trixnity.messenger.util.setSecret
import io.github.oshai.kotlinlogging.KotlinLogging
import io.realm.kotlin.Realm
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import okio.Path.Companion.toOkioPath
import org.koin.core.context.GlobalContext
import org.koin.core.module.Module
import java.io.File
import java.security.SecureRandom

private val log = KotlinLogging.logger {}

actual suspend fun createRepositoriesModule(context: Any?, accountName: String): Module {
    val messengerConfig = MessengerConfig.instance
    require(context is Context)
    val dbFolder = context.getDbFolder(accountName).absolutePath
    log.debug { "createRepositoriesModule with config: $messengerConfig" }

    return if (messengerConfig.encryptDb) {
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

internal actual suspend fun createMediaStore(context: Any?, accountName: String): MediaStore {
    require(context is Context)
    return OkioMediaStore(context.filesDir.toOkioPath().resolve("media"))
}

private fun createPassword(): String {
    val secureRandom = SecureRandom()
    val alphabet = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return generateSequence { alphabet[secureRandom.nextInt(alphabet.size)] }
        .take(Realm.ENCRYPTION_KEY_LENGTH).joinToString("")
}

fun Context.getDbFolder(accountName: String) =
    filesDir.resolve("${MessengerConfig.instance.dbName}_${accountName.cleanAccountName()}")

actual fun deleteDatabase(accountName: String) {
    getContext().getDbFolder(accountName).deleteRecursively()
}

actual fun closeApp() {
    getContext().findActivity()?.finishAndRemoveTask()
}

actual fun getVersion(): String {
    return getContext().packageManager.getPackageInfo(getContext().packageName, 0)?.versionName ?: "unbekannt"
}

actual fun getLicenses(): String {
    val text = getContext().assets.open("open_source_licenses.json").readBytes().decodeToString()
    val json = Json.parseToJsonElement(text)
    return json.jsonArray.joinToString(System.lineSeparator() + System.lineSeparator()) {
        val projectLicense = it.jsonObject
        val licenses =
            projectLicense["licenses"]?.jsonArray
                ?.joinToString(System.lineSeparator()) { "${it.jsonObject["license"]} (${it.jsonObject["license_url"]})" }
                ?: ""
        """${projectLicense["project"]} (${projectLicense["version"]})
                |  URL: ${projectLicense["url"]}
                |  Lizenz(en): $licenses
            """.trimMargin()
    }
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

actual fun getLogContent(): String {
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
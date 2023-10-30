package de.connect2x.trixnity.messenger.util

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.connect2x.trixnity.messenger.getContext
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }

private fun getEncryptedSharedPreferences(): SharedPreferences {
    val masterKeyAlias = MasterKey.Builder(getContext()).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    return EncryptedSharedPreferences.create(
        getContext(),
        "encrypted_secrets",
        masterKeyAlias,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

actual suspend fun getSecret(id: String): String? {
    try {
        val encryptedSharedPreferences = getEncryptedSharedPreferences()
        return encryptedSharedPreferences.getString(id, null)
    } catch (exc: Exception) {
        log.error(exc) { "Cannot read of find secret ('$id')." }
    }
    return null
}

actual suspend fun setSecret(id: String, secret: String) {
    try {
        val encryptedSharedPreferences = getEncryptedSharedPreferences()
        encryptedSharedPreferences.edit().apply {
            putString(id, secret)
        }.apply()
    } catch (exc: Exception) {
        log.error(exc) { "Cannot write secret ('$id')." }
    }
}
package de.connect2x.trixnity.messenger.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.*
import net.folivo.trixnity.crypto.core.SecureRandom
import org.koin.core.module.Module
import org.koin.dsl.module

private val log = KotlinLogging.logger { }

actual fun platformGetPlatformSecret(): Module = module {
    single<GetPlatformSecret> {
        val context = get<Context>()
        GetPlatformSecret { id, sizeOnCreate ->
            try {
                val encryptedSharedPreferences = getEncryptedSharedPreferences(context)
                val existingKey = encryptedSharedPreferences.getString(id, null)?.decodeBase64Bytes()
                if (existingKey == null) {
                    val newKey = SecureRandom.nextBytes(sizeOnCreate)
                    encryptedSharedPreferences.edit().apply {
                        putString(id, newKey.encodeBase64())
                    }.apply()
                    newKey
                } else {
                    existingKey
                }
            } catch (exc: Exception) {
                log.error(exc) { "Cannot read or set secret ('$id')." }
                null
            }
        }
    }
}

private fun getEncryptedSharedPreferences(context: Context): SharedPreferences {
    val masterKeyAlias = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    return EncryptedSharedPreferences.create(
        context,
        "encrypted_secrets",
        masterKeyAlias,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
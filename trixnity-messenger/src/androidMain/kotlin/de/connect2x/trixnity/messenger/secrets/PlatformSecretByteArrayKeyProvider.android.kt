package de.connect2x.trixnity.messenger.secrets

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.*
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.crypto.core.SecureRandom
import org.koin.core.module.Module
import org.koin.dsl.module

private val log = KotlinLogging.logger {}

actual fun platformSecretByteArrayKeyProviderModule(): Module = module {
    single<SecretByteArrayKeyProvider> {
        val context = get<Context>()
        object : SecretByteArrayKeyProvider {
            override val id = "de.connect2x.trixnity.messenger.secrets.platform"
            override val level: Int = 0

            override suspend fun get(extra: JsonObject?, getInputKey: GetKey?): SecretByteArrayKeyProvider.GetResult? {
                return try {
                    val encryptedSharedPreferences = getEncryptedSharedPreferences(context)
                    val existingKey =
                        encryptedSharedPreferences.getString(id, null)?.decodeBase64Bytes()
                            ?: encryptedSharedPreferences // TODO for backwards compatibility, can be removed in future
                                .getString("secret_byte_array_key_key", null)?.decodeBase64Bytes()
                    SecretByteArrayKeyProvider.GetResult({ size ->
                        when {
                            existingKey == null -> {
                                val newKey = SecureRandom.nextBytes(size)
                                encryptedSharedPreferences.edit().apply {
                                    putString(id, newKey.encodeBase64())
                                }.apply()
                                newKey
                            }

                            existingKey.size < size -> {
                                val newKey = existingKey + SecureRandom.nextBytes(size - existingKey.size)
                                encryptedSharedPreferences.edit().apply {
                                    putString(id, newKey.encodeBase64())
                                }.apply()
                                newKey
                            }

                            else -> existingKey.copyOf(size)
                        }
                    }, null)
                } catch (exc: Exception) {
                    log.error(exc) { "Cannot read or set secret ('$id')." }
                    null
                }
            }

            @Deprecated("for backwards compatibility")
            override suspend fun getLegacy(): ByteArray? {
                val encryptedSharedPreferences = getEncryptedSharedPreferences(context)
                return encryptedSharedPreferences
                    .getString("secret_byte_array_key_key", null)?.decodeBase64Bytes()
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

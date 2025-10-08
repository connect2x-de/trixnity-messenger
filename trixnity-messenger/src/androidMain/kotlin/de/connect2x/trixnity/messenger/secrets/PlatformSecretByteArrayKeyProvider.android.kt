@file:Suppress("DEPRECATION") // TODO: migrate this to new crypto API eventually

package de.connect2x.trixnity.messenger.secrets

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.connect2x.trixnity.messenger.util.ContextGetter
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.*
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.crypto.core.SecureRandom
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val log = KotlinLogging.logger {}

actual fun platformSecretByteArrayKeyProviderModule(): Module = module {
    single<SecretByteArrayKeyProvider>(named(PLATFORM_SECRET_BYTE_ARRAY_KEY_PROVIDER_ID)) {
        val contextGetter = get<ContextGetter>()
        object : SecretByteArrayKeyProvider {
            override val id = PLATFORM_SECRET_BYTE_ARRAY_KEY_PROVIDER_ID
            override val level: Int = 0

            override suspend fun get(extra: JsonObject?, getInputKey: GetKey?): GetKey {
                return GetKey { size ->
                    try {
                        val encryptedSharedPreferences = getEncryptedSharedPreferences(contextGetter())
                        val existingKey = encryptedSharedPreferences.getString(id, null)?.decodeBase64Bytes()
                        val key = when {
                            existingKey == null -> {
                                val newKey = SecureRandom.nextBytes(size)
                                encryptedSharedPreferences.edit {
                                    putString(id, newKey.encodeBase64())
                                }
                                newKey
                            }

                            existingKey.size < size -> {
                                val newKey = existingKey + SecureRandom.nextBytes(size - existingKey.size)
                                encryptedSharedPreferences.edit {
                                    putString(id, newKey.encodeBase64())
                                }
                                newKey
                            }

                            else -> existingKey.copyOf(size)
                        }
                        if (getInputKey == null) key
                        else hkdfSha256(key = key, salt = getInputKey(32), keyBytesLength = size)
                    } catch (ex: Exception) {
                        throw SecretByteArrayException("cannot read or set secret ('$id')", ex)
                    }
                }
            }

            override suspend fun rotate(
                oldExtra: JsonObject?,
                getOldInputKey: GetKey?,
                getNewInputKey: GetKey?
            ): SecretByteArrayKeyProvider.RotateResult =
                SecretByteArrayKeyProvider.RotateResult(
                    getOldKey = get(null, getOldInputKey),
                    getNewKey = get(null, getNewInputKey),
                    newExtra = null,
                )

            @Deprecated("for backwards compatibility")
            override suspend fun getLegacy(): ByteArray? {
                val encryptedSharedPreferences = getEncryptedSharedPreferences(contextGetter())
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

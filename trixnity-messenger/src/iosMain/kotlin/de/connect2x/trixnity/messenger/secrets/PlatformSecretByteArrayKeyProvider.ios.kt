package de.connect2x.trixnity.messenger.secrets

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.toByteArray
import de.connect2x.trixnity.messenger.util.toNSData
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.crypto.core.SecureRandom
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.CoreFoundation.CFAutorelease
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.OSStatus

private val log = KotlinLogging.logger {}

@OptIn(ExperimentalForeignApi::class)
actual fun platformSecretByteArrayKeyProviderModule(): Module = module {
    single<SecretByteArrayKeyProvider>(named(PLATFORM_SECRET_BYTE_ARRAY_KEY_PROVIDER_ID)) {
        val config = get<MatrixMessengerConfiguration>()
        object : SecretByteArrayKeyProvider {
            override val id = PLATFORM_SECRET_BYTE_ARRAY_KEY_PROVIDER_ID
            override val level: Int = 0

            override suspend fun get(extra: JsonObject?, getInputKey: GetKey?): GetKey {
                return GetKey { size ->
                    try {
                        val appId = config.appId
                        val existingKey = getSecret(appId, id)
                        val key = when {
                            existingKey == null -> {
                                val newKey = SecureRandom.nextBytes(size)
                                context(appId, id, newKey.toNSData()) { (appIdRef, idRef, newKeyRef) ->
                                    val query = query(
                                        kSecClass to kSecClassGenericPassword,
                                        kSecAttrService to appIdRef,
                                        kSecAttrAccount to idRef,
                                        kSecValueData to newKeyRef,
                                    )
                                    SecItemAdd(query, null).checkState()
                                }
                                newKey
                            }

                            existingKey.size < size -> {
                                val newKey = existingKey + SecureRandom.nextBytes(size - existingKey.size)
                                context(appId, id, newKey.toNSData()) { (appIdRef, idRef, newKeyRef) ->
                                    val query = query(
                                        kSecClass to kSecClassGenericPassword,
                                        kSecAttrService to appIdRef,
                                        kSecAttrAccount to idRef,
                                        kSecValueData to newKeyRef,
                                    )
                                    SecItemAdd(query, null).checkState()
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
                return getSecret(config.appId, "secret_byte_array_key_key")
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun getSecret(appId: String, id: String) =
    context(appId, id) { (appIdRef, idRef) ->
        val query = query(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to appIdRef,
            kSecAttrAccount to idRef,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne,
        )
        memScoped {
            val result = alloc<CFTypeRefVar>()
            SecItemCopyMatching(query, result.ptr).checkState()
            CFBridgingRelease(result.value) as? NSData
        }
    }?.toByteArray()

@OptIn(ExperimentalForeignApi::class)
private class Context {
    fun query(vararg pairs: Pair<CFStringRef?, CFTypeRef?>): CFDictionaryRef? {
        val map = mapOf(*pairs)
        return CFDictionaryCreateMutable(
            null, map.size.convert(), null, null
        ).apply {
            map.entries.forEach { CFDictionaryAddValue(this, it.key, it.value) }
        }.apply {
            CFAutorelease(this)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun <T> context(vararg values: Any?, block: Context.(List<CFTypeRef?>) -> T): T {
    val custom = arrayOf(*values).map { CFBridgingRetain(it) }
    return block.invoke(Context(), custom).apply {
        custom.forEach { CFBridgingRelease(it) }
    }
}

private fun OSStatus.checkState() {
    when {
        toUInt() == platform.darwin.noErr -> return
        this == platform.Security.errSecItemNotFound -> {
            log.warn { "SecItem not found: $this" }
            return
        }

        else -> throw IllegalStateException(
            "Keychain access failed: errorCode=$this " + when (this) {
                platform.Security.errSecInteractionNotAllowed -> "errSecInteractionNotAllowed"
                platform.Security.errSecUnimplemented -> "errSecUnimplemented"
                platform.Security.errSecNotAvailable -> "errSecNotAvailable"
                platform.Security.errSecAuthFailed -> "errSecAuthFailed"
                platform.Security.errSecAllocate -> "errSecAllocate"
                platform.Security.errSecDecode -> "errSecDecode"
                platform.Security.errSecBadReq -> "errSecBadReq"
                platform.Security.errSecParam -> "errSecParam"
                platform.Security.errSecFileTooBig -> "errSecFileTooBig"
                platform.Security.errSecInvalidKeyLabel -> "errSecInvalidKeyLabel"
                platform.Security.errSecInvalidAttributeKey -> "errSecInvalidAttributeKey"
                platform.Security.errSecInvalidKeychain -> "errSecInvalidKeychain"
                else -> ""
            }
        )
    }
}

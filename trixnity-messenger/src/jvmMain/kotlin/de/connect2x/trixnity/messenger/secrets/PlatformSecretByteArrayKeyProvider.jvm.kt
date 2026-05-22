package de.connect2x.trixnity.messenger.secrets

import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.platform.mac.CoreFoundation
import com.sun.jna.platform.mac.CoreFoundation.CFBooleanRef
import com.sun.jna.platform.mac.CoreFoundation.CFDataRef
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.ptr.PointerByReference
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.crypto.core.SecureRandom
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.Credential
import de.connect2x.trixnity.messenger.util.MacOsCoreFoundation
import de.connect2x.trixnity.messenger.util.MacOsSecurity
import de.connect2x.trixnity.messenger.util.OS
import de.connect2x.trixnity.messenger.util.SecretNotFoundException
import de.connect2x.trixnity.messenger.util.WinCredentials
import de.connect2x.trixnity.messenger.util.getOs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val log: Logger = Logger("de.connect2x.trixnity.messenger.secrets.PlatformSecretByteArrayKeyProviderKt")

actual fun platformSecretByteArrayKeyProviderModule(): Module = module {
    single<SecretByteArrayKeyProvider>(named(PLATFORM_SECRET_BYTE_ARRAY_KEY_PROVIDER_ID)) {
        val config = get<MatrixMessengerConfiguration>()
        object : SecretByteArrayKeyProvider {
            override val id = PLATFORM_SECRET_BYTE_ARRAY_KEY_PROVIDER_ID
            override val level: Int = 0

            override suspend fun get(extra: JsonObject?, getInputKey: GetKey?): GetKey? {
                val appId = config.appId
                return when (getOs()) {
                    OS.MAC_OS,
                    OS.WINDOWS -> {
                        GetKey { size ->
                            try {
                                val existingKey = getSecret(appId, id)
                                val key =
                                    when {
                                        existingKey == null -> {
                                            val newKey = SecureRandom.nextBytes(size)
                                            setSecret(appId, id, newKey)
                                            newKey
                                        }

                                        existingKey.size < size -> {
                                            val newKey = existingKey + SecureRandom.nextBytes(size - existingKey.size)
                                            setSecret(appId, id, newKey)
                                            newKey
                                        }

                                        else -> existingKey.copyOf(size)
                                    }
                                if (getInputKey == null) key
                                else hkdfSha256(key = key, salt = getInputKey(32), keyBytesLength = size)
                            } catch (exc: Exception) {
                                throw SecretByteArrayException("cannot read or set secret ('$id')", exc)
                            }
                        }
                    }

                    OS.LINUX -> null
                }
            }

            override suspend fun rotate(
                oldExtra: JsonObject?,
                getOldInputKey: GetKey?,
                getNewInputKey: GetKey?,
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

private suspend fun getSecret(appId: String, secretId: String): ByteArray? =
    withContext(Dispatchers.IO) {
        when (getOs()) {
            OS.WINDOWS -> {
                val targetName = "$appId-$secretId"
                val credentialRef = PointerByReference()
                val success = WinCredentials.CredReadA(targetName = targetName, credentialRef = credentialRef)
                log.debug { "Read secret ('$targetName') from credentials store: $success" }
                if (success) {
                    try {
                        val credential = Credential(credentialRef.value)
                        credential.credentialBlob.getByteArray(0, credential.credentialBlobSize)
                    } finally {
                        WinCredentials.CredFree(credentialRef.value)
                    }
                } else {
                    when (val error = Kernel32.INSTANCE.GetLastError()) {
                        Kernel32.ERROR_NOT_FOUND -> {
                            log.warn {
                                "Cannot read secret ('$targetName') from credentials store. Cannot find target name '$targetName'"
                            }
                            null
                        }

                        Kernel32.ERROR_NO_SUCH_LOGON_SESSION ->
                            throw SecretNotFoundException(
                                "Cannot read secret ('$targetName') from credentials store. The logon session cannot be found."
                            )
                        Kernel32.ERROR_INVALID_FLAGS ->
                            throw SecretNotFoundException(
                                "Cannot read secret ('$targetName') from credentials store. The provided flags are invalid."
                            )
                        else ->
                            throw SecretNotFoundException(
                                "Cannot read secret ('$targetName') from credentials store. Unknown error: $error."
                            )
                    }
                }
            }

            OS.MAC_OS -> {
                val attributes =
                    CoreFoundation.INSTANCE.CFDictionaryCreateMutable(null, CoreFoundation.CFIndex(5), null, null)
                        .apply {
                            CoreFoundation.INSTANCE.CFDictionarySetValue(
                                this,
                                CFStringRef(MacOsSecurity.kSecClass),
                                CFStringRef(MacOsSecurity.kSecClassGenericPassword),
                            )
                            CoreFoundation.INSTANCE.CFDictionarySetValue(
                                this,
                                CFStringRef(MacOsSecurity.kSecAttrService),
                                appId.toCFString(),
                            )
                            CoreFoundation.INSTANCE.CFDictionarySetValue(
                                this,
                                CFStringRef(MacOsSecurity.kSecAttrAccount),
                                secretId.toCFString(),
                            )
                            CoreFoundation.INSTANCE.CFDictionarySetValue(
                                this,
                                CFStringRef(MacOsSecurity.kSecReturnData),
                                CFBooleanRef(MacOsCoreFoundation.kCFBooleanTrue),
                            )
                            CoreFoundation.INSTANCE.CFDictionarySetValue(
                                this,
                                CFStringRef(MacOsSecurity.kSecMatchLimit),
                                CFStringRef(MacOsSecurity.kSecMatchLimitOne),
                            )
                        }
                log.debug { "read secret ('$secretId') from keychain" }
                val secItem = PointerByReference()
                val result = MacOsSecurity.SecItemCopyMatching(attributes, secItem)

                CoreFoundation.INSTANCE.CFRelease(attributes)
                when (result) {
                    MacOsSecurity.CODE_SUCCESS -> {
                        log.debug { "Successfully read secret ('$secretId') from keychain." }
                        val secItemData = CFDataRef(secItem.value)
                        val secretValue = secItemData.toByteArray()
                        MacOsSecurity.SecKeychainItemFreeContent(null, secItem.value)
                        secretValue
                    }

                    MacOsSecurity.CODE_NOT_FOUND -> null
                    else -> {
                        val errorMessage = macOsConvertErrorCodeToMessage(result)
                        throw SecretNotFoundException(
                            "Cannot read secret ('$secretId') from keychain. ${errorMessage ?: "Unknown error"}"
                        )
                    }
                }
            }

            OS.LINUX -> throw IllegalStateException("not supported")
        }
    }

private fun setSecret(appId: String, secretId: String, secretValue: ByteArray) {
    when (getOs()) {
        OS.WINDOWS -> {
            val targetName = "$appId-$secretId"
            val memory = Memory(secretValue.size.toLong())
            memory.write(0, secretValue, 0, secretValue.size)
            val success =
                WinCredentials.CredWriteA(
                    Credential(targetName = targetName, credentialBlob = memory, credentialBlobSize = secretValue.size),
                    WinDef.DWORD(0),
                )
            memory.clear()
            log.debug { "Write secret ('$targetName') to credentials store: $success" }
            if (success.not()) {
                when (val error = Kernel32.INSTANCE.GetLastError()) {
                    Kernel32.ERROR_NO_SUCH_LOGON_SESSION ->
                        log.error {
                            "Cannot write secret ('$targetName') to credentials store. The logon session cannot be found."
                        }
                    Kernel32.ERROR_INVALID_PARAMETER ->
                        log.error {
                            "Cannot write secret ('$targetName') to credentials store. Tried to change protected fields of an existing credential."
                        }
                    Kernel32.ERROR_INVALID_FLAGS ->
                        log.error {
                            "Cannot write secret ('$targetName') to credentials store. The provided flags are invalid."
                        }
                    else ->
                        log.error { "Cannot write secret ('$targetName') to credentials store. Unknown error: $error." }
                }
            }
        }

        OS.MAC_OS -> {
            val attributes =
                CoreFoundation.INSTANCE.CFDictionaryCreateMutable(null, CoreFoundation.CFIndex(4), null, null).apply {
                    CoreFoundation.INSTANCE.CFDictionarySetValue(
                        this,
                        CFStringRef(MacOsSecurity.kSecClass),
                        CFStringRef(MacOsSecurity.kSecClassGenericPassword),
                    )
                    CoreFoundation.INSTANCE.CFDictionarySetValue(
                        this,
                        CFStringRef(MacOsSecurity.kSecAttrService),
                        appId.toCFString(),
                    )
                    CoreFoundation.INSTANCE.CFDictionarySetValue(
                        this,
                        CFStringRef(MacOsSecurity.kSecAttrAccount),
                        secretId.toCFString(),
                    )
                    CoreFoundation.INSTANCE.CFDictionarySetValue(
                        this,
                        CFStringRef(MacOsSecurity.kSecValueData),
                        secretValue.toCFData(),
                    )
                }
            log.debug { "write secret ('$secretId') to keychain" }
            val result = MacOsSecurity.SecItemAdd(attributes, null)
            CoreFoundation.INSTANCE.CFRelease(attributes)
            when (result) {
                MacOsSecurity.CODE_SUCCESS -> log.debug { "Successfully wrote secret ('$secretId') to keychain." }
                else -> {
                    val errorMessage = macOsConvertErrorCodeToMessage(result)
                    log.error { "Cannot save secret ('$secretId') to keychain. ${errorMessage ?: "Unknown error."}" }
                    throw IllegalStateException(errorMessage)
                }
            }
        }

        OS.LINUX -> throw IllegalStateException("not supported")
    }
}

private fun macOsConvertErrorCodeToMessage(errorCode: Int): String? {
    val messagePointer: Pointer = MacOsSecurity.SecCopyErrorMessageString(errorCode, null) ?: return null
    val charArray = CharArray(MacOsCoreFoundation.CFStringGetLength(messagePointer).toInt())
    for (i in charArray.indices) {
        charArray[i] = MacOsCoreFoundation.CFStringGetCharacterAtIndex(messagePointer, i.toLong())
    }
    MacOsCoreFoundation.CFRelease(messagePointer)
    return String(charArray)
}

private fun String.toCFString(): CFStringRef {
    val chars = this.toCharArray()
    return CoreFoundation.INSTANCE.CFStringCreateWithCharacters(
        null,
        chars,
        CoreFoundation.CFIndex(chars.size.toLong()),
    )
}

private fun ByteArray.toCFData(): CFDataRef {
    val memory = Memory(size.toLong())
    memory.write(0, this, 0, size)
    return CoreFoundation.INSTANCE.CFDataCreate(null, memory, CoreFoundation.CFIndex(memory.size()))
}

private fun CFDataRef.toByteArray(): ByteArray {
    val bytePointer = CoreFoundation.INSTANCE.CFDataGetBytePtr(this)
    val byteLength = CoreFoundation.INSTANCE.CFDataGetLength(this).toInt()
    return bytePointer.getByteArray(0, byteLength)
}

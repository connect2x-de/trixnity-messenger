package de.connect2x.trixnity.messenger.util

import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.ptr.PointerByReference
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.folivo.trixnity.crypto.core.SecureRandom
import org.koin.core.module.Module
import org.koin.dsl.module

private val log = KotlinLogging.logger { }

actual fun platformGetPlatformSecret(): Module = module {
    single<GetPlatformSecret> {
        GetPlatformSecret { id, sizeOnCreate ->
            val appId = get<MatrixMessengerConfiguration>().appId
            when (getOs()) {
                OS.MAC_OS, OS.WINDOWS -> {
                    try {
                        val existingKey = getSecret(appId, id)?.decodeBase64Bytes()
                        if (existingKey == null) {
                            val newKey = SecureRandom.nextBytes(sizeOnCreate)
                            setSecret(appId, id, newKey.encodeBase64())
                            newKey
                        } else {
                            existingKey
                        }
                    } catch (exc: Exception) {
                        log.error(exc) { "Cannot read or set secret ('$id')." }
                        null
                    }
                }

                OS.LINUX -> null
            }
        }
    }
}

private suspend fun getSecret(appId: String, secretId: String): String? = withContext(Dispatchers.IO) {
    when (getOs()) {
        OS.WINDOWS -> {
            val targetName = "$appId-$secretId"
            val credentialRef = PointerByReference()
            val success = WinCredentials.CredReadA(targetName = targetName, credentialRef = credentialRef)
            log.debug { "Read secret ('$targetName') from credentials store: $success" }
            if (success) {
                try {
                    val credential = Credential(credentialRef.value)
                    credential.credentialBlob.getByteArray(0, credential.credentialBlobSize).decodeToString()
                } finally {
                    WinCredentials.CredFree(credentialRef.value)
                }
            } else {
                when (val error = Kernel32.INSTANCE.GetLastError()) {
                    Kernel32.ERROR_NOT_FOUND -> {
                        log.warn { "Cannot read secret ('$targetName') from credentials store. Cannot find target name '$targetName'" }
                        null
                    }

                    Kernel32.ERROR_NO_SUCH_LOGON_SESSION -> throw SecretNotFoundException("Cannot read secret ('$targetName') from credentials store. The logon session cannot be found.")
                    Kernel32.ERROR_INVALID_FLAGS -> throw SecretNotFoundException("Cannot read secret ('$targetName') from credentials store. The provided flags are invalid.")
                    else -> throw SecretNotFoundException("Cannot read secret ('$targetName') from credentials store. Unknown error: $error.")
                }
            }
        }

        OS.MAC_OS -> {
            val serviceName = appId.toByteArray()
            val accountName = secretId.toByteArray()
            val passwordSize = IntArray(1)
            val passwordRef = PointerByReference()
            val itemRef = PointerByReference()
            val result = MacOsCredentials.SecKeychainFindGenericPassword(
                null,
                serviceName.size,
                serviceName,
                accountName.size,
                accountName,
                passwordSize,
                passwordRef,
                itemRef,
            )
            when (result) {
                0 -> {
                    log.debug { "Read secret ('$secretId') from keychain." }
                    val pointer = passwordRef.value
                    val password = pointer?.getByteArray(0, passwordSize[0])?.decodeToString()
                    MacOsCredentials.SecKeychainItemFreeContent(null, pointer)
                    password
                }

                -25300 -> {
                    log.warn { "Cannot find secret ('$secretId') in keychain." }
                    null
                }

                else -> {
                    val errorMessage = macOsConvertErrorCodeToMessage(result)
                    throw SecretNotFoundException("Cannot read secret ('$secretId') from keychain. ${errorMessage ?: "Unknown error"}")
                }
            }
        }

        OS.LINUX -> throw IllegalStateException("not supported")
    }
}

private fun setSecret(appId: String, secretId: String, secretValue: String) {
    when (getOs()) {
        OS.WINDOWS -> {
            val targetName = "$appId-$secretId"
            val byteArray = secretValue.toByteArray()
            val memory = Memory(byteArray.size.toLong())
            memory.write(0, byteArray, 0, byteArray.size)
            val success = WinCredentials.CredWriteA(
                Credential(
                    targetName = targetName,
                    credentialBlob = memory,
                    credentialBlobSize = byteArray.size
                ),
                WinDef.DWORD(0)
            )
            memory.clear()
            log.debug { "Write secret ('$targetName') to credentials store: $success" }
            if (success.not()) {
                when (val error = Kernel32.INSTANCE.GetLastError()) {
                    Kernel32.ERROR_NO_SUCH_LOGON_SESSION -> log.error { "Cannot write secret ('$targetName') to credentials store. The logon session cannot be found." }
                    Kernel32.ERROR_INVALID_PARAMETER -> log.error { "Cannot write secret ('$targetName') to credentials store. Tried to change protected fields of an existing credential." }
                    Kernel32.ERROR_INVALID_FLAGS -> log.error { "Cannot write secret ('$targetName') to credentials store. The provided flags are invalid." }
                    else -> log.error { "Cannot write secret ('$targetName') to credentials store. Unknown error: $error." }
                }
            }
        }

        OS.MAC_OS -> {
            val serviceName = appId.toByteArray()
            val accountName = secretId.toByteArray()
            val itemRef = PointerByReference()
            val secretByteArray = secretValue.toByteArray()
            val pointer = itemRef.value
            if (pointer == null) {
                val result = MacOsCredentials.SecKeychainAddGenericPassword(
                    null,
                    serviceName.size,
                    serviceName,
                    accountName.size,
                    accountName,
                    secretByteArray.size,
                    secretByteArray
                )
                when (result) {
                    0 -> log.debug { "Successfully wrote secret ('$secretId') to keychain." }
                    else -> {
                        val errorMessage = macOsConvertErrorCodeToMessage(result)
                        log.error { "Cannot save secret ('$secretId') to keychain. ${errorMessage ?: "Unknown error."}" }
                    }
                }
            }
            secretByteArray.fill(0)
        }

        OS.LINUX -> throw IllegalStateException("not supported")
    }
}

private fun macOsConvertErrorCodeToMessage(errorCode: Int): String? {
    val messagePointer: Pointer = MacOsCredentials.SecCopyErrorMessageString(errorCode, null) ?: return null
    val charArray = CharArray(CoreFoundationLibrary.CFStringGetLength(messagePointer).toInt())
    for (i in charArray.indices) {
        charArray[i] = CoreFoundationLibrary.CFStringGetCharacterAtIndex(messagePointer, i.toLong())
    }
    CoreFoundationLibrary.CFRelease(messagePointer)
    return String(charArray)
}

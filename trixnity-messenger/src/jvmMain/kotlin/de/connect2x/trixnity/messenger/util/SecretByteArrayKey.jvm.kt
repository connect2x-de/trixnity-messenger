package de.connect2x.trixnity.messenger.util

import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.ptr.PointerByReference
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.*
import net.folivo.trixnity.crypto.core.SecureRandom
import org.koin.core.module.Module
import org.koin.dsl.module

private val log = KotlinLogging.logger { }

actual fun platformGetPlatformSecret(): Module = module {
    single<GetPlatformSecret> {
        GetPlatformSecret { id, sizeOnCreate ->
            when (getOs()) {
                OS.MAC_OS, OS.WINDOWS -> {
                    try {
                        val existingKey = getSecret(id)?.decodeBase64Bytes()
                        if (existingKey == null) {
                            val newKey = SecureRandom.nextBytes(sizeOnCreate)
                            setSecret(id, newKey.encodeBase64())
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

private fun getSecret(id: String): String? {
    return when (getOs()) {
        OS.WINDOWS -> {
            val credentialRef = PointerByReference()
            val success = WinCredentials.CredReadA(targetName = id, credentialRef = credentialRef)
            log.debug { "Read secret ('$id') from credentials store: $success" }
            return if (success) {
                try {
                    val credential = Credential(credentialRef.value)
                    credential.credentialBlob.getByteArray(0, credential.credentialBlobSize).decodeToString()
                } finally {
                    WinCredentials.CredFree(credentialRef.value)
                }
            } else {
                when (val error = Kernel32.INSTANCE.GetLastError()) {
                    Kernel32.ERROR_NOT_FOUND -> {
                        log.warn { "Cannot read secret ('$id') from credentials store. Cannot find target name '$id'" }
                        null
                    }

                    Kernel32.ERROR_NO_SUCH_LOGON_SESSION -> throw SecretNotFoundException("Cannot read secret ('$id') from credentials store. The logon session cannot be found.")
                    Kernel32.ERROR_INVALID_FLAGS -> throw SecretNotFoundException("Cannot read secret ('$id') from credentials store. The provided flags are invalid.")
                    else -> throw SecretNotFoundException("Cannot read secret ('$id') from credentials store. Unknown error: $error.")
                }
            }
        }

        OS.MAC_OS -> {
            val serviceName = id.toByteArray()
            val passwordSize = IntArray(1)
            val passwordRef = PointerByReference()
            val itemRef = PointerByReference()
            val result = MacOsCredentials.SecKeychainFindGenericPassword(
                null,
                serviceName.size,
                serviceName,
                0,
                null,
                passwordSize,
                passwordRef,
                itemRef,
            )
            when (result) {
                0 -> {
                    log.debug { "Read secret ('$id') from keychain." }
                    val pointer = passwordRef.value
                    val password = pointer?.getByteArray(0, passwordSize[0])?.decodeToString()
                    MacOsCredentials.SecKeychainItemFreeContent(null, pointer)
                    password
                }

                -25300 -> {
                    log.warn { "Cannot find secret ('$id') in keychain." }
                    null
                }

                else -> {
                    val errorMessage = macOsConvertErrorCodeToMessage(result)
                    throw SecretNotFoundException("Cannot read secret ('$id') from keychain. ${errorMessage ?: "Unknown error"}")
                }
            }
        }

        OS.LINUX -> throw IllegalStateException("not supported")
    }
}

private fun setSecret(id: String, secret: String) {
    when (getOs()) {
        OS.WINDOWS -> {
            val byteArray = secret.toByteArray()
            val memory = Memory(byteArray.size.toLong())
            memory.write(0, byteArray, 0, byteArray.size)
            val success = WinCredentials.CredWriteA(
                Credential(
                    targetName = id,
                    credentialBlob = memory,
                    credentialBlobSize = byteArray.size
                ),
                WinDef.DWORD(0)
            )
            memory.clear()
            log.debug { "Write secret ('$id') to credentials store: $success" }
            if (success.not()) {
                when (val error = Kernel32.INSTANCE.GetLastError()) {
                    Kernel32.ERROR_NO_SUCH_LOGON_SESSION -> log.error { "Cannot write secret ('$id') to credentials store. The logon session cannot be found." }
                    Kernel32.ERROR_INVALID_PARAMETER -> log.error { "Cannot write secret ('$id') to credentials store. Tried to change protected fields of an existing credential." }
                    Kernel32.ERROR_INVALID_FLAGS -> log.error { "Cannot write secret ('$id') to credentials store. The provided flags are invalid." }
                    else -> log.error { "Cannot write secret ('$id') to credentials store. Unknown error: $error." }
                }
            }
        }

        OS.MAC_OS -> {
            val serviceName = id.toByteArray()
            val itemRef = PointerByReference()
            val secretByteArray = secret.toByteArray()
            val pointer = itemRef.value
            if (pointer == null) {
                val result = MacOsCredentials.SecKeychainAddGenericPassword(
                    null,
                    serviceName.size,
                    serviceName,
                    0,
                    null,
                    secretByteArray.size,
                    secretByteArray
                )
                when (result) {
                    0 -> log.debug { "Successfully wrote secret ('$id') to keychain." }
                    else -> {
                        val errorMessage = macOsConvertErrorCodeToMessage(result)
                        log.error { "Cannot save secret ('$id') to keychain. ${errorMessage ?: "Unknown error."}" }
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
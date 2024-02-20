package de.connect2x.trixnity.messenger.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.*
import net.folivo.trixnity.crypto.core.SecureRandom
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.CoreFoundation.*
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Security.*
import platform.darwin.OSStatus
import platform.darwin.noErr

private val log = KotlinLogging.logger { }

@OptIn(ExperimentalForeignApi::class)
actual fun platformGetPlatformSecret(): Module = module {
    single<GetPlatformSecret> {
        GetPlatformSecret { id, sizeOnCreate ->
            try {
                val existingKey = context(id) { (idRef) ->
                    val query = query(
                        kSecClass to kSecClassGenericPassword,
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
                if (existingKey == null) {
                    val newKey = SecureRandom.nextBytes(sizeOnCreate)
                    context(id, newKey.toNSData()) { (idRef, newKeyRef) ->
                        val query = query(
                            kSecClass to kSecClassGenericPassword,
                            kSecAttrAccount to idRef,
                            kSecValueData to newKeyRef,
                        )
                        SecItemAdd(query, null).checkState()
                    }
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
    check(toUInt() == noErr) {
        "Keychain access failed: errorCode=$this " + when (this) {
            errSecInteractionNotAllowed -> "errSecInteractionNotAllowed"
            errSecUnimplemented -> "errSecUnimplemented"
            errSecNotAvailable -> "errSecNotAvailable"
            errSecItemNotFound -> "errSecItemNotFound"
            errSecAuthFailed -> "errSecAuthFailed"
            errSecAllocate -> "errSecAllocate"
            errSecDecode -> "errSecDecode"
            errSecBadReq -> "errSecBadReq"
            errSecParam -> "errSecParam"
            errSecFileTooBig -> "errSecFileTooBig"
            errSecInvalidKeyLabel -> "errSecInvalidKeyLabel"
            errSecInvalidAttributeKey -> "errSecInvalidAttributeKey"
            errSecInvalidKeychain -> "errSecInvalidKeychain"
            else -> ""
        }
    }
}
package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import net.folivo.trixnity.crypto.core.SecureRandom
import org.koin.core.module.Module
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
import platform.Security.errSecAllocate
import platform.Security.errSecAuthFailed
import platform.Security.errSecBadReq
import platform.Security.errSecDecode
import platform.Security.errSecFileTooBig
import platform.Security.errSecInteractionNotAllowed
import platform.Security.errSecInvalidAttributeKey
import platform.Security.errSecInvalidKeyLabel
import platform.Security.errSecInvalidKeychain
import platform.Security.errSecItemNotFound
import platform.Security.errSecNotAvailable
import platform.Security.errSecParam
import platform.Security.errSecUnimplemented
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.OSStatus
import platform.darwin.noErr

private val log = KotlinLogging.logger { }

@OptIn(ExperimentalForeignApi::class)
actual fun platformGetPlatformSecret(): Module = module {
    single<GetPlatformSecret> {
        GetPlatformSecret { id, sizeOnCreate ->
            try {
                val appId = get<MatrixMessengerConfiguration>().appId
                val existingKey = context(appId, id) { (appIdRef, idRef) ->
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
                if (existingKey == null) {
                    val newKey = SecureRandom.nextBytes(sizeOnCreate)
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

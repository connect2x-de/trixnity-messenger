package de.connect2x.trixnity.messenger.util

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef
import com.sun.jna.ptr.PointerByReference

object MacOsSecurity : Library {
    private const val JNA_LIBRARY_NAME: String = "Security"
    private val JNA_NATIVE_LIB: NativeLibrary = NativeLibrary.getInstance(JNA_LIBRARY_NAME)

    const val CODE_SUCCESS: Int = 0
    const val CODE_NOT_FOUND: Int = -25300

    init {
        Native.register(MacOsSecurity::class.java, JNA_NATIVE_LIB)
    }

    @JvmStatic
    val kSecClass: Pointer =
        JNA_NATIVE_LIB.getGlobalVariableAddress("kSecClass").getPointer(0L)
            ?: throw IllegalStateException("kSecClass not found")

    @JvmStatic
    val kSecAttrService: Pointer =
        JNA_NATIVE_LIB.getGlobalVariableAddress("kSecAttrService").getPointer(0L)
            ?: throw IllegalStateException("kSecAttrService not found")

    @JvmStatic
    val kSecAttrAccount: Pointer =
        JNA_NATIVE_LIB.getGlobalVariableAddress("kSecAttrAccount").getPointer(0L)
            ?: throw IllegalStateException("kSecAttrAccount not found")

    @JvmStatic
    val kSecReturnData: Pointer =
        JNA_NATIVE_LIB.getGlobalVariableAddress("kSecReturnData").getPointer(0L)
            ?: throw IllegalStateException("kSecReturnData not found")

    @JvmStatic
    val kSecValueData: Pointer =
        JNA_NATIVE_LIB.getGlobalVariableAddress("kSecValueData").getPointer(0L)
            ?: throw IllegalStateException("kSecValueData not found")

    @JvmStatic
    val kSecMatchLimit: Pointer =
        JNA_NATIVE_LIB.getGlobalVariableAddress("kSecMatchLimit").getPointer(0L)
            ?: throw IllegalStateException("kSecMatchLimit not found")

    @JvmStatic
    val kSecClassGenericPassword: Pointer =
        JNA_NATIVE_LIB.getGlobalVariableAddress("kSecClassGenericPassword").getPointer(0L)
            ?: throw IllegalStateException("kSecClassGenericPassword not found")

    @JvmStatic
    val kSecMatchLimitOne: Pointer =
        JNA_NATIVE_LIB.getGlobalVariableAddress("kSecMatchLimitOne").getPointer(0L)
            ?: throw IllegalStateException("kSecMatchLimitOne not found")

    external fun SecItemCopyMatching(query: CFDictionaryRef?, result: PointerByReference?): Int

    external fun SecItemAdd(attributes: CFDictionaryRef?, result: PointerByReference?): Int

    external fun SecKeychainItemFreeContent(attrList: CFDictionaryRef?, data: Pointer?)

    external fun SecCopyErrorMessageString(status: Int, reserved: Pointer?): Pointer?
}

object MacOsCoreFoundation : Library {
    private const val JNA_LIBRARY_NAME: String = "CoreFoundation"
    private val JNA_NATIVE_LIB: NativeLibrary = NativeLibrary.getInstance(JNA_LIBRARY_NAME)

    init {
        Native.register(MacOsCoreFoundation::class.java, JNA_NATIVE_LIB)
    }

    @JvmStatic
    val kCFBooleanTrue: Pointer =
        JNA_NATIVE_LIB.getGlobalVariableAddress("kCFBooleanTrue").getPointer(0L)
            ?: throw IllegalStateException("kCFBooleanTrue not found")

    external fun CFStringGetLength(theString: Pointer?): Long

    external fun CFStringGetCharacterAtIndex(theString: Pointer?, idx: Long): Char

    external fun CFRelease(cf: Pointer?)
}

package de.connect2x.trixnity.messenger.util

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference

object MacOsCredentials : Library {
    private const val JNA_LIBRARY_NAME: String = "Security"
    private val JNA_NATIVE_LIB: NativeLibrary = NativeLibrary.getInstance(JNA_LIBRARY_NAME)

    init {
        Native.register(MacOsCredentials::class.java, JNA_NATIVE_LIB)
    }

    external fun SecKeychainAddGenericPassword(
        keychain: Pointer?,
        serviceNameLength: Int,
        serviceName: ByteArray,
        accountNameLength: Int,
        accountName: ByteArray?,
        passwordLength: Int,
        passwordData: ByteArray?,
        itemRef: Pointer? = null
    ): Int

    external fun SecKeychainFindGenericPassword(
        keychainOrArray: Pointer?,
        serviceNameLength: Int,
        serviceName: ByteArray,
        accountNameLength: Int,
        accountName: ByteArray?,
        passwordLength: IntArray?,
        passwordData: PointerByReference?,
        itemRef: PointerByReference?
    ): Int

    external fun SecKeychainItemFreeContent(attrList: Pointer?, data: Pointer?)

    external fun SecCopyErrorMessageString(status: Int, reserved: Pointer?): Pointer?
}

object CoreFoundationLibrary : Library {
    private const val JNA_LIBRARY_NAME: String = "CoreFoundation"
    private val JNA_NATIVE_LIB: NativeLibrary = NativeLibrary.getInstance(JNA_LIBRARY_NAME)

    init {
        Native.register(CoreFoundationLibrary::class.java, JNA_NATIVE_LIB)
    }

    external fun CFStringGetLength(theString: Pointer?): Long

    external fun CFStringGetCharacterAtIndex(theString: Pointer?, idx: Long): Char

    external fun CFRelease(cf: Pointer?)
}
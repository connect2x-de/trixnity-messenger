package de.connect2x.trixnity.messenger.util

import com.sun.jna.*
import com.sun.jna.platform.win32.WinBase.FILETIME
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.ptr.PointerByReference

object WinCredentials : Library {
    private const val JNA_LIBRARY_NAME: String = "Advapi32"
    private val JNA_NATIVE_LIB: NativeLibrary = NativeLibrary.getInstance(JNA_LIBRARY_NAME)

    init {
        Native.register(WinCredentials::class.java, JNA_NATIVE_LIB)
    }

    external fun CredReadA(
        targetName: String,
        type: DWORD = DWORD(1), // GENERIC
        flags: DWORD = DWORD(0),
        credentialRef: PointerByReference,
    ): Boolean

    external fun CredWriteA(
        credential: Credential,
        flags: DWORD,
    ): Boolean

    external fun CredFree(
        credential: Pointer,
    ): Boolean

    external fun CredDeleteA(
        targetName: String,
    )
}


class Credential(pointer: Pointer?) : Structure(pointer) {

    constructor(
        targetName: String,
        credentialBlobSize: Int,
        credentialBlob: Pointer,
    ) : this(null) {
        this.targetName = targetName
        this.credentialBlobSize = credentialBlobSize
        this.credentialBlob = credentialBlob
    }

    init {
        read()
    }

    @JvmField
    var flags: Int = 0

    @JvmField
    var type: Int = 1 // GENERIC

    lateinit var targetName: String
    lateinit var comment: String
    lateinit var lastWritten: FILETIME

    @JvmField
    var credentialBlobSize: Int = 0

    lateinit var credentialBlob: Pointer

    @JvmField
    var persist: Int = 2 // PERSIST LOCALLY

    @JvmField
    var attributeCount: Int = 0

    lateinit var attributes: Pointer
    lateinit var targetAlias: String
    lateinit var userName: String

    override fun getFieldOrder(): MutableList<String> {
        return mutableListOf(
            "flags",
            "type",
            "targetName",
            "comment",
            "lastWritten",
            "credentialBlobSize",
            "credentialBlob",
            "persist",
            "attributeCount",
            "attributes",
            "targetAlias",
            "userName"
        )
    }
}

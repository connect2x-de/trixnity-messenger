package de.connect2x.trixnity.messenger

/**
 * Dummy implementations for Android custom parcelization.
 */

actual typealias RawValue = JvmWildcard // we have to define an implementation here that does nothing harmful

actual interface Parceler<T> {
    actual fun T.write(parcel: Parcel, flags: Int)
    actual fun create(parcel: Parcel): T
}

actual class Parcel {

    actual fun writeBoolean(b: Boolean) {
    }

    actual fun writeInt(i: Int) {
    }

    actual fun writeString(s: String?) {
    }

    actual fun writeArray(a: Array<*>?) {}

    actual fun readBoolean(): Boolean {
        return false
    }

    actual fun readInt(): Int {
        return 0
    }

    actual fun readString(): String? {
        return null
    }

    actual fun readArray(classLoader: ClassLoader?): Array<*>? {
        return null
    }
}

actual typealias ClassLoader = java.lang.ClassLoader

actual fun getClassLoader() = String::class.java.classLoader

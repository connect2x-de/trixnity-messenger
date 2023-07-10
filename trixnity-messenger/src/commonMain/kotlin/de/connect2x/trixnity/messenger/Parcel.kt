package de.connect2x.trixnity.messenger

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
expect annotation class RawValue()

expect interface Parceler<T> {
    fun T.write(parcel: Parcel, flags: Int)
    fun create(parcel: Parcel): T
}

expect class Parcel {
    fun writeBoolean(b: Boolean)
    fun writeInt(i: Int)
    fun writeString(s: String?)
    fun writeArray(a: Array<*>?)

    fun readBoolean(): Boolean
    fun readInt(): Int
    fun readString(): String?
    fun readArray(classLoader: ClassLoader?): Array<*>?
}

expect abstract class ClassLoader

expect fun getClassLoader(): ClassLoader
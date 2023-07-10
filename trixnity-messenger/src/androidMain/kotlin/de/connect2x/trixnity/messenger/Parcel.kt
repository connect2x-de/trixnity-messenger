package de.connect2x.trixnity.messenger

import android.os.Parcel

actual typealias RawValue = kotlinx.parcelize.RawValue

actual typealias Parceler<T> = kotlinx.parcelize.Parceler<T>

actual typealias Parcel = Parcel

actual typealias ClassLoader = java.lang.ClassLoader

actual fun getClassLoader(): ClassLoader = Parcel::javaClass.javaClass.classLoader
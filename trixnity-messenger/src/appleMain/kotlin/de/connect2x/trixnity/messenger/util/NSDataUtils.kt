package de.connect2x.trixnity.messenger.util

import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(
        bytes = allocArrayOf(this@toNSData),
        length = this@toNSData.size.convert()
    )
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray = ByteArray(this@toByteArray.length.toInt()).apply {
    if (isNotEmpty())
        usePinned {
            memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
}
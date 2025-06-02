package de.connect2x.trixnity.messenger.util

import android.graphics.BitmapFactory
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray

actual suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow, maxMediaSize: Long): Pair<Int?, Int?> {
    val byteArray = byteArrayFlow.toByteArray(maxMediaSize)
    val bitmap = byteArray?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    return bitmap?.width to bitmap?.height
}


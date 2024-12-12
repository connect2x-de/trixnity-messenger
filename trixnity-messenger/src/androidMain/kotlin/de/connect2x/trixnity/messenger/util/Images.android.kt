package de.connect2x.trixnity.messenger.util

import android.graphics.BitmapFactory
import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import net.folivo.trixnity.utils.ByteArrayFlow

actual suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow, maxMediaSize: Long): Pair<Int?, Int?> {
    val byteArray = byteArrayFlow.limitedByteArrayOrNull(maxMediaSize)
    val bitmap = byteArray?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    return bitmap?.width to bitmap?.height
}


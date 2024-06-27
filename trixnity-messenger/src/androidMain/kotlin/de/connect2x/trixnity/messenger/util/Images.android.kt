package de.connect2x.trixnity.messenger.util

import android.graphics.BitmapFactory
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray

actual suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow): Pair<Int?, Int?> {
    val byteArray = byteArrayFlow.toByteArray()
    val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    return bitmap?.width to bitmap?.height
}

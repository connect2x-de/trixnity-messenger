package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray
import org.apache.tika.Tika

actual suspend fun guessFileType(byteArrayFlow: ByteArrayFlow): String {
    val mimeType = Tika().detect(byteArrayFlow.toByteArray())
    return mimeType
}
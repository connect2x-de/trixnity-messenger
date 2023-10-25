package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.files.Blob
import org.w3c.files.File
import org.w3c.files.FileReader

actual suspend fun guessFileType(byteArrayFlow: ByteArrayFlow): String {
    var fileType = ""

    val file = File(emptyArray(), "")
    val fileReader = FileReader()
    fileReader.onloadend = { _ ->
        if (fileReader.readyState == FileReader.DONE) {
            val result = Uint8Array(fileReader.result as ArrayBuffer)
            var bytes = emptyArray<String>()
            var i = 0
            while (i < result.length) {
                bytes += result[i].toString(16)
                i++
            }
            val hex = bytes.joinToString { it }.uppercase()
            println("------- ${file.type}, $hex")
            fileType = file.type
        }
    }
    val blob = byteArrayFlow.toByteArray().slice(IntRange(0, 4))
    fileReader.readAsArrayBuffer(Blob(blob.toTypedArray()))

    return fileType
}
package de.connect2x.trixnity.messenger.util

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import platform.Foundation.NSMakeRange
import platform.Foundation.NSString
import platform.Foundation.NSStringEnumerationByComposedCharacterSequences
import platform.Foundation.enumerateSubstringsInRange

@OptIn(ExperimentalForeignApi::class)
actual val String.graphemeCount: Int
    get() {
        var count = 0
        (this as NSString).enumerateSubstringsInRange(
            range = NSMakeRange(0U, length.toULong()),
            options = NSStringEnumerationByComposedCharacterSequences
        ) { _, _, _, _ ->
            ++count
        }
        return count
    }

@OptIn(ExperimentalForeignApi::class)
actual inline fun String.forEachGrapheme(crossinline consumer: (graph: String, index: Int) -> Boolean) {
    var index = 0
    (this as NSString).enumerateSubstringsInRange(
        range = NSMakeRange(0U, length.toULong()),
        options = NSStringEnumerationByComposedCharacterSequences
    ) { graph, _, _, stop ->
        val result = consumer(requireNotNull(graph), index)
        requireNotNull(stop).reinterpret<ByteVar>()[0] = if(result) 0 else 1
        ++index
    }
}

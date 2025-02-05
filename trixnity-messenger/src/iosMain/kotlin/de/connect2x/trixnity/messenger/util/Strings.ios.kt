package de.connect2x.trixnity.messenger.util

import kotlinx.cinterop.ExperimentalForeignApi
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
actual inline fun String.forEachGrapheme(crossinline consumer: (graph: String, index: Int) -> Unit) {
    var index = 0
    (this as NSString).enumerateSubstringsInRange(
        range = NSMakeRange(0U, length.toULong()),
        options = NSStringEnumerationByComposedCharacterSequences
    ) { graph, _, _, _ ->
        consumer(requireNotNull(graph), index)
        ++index
    }
}

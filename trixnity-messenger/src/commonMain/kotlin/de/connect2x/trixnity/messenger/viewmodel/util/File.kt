package de.connect2x.trixnity.messenger.viewmodel.util


fun checkFileSizeExceedsLimit(fileSize: Long?, maxSizeMB: Int): Boolean {
    // If we do not have any size information available, we should assume that the file size is too large
    // We already to that: see trixnity-messenger/src/commonMain/kotlin/de/connect2x/trixnity/messenger/viewmodel/room/timeline/elements/util/Thumbnails.kt
    if (fileSize == null) {
        return true
    }
    val maxSizeBytes = maxSizeMB * 1_000_000L // Convert megabytes to bytes
    return fileSize > maxSizeBytes
}

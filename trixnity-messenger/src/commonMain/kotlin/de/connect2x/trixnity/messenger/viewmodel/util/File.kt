package de.connect2x.trixnity.messenger.viewmodel.util


fun checkFileSizeExceedsLimit(fileSize: Long?, maxSizeBytes: Long): Boolean {
    // If we do not have any size information available, we should assume that the file size is too large
    // We already to that: see trixnity-messenger/src/commonMain/kotlin/de/connect2x/trixnity/messenger/viewmodel/room/timeline/elements/util/Thumbnails.kt
    if (fileSize == null) {
        return true
    }
    return fileSize > maxSizeBytes
}

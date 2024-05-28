package de.connect2x.trixnity.messenger.viewmodel.util


fun checkFileSizeExceedsLimit(fileSize: Int?, maxSizeMB: Int): Boolean {
    val maxSizeBytes = maxSizeMB * 1_000_000L // Convert megabytes to bytes
    return (fileSize?.compareTo(maxSizeBytes) ?: 0) > 0}

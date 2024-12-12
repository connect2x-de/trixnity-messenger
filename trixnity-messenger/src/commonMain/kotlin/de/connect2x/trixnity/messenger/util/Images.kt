package de.connect2x.trixnity.messenger.util

import net.folivo.trixnity.utils.ByteArrayFlow

// TODO find a way to get the image dimensions without loading the image into memory
expect suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow, maxMediaSize: Long): Pair<Int?, Int?>

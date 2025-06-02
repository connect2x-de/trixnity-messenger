package de.connect2x.trixnity.messenger.util

import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.module.Module

fun interface GetImageDimensions {
    suspend operator fun invoke(byteArrayFlow: ByteArrayFlow, maxMediaSize: Long): Pair<Int?, Int?>
}

// TODO find a way to get the image dimensions without loading the image into memory
expect fun platformGetImageDimensionsModule(): Module

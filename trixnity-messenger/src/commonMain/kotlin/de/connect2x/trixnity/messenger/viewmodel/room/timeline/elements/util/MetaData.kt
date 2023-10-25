package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import net.folivo.trixnity.utils.ByteArrayFlow

data class MetaData(
    val fileType: String
)

expect suspend fun guessFileType(byteArrayFlow: ByteArrayFlow): String
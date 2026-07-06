package de.connect2x.trixnity.messenger

interface AppIcon {
    suspend fun readBytes(): ByteArray
}

data class ByteArrayAppIcon(val data: ByteArray) : AppIcon {
    override suspend fun readBytes(): ByteArray = data

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ByteArrayAppIcon

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}

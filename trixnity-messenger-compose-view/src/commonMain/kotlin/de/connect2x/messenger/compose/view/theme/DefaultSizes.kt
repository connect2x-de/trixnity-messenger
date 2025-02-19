package de.connect2x.messenger.compose.view.theme

interface DefaultSizes {
    val fontSize: Float
    val displaySize: Float
}

class DefaultSizesImpl : DefaultSizes {
    override val fontSize: Float = 1.0f
    override val displaySize: Float = 1.0f
}

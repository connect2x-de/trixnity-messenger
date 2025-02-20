package de.connect2x.messenger.compose.view.theme

interface DefaultSizes {
    val minFontSize: Float
    val maxFontSize: Float
    val fontSize: Float

    val minDisplaySize: Float
    val maxDisplaySize: Float
    val displaySize: Float
}

class DefaultSizesImpl : DefaultSizes {
    override val minFontSize: Float = 0.7f
    override val maxFontSize: Float = 1.3f
    override val fontSize: Float = 1.0f

    override val minDisplaySize: Float = 0.5f
    override val maxDisplaySize: Float = 1.5f
    override val displaySize: Float = 1.0f
}

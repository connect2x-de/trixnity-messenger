package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.runtime.Immutable

@Immutable
interface DefaultSizes {
    val minFontSize: Float
    val maxFontSize: Float
    val fontSize: Float

    val minDisplaySize: Float
    val maxDisplaySize: Float
    val displaySize: Float
}

@Immutable
class DefaultSizesImpl : DefaultSizes {
    // 50% 75% 100% 125% 150% 175% 200%
    override val minFontSize: Float = 0.50f
    override val maxFontSize: Float = 2.00f
    override val fontSize: Float = 1.0f

    override val minDisplaySize: Float = 0.5f
    override val maxDisplaySize: Float = 1.5f
    override val displaySize: Float = 1.0f
}

package de.connect2x.messenger.compose.view.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class MoonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val center = size.center
        val radius = size.minDimension / 2f
        val mainCircle = Path().apply {
            addOval(Rect(center, radius))
        }
        val initialOffset = center - Offset(-radius * 0.55f, radius * 0.55f)
        val subtractCircle = Path().apply {
            addOval(Rect(initialOffset, radius * 0.55f))
        }
        val moonToSunPath = Path().apply {
            op(mainCircle, subtractCircle, PathOperation.Difference)
        }
        return Outline.Generic(moonToSunPath)
    }
}

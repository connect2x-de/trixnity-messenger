package de.connect2x.trixnity.messenger.compose.view.common.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal fun Modifier.pieSlice(
    sliceCount: Int,
    sliceIndex: Int,
    borderAngle: Float = 0F,
    arcIterations: Int = 10,
): Modifier =
    then(
        Modifier.drawWithContent {
            if (sliceCount <= 0) return@drawWithContent
            val diameter = size.minDimension
            val radius = diameter / 2F
            val center = Offset(radius, radius)

            val baseAngle = 360F / sliceCount
            val sliceAngle = baseAngle - borderAngle
            val startAngle = (sliceIndex * baseAngle + borderAngle / 2F) + 90F

            // Approximate the arc ourselves so we can properly handle border angles and avoid pre-clipping
            val path =
                Path().apply {
                    moveTo(center.x, center.y)
                    val startRad = startAngle.toDouble() * PI / 180.0
                    val endRad = (startAngle + sliceAngle).toDouble() * PI / 180.0
                    val startX = center.x + radius * cos(startRad).toFloat()
                    val startY = center.y + radius * sin(startRad).toFloat()
                    lineTo(startX, startY)
                    for (step in 1..arcIterations) {
                        val t = step / arcIterations.toFloat()
                        val angle = startRad + (endRad - startRad) * t
                        val x = center.x + radius * cos(angle).toFloat()
                        val y = center.y + radius * sin(angle).toFloat()
                        lineTo(x, y)
                    }
                    close()
                }
            clipPath(path) { this@drawWithContent.drawContent() }
        }
    )

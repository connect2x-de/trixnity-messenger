package de.connect2x.messenger.compose.view.common.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val HomeHealth: ImageVector
    get() {
        if (_HomeHealth != null) {
            return _HomeHealth!!
        }
        _HomeHealth = ImageVector.Builder(
            name = "HomeHealth",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFF1F1F1F))) {
                moveTo(420f, 680f)
                horizontalLineToRelative(120f)
                verticalLineToRelative(-100f)
                horizontalLineToRelative(100f)
                verticalLineToRelative(-120f)
                lineTo(540f, 460f)
                verticalLineToRelative(-100f)
                lineTo(420f, 360f)
                verticalLineToRelative(100f)
                lineTo(320f, 460f)
                verticalLineToRelative(120f)
                horizontalLineToRelative(100f)
                verticalLineToRelative(100f)
                close()
                moveTo(160f, 840f)
                verticalLineToRelative(-480f)
                lineToRelative(320f, -240f)
                lineToRelative(320f, 240f)
                verticalLineToRelative(480f)
                lineTo(160f, 840f)
                close()
                moveTo(240f, 760f)
                horizontalLineToRelative(480f)
                verticalLineToRelative(-360f)
                lineTo(480f, 220f)
                lineTo(240f, 400f)
                verticalLineToRelative(360f)
                close()
                moveTo(480f, 490f)
                close()
            }
        }.build()

        return _HomeHealth!!
    }

@Suppress("ObjectPropertyName")
private var _HomeHealth: ImageVector? = null

package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.messengerDpConstants
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun ColumnScope.WizardImage(
    drawableResource: DrawableResource,
    contentDescription: String?,
    height: Dp,
    boxWithConstraintsScope: BoxWithConstraintsScope,
) {
    Image(
        painterResource(drawableResource),
        contentDescription = contentDescription,
        colorFilter = if (isSystemInDarkTheme()) ColorFilter.colorMatrix(invertColorMatrix()) else null,
        modifier = Modifier
            .padding(
                horizontal =
                if (boxWithConstraintsScope.maxWidth < 400.dp) MaterialTheme.messengerDpConstants.verySmall
                else MaterialTheme.messengerDpConstants.middle,
                vertical = MaterialTheme.messengerDpConstants.verySmall,
            )
            .align(Alignment.CenterHorizontally)
            .fillMaxWidth()
            .height(height)
    )
}

@Composable
fun WizardImage(
    drawableResource: DrawableResource,
    contentDescription: String?,
    width: Dp,
    height: Dp = width,
) {
    Image(
        painterResource(drawableResource),
        contentDescription = contentDescription,
        colorFilter = if (isSystemInDarkTheme()) ColorFilter.colorMatrix(invertColorMatrix()) else null,
        modifier = Modifier
            .width(width)
            .height(height)
    )
}

private fun invertColorMatrix() = ColorMatrix( // invert colors in dark mode
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    )
)

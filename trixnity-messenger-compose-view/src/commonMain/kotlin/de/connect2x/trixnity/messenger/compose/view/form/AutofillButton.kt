package de.connect2x.trixnity.messenger.compose.view.form

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance

/**
 * A small button to manually trigger autofill behavior via a [HiddenAutofillForm].
 *
 * Implementation Detail: This is basically a dummy button on the compose side laying the Username Input of the
 * [HiddenAutofillForm] directly on top. This ensures that there is a real user interaction with the Username Input
 * while making it look and feel like a real button.
 *
 * For more detailed information see package.md inside the webMain actual implementation.
 */
@Composable
internal fun AutofillButton(
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val hiddenAutofillForm =
        rememberHiddenAutofillForm(
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            interactionSource = interactionSource,
        )

    LaunchedEffect(Unit) {
        interactionSource.interactions.filterIsInstance<PressInteraction.Release>().distinctUntilChanged().collect {
            focusManager.clearFocus(true)
        }
    }

    TextButton(
        onClick = { /* handled via hiddenAutofillForm */ },
        modifier =
            modifier.onGloballyPositioned { layoutCoordinates ->
                hiddenAutofillForm.layoutFrom(layoutCoordinates, density)
            },
        interactionSource = interactionSource,
    ) {
        AutofillText()
    }
}

private fun HiddenAutofillForm.layoutFrom(coordinates: LayoutCoordinates, density: Density) {
    val usernameRect = computeRect(coordinates, density).withButtonOffsets()

    layoutUsername(
        left = usernameRect.left,
        top = usernameRect.top,
        width = usernameRect.width,
        height = usernameRect.height,
    )
    layoutPassword(left = usernameRect.left - 40, top = usernameRect.top, width = 30, height = usernameRect.height)
}

/** IMPORTANT: When changing the text you might need to adapt the offsets inside [withButtonOffsets] */
@Composable
private fun AutofillText() {
    Text(text = "Autofill")
}

/** IMPORTANT: When changing the text used by [AutofillText], you might need to adapt the offsets */
private fun IntRect.withButtonOffsets(): IntRect {
    return IntRect(left = left, top = top + 4, right = right, bottom = bottom - 4)
}

private fun computeRect(layoutCoordinates: LayoutCoordinates, density: Density): IntRect {
    return layoutCoordinates
        .findRootCoordinates()
        .localBoundingBoxOf(sourceCoordinates = layoutCoordinates, clipBounds = false)
        .round(density)
}

private fun Rect.round(density: Density): IntRect {
    val left = floor(left / density.density).toInt()
    val top = floor(top / density.density).toInt()
    val right = ceil(right / density.density).toInt()
    val bottom = ceil(bottom / density.density).toInt()

    return IntRect(left, top, right, bottom)
}

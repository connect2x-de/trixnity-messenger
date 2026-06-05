package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ButtonStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants

/** A button that shows a loading spinner instead of the content depending on the loading state */
@Composable
fun ThemedLoadingButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: ButtonStyle = MaterialTheme.components.secondaryButton,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable (RowScope.() -> Unit),
) {
    ThemedButton(onClick, modifier, enabled, style, interactionSource) {
        Box {
            Row(if (isLoading) Modifier.alpha(0f) else Modifier) {
                content()
            }
            if (isLoading) {
                ThemedProgressIndicator(
                    modifier = Modifier.size(MaterialTheme.messengerDpConstants.middle).align(Alignment.Center),
                    style = MaterialTheme.components.circularProgressIndicator,
                )
            }
        }
    }
}

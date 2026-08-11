package de.connect2x.trixnity.messenger.compose.view.common.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import de.connect2x.trixnity.messenger.compose.view.theme.components.SurfaceStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants

@Composable
fun WizardSection(
    contentSpacing: Dp = MaterialTheme.messengerDpConstants.small,
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.messengerDpConstants.middle),
    content: @Composable ColumnScope.() -> Unit,
) {
    ThemedSurface(
        Modifier.fillMaxWidth(),
        style =
            SurfaceStyle.default(
                shape = RoundedCornerShape(MaterialTheme.messengerDpConstants.middle),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            modifier = Modifier.padding(contentPadding),
        ) {
            content()
        }
    }
}

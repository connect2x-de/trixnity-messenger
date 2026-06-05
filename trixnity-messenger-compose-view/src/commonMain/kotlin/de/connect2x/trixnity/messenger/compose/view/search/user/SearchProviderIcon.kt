package de.connect2x.trixnity.messenger.compose.view.search.user

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.theme.components.SurfaceStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface

@Composable
fun BoxScope.SearchProviderIcon(modifier: Modifier = Modifier, icon: @Composable () -> Unit) {
    ThemedSurface(
        style =
            SurfaceStyle.default(
                shape = CircleShape,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                contentPadding = PaddingValues(2.dp),
            ),
        modifier = modifier,
    ) {
        icon()
    }
}

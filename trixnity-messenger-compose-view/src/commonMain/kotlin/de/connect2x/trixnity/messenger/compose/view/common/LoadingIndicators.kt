package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator

@Composable
fun LoadingSpinner(modifier: Modifier = Modifier) {
    Box(Modifier.fillMaxWidth().then(modifier)) {
        ThemedProgressIndicator(Modifier.align(Alignment.Center), MaterialTheme.components.circularProgressIndicator)
    }
}

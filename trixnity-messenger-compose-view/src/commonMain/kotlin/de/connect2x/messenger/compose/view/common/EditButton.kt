package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.buttonPointerModifier

@Composable
fun EditButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = modifier.then(Modifier.size(50.dp).buttonPointerModifier()),
    ) {
        content()
    }
}

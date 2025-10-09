package de.connect2x.messenger.previews.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.theme.components.ThemedSelect

private enum class Movement {
    Running,
    Walking,
    Hiking,
    Cycling,
}

@Preview
@Composable
private fun ThemedSelectPreview() {
    val options = listOf(Movement.Running, Movement.Walking, Movement.Hiking,Movement.Cycling)
    val selected = remember { mutableStateOf(Movement.Running) }

    ThemedSelect(
        value = selected.value,
        onValueChange = { selected.value = it },
        options = options,
        label = { Text("Movement") },
        leadingIcon = {
            Icon(
                when (it) {
                    Movement.Running -> Icons.AutoMirrored.Filled.DirectionsRun
                    Movement.Walking ->Icons.AutoMirrored.Filled.DirectionsWalk
                    Movement.Hiking -> Icons.Filled.Hiking
                    Movement.Cycling -> Icons.AutoMirrored.Filled.DirectionsBike
                },
                contentDescription = null,
            )
        },
        render = {
            when (it) {
                Movement.Running -> "Laufen"
                Movement.Walking -> "Gehen"
                Movement.Hiking -> "Wandern"
                Movement.Cycling -> "Radeln"
            }
        }
    )
}

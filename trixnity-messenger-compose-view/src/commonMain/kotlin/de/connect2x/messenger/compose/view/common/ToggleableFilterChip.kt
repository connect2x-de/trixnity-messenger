package de.connect2x.messenger.compose.view.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedFilterChip
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun <T> ToggleableFilterChip(
    appliedFilters: MutableStateFlow<Set<T>>,
    filters: Set<T>,
    label: @Composable (() -> Unit)
) {
    val applied = appliedFilters.collectAsState().value.containsAll(filters)

    ThemedFilterChip(
        style = MaterialTheme.components.primaryChip,
        selected = applied,
        leadingIcon = if (applied) {
            @Composable { Icon(Icons.Default.Check, contentDescription = null) }
        } else null,
        onClick = {
            if (applied) {
                appliedFilters.value = appliedFilters.value.minus(filters)
            } else {
                appliedFilters.value = appliedFilters.value.plus(filters)
            }
        },
        label = label
    )
}

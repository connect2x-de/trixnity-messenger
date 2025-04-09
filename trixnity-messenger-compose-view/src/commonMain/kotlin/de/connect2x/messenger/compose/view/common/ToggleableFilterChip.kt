package de.connect2x.messenger.compose.view.common

import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun <T> ToggleableFilterChip(
    appliedFilters: MutableStateFlow<Set<T>>,
    filters: Set<T>,
    label: @Composable (() -> Unit)
) {
    var applied = appliedFilters.collectAsState().value.containsAll(filters)

    FilterChip(
        selected = applied,
        onClick = {
            if (applied) {
                appliedFilters.value = appliedFilters.value.minus(filters)
            } else {
                appliedFilters.value = appliedFilters.value.plus(filters)
            }
        },
        label = @Composable {
            label()
        }
    )
}

package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ChipColors
import androidx.compose.material3.ChipElevation
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.material3.SuggestionChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.theme.components

data class ChipStyle(
    val shape: Shape,
    val colors: Colors,
    val elevation: Elevation?,
    val enabledBorder: BorderStroke?,
    val selectedBorder: BorderStroke?,
    val disabledBorder: BorderStroke?,
    val focusedBorder: BorderStroke?,
) {
    @Immutable
    data class Elevation(
        val elevation: Dp,
        val pressedElevation: Dp,
        val focusedElevation: Dp,
        val hoveredElevation: Dp,
        val draggedElevation: Dp,
        val disabledElevation: Dp,
    ) {
        fun forSelectableChip() = SelectableChipElevation(
            elevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            draggedElevation,
            disabledElevation,
        )

        fun forChip() = ChipElevation(
            elevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            draggedElevation,
            disabledElevation,
        )

        fun forStaticChip() = ChipElevation(
            elevation,
            elevation,
            elevation,
            elevation,
            elevation,
            elevation,
        )

        companion object {
            // Tokens taken from ElevationTokens
            private val Level0 = 0.0.dp
            private val Level1 = 1.0.dp
            private val Level2 = 3.0.dp
            private val Level3 = 6.0.dp
            private val Level4 = 8.0.dp
            private val Level5 = 12.0.dp

            @Composable
            fun default(
                elevation: Dp = Level0,
                pressedElevation: Dp = Level0,
                focusedElevation: Dp = Level0,
                hoveredElevation: Dp = Level1,
                draggedElevation: Dp = Level4,
                disabledElevation: Dp = elevation
            ) = Elevation(
                elevation = elevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
                disabledElevation = disabledElevation,
            )

            @Composable
            fun elevated(
                elevation: Dp = Level1,
                pressedElevation: Dp = Level1,
                focusedElevation: Dp = Level1,
                hoveredElevation: Dp = Level2,
                draggedElevation: Dp = Level4,
                disabledElevation: Dp = Level0
            ) = Elevation(
                elevation = elevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
                disabledElevation = disabledElevation,
            )
        }
    }

    @Immutable
    data class Colors(
        val containerColor: Color,
        val labelColor: Color,
        val leadingIconColor: Color,
        val trailingIconColor: Color,
        val disabledContainerColor: Color,
        val disabledLabelColor: Color,
        val disabledLeadingIconColor: Color,
        val disabledTrailingIconColor: Color,
        val selectedContainerColor: Color,
        val disabledSelectedContainerColor: Color,
        val selectedLabelColor: Color,
        val selectedLeadingIconColor: Color,
        val selectedTrailingIconColor: Color,
    ) {
        fun forSelectableChip() = SelectableChipColors(
            containerColor,
            labelColor,
            leadingIconColor,
            trailingIconColor,
            disabledContainerColor,
            disabledLabelColor,
            disabledLeadingIconColor,
            disabledTrailingIconColor,
            selectedContainerColor,
            disabledSelectedContainerColor,
            selectedLabelColor,
            selectedLeadingIconColor,
            selectedTrailingIconColor,
        )

        fun forChip() = ChipColors(
            containerColor,
            labelColor,
            leadingIconColor,
            trailingIconColor,
            disabledContainerColor,
            disabledLabelColor,
            disabledLeadingIconColor,
            disabledTrailingIconColor,
        )

        fun forStaticChip() = ChipColors(
            selectedContainerColor,
            selectedLabelColor,
            selectedLeadingIconColor,
            selectedTrailingIconColor,
            selectedContainerColor,
            selectedLabelColor,
            selectedLeadingIconColor,
            selectedTrailingIconColor,
        )

        companion object {
            // Tokens taken from FilterChipTokens
            private val DisabledLabelTextColor: Color @Composable get() = MaterialTheme.colorScheme.onSurface
            private const val DisabledLabelTextOpacity = 0.38f
            private val ElevatedDisabledContainerColor: Color @Composable get() = MaterialTheme.colorScheme.onSurface
            private const val ElevatedDisabledContainerOpacity = 0.12f
            private val ElevatedSelectedContainerColor: Color @Composable get() = MaterialTheme.colorScheme.secondaryContainer
            private val ElevatedUnselectedContainerColor: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
            private val FlatDisabledSelectedContainerColor: Color @Composable get() = MaterialTheme.colorScheme.onSurface
            private const val FlatDisabledSelectedContainerOpacity = 0.12f
            private val FlatDisabledUnselectedOutlineColor: Color @Composable get() = MaterialTheme.colorScheme.onSurface
            private const val FlatDisabledUnselectedOutlineOpacity = 0.12f
            private val FlatSelectedContainerColor: Color @Composable get() = MaterialTheme.colorScheme.secondaryContainer
            private val FlatUnselectedFocusOutlineColor: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
            private val FlatUnselectedOutlineColor: Color @Composable get() = MaterialTheme.colorScheme.outline
            private val FocusIndicatorColor: Color @Composable get() = MaterialTheme.colorScheme.secondary
            private val SelectedDraggedLabelTextColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val SelectedFocusLabelTextColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val SelectedHoverLabelTextColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val SelectedLabelTextColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val SelectedPressedLabelTextColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val UnselectedDraggedLabelTextColor: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
            private val UnselectedFocusLabelTextColor: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
            private val UnselectedHoverLabelTextColor: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
            private val UnselectedLabelTextColor: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
            private val UnselectedPressedLabelTextColor: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
            private val DisabledLeadingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSurface
            private const val DisabledLeadingIconOpacity = 0.38f
            private val SelectedDraggedLeadingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val SelectedFocusLeadingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val SelectedHoverLeadingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val SelectedLeadingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val SelectedPressedLeadingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val UnselectedDraggedLeadingIconColor: Color @Composable get() = MaterialTheme.colorScheme.primary
            private val UnselectedFocusLeadingIconColor: Color @Composable get() = MaterialTheme.colorScheme.primary
            private val UnselectedHoverLeadingIconColor: Color @Composable get() = MaterialTheme.colorScheme.primary
            private val UnselectedLeadingIconColor: Color @Composable get() = MaterialTheme.colorScheme.primary
            private val UnselectedPressedLeadingIconColor: Color @Composable get() = MaterialTheme.colorScheme.primary
            private val DisabledTrailingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSurface
            private const val DisabledTrailingIconOpacity = 0.38f
            private val SelectedDraggedTrailingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val SelectedFocusTrailingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val SelectedHoverTrailingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val SelectedPressedTrailingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val SelectedTrailingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
            private val UnselectedDraggedTrailingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
            private val UnselectedFocusTrailingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
            private val UnselectedHoverTrailingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
            private val UnselectedPressedTrailingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
            private val UnselectedTrailingIconColor: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

            @Composable
            fun default(
                containerColor: Color = Color.Transparent,
                labelColor: Color = UnselectedLabelTextColor,
                leadingIconColor: Color = UnselectedLeadingIconColor,
                trailingIconColor: Color = UnselectedTrailingIconColor,
                disabledContainerColor: Color = Color.Transparent,
                disabledLabelColor: Color = DisabledLabelTextColor.copy(alpha = DisabledLabelTextOpacity),
                disabledLeadingIconColor: Color = DisabledLeadingIconColor.copy(alpha = DisabledLeadingIconOpacity),
                disabledTrailingIconColor: Color = DisabledTrailingIconColor.copy(alpha = DisabledTrailingIconOpacity),
                selectedContainerColor: Color = FlatSelectedContainerColor,
                disabledSelectedContainerColor: Color = FlatDisabledSelectedContainerColor.copy(alpha = FlatDisabledSelectedContainerOpacity),
                selectedLabelColor: Color = SelectedLabelTextColor,
                selectedLeadingIconColor: Color = SelectedLeadingIconColor,
                selectedTrailingIconColor: Color = SelectedTrailingIconColor
            ) = Colors(
                containerColor,
                labelColor,
                leadingIconColor,
                trailingIconColor,
                disabledContainerColor,
                disabledLabelColor,
                disabledLeadingIconColor,
                disabledTrailingIconColor,
                selectedContainerColor,
                disabledSelectedContainerColor,
                selectedLabelColor,
                selectedLeadingIconColor,
                selectedTrailingIconColor,
            )

            @Composable
            fun elevated(
                containerColor: Color = ElevatedUnselectedContainerColor,
                labelColor: Color = UnselectedLabelTextColor,
                leadingIconColor: Color = UnselectedLeadingIconColor,
                trailingIconColor: Color = UnselectedTrailingIconColor,
                disabledContainerColor: Color = ElevatedDisabledContainerColor.copy(alpha = ElevatedDisabledContainerOpacity),
                disabledLabelColor: Color = DisabledLabelTextColor.copy(alpha = DisabledLabelTextOpacity),
                disabledLeadingIconColor: Color = DisabledLeadingIconColor.copy(alpha = DisabledLeadingIconOpacity),
                disabledTrailingIconColor: Color = DisabledTrailingIconColor.copy(alpha = DisabledTrailingIconOpacity),
                selectedContainerColor: Color = ElevatedSelectedContainerColor,
                disabledSelectedContainerColor: Color = ElevatedDisabledContainerColor.copy(alpha = ElevatedDisabledContainerOpacity),
                selectedLabelColor: Color = SelectedLabelTextColor,
                selectedLeadingIconColor: Color = SelectedLeadingIconColor,
                selectedTrailingIconColor: Color = SelectedTrailingIconColor
            ) = Colors(
                containerColor,
                labelColor,
                leadingIconColor,
                trailingIconColor,
                disabledContainerColor,
                disabledLabelColor,
                disabledLeadingIconColor,
                disabledTrailingIconColor,
                selectedContainerColor,
                disabledSelectedContainerColor,
                selectedLabelColor,
                selectedLeadingIconColor,
                selectedTrailingIconColor,
            )

            @Composable
            fun static(
                containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            ) = Colors(
                containerColor,
                contentColor,
                contentColor,
                contentColor,
                containerColor,
                contentColor,
                contentColor,
                contentColor,
                containerColor,
                containerColor,
                contentColor,
                contentColor,
                contentColor,
            )
        }
    }

    fun border(enabled: Boolean, selected: Boolean = false, hasFocus: Boolean = false) = when {
        !enabled -> disabledBorder
        enabled && hasFocus -> focusedBorder
        selected -> selectedBorder
        else -> enabledBorder
    }

    companion object {
        @Composable
        fun default(
            shape: Shape = FilterChipDefaults.shape,
            colors: Colors = Colors.default(),
            elevation: Elevation? = Elevation.default(),
            enabledBorder: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled = true, selected = false),
            selectedBorder: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled = true, selected = true),
            disabledBorder: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled = false, selected = false),
            focusedBorder: BorderStroke? = null,
        ) = ChipStyle(
            shape,
            colors,
            elevation,
            enabledBorder,
            selectedBorder,
            disabledBorder,
            focusedBorder,
        )

        @Composable
        fun elevated(
            shape: Shape = FilterChipDefaults.shape,
            colors: Colors = Colors.elevated(),
            elevation: Elevation? = Elevation.elevated(),
            enabledBorder: BorderStroke? = null,
            selectedBorder: BorderStroke? = null,
            disabledBorder: BorderStroke? = null,
            focusedBorder: BorderStroke? = null,
        ) = ChipStyle(
            shape,
            colors,
            elevation,
            enabledBorder,
            selectedBorder,
            disabledBorder,
            focusedBorder,
        )
    }
}

@Composable
fun ThemedAssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    style: ChipStyle = MaterialTheme.components.commonChip,
    interactionSource: MutableInteractionSource? = null,
) {
    val hasFocus = remember { mutableStateOf(false) }

    AssistChip(
        onClick,
        label,
        modifier
            .onFocusChanged { hasFocus.value = it.hasFocus },
        enabled,
        leadingIcon,
        trailingIcon,
        style.shape,
        style.colors.forChip(),
        style.elevation?.forChip(),
        style.border(enabled, hasFocus = hasFocus.value),
        interactionSource,
    )
}

@Composable
fun ThemedSuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    style: ChipStyle = MaterialTheme.components.commonChip,
    interactionSource: MutableInteractionSource? = null,
) {
    val hasFocus = remember { mutableStateOf(false) }

    SuggestionChip(
        onClick,
        label,
        modifier
            .onFocusChanged { hasFocus.value = it.hasFocus },
        enabled,
        icon,
        style.shape,
        style.colors.forChip(),
        style.elevation?.forChip(),
        style.border(enabled, hasFocus = hasFocus.value),
        interactionSource,
    )
}

@Composable
fun ThemedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    style: ChipStyle = MaterialTheme.components.commonChip,
    interactionSource: MutableInteractionSource? = null
) {
    val hasFocus = remember { mutableStateOf(false) }

    FilterChip(
        selected,
        onClick,
        label,
        modifier
            .onFocusChanged { hasFocus.value = it.hasFocus }
            .semantics(mergeDescendants = true) {
                // Role.Checkbox is also set internally by material3
                // it unfortunately adds 'readonly' under windows/NVDA
                // Role.Switch produced even worse results however
                role = Role.Checkbox
                toggleableState = ToggleableState(selected)
            },
        enabled,
        leadingIcon,
        trailingIcon,
        style.shape,
        style.colors.forSelectableChip(),
        style.elevation?.forSelectableChip(),
        style.border(enabled, selected, hasFocus.value),
        interactionSource,
    )
}

@Composable
fun ThemedInputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    avatar: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    style: ChipStyle = MaterialTheme.components.commonChip,
    interactionSource: MutableInteractionSource? = null
) {
    val hasFocus = remember { mutableStateOf(false) }

    InputChip(
        selected,
        onClick,
        label,
        modifier
            .onFocusChanged { hasFocus.value = it.hasFocus },
        enabled,
        leadingIcon,
        avatar,
        trailingIcon,
        style.shape,
        style.colors.forSelectableChip(),
        style.elevation?.forSelectableChip(),
        style.border(enabled, selected, hasFocus.value),
        interactionSource,
    )
}

@Composable
fun ThemedInfoChip(
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    style: ChipStyle = MaterialTheme.components.commonChip,
) {
    val hasFocus = remember { mutableStateOf(false) }

    SuggestionChip(
        onClick = {},
        label = label,
        modifier = modifier
            .onFocusChanged { hasFocus.value = it.hasFocus },
        enabled = false,
        icon = icon,
        shape = style.shape,
        colors = style.colors.forStaticChip(),
        elevation = style.elevation?.forStaticChip(),
        border = style.border(enabled = true, selected = true, hasFocus.value),
        interactionSource = null,
    )
}

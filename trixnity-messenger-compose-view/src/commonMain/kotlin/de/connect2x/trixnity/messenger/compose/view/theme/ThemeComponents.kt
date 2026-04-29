package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.chipColors
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import de.connect2x.trixnity.messenger.compose.view.theme.components.ApplySystemUiTheme
import de.connect2x.trixnity.messenger.compose.view.theme.components.AvatarStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ButtonStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.CheckboxStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ChipStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.DialogStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.DividerStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.DropdownMenuItemStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.FloatingActionButtonStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.IconButtonStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.InputAreaStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.LibraryStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ListItemStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.LocalContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ProgressIndicatorStyle.CircularProgressIndicatorStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ProgressIndicatorStyle.LinearProgressIndicatorStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.RadioButtonStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.SelectStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.SelectionStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.SliderStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.SurfaceStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.SwitchStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.SystemUiStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.TooltipStyle

@Composable
fun MaterialThemeComponents(
    componentStyles: ThemeComponents,
    content: @Composable () -> Unit
) {
    // We need this double nesting to set a specific LocalContent color
    val contentColor = LocalContentColor.current
    CompositionLocalProvider(
        LocalContentColor provides Color.LocalContent
    ) {
        val components = componentStyles.create()
        ApplySystemUiTheme(components.systemUi)
        CompositionLocalProvider(
            LocalComponentStyles provides components,
            LocalContentColor provides contentColor,
        ) {
            content()
        }
    }
}

interface ThemeComponents {
    @Composable
    fun create(): ComponentStyles
}

class ThemeComponentsImpl : ThemeComponents {
    @Composable
    private fun focusedBorder(color: Color, defaultBorder: BorderStroke? = null): BorderStroke? =
        if (IsFocusHighlighting.current) {
            BorderStroke(
                width = MaterialTheme.messengerFocusIndicator.borderWidth,
                color = color,
            )
        } else defaultBorder

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalThemingApi::class)
    @Composable
    // This configuration tries to be as faithful as possible to our old design.
    // Even in places where our old design has low contrast or uneven spacing.
    override fun create(): ComponentStyles = ComponentStyles(
        // system
        systemUi = SystemUiStyle.default(
            header = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            footer = MaterialTheme.colorScheme.background,
        ),
        // buttons
        primaryButton = ButtonStyle.filled(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            textStyle = MaterialTheme.typography.labelLarge,
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onPrimaryContainer),
        ),
        secondaryButton = ButtonStyle.filled(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            textStyle = MaterialTheme.typography.labelLarge,
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSecondaryContainer),
        ),
        commonButton = ButtonStyle.outlined(
            textStyle = MaterialTheme.typography.labelLarge,
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onPrimaryContainer),
        ),
        destructiveButton = ButtonStyle.filled(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            textStyle = MaterialTheme.typography.labelLarge,
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onError),
        ),
        primaryIconButton = IconButtonStyle.filled(
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onPrimaryContainer),
        ),
        secondaryIconButton = IconButtonStyle.filled(
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSecondaryContainer),
        ),
        commonIconButton = IconButtonStyle.default(
            colors = IconButtonDefaults.iconToggleButtonColors(
                contentColor = Color.LocalContent,
            ),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSurface),
        ),
        destructiveIconButton = IconButtonStyle.default(
            colors = IconButtonDefaults.iconToggleButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onErrorContainer),
        ),
        floatingActionButton = FloatingActionButtonStyle.default(
            size = 40.dp,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onPrimaryContainer),
        ),
        floatingActionButtonDisabled = FloatingActionButtonStyle.default(
            size = 40.dp,
            containerColor = Color.LightGray,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            focusedBorder = null, // needs no focus
        ),
        reactionButton = ButtonStyle.outlined(
            iconSize = 18.dp,
            iconSpacing = 4.dp,
            contentPadding = PaddingValues(12.dp, 4.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSurfaceVariant),
        ),
        selectedReactionButton = ButtonStyle.filledTonal(
            iconSize = 18.dp,
            iconSpacing = 4.dp,
            contentPadding = PaddingValues(12.dp, 4.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSecondary),
        ),
        // other inputs
        listItem = ListItemStyle.default(),
        checkbox = CheckboxStyle.default(),
        radioButton = RadioButtonStyle.default(),
        switch = SwitchStyle.default(),
        // surfaces
        background = SurfaceStyle.default(),
        popup = SurfaceStyle.default(
            shadowElevation = 4.dp,
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.medium
        ),
        sidebar = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 4.dp,
        ),
        details = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 4.dp,
        ),
        header = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ),
        timeline = SurfaceStyle.default(),
        label = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.25f)
                .compositeOver(MaterialTheme.colorScheme.surface),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp),
            textStyle = MaterialTheme.typography.bodySmall,
        ),
        errorBanner = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        warningBanner = SurfaceStyle.default(
            color = MaterialTheme.messengerColors.warning,
            contentColor = contentColorFor(MaterialTheme.messengerColors.warning),
        ),
        commonBanner = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        // dialogs
        dialog = SurfaceStyle.default(),
        adaptiveDialog = DialogStyle.adaptiveDialog(),
        modalDialog = DialogStyle.modalDialog(),
        // dividers
        horizontalDivider = DividerStyle.default(),
        verticalDivider = DividerStyle.default(),
        // room list
        roomListElement = SurfaceStyle.default(
            color = Color.Unspecified,
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onPrimaryContainer),
        ),
        roomListSelection = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onBackground,
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onPrimaryContainer),
        ),
        roomListDivider = DividerStyle.default(
            padding = PaddingValues(horizontal = 10.dp),
        ),
        accountSelector = ButtonStyle.filled(
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
                containerColor = Color.Transparent,
                disabledContentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = Color.Transparent,
            ),
            elevation = null,
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSurface),
        ),
        // input area
        inputAreaSurface = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ),
        inputArea = InputAreaStyle.default(
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = TextFieldDefaults.colors(
                cursorColor = MaterialTheme.colorScheme.onSurface,

                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,

                focusedIndicatorColor =
                    if (IsFocusHighlighting.current) MaterialTheme.colorScheme.primary
                    else Color.Transparent,
                unfocusedIndicatorColor =
                    if (IsFocusHighlighting.current) MaterialTheme.colorScheme.onSurfaceVariant
                    else Color.Transparent,
                disabledIndicatorColor =
                    if (IsFocusHighlighting.current) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else Color.Transparent,
                errorIndicatorColor =
                    if (IsFocusHighlighting.current) MaterialTheme.colorScheme.error
                    else Color.Transparent,

                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                errorPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),

                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                errorTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            contentPadding = PaddingValues(7.dp),
        ),
        // file viewer
        fileViewerSurface = SurfaceStyle.default(
            color = Color.Black,
            contentColor = Color.LightGray,
        ),
        fileViewerIconButton = IconButtonStyle.filled(
            colors = IconButtonDefaults.iconToggleButtonColors(
                containerColor = Color.DarkGray,
                contentColor = Color.LightGray,
            ),
        ),
        // message bubbles
        messageBubbleOwn = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(8.dp),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSecondary),
        ),
        messageBubbleOther = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            shape = RoundedCornerShape(8.dp),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSecondary),
        ),
        messageBubbleError = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(8.dp),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onPrimary),
        ),
        messageReference = SurfaceStyle.default(
            shape = RoundedCornerShape(8.dp),
            color = Color(0x55FFFFFF),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSurface),
        ),
        // tooltip
        tooltip = TooltipStyle.default(
            contentPadding = PaddingValues(5.dp),
            colors = TooltipDefaults.richTooltipColors(
                contentColor = MaterialTheme.colorScheme.onTertiary,
                containerColor = MaterialTheme.colorScheme.tertiary,
            ),
            textStyle = MaterialTheme.typography.bodySmall,
        ),
        // loading spinner
        circularProgressIndicator = CircularProgressIndicatorStyle.default(),
        smallCircularProgressIndicator = CircularProgressIndicatorStyle.default(
            size = 32.dp,
        ),
        extraSmallCircularProgressIndicator = CircularProgressIndicatorStyle.default(
            size = 24.dp,
        ),
        switchProgressIndicator = CircularProgressIndicatorStyle.default(
            size = 24.dp,
            strokeWidth = 2.dp,
        ),
        linearProgressIndicator = LinearProgressIndicatorStyle.default(),
        // slider
        slider = SliderStyle.default(
            colors = SliderDefaults.colors(),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSurfaceVariant)
        ),
        // avatar
        avatar = AvatarStyle.default(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            outerBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer),
            innerBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
            shape = CircleShape,
        ),
        // chips
        primaryChip = ChipStyle.default(
            colors = ChipStyle.Colors.default(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onPrimaryContainer),
        ),
        secondaryChip = ChipStyle.default(
            colors = ChipStyle.Colors.default(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSecondaryContainer),
        ),
        commonChip = ChipStyle.default(
            colors = ChipStyle.Colors.default(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSurface),
        ),
        destructiveChip = ChipStyle.default(
            colors = ChipStyle.Colors.default(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
                selectedTrailingIconColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onErrorContainer),
        ),
        mentionChip = ChipStyle.elevated(
            colors = ChipStyle.Colors.elevated(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSurface),
        ),
        // dropdown
        dropdownMenu = SurfaceStyle.default(
            color = MenuDefaults.containerColor,
            tonalElevation = MenuDefaults.TonalElevation,
            shadowElevation = MenuDefaults.ShadowElevation,
            shape = MenuDefaults.shape,
            border = focusedBorder(MaterialTheme.colorScheme.onSurface),
        ),
        dropdownMenuItem = DropdownMenuItemStyle.default(
            contentPadding = PaddingValues(horizontal = 10.dp),
            focusedBorder = focusedBorder(MaterialTheme.colorScheme.onSurface),
        ),
        contextMenu = SurfaceStyle.default(
            color = MenuDefaults.containerColor,
            tonalElevation = MenuDefaults.TonalElevation,
            shadowElevation = MenuDefaults.ShadowElevation,
            shape = MenuDefaults.shape,
            border = focusedBorder(
                MaterialTheme.colorScheme.onSurface,
                BorderStroke(
                    Dp.Hairline,
                    if (CurrentThemeSettings.isDarkMode()) Color.DarkGray else Color.LightGray
                ),
            ),
        ),
        // select, like HTML <select>
        select = SelectStyle.default(),
        // selection
        selectionOnSurface = SelectionStyle.onSurface(),
        selectionOnPrimary = SelectionStyle.onPrimary(),
        // other
        settingsItem = ListItemStyle.default(
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            )
        ),
        library = LibraryStyle.default(
            colors = LibraryDefaults.libraryColors(
                libraryBackgroundColor = Color.Transparent,
                licenseChipColors = LibraryDefaults.chipColors(),
            ),
            textStyles = LibraryDefaults.libraryTextStyles(
                defaultOverflow = TextOverflow.Ellipsis,
                nameMaxLines = 10,
                versionMaxLines = 2
            ),
        ),
    )
}

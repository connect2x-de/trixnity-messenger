package de.connect2x.messenger.compose.view.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import de.connect2x.messenger.compose.view.theme.components.AvatarStyle
import de.connect2x.messenger.compose.view.theme.components.ButtonStyle
import de.connect2x.messenger.compose.view.theme.components.ChipStyle
import de.connect2x.messenger.compose.view.theme.components.DividerStyle
import de.connect2x.messenger.compose.view.theme.components.IconButtonStyle
import de.connect2x.messenger.compose.view.theme.components.FloatingActionButtonStyle
import de.connect2x.messenger.compose.view.theme.components.InputAreaStyle
import de.connect2x.messenger.compose.view.theme.components.ProgressIndicatorStyle.CircularProgressIndicatorStyle
import de.connect2x.messenger.compose.view.theme.components.ProgressIndicatorStyle.LinearProgressIndicatorStyle
import de.connect2x.messenger.compose.view.theme.components.SliderStyle
import de.connect2x.messenger.compose.view.theme.components.SurfaceStyle
import de.connect2x.messenger.compose.view.theme.components.SwitchStyle
import de.connect2x.messenger.compose.view.theme.components.TooltipStyle

data class ComponentStyles(
    // buttons
    val primaryButton: ButtonStyle,
    val secondaryButton: ButtonStyle,
    val commonButton: ButtonStyle,
    val destructiveButton: ButtonStyle,
    val primaryIconButton: IconButtonStyle,
    val secondaryIconButton: IconButtonStyle,
    val commonIconButton: IconButtonStyle,
    val destructiveIconButton: IconButtonStyle,
    val floatingActionButton: FloatingActionButtonStyle,
    val floatingActionButtonDisabled: FloatingActionButtonStyle,
    val reactionButton: ButtonStyle,
    val selectedReactionButton: ButtonStyle,
    // other inputs
    val switch: SwitchStyle,
    // surfaces
    val background: SurfaceStyle,
    val dialog: SurfaceStyle,
    val popup: SurfaceStyle,
    val sidebar: SurfaceStyle,
    val details: SurfaceStyle,
    val header: SurfaceStyle,
    val timeline: SurfaceStyle,
    // banners
    val errorBanner: SurfaceStyle,
    val warningBanner: SurfaceStyle,
    val commonBanner: SurfaceStyle,
    // labels
    val label: SurfaceStyle,
    // dividers
    val horizontalDivider: DividerStyle?,
    val verticalDivider: DividerStyle?,
    // room list
    val roomListSelection: SurfaceStyle,
    val roomListElement: SurfaceStyle,
    val roomListDivider: DividerStyle?,
    val accountSelector: ButtonStyle,
    // input area
    val inputAreaSurface: SurfaceStyle,
    val inputArea: InputAreaStyle,
    // file viewer
    val fileViewerSurface: SurfaceStyle,
    val fileViewerIconButton: IconButtonStyle,
    // messages
    val messageBubbleOwn: SurfaceStyle,
    val messageBubbleOther: SurfaceStyle,
    val messageBubbleError: SurfaceStyle,
    val messageReference: SurfaceStyle,
    // tooltips
    val tooltip: TooltipStyle,
    // progress indicators
    val circularProgressIndicator: CircularProgressIndicatorStyle,
    val smallCircularProgressIndicator: CircularProgressIndicatorStyle,
    val extraSmallCircularProgressIndicator: CircularProgressIndicatorStyle,
    val linearProgressIndicator: LinearProgressIndicatorStyle,
    // slider
    val slider: SliderStyle,
    // avatar
    val avatar: AvatarStyle,
    // chips
    val primaryChip: ChipStyle,
    val secondaryChip: ChipStyle,
    val commonChip: ChipStyle,
    val destructiveChip: ChipStyle,
)

internal val LocalComponentStyles = staticCompositionLocalOf<ComponentStyles> { error("compositionLocal not defined") }

val MaterialTheme.components: ComponentStyles
    @Composable
    get() = LocalComponentStyles.current

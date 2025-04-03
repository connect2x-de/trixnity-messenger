package de.connect2x.messenger.compose.view.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import de.connect2x.messenger.compose.view.theme.components.ButtonStyle
import de.connect2x.messenger.compose.view.theme.components.DividerStyle
import de.connect2x.messenger.compose.view.theme.components.IconButtonStyle
import de.connect2x.messenger.compose.view.theme.components.FloatingActionButtonStyle
import de.connect2x.messenger.compose.view.theme.components.InputAreaStyle
import de.connect2x.messenger.compose.view.theme.components.ProgressIndicatorStyle.CircularProgressIndicatorStyle
import de.connect2x.messenger.compose.view.theme.components.ProgressIndicatorStyle.LinearProgressIndicatorStyle
import de.connect2x.messenger.compose.view.theme.components.SurfaceStyle
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
    // surfaces
    val background: SurfaceStyle,
    val dialog: SurfaceStyle,
    val popup: SurfaceStyle,
    val sidebar: SurfaceStyle,
    val header: SurfaceStyle,
    val timeline: SurfaceStyle,
    // dividers
    val divider: DividerStyle?,
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
)

internal val LocalComponentStyles = staticCompositionLocalOf<ComponentStyles> { error("compositionLocal not defined") }

val MaterialTheme.components: ComponentStyles
    @Composable
    get() = LocalComponentStyles.current

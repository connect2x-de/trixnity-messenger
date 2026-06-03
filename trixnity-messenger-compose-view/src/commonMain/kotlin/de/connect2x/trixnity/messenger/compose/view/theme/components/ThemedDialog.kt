package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.SINGLE_PANE_THRESHOLD
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components

data class DialogStyle(
    val container: SurfaceStyle,
    val header: SurfaceStyle,
    val footer: SurfaceStyle,
    val maxWidth: Dp,
    val divider: DividerStyle?,
    val buttonsMainAxisSpacing: Dp,
    val buttonsCrossAxisSpacing: Dp,
) {
    companion object {
        @Composable
        fun adaptiveDialog(
            container: SurfaceStyle =
                SurfaceStyle.default(
                    shape = MaterialTheme.shapes.extraLarge,
                    padding = PaddingValues(horizontal = 56.dp, vertical = 72.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                ),
            header: SurfaceStyle =
                SurfaceStyle.default(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentPadding = PaddingValues(top = 24.dp),
                ),
            footer: SurfaceStyle =
                SurfaceStyle.default(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 16.dp),
                ),
            maxWidth: Dp = 800.dp,
            divider: DividerStyle? = DividerStyle.default(padding = PaddingValues(0.dp)),
            buttonsMainAxisSpacing: Dp = 8.dp,
            buttonsCrossAxisSpacing: Dp = 12.dp,
        ) = DialogStyle(container, header, footer, maxWidth, divider, buttonsMainAxisSpacing, buttonsCrossAxisSpacing)

        @Composable
        fun modalDialog(
            container: SurfaceStyle =
                SurfaceStyle.default(
                    shape = MaterialTheme.shapes.extraLarge,
                    padding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                ),
            header: SurfaceStyle =
                SurfaceStyle.default(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentPadding = PaddingValues(top = 24.dp),
                ),
            footer: SurfaceStyle =
                SurfaceStyle.default(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 16.dp),
                ),
            maxWidth: Dp = 560.dp,
            divider: DividerStyle? = null,
            buttonsMainAxisSpacing: Dp = 8.dp,
            buttonsCrossAxisSpacing: Dp = 12.dp,
        ) = DialogStyle(container, header, footer, maxWidth, divider, buttonsMainAxisSpacing, buttonsCrossAxisSpacing)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemedAdaptiveDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    style: DialogStyle = MaterialTheme.components.adaptiveDialog,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalDensity provides density) { AdaptiveDialogWrapper(modifier, style, content) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveDialogWrapper(
    modifier: Modifier = Modifier,
    style: DialogStyle = MaterialTheme.components.adaptiveDialog,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isSinglePane = this@BoxWithConstraints.maxWidth < SINGLE_PANE_THRESHOLD.dp

        val paddingModifier =
            if (isSinglePane) Modifier
            else Modifier.padding(style.container.padding).requiredSizeIn(maxWidth = style.maxWidth)
        Surface(
            modifier =
                modifier.align(Alignment.Center).fillMaxSize().then(paddingModifier).semantics { paneTitle = "Dialog" },
            shape = if (isSinglePane) RectangleShape else style.container.shape,
            color = style.container.color,
            contentColor = style.container.contentColor,
            tonalElevation = style.container.tonalElevation,
            shadowElevation = style.container.shadowElevation,
            border = style.container.border,
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun AdaptiveDialogHeader(
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    style: DialogStyle = MaterialTheme.components.adaptiveDialog,
    title: @Composable ColumnScope.() -> Unit,
) {
    val i18n = DI.get<I18nView>()
    ThemedSurface(style = style.header, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack == null) {
                Spacer(Modifier.width(24.dp))
            } else {
                Spacer(Modifier.width(16.dp))
                Tooltip({ Text(i18n.actionBack()) }) {
                    ThemedIconButton(style = MaterialTheme.components.commonIconButton, onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = i18n.actionBack())
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f).semantics(true) { heading() }) {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.merge(MaterialTheme.typography.titleLarge)
                ) {
                    title()
                }
            }
            if (onClose != null) {
                Spacer(Modifier.width(12.dp))
                Tooltip({ Text(i18n.actionClose()) }) {
                    ThemedIconButton(style = MaterialTheme.components.commonIconButton, onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = i18n.actionClose())
                    }
                }
            }
            Spacer(Modifier.width(24.dp))
        }
    }
}

@Composable
fun ColumnScope.AdaptiveDialogContent(
    style: DialogStyle = MaterialTheme.components.adaptiveDialog,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.weight(weight = 1f, fill = true).align(Alignment.Start)) {
        Column(Modifier.padding(style.container.contentPadding)) { content() }
    }
}

@Composable
fun ColumnScope.AdaptiveDialogScrollContent(
    modifier: Modifier = Modifier,
    style: DialogStyle = MaterialTheme.components.adaptiveDialog,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.weight(weight = 1f, fill = true).align(Alignment.Start)) {
        Column(Modifier.padding(style.container.contentPadding).verticalScroll(scrollState)) { content() }
        VerticalScrollbar(modifier = Modifier.align(Alignment.CenterEnd), scrollState)
    }
}

@Composable
fun ColumnScope.AdaptiveDialogScrollContent(
    style: DialogStyle = MaterialTheme.components.adaptiveDialog,
    scrollState: LazyListState,
    reverseLayout: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.weight(weight = 1f, fill = true).align(Alignment.Start)) {
        Column(Modifier.padding(style.container.contentPadding)) { content() }
        VerticalScrollbar(modifier = Modifier.align(Alignment.CenterEnd), scrollState, reverseLayout)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.AdaptiveDialogFooter(
    style: DialogStyle = MaterialTheme.components.adaptiveDialog,
    content: @Composable () -> Unit,
) {
    if (style.divider != null) {
        ThemedHorizontalDivider(style = style.divider)
    }
    ThemedSurface(modifier = Modifier.heightIn(min = 72.dp).fillMaxWidth(), style = style.footer) {
        FlowRow(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(style.buttonsMainAxisSpacing, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(style.buttonsCrossAxisSpacing, Alignment.Bottom),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemedModalDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    style: DialogStyle = MaterialTheme.components.modalDialog,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalDensity provides density) {
            val layoutDirection = LocalLayoutDirection.current
            val horizontalPadding =
                style.container.padding.calculateStartPadding(layoutDirection) +
                    style.container.padding.calculateEndPadding(layoutDirection)
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Surface(
                    modifier =
                        modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(style.container.padding)
                            .requiredSizeIn(maxWidth = minOf(maxWidth - horizontalPadding, style.maxWidth))
                            .semantics { paneTitle = "Dialog" },
                    shape = style.container.shape,
                    color = style.container.color,
                    contentColor = style.container.contentColor,
                    tonalElevation = style.container.tonalElevation,
                    shadowElevation = style.container.shadowElevation,
                    border = style.container.border,
                ) {
                    Column(content = content)
                }
            }
        }
    }
}

@Composable
fun ModalDialogHeader(
    style: DialogStyle = MaterialTheme.components.modalDialog,
    title: @Composable ColumnScope.() -> Unit,
) {
    ThemedSurface(style = style.header, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.merge(MaterialTheme.typography.titleLarge)
                ) {
                    title()
                }
            }
            Spacer(Modifier.width(24.dp))
        }
    }
}

@Composable
fun ColumnScope.ModalDialogContent(
    style: DialogStyle = MaterialTheme.components.modalDialog,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.weight(weight = 1f, fill = false).padding(style.container.contentPadding).align(Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(style.container.contentPadding.calculateBottomPadding()),
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModalDialogFooter(
    style: DialogStyle = MaterialTheme.components.modalDialog,
    content: @Composable RowScope.() -> Unit,
) {
    ThemedSurface(modifier = Modifier.fillMaxWidth(), style = style.footer) {
        FlowRow(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(style.buttonsMainAxisSpacing, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(style.buttonsCrossAxisSpacing, Alignment.Bottom),
        ) {
            content()
        }
    }
}

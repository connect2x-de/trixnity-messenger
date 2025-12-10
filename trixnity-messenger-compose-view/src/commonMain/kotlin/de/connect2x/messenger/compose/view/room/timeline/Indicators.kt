package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.common.modifier.focusHighlighting
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components.SurfaceStyle
import de.connect2x.messenger.compose.view.theme.components.ThemedSurface

@Composable
fun UnreadMessagesIndicator() {
    val i18n = DI.get<I18nView>()
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(
            Modifier.weight(1.0f).padding(end = 20.dp),
            color = MaterialTheme.colorScheme.tertiary,
            thickness = 3.dp
        )
        Text(
            text = i18n.indicatorUnreadMessages(),
            color = MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
        HorizontalDivider(
            Modifier.weight(1.0f).padding(start = 20.dp),
            color = MaterialTheme.colorScheme.tertiary,
            thickness = 3.dp
        )
    }
}

@Composable
fun DateStickyHeader(date: String, focusable: Boolean) {
    Indicator(MaterialTheme.colorScheme.tertiaryContainer, withPadding = true, focusable = focusable) {
        IndicatorText(date, MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
fun Indicator(
    containerColor: Color,
    withPadding: Boolean = true,
    focusable: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    BoxWithConstraints {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(
                    top = if (withPadding) 10.dp else 0.dp,
                    start = maxWidth / 4, // only use max half the screen's width (on both sides == 4 // )
                    end = maxWidth / 4,
                )
        ) {
            if (focusable) {
                // When Surface gets an onClick handler it sets the minimum size via LocalMinimumInteractiveComponentSize.
                // In this case it's too large, however.
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    ThemedSurface(
                        style = SurfaceStyle.default(
                            shape = RoundedCornerShape(8.dp),
                            color = containerColor,
                            contentPadding = PaddingValues(5.dp)
                        ),
                        onClick = {},
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .focusHighlighting(interactionSource)
                    ) {
                        content()
                    }
                }
            } else {
                ThemedSurface(
                    style = SurfaceStyle.default(
                        shape = RoundedCornerShape(8.dp),
                        color = containerColor,
                        contentPadding = PaddingValues(5.dp)
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun IndicatorText(message: String, color: Color) {
    Tooltip({ Text(message) }) {
        Text(
            message,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
        )
    }
}

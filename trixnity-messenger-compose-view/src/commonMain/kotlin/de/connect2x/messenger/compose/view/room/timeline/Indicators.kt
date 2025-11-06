package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView

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
fun DateStickyHeader(date: String) {
    Indicator(MaterialTheme.colorScheme.tertiaryContainer, withPadding = true) {
        IndicatorText(date, MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
fun Indicator(
    containerColor: Color,
    withPadding: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
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
            Box(
                Modifier.background(color = containerColor, shape = RoundedCornerShape(8.dp))
                    .align(Alignment.Center)
                    .padding(5.dp)
            ) {
                content()
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

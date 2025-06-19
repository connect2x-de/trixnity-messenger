package de.connect2x.messenger.compose.view.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.files.toImageBitmap
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.SystemDensity
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import net.folivo.trixnity.core.model.events.m.Presence

data class AvatarStyle(
    val color: Color,
    val contentColor: Color,
    val outerBorder: BorderStroke,
    val innerBorder: BorderStroke,
    val shape: Shape,
    val badgeSize: Dp,
    val badgeShape: Shape,
) {
    companion object {
        @Composable
        fun default(
            color: Color = MaterialTheme.colorScheme.primaryContainer,
            contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
            outerBorder: BorderStroke = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            innerBorder: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
            shape: Shape = CircleShape,
            badgeSize: Dp = 10.dp,
            badgeShape: Shape = CircleShape,
        ) = AvatarStyle(
            color = color,
            contentColor = contentColor,
            outerBorder = outerBorder,
            innerBorder = innerBorder,
            shape = shape,
            badgeSize = badgeSize,
            badgeShape = badgeShape,
        )
    }
}

@Composable
fun ThemedUserAvatar(
    initials: String,
    image: ByteArray? = null,
    size: Dp = avatarSize().dp,
    style: AvatarStyle = MaterialTheme.components.avatar,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
) {
    val bitmap = remember(image) { image?.toImageBitmap() }
    ThemedAvatar(size, modifier, style, overlay) {
        if (bitmap != null) {
            AvatarContentImage(bitmap, size)
        } else {
            AvatarContentText(initials, size)
        }
    }
}

@Composable
fun ThemedAvatar(
    size: Dp,
    modifier: Modifier = Modifier,
    style: AvatarStyle = MaterialTheme.components.avatar,
    overlay: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier.wrapContentSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            modifier = modifier
                .size(size)
                .background(style.color, shape = style.shape)
                .border(style.outerBorder, style.shape)
                .border(style.innerBorder.let { it.copy(width = it.width + style.outerBorder.width) }, style.shape)
                .clip(style.shape),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides style.contentColor,
            ) {
                content()
            }
        }
        overlay()
    }
}

@Composable
fun AvatarContentImage(image: ImageBitmap, size: Dp) {
    Image(
        image,
        modifier = Modifier.size(size),
        contentScale = ContentScale.Fit,
        contentDescription = null
    )
}

@Composable
fun AvatarContentIcon(icon: ImageVector, size: Dp) {
    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(size * 0.6f)
    )
}

@Composable
fun AvatarContentText(text: String, size: Dp) {
    Text(
        text,
        textAlign = TextAlign.Center,
        fontSize = with(SystemDensity.current) { size.toSp() * 0.4f },
    )
}

@Composable
fun AvatarPresenceBadge(
    presence: Presence?,
    style: AvatarStyle = MaterialTheme.components.avatar,
) {
    if (presence == null) return

    val i18n = DI.get<I18nView>()

    val color = when (presence) {
        Presence.ONLINE -> MaterialTheme.messengerColors.presenceOnline
        Presence.OFFLINE -> MaterialTheme.messengerColors.presenceOffline
        Presence.UNAVAILABLE -> MaterialTheme.messengerColors.presenceUnavailable
    }

    val tooltip = when (presence) {
        Presence.ONLINE -> i18n.presenceOnline()
        Presence.OFFLINE -> i18n.presenceOffline()
        Presence.UNAVAILABLE -> i18n.presenceUnavailable()
    }

    Tooltip({ Text(tooltip) }) {
        Box(
            Modifier.size(style.badgeSize)
                .background(color, shape = style.badgeShape)
                .border(style.innerBorder, style.badgeShape)
        )
    }
}

package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.model.events.m.Presence
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.MoonShape
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.modifier.pieSlice
import de.connect2x.trixnity.messenger.compose.view.files.toImageBitmap
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.SystemDensity
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize

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
            innerBorder: BorderStroke = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
            shape: Shape = CircleShape,
            badgeSize: Dp = 12.dp,
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
private inline fun ThemedUserAvatarBase(
    presence: Presence? = null,
    size: Dp = avatarSize().dp,
    style: AvatarStyle = MaterialTheme.components.avatar,
    modifier: Modifier = Modifier,
    noinline overlay: @Composable BoxScope.() -> Unit = {},
    crossinline content: @Composable BoxScope.() -> Unit
) {
    val tooltip = presenceText(presence)
    tooltip?.let {
        Tooltip({ Text(tooltip) }) {
            ThemedAvatar(size, modifier, style, overlay) {
                content()
            }
        }
    } ?: ThemedAvatar(size, modifier, style, overlay) {
        content()
    }
}

@Composable
fun ThemedUserAvatar(
    initials: String,
    image: ByteArray? = null,
    presence: Presence? = null,
    size: Dp = avatarSize().dp,
    style: AvatarStyle = MaterialTheme.components.avatar,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val bitmap = remember(image) { image?.toImageBitmap() }
    ThemedUserAvatarBase(presence, size, style, modifier, overlay) {
        if (bitmap != null) {
            AvatarContentImage(bitmap, size)
        } else {
            AvatarContentText(initials, size)
        }
    }
}

@Composable
fun ThemedUserAvatarStack(
    initials: String,
    images: List<ByteArray?> = emptyList(),
    presence: Presence? = null,
    size: Dp = avatarSize().dp,
    style: AvatarStyle = MaterialTheme.components.avatar,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val bitmaps = remember(images) { images.mapNotNull { image -> image?.toImageBitmap() } }
    ThemedUserAvatarBase(presence, size, style, modifier, overlay) {
        if (bitmaps.isNotEmpty()) {
            AvatarContentImageStack(bitmaps, size)
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
    overlay: @Composable BoxScope.() -> Unit = {},
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
fun AvatarContentImageStack(
    images: List<ImageBitmap>,
    size: Dp,
    maxSlices: Int = 4
) {
    val sliceCount = images.size.coerceAtMost(maxSlices)
    Box(modifier = Modifier.size(size)) {
        for (sliceIndex in 0..<sliceCount) {
            Image(
                images[sliceIndex],
                modifier = Modifier.matchParentSize().pieSlice(sliceCount, sliceIndex),
                contentScale = ContentScale.Fit,
                contentDescription = null
            )
        }
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
        modifier = Modifier.semantics { this.text = AnnotatedString("") },
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

    val shape = when (presence) {
        Presence.UNAVAILABLE -> MoonShape()
        else -> style.badgeShape
    }

    val icon = when (presence) {
        Presence.OFFLINE -> Icons.Outlined.Close
        else -> null
    }

    val color = when (presence) {
        Presence.ONLINE -> MaterialTheme.messengerColors.presenceOnline
        Presence.OFFLINE -> MaterialTheme.messengerColors.presenceOffline
        Presence.UNAVAILABLE -> MaterialTheme.messengerColors.presenceUnavailable
    }

    Box(
        Modifier.size(style.badgeSize)
            .background(color, shape)
            .border(style.innerBorder, shape)
    ) {
        if (icon != null) {
            val brush = style.innerBorder.brush
            val color = if (brush is SolidColor) brush.value else MaterialTheme.colorScheme.surface
            Icon(
                icon,
                contentDescription = presenceText(presence),
                modifier = Modifier.align(Alignment.Center).size(style.badgeSize * 0.75f),
                tint = color,
            )
        }
    }
}

@Composable
private fun presenceText(
    presence: Presence?,
): String? {
    val i18n = DI.get<I18nView>()
    return when (presence) {
        Presence.ONLINE -> i18n.presenceOnline()
        Presence.OFFLINE -> i18n.presenceOffline()
        Presence.UNAVAILABLE -> i18n.presenceUnavailable()
        null -> null
    }
}

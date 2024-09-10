package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.files.imageBitmapFromBytes
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.core.model.events.m.Presence


private val log = KotlinLogging.logger { }

@Composable
fun Avatar(
    image: ByteArray?,
    initials: String,
    size: Dp = avatarSize().dp,
    overlay: @Composable (BoxScope.() -> Unit)? = null
) {
    val i18n = DI.current.get<I18nView>()
    image?.let { imageBitmapFromBytes(it) }?.let { bitmap ->
        val maxScaleX = size / bitmap.width
        val maxScaleY = size / bitmap.height

        val scale = max(maxScaleX, maxScaleY)

        val width = scale * bitmap.width
        val height = scale * bitmap.height

        log.trace { "size ($size), image (${bitmap.width},${bitmap.height}), scale ($scale), dim ($width,$height)" }

        Box {
            AvatarWithImage(size) {
                Image(
                    bitmap,
                    i18n.commonAvatar(),
                    Modifier.size(width, height),
                    contentScale = ContentScale.Crop
                )
            }
            overlay?.invoke(this)
        }
    }
    if (image == null) {
        Box {
            AvatarWithInitials(initials, size)
            overlay?.invoke(this)
        }
    }
}

@Composable
fun AvatarWithInitials(initials: String, size: Dp = avatarSize().dp) {
    AvatarBase(size) {
        Text(
            initials,
            textAlign = TextAlign.Center,
            fontSize = with(LocalDensity.current) { size.toSp() * 0.4 },
        )
    }
}

@Composable
fun AvatarWithImage(size: Dp = avatarSize().dp, content: @Composable BoxScope.() -> Unit) {
    AvatarBase(size) {
        Box(Modifier.align(Alignment.Center)) {
            content()
        }
    }
}

@Composable
fun AvatarWithPresence(image: ByteArray?, initials: String, presence: Presence?) {
    val i18n = DI.current.get<I18nView>()
    Avatar(image, initials) {
        when (presence) {
            null -> {}
            Presence.ONLINE -> PresenceIcon(i18n.presenceOnline(), MaterialTheme.messengerColors.presenceOnline)
            Presence.OFFLINE -> PresenceIcon(i18n.presenceOffline(), MaterialTheme.messengerColors.presenceOffline)
            Presence.UNAVAILABLE -> PresenceIcon(
                i18n.presenceUnavailable(),
                MaterialTheme.messengerColors.presenceUnavailable
            )
        }
    }
}

@Composable
private fun AvatarBase(size: Dp = avatarSize().dp, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(2.dp, MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .wrapContentSize(Alignment.Center)
    ) {
        content()
    }
}

@Composable
private fun BoxScope.PresenceIcon(description: String, color: Color) {
    Box(Modifier.align(Alignment.BottomEnd), contentAlignment = Alignment.Center) {
        Tooltip({ TooltipText(description) }) {
            Icon(
                Icons.Default.Circle,
                description,
                Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.background,
            )
        }
        Icon(
            Icons.Default.Circle,
            description,
            Modifier.size(10.dp),
            tint = color,
        )
    }
}

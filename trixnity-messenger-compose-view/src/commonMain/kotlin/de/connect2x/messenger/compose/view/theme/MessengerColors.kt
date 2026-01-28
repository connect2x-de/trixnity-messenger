package de.connect2x.messenger.compose.view.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import de.connect2x.trixnity.core.model.UserId
import kotlin.math.absoluteValue

@Immutable
data class MessengerColors(
    val success: Color,
    val presenceOnline: Color,
    val presenceOffline: Color,
    val presenceUnavailable: Color,
    val verificationTrusted: Color,
    val verificationUntrusted: Color,
    val verificationNeutral: Color,
    val metaDataPreview: Color,
    val metaDataPreviewBackground: Color,
    val blockedUser: Color,
    val warning: Color,
    val link: Color,
    val linkByMe: Color,
    val userColors: List<Color>,
) {
    @Stable
    fun getUserColor(userId: UserId): Color = userColors[(userId.hashCode() % userColors.size).absoluteValue]
}

val MessengerColorsProvider = staticCompositionLocalOf<MessengerColors> { error("compositionLocal not defined") }

val MaterialTheme.messengerColors: MessengerColors
    @Composable
    @ReadOnlyComposable
    get() = MessengerColorsProvider.current

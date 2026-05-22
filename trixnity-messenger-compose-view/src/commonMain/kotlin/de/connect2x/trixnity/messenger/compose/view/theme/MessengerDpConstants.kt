package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

interface MessengerDpConstants {
    val verySmall: Dp
    val small: Dp
    val middle: Dp
    val large: Dp
    val veryLarge: Dp
    val touchTarget: Dp
}

@Immutable
object DefaultMessengerDpConstantValues : MessengerDpConstants {
    /** 5.dp */
    override val verySmall = 5.dp

    /** 10.dp */
    override val small = 10.dp

    /** 20.dp */
    override val middle = 20.dp

    /** 40.dp */
    override val large = 40.dp

    /** 80.dp */
    override val veryLarge = 80.dp

    /** 50.dp */
    override val touchTarget = 50.dp
}

val DefaultMessengerDpConstants: MessengerDpConstants
    @Composable get() = DefaultMessengerDpConstantValues

internal val MessengerDpConstantsProvider =
    staticCompositionLocalOf<MessengerDpConstants> { error("compositionLocal not defined") }

val MaterialTheme.messengerDpConstants: MessengerDpConstants
    @Composable @ReadOnlyComposable get() = MessengerDpConstantsProvider.current

package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
object MessengerFocusIndicator {
    val borderWidth: Dp = 2.dp
}

val MaterialTheme.messengerFocusIndicator: MessengerFocusIndicator
    @Composable
    @ReadOnlyComposable
    get() = MessengerFocusIndicator

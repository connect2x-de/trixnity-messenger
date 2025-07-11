package de.connect2x.messenger.compose.view.richtext

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

internal val Double.em: Dp
    @Stable
    @Composable
    get() = with (LocalDensity.current) {
        LocalTextStyle.current.fontSize.times(this@em).toDp()
    }

internal val Float.em: Dp
    @Stable
    @Composable
    get() = with (LocalDensity.current) {
        LocalTextStyle.current.fontSize.times(this@em).toDp()
    }

internal val Int.em: Dp
    @Stable
    @Composable
    get() = with (LocalDensity.current) {
        LocalTextStyle.current.fontSize.times(this@em).toDp()
    }

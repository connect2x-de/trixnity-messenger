package org.example.project

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

val Double.em: Dp
    @Stable
    @Composable
    get() = with (LocalDensity.current) {
        LocalTextStyle.current.fontSize.times(this@em).toDp()
    }

val Float.em: Dp
    @Stable
    @Composable
    get() = with (LocalDensity.current) {
        LocalTextStyle.current.fontSize.times(this@em).toDp()
    }

val Int.em: Dp
    @Stable
    @Composable
    get() = with (LocalDensity.current) {
        LocalTextStyle.current.fontSize.times(this@em).toDp()
    }
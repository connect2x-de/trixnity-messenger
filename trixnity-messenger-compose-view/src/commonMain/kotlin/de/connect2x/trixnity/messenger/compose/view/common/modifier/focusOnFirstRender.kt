package de.connect2x.trixnity.messenger.compose.view.common.modifier

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

fun Modifier.focusOnFirstRender(): Modifier = composed {
    val focusRequester = remember { FocusRequester() }
    focusOnFirstRender(focusRequester)
}

fun Modifier.focusOnFirstRender(focusRequester: FocusRequester): Modifier = composed {
    var hasRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasRequested) {
            focusRequester.requestFocus()
            hasRequested = true
        }
    }

    this.focusRequester(focusRequester)
}

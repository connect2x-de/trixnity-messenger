package de.connect2x.trixnity.messenger.compose.view.theme.components

import android.content.ClipData
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalTextToolbar
import kotlinx.coroutines.launch

@Composable
actual fun ThemedTextFieldWithToolbar() {
    val textToolBar = LocalTextToolbar.current
    val clipboard = LocalClipboard.current

    var content by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    var layoutCoordinates by remember {
        mutableStateOf<LayoutCoordinates?>(
            null
        )
    }

    LaunchedEffect(Unit) {
        interactionSource.interactions.collect {
            when (it) {
                is PressInteraction.Cancel -> {
                    val rect = layoutCoordinates?.boundsInWindow()

                    rect?.let {
                        textToolBar.showMenu(
                            rect = it,
                            onCopyRequested = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(
                                            ClipData.newPlainText(
                                                "Popup TextField Content",
                                                content
                                            )
                                        )
                                    )
                                }
                            },
                            onPasteRequested = {
                                scope.launch {
                                    clipboard.getClipEntry()?.clipData?.getItemAt(
                                        0
                                    )?.text?.also {
                                        content = it.toString()
                                    }
                                }
                            },
                            onCutRequested = null,
                            onSelectAllRequested = null,
                            onAutofillRequested = {}
                        )
                    }
                }
            }
        }
    }
    TextField(
        value = content,
        onValueChange = {
            textToolBar.hide()
            content = it
        },
        interactionSource = interactionSource,
        modifier = Modifier.onGloballyPositioned {
            layoutCoordinates = it
        })
    val state = rememberTextFieldState()
    val selectionMiddle = remember { mutableIntStateOf(0) }
    BasicTextField(state = state, onTextLayout = {
        it().also { result ->
            result?.getHorizontalPosition(state.selection.start, true)?.let {
                selectionMiddle.intValue = it.toInt()
            }
        }
    })
}

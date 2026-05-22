package de.connect2x.trixnity.messenger.compose.view.theme.components

import android.content.ClipData
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.getSelectedText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
actual fun OutlinedTextFieldWithToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    readOnly: Boolean,
    textStyle: TextStyle,
    label: @Composable (() -> Unit)?,
    placeholder: @Composable (() -> Unit)?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    prefix: @Composable (() -> Unit)?,
    suffix: @Composable (() -> Unit)?,
    supportingText: @Composable (() -> Unit)?,
    isError: Boolean,
    visualTransformation: VisualTransformation,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    singleLine: Boolean,
    maxLines: Int,
    minLines: Int,
    interactionSource: MutableInteractionSource?,
    shape: Shape,
    colors: TextFieldColors,
) {
    val textToolBar = LocalTextToolbar.current
    val clipboard = LocalClipboard.current

    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val valueState = remember { mutableStateOf(value) }
    LaunchedEffect(value) { valueState.value = value }
    DisposableEffect(Unit) { onDispose { textToolBar.hide() } }
    LaunchedEffect(value.selection) {
        if (!value.selection.collapsed) {
            layoutCoordinates?.let { showToolbar(valueState, onValueChange, textToolBar, it, scope, clipboard) }
        }
    }
    LaunchedEffect(Unit) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Cancel) {
                layoutCoordinates?.let { showToolbar(valueState, onValueChange, textToolBar, it, scope, clipboard) }
            }
        }
    }
    OutlinedTextField(
        value = value,
        interactionSource = interactionSource,
        onValueChange = {
            onValueChange(it)
            if (it.selection.collapsed) {
                textToolBar.hide()
            }
        },
        modifier =
            modifier
                .onFocusChanged {
                    if (!it.hasFocus) {
                        textToolBar.hide()
                    }
                }
                .onGloballyPositioned { layoutCoordinates = it },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = shape,
        colors = colors,
    )
}

private fun showToolbar(
    valueState: MutableState<TextFieldValue>,
    onValueChange: (TextFieldValue) -> Unit,
    textToolBar: TextToolbar,
    layoutCoordinates: LayoutCoordinates,
    scope: CoroutineScope,
    clipboard: Clipboard,
) {
    val rect = layoutCoordinates.boundsInWindow()
    rect.let {
        textToolBar.showMenu(
            rect = it,
            onCopyRequested = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText("Popup TextField Content", valueState.value.getSelectedText()))
                    )
                }
            },
            onPasteRequested = {
                scope.launch {
                    clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.also {
                        val newCursorPos = valueState.value.selection.start + it.length
                        onValueChange(
                            valueState.value.copy(
                                text =
                                    StringBuilder(valueState.value.text)
                                        .deleteRange(valueState.value.selection.start, valueState.value.selection.end)
                                        .insert(valueState.value.selection.start, it.toString())
                                        .toString(),
                                selection = TextRange(newCursorPos),
                            )
                        )
                    }
                }
            },
            onCutRequested = {
                val selectedText = valueState.value.getSelectedText()
                onValueChange(
                    valueState.value.copy(
                        text =
                            StringBuilder(valueState.value.text)
                                .delete(valueState.value.selection.start, valueState.value.selection.end)
                                .toString(),
                        selection = TextRange(valueState.value.selection.start),
                    )
                )
                scope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Popup TextField Content", selectedText)))
                }
            },
            onSelectAllRequested = {
                onValueChange(valueState.value.copy(selection = TextRange(0, valueState.value.text.length)))
            },
            onAutofillRequested = null,
        )
    }
}

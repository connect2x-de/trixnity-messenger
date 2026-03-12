package de.connect2x.trixnity.messenger.compose.view.theme.components

import android.content.ClipData
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.getSelectedText
import kotlinx.coroutines.launch

@Composable
actual fun ThemedOutlinedTextFieldWithToolbar(
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
    colors: TextFieldColors
) {
    val textToolBar = LocalTextToolbar.current
    val clipboard = LocalClipboard.current

    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    var layoutCoordinates by remember {
        mutableStateOf<LayoutCoordinates?>(
            null
        )
    }
    LaunchedEffect(value.selection) {
        if (!value.selection.collapsed) {
            val rect = layoutCoordinates?.boundsInWindow()

            rect?.let {
                textToolBar.showMenu(
                    rect = it, onCopyRequested = {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(
                                ClipData.newPlainText(
                                    "Popup TextField Content", value.getSelectedText()
                                )
                            )
                        )
                    }
                }, onPasteRequested = {
                    scope.launch {
                        clipboard.getClipEntry()?.clipData?.getItemAt(
                            0
                        )?.text?.also {
                            val newCursorPos = value.selection.start + it.length
                            onValueChange(
                                value.copy(
                                    text = StringBuilder(value.text).deleteRange(
                                        value.selection.start, value.selection.end
                                    ).insert(value.selection.start, it.toString()).toString(),
                                    selection = TextRange(newCursorPos)
                                )
                            )
                        }
                    }
                }, onCutRequested = {
                    val selectedText = value.getSelectedText()
                    onValueChange(
                        value.copy(
                            text = StringBuilder(value.text).delete(
                                value.selection.start, value.selection.end
                            ).toString(), selection = TextRange(value.selection.start)
                        ),
                    )
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(
                                ClipData.newPlainText(
                                    "Popup TextField Content", selectedText
                                )
                            )
                        )
                    }
                }, onSelectAllRequested = {
                    onValueChange(value.copy(selection = TextRange(0, value.text.length)))
                }, onAutofillRequested = null
                )
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
        modifier = modifier.onGloballyPositioned {
            layoutCoordinates = it
        },
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

package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.icons.HelpIcon
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.util.EditableTextFieldViewModel


@Composable
fun EditableTextField(
    viewModel: EditableTextFieldViewModel,
    isEditable: Boolean,
    textCaption: String = "",
    textPlaceholder: String = "",
    textInfoCannotChange: String = "",
) {
    val i18n = DI.current.get<I18nView>()
    val isLoading by viewModel.isLoading.collectAsState()
    val state = viewModel.state.collectAsState().value
    val displayValue by state.collectAsStateForTextField()
    val isBeingEdited = state.isEditing()

    Column {
        Text(text = textCaption, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(10.dp))
        when {
            isLoading -> LoadingSpinner()

            isEditable -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isBeingEdited) {
                        IconButton(
                            onClick = viewModel::cancelEdit,
                            modifier = Modifier.buttonPointerModifier(),
                        ) {
                            Icon(Icons.Outlined.Clear, i18n.commonCancel())
                        }
                        Spacer(Modifier.size(10.dp))
                    }
                    OutlinedTextField(
                        value = displayValue,
                        onValueChange = { state.setEdit(it) },
                        enabled = isBeingEdited,
                        placeholder = { Text(textPlaceholder) },
                        modifier = Modifier.weight(1.0f, fill = true)
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = {
                                    if (!isBeingEdited) viewModel.startEdit()
                                })
                            },
                        colors = TextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.surfaceTint,
                        )
                    )
                    if (isBeingEdited) {
                        Spacer(Modifier.size(10.dp))
                        IconButton(
                            onClick = viewModel::applyEdit,
                            modifier = Modifier.buttonPointerModifier(),
                        ) {
                            Icon(Icons.Default.Check, i18n.commonRename())
                        }
                    }
                }
            }

            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = displayValue,
                        onValueChange = {},
                        enabled = false,
                    )
                    HelpIcon(textInfoCannotChange)
                }
            }
        }
    }
}

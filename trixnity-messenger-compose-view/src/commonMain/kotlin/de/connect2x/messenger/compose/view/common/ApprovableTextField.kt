package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.icons.HelpIcon
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.util.collectAsStateForLoadingIndicator
import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModel


@Composable
fun ApprovableTextField(
    viewModel: ApprovableTextFieldViewModel,
    isEditable: Boolean,
    textCaption: String = "",
    textPlaceholder: String = "",
    textInfoCannotChange: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val i18n = DI.get<I18nView>()
    val isEdit by viewModel.isEdit.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showLoading by viewModel.isLoading.collectAsStateForLoadingIndicator()
    var value by viewModel.collectAsTextFieldValueState()

    Column {
        Text(text = textCaption, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(10.dp))
        when {
            isLoading -> if(showLoading) {
                LoadingSpinner()
            }

            isEditable -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isEdit) {
                        Tooltip({ Text(i18n.commonCancel()) }) {
                            ThemedIconButton(
                                style = MaterialTheme.components.commonIconButton,
                                onClick = viewModel::cancelEdit,
                            ) {
                                Icon(Icons.Outlined.Clear, i18n.commonCancel())
                            }
                        }
                        Spacer(Modifier.size(10.dp))
                    }
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        enabled = isEdit,
                        placeholder = { Text(textPlaceholder) },
                        modifier = Modifier.weight(1.0f, fill = true)
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = {
                                    if (!isEdit) viewModel.startEdit()
                                })
                            },
                        colors = TextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.surfaceTint,
                        ),
                        keyboardOptions = keyboardOptions
                    )
                    if (isEdit) {
                        Spacer(Modifier.size(10.dp))
                        Tooltip({ Text(i18n.commonRename()) }) {
                            ThemedIconButton(
                                style = MaterialTheme.components.commonIconButton,
                                onClick = viewModel::approveEdit,
                            ) {
                                Icon(Icons.Default.Check, i18n.commonRename())
                            }
                        }
                    }
                }
            }

            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = {},
                        enabled = false,
                    )
                    HelpIcon(textInfoCannotChange)
                }
            }
        }
    }
}

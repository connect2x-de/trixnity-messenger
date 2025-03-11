package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.i18n.I18nView

@Composable
fun TextFieldModal(
    title: String,
    description: String? = null,
    textFieldValueState: MutableState<TextFieldValue>,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    width: Dp = 800.dp,
) {
    val i18n = DI.current.get<I18nView>()

    MessengerModal(
        onCancel,
        title,
        width
    ) {
        Column {
            description?.let { Text(it) }
            OutlinedTextField(
                value = textFieldValueState.value,
                onValueChange = {
                    textFieldValueState.value = it
                }
            )

            MessengerModalButtonRow(
                button1 = {
                    CloseModalButton(onCancel)
                },
                button2 = {
                    NextButton(true, i18n.commonSubmit(), onSubmit)
                }
            )
        }
    }
}

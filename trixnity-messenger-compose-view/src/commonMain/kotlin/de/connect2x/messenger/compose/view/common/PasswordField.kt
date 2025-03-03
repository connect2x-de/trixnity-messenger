package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PasswordField(
    password: MutableState<TextFieldValue>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    val passwordVisible = remember { mutableStateOf(false) }
    val i18n = DI.get<I18nView>()

    OutlinedTextField(
        value = password.value,
        onValueChange = { password.value = it },
        label = label,
        enabled = enabled,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .autofill(AutofillType.Password) {
                password.value = TextFieldValue(it)
            }
            .then(modifier),
        visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        trailingIcon = {
            IconButton(
                onClick = { passwordVisible.value = !passwordVisible.value },
                modifier = Modifier.buttonPointerModifier().focusable(false),
            ) {
                if (passwordVisible.value) Icon(Icons.Default.VisibilityOff, i18n.passwordVisibilityOff())
                else Icon(Icons.Default.Visibility, i18n.passwordVisibility())
            }
        }
    )
}

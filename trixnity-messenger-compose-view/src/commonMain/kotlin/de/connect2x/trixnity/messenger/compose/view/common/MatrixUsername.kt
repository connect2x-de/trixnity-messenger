@file:Suppress("DEPRECATION") // TODO: Remove once the androidx.compose.ui.autofill.ContentType API is public

package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.modifier.autofill
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.theme.components.OutlinedTextFieldWithToolbar

interface MatrixUsernameView {
    @Composable
    fun create(
        username: MutableState<TextFieldValue>,
        label: String,
        enabled: Boolean,
        modifier: Modifier,
        trailingIcon: @Composable (() -> Unit)?,
    )
}

@Composable
fun MatrixUsername(
    username: MutableState<TextFieldValue>,
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    DI.get<MatrixUsernameView>().create(username, label, enabled, modifier, trailingIcon)
}

class MatrixUsernameViewImpl : MatrixUsernameView {
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun create(
        username: MutableState<TextFieldValue>,
        label: String,
        enabled: Boolean,
        modifier: Modifier,
        trailingIcon: @Composable (() -> Unit)?,
    ) {
        OutlinedTextFieldWithToolbar(
            enabled = enabled,
            value = username.value,
            singleLine = true,
            onValueChange = { username.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .autofill(AutofillType.Username) {
                    username.value = TextFieldValue(it)
                }
                .then(modifier),
            label = { Text(label) },
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
        )
    }
}

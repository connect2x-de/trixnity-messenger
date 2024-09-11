package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get

interface MatrixUsernameView {
    @Composable
    fun create(
        username: State<String>,
        setUsername: (String) -> Unit,
        label: String,
        enabled: Boolean,
        modifier: Modifier,
        trailingIcon: @Composable (() -> Unit)?,
    )
}

@Composable
fun MatrixUsername(
    username: State<String>,
    setUsername: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    DI.get<MatrixUsernameView>().create(username, setUsername, label, enabled, modifier, trailingIcon)
}

class MatrixUsernameViewImpl : MatrixUsernameView {
    @Composable
    override fun create(
        username: State<String>,
        setUsername: (String) -> Unit,
        label: String,
        enabled: Boolean,
        modifier: Modifier,
        trailingIcon: @Composable (() -> Unit)?,
    ) {
        OutlinedTextField(
            enabled = enabled,
            value = username.value,
            singleLine = true,
            onValueChange = { setUsername(it) },
            modifier = Modifier.fillMaxWidth().then(modifier),
            label = { Text(label) },
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text
            ),
        )
    }
}

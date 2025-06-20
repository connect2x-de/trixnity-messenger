package de.connect2x.messenger.compose.view.connecting

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.maxLength
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel

interface ServerInputFieldView {
    @Composable
    fun create(addMatrixAccountViewModel: AddMatrixAccountViewModel)
}

@Composable
fun ServerInputField(addMatrixAccountViewModel: AddMatrixAccountViewModel) {
    DI.get<ServerInputFieldView>().create(addMatrixAccountViewModel)
}

class ServerInputFieldViewImpl : ServerInputFieldView {
    @Composable
    override fun create(addMatrixAccountViewModel: AddMatrixAccountViewModel) {
        val (serverUrl, maxLength) = addMatrixAccountViewModel.serverUrl.collectAsTextFieldValueState()
        val i18n = DI.get<I18nView>()

        OutlinedTextField(
            value = serverUrl.value,
            singleLine = true,
            onValueChange = { serverUrl.value = it.maxLength(maxLength) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri
            ),

            modifier = Modifier.fillMaxWidth(),
            label = { Text(i18n.addMatrixClientServerMatrix()) },
        )
    }
}

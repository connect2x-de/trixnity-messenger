package de.connect2x.messenger.compose.view.uia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.MatrixUsername
import de.connect2x.messenger.compose.view.common.PasswordField
import de.connect2x.messenger.compose.view.common.TabInTextField
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModel

interface UiaPasswordInputView {
    @Composable
    fun create(uiaStepPasswordViewModel: UiaStepPasswordViewModel)
}

@Composable
fun UiaPasswordInput(uiaStepPasswordViewModel: UiaStepPasswordViewModel) {
    DI.get<UiaPasswordInputView>().create(uiaStepPasswordViewModel)
}

class UiaPasswordInputViewImpl : UiaPasswordInputView {
    @Composable
    override fun create(uiaStepPasswordViewModel: UiaStepPasswordViewModel) {
        val i18n = DI.get<I18nView>()
        val isSubmitting = uiaStepPasswordViewModel.isSubmitting.collectAsState().value
        val error = uiaStepPasswordViewModel.error.collectAsState().value
        val tabToNextAndEnterSend = TabInTextField(true, uiaStepPasswordViewModel::submit)
        UiaModalBox {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                UiaHeading(i18n.uiaPasswordTitle())
                if (error != null) {
                    ErrorView(error)
                }
                if (isSubmitting) LoadingSpinner()
                Spacer(Modifier.height(20.dp))
                MatrixUsername(
                    username = uiaStepPasswordViewModel.username.collectAsTextFieldValueState(),
                    label = i18n.addMatrixClientMatrixUsername(),
                    modifier = tabToNextAndEnterSend,
                )
                Spacer(Modifier.height(20.dp))
                PasswordField(
                    password = uiaStepPasswordViewModel.password.collectAsTextFieldValueState(),
                    modifier = tabToNextAndEnterSend
                ) { Text(i18n.addMatrixClientPassword()) }
                Spacer(Modifier.height(40.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ThemedButton(
                        style = MaterialTheme.components.commonButton,
                        onClick = uiaStepPasswordViewModel::cancel,
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        enabled = !isSubmitting,
                        onClick = uiaStepPasswordViewModel::submit,
                    ) {
                        Text(i18n.uiaPasswordButtonSubmit().capitalize(Locale.current))
                    }
                }
            }
        }
    }
}

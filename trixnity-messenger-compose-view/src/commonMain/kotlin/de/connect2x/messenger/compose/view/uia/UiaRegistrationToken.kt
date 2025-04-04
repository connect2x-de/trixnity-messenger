package de.connect2x.messenger.compose.view.uia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.TabInTextField
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.util.collectAsStateForLoadingIndicator
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepRegistrationTokenViewModel

interface UiaRegistrationTokenView {
    @Composable
    fun create(uiaStepRegistrationTokenViewModel: UiaStepRegistrationTokenViewModel)
}

@Composable
fun UiaRegistrationToken(uiaStepRegistrationTokenViewModel: UiaStepRegistrationTokenViewModel) {
    DI.get<UiaRegistrationTokenView>().create(uiaStepRegistrationTokenViewModel)
}

class UiaRegistrationTokenViewImpl : UiaRegistrationTokenView {
    @Composable
    override fun create(uiaStepRegistrationTokenViewModel: UiaStepRegistrationTokenViewModel) {
        val i18n = DI.get<I18nView>()
        val isSubmitting = uiaStepRegistrationTokenViewModel.isSubmitting.collectAsStateForLoadingIndicator().value
        val error = uiaStepRegistrationTokenViewModel.error.collectAsState().value
        val tabToNextAndEnterSend = TabInTextField(true, uiaStepRegistrationTokenViewModel::submit)
        UiaModalBox {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                UiaHeading(i18n.uiaRegistrationTokenTitle())
                if (error != null) {
                    ErrorView(error)
                }
                if (isSubmitting) LoadingSpinner()
                Spacer(Modifier.height(20.dp))
                RegistrationToken(uiaStepRegistrationTokenViewModel, tabToNextAndEnterSend)
                Spacer(Modifier.height(40.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ThemedButton(
                        style = MaterialTheme.components.commonButton,
                        onClick = uiaStepRegistrationTokenViewModel::cancel,
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        enabled = !isSubmitting,
                        onClick = uiaStepRegistrationTokenViewModel::submit,
                    ) {
                        Text(i18n.uiaRegistrationTokenButtonSubmit().capitalize(Locale.current))
                    }
                }
            }
        }
    }
}

@Composable
fun RegistrationToken(
    uiaStepRegistrationTokenViewModel: UiaStepRegistrationTokenViewModel,
    tabToNextAndEnterSend: Modifier,
) {
    var registrationToken by uiaStepRegistrationTokenViewModel.registrationToken.collectAsTextFieldValueState()
    val i18n = DI.get<I18nView>()
    OutlinedTextField(
        enabled = true,
        value = registrationToken,
        singleLine = true,
        onValueChange = { registrationToken = it },
        modifier = Modifier.fillMaxWidth().then(tabToNextAndEnterSend),
        label = { Text(i18n.uiaRegistrationTokenAddToken()) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Email
        ),
    )
}

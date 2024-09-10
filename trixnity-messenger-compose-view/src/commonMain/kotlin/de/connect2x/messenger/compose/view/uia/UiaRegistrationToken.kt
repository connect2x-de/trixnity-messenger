package de.connect2x.messenger.compose.view.uia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.TabInTextField
import de.connect2x.messenger.compose.view.common.collectAsStateForTextField
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepRegistrationTokenViewModel

interface UiaRegistrationTokenView {
    @Composable
    fun create(uiaStepRegistrationTokenViewModel: UiaStepRegistrationTokenViewModel)
}

@Composable
fun UiaRegistrationToken(uiaStepRegistrationTokenViewModel: UiaStepRegistrationTokenViewModel) {
    DI.current.get<UiaRegistrationTokenView>().create(uiaStepRegistrationTokenViewModel)
}

class UiaRegistrationTokenViewImpl : UiaRegistrationTokenView {
    @Composable
    override fun create(uiaStepRegistrationTokenViewModel: UiaStepRegistrationTokenViewModel) {
        val i18n = DI.current.get<I18nView>()
        val isSubmitting = uiaStepRegistrationTokenViewModel.isSubmitting.collectAsState().value
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
                    OutlinedButton(
                        onClick = uiaStepRegistrationTokenViewModel::cancel,
                        modifier = Modifier.buttonPointerModifier(),
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                    Button(
                        enabled = !isSubmitting,
                        onClick = uiaStepRegistrationTokenViewModel::submit,
                        modifier = Modifier.buttonPointerModifier(),
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
    val username by uiaStepRegistrationTokenViewModel.registrationToken.collectAsStateForTextField()
    val i18n = DI.current.get<I18nView>()
    OutlinedTextField(
        enabled = true,
        value = username,
        singleLine = true,
        onValueChange = { uiaStepRegistrationTokenViewModel.registrationToken.value = it },
        modifier = Modifier.fillMaxWidth().then(tabToNextAndEnterSend),
        label = { Text(i18n.uiaRegistrationTokenAddToken()) },
        keyboardOptions = KeyboardOptions(
            autoCorrect = false,
            capitalization = KeyboardCapitalization.None,
            keyboardType = KeyboardType.Email,
        ),
    )
}
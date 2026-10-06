package de.connect2x.trixnity.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.Platform
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.common.MatrixUsername
import de.connect2x.trixnity.messenger.compose.view.common.PasswordField
import de.connect2x.trixnity.messenger.compose.view.common.TabInTextField
import de.connect2x.trixnity.messenger.compose.view.common.wizard.BackButton
import de.connect2x.trixnity.messenger.compose.view.common.wizard.NextButton
import de.connect2x.trixnity.messenger.compose.view.common.wizard.WizardPanel
import de.connect2x.trixnity.messenger.compose.view.common.wizard.WizardSection
import de.connect2x.trixnity.messenger.compose.view.form.AutofillButton
import de.connect2x.trixnity.messenger.compose.view.form.LocalHiddenRegistrationForm
import de.connect2x.trixnity.messenger.compose.view.form.rememberHiddenRegistrationForm
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.isWeb
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountState
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModel

interface PasswordLoginView {
    @Composable fun create(passwordLoginViewModel: PasswordLoginViewModel)
}

@Composable
fun PasswordLogin(passwordLoginViewModel: PasswordLoginViewModel) {
    DI.get<PasswordLoginView>().create(passwordLoginViewModel)
}

class PasswordLoginViewImpl : PasswordLoginView {
    @Composable
    override fun create(passwordLoginViewModel: PasswordLoginViewModel) {
        val hiddenRegistrationForm = LocalHiddenRegistrationForm.current
        val state = passwordLoginViewModel.addMatrixAccountState.collectAsState().value
        val canLogin = passwordLoginViewModel.canLogin.collectAsState().value
        val tabToNextAndEnterSend =
            TabInTextField(canLogin) {
                hiddenRegistrationForm.submit(
                    username = passwordLoginViewModel.username.value.text,
                    password = passwordLoginViewModel.password.value.text,
                )
                passwordLoginViewModel.tryLogin()
            }
        val i18n = DI.get<I18nView>()
        val username = passwordLoginViewModel.username.collectAsTextFieldValueState()
        val password = passwordLoginViewModel.password.collectAsTextFieldValueState()

        WizardSection(contentSpacing = MaterialTheme.messengerDpConstants.middle) {
            MatrixUsername(
                username = username,
                label = i18n.addMatrixClientMatrixUsername(),
                enabled = passwordLoginViewModel.addMatrixAccountState.collectAsState().value.inputEnabled(),
                modifier = tabToNextAndEnterSend,
            )
            PasswordField(
                password = password,
                enabled = passwordLoginViewModel.addMatrixAccountState.collectAsState().value.inputEnabled(),
                modifier = tabToNextAndEnterSend,
            ) {
                Text(i18n.addMatrixClientPassword())
            }
            if (Platform.current.isWeb) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AutofillButton(
                        onUsernameChange = passwordLoginViewModel.username::update,
                        onPasswordChange = passwordLoginViewModel.password::update,
                    )
                }
            }
            PasswordLoginState(state)
        }
    }
}

@Composable
fun PasswordLoginState(state: AddMatrixAccountState) {
    Box(Modifier.defaultMinSize(minHeight = 20.dp)) {
        when (state) {
            AddMatrixAccountState.None -> {}
            AddMatrixAccountState.Connecting ->
                ThemedProgressIndicator(Modifier.fillMaxWidth(), MaterialTheme.components.linearProgressIndicator)

            is AddMatrixAccountState.Failure -> Text(state.message, color = MaterialTheme.colorScheme.error)

            AddMatrixAccountState.Success -> {}
        }
    }
}

@Composable
fun PasswordLoginWizardPanel(viewModel: PasswordLoginViewModel) {
    val i18n = DI.current.get<I18nView>()
    WizardPanel(
        title = { i18n.loginAt(viewModel.serverUrl) },
        content = { PasswordLogin(viewModel) },
        buttons = {
            val state = viewModel.addMatrixAccountState.collectAsState().value
            val canLogin = viewModel.canLogin.collectAsState().value
            val hiddenRegistrationForm = rememberHiddenRegistrationForm()
            BackButton { viewModel.back() }

            NextButton(text = i18n.login(), enabled = canLogin && state !is AddMatrixAccountState.Connecting) {
                hiddenRegistrationForm.submit(
                    username = viewModel.username.value.text,
                    password = viewModel.password.value.text,
                )
                viewModel.tryLogin()
            }
        },
        wizardId = "ConnectingWizard",
    )
}

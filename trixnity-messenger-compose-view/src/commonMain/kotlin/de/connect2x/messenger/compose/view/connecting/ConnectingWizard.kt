package de.connect2x.messenger.compose.view.connecting

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardNextButton
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountState
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterNewAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModel

const val ADD_MATRIX_ACCOUNT = "ADD_MATRIX_ACCOUNT"
const val PASSWORD_LOGIN = "PASSWORD_LOGIN"
const val SSO_LOGIN = "SSO_LOGIN"
const val REGISTER_NEW_ACCOUNT = "REGISTER_NEW_ACCOUNT"

interface AdditionalConnectingWizardStep {
    fun <T : Any> create(viewModel: T): WizardStep
}

class AdditionalConnectingWizardStepImpl : AdditionalConnectingWizardStep {
    override fun <T : Any> create(viewModel: T): WizardStep {
        throw IllegalArgumentException("Do not know how to create a step with ${viewModel::class.simpleName}")
    }
}

@Composable
fun <T : Any> ConnectingWizard(viewModel: T) {
    val di = DI.current
    val i18n = DI.get<I18nView>()
    val wizardStep = when (viewModel) {
        is AddMatrixAccountViewModel -> WizardStep(
            id = ADD_MATRIX_ACCOUNT,
            title = {
                val isFirstMatrixClient = viewModel.isFirstMatrixClient.collectAsState().value
                if (isFirstMatrixClient != false) {
                    i18n.addMatrixClientCreateMatrixAccount()
                } else {
                    i18n.addMatrixClientAnotherMatrixClient()
                }
            },
            content = {
                AddMatrixAccount(viewModel)
            },
            additionalButton = {
                OutlinedButton(
                    onClick = viewModel::cancel,
                    modifier = Modifier.buttonPointerModifier()
                ) {
                    Text(i18n.commonCancel().capitalize(Locale.current))
                }
            },
            nextButton = WizardNextButton.None, // user selects preferred login method directly
        )

        is PasswordLoginViewModel -> WizardStep(
            id = PASSWORD_LOGIN,
            title = { i18n.loginAt(viewModel.serverUrl) },
            content = {
                PasswordLogin(viewModel)
            },
            additionalButton = {
                OutlinedButton(
                    onClick = viewModel::back,
                    modifier = Modifier.buttonPointerModifier()
                ) {
                    Text(i18n.commonBack().capitalize(Locale.current))
                }
            },
            nextButton = WizardNextButton.Custom {
                val state = viewModel.addMatrixAccountState.collectAsState().value
                val canLogin = viewModel.canLogin.collectAsState().value
                Button(
                    enabled = canLogin && state !is AddMatrixAccountState.Connecting,
                    onClick = { viewModel.tryLogin() },
                    modifier = Modifier.buttonPointerModifier(enabled = canLogin)
                ) {
                    Text(i18n.login())
                }
            }
        )

        is SSOLoginViewModel -> WizardStep(
            id = SSO_LOGIN,
            title = { i18n.loginAt(viewModel.serverUrl) },
            content = {
                SSOLogin(viewModel)
            },
            additionalButton = {
                OutlinedButton(
                    onClick = viewModel::back,
                    modifier = Modifier.buttonPointerModifier()
                ) {
                    Text(i18n.commonBack().capitalize(Locale.current))
                }
            },
            nextButton = WizardNextButton.Custom {
                val state = viewModel.addMatrixAccountState.collectAsState().value
                val waitForRedirect = viewModel.waitForRedirect.collectAsState().value
                val canLogin = !waitForRedirect && state !is AddMatrixAccountState.Connecting
                Button(
                    enabled = canLogin,
                    onClick = { viewModel.tryLogin() },
                    modifier = Modifier.buttonPointerModifier(enabled = canLogin)
                ) {
                    Text(i18n.login())
                }
            },
        )

        is RegisterNewAccountViewModel -> WizardStep(
            id = REGISTER_NEW_ACCOUNT,
            title = { i18n.registrationHeader() },
            content = {
                RegisterNewAccount(viewModel)
            },
            additionalButton = {
                OutlinedButton(
                    onClick = viewModel::back,
                    modifier = Modifier.buttonPointerModifier()
                ) {
                    Text(i18n.commonBack().capitalize(Locale.current))
                }
            },
            nextButton = WizardNextButton.Custom {
                val canRegisterNewUser = viewModel.canRegisterNewUser.collectAsState().value
                Button(
                    enabled = canRegisterNewUser,
                    onClick = { viewModel.register() },
                    modifier = Modifier.buttonPointerModifier(enabled = canRegisterNewUser)
                ) {
                    Text(i18n.register())
                }
            },
        )

        else -> di.get<AdditionalConnectingWizardStep>().create(viewModel)
    }

    return Wizard(listOf(wizardStep))
}

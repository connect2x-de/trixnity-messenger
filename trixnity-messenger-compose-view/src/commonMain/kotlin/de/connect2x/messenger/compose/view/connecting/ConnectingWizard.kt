package de.connect2x.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardNavigationButton
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountState
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountWarningViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModel

const val ADD_MATRIX_ACCOUNT_WARNING = "CREATE_ACCOUNT_WARNING"
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
    val i18n = DI.get<I18nView>()
    val additionalConnectingWizardStep = DI.get<AdditionalConnectingWizardStep>()
    val wizardStep = remember(viewModel, i18n, additionalConnectingWizardStep) {
        listOf(
            when (viewModel) {
                is AddMatrixAccountWarningViewModel -> addMatrixAccountWarningStep(viewModel, i18n)
                is AddMatrixAccountViewModel -> addMatrixAccountStep(viewModel, i18n)
                is PasswordLoginViewModel -> passwordLoginStep(viewModel, i18n)
                is SSOLoginViewModel -> SSOLoginStep(viewModel, i18n)
                is RegisterMatrixAccountViewModel -> registerMatrixAccountStep(viewModel, i18n)
                else -> additionalConnectingWizardStep.create(viewModel)
            }
        )
    }

    return Wizard(wizardStep)
}

private fun addMatrixAccountWarningStep(viewModel: AddMatrixAccountWarningViewModel, i18n: I18nView): WizardStep {
    return WizardStep(
        id = ADD_MATRIX_ACCOUNT_WARNING,
        title = { i18n.accountsOverviewCreateNewAccount() },
        content = {
            val isMultiProfile = viewModel.isMultiProfile.collectAsState().value
            Column {
                Text(i18n.accountOverviewWarning())
                if (isMultiProfile) Text(i18n.accountOverviewWarningMultipleAccounts())
            }
        },
        backButton = {
            WizardNavigationButton.Custom {
                ThemedButton(
                    style = MaterialTheme.components.commonButton,
                    onClick = viewModel::cancelWarning,
                    content = { Text(i18n.actionCancel()) },
                )
            }
        },

        additionalButton = {
            val isMultiProfile = viewModel.isMultiProfile.collectAsState().value

            if (isMultiProfile) ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = viewModel::logoutFromProfile,
                content = { Text(i18n.accountsOverviewLogout()) },
            )
        },
        nextButton = {
            WizardNavigationButton.Custom {
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    onClick = viewModel::createAccount,
                    content = { Text(i18n.accountsOverviewCreateNewAccount()) },
                )
            }
        }
    )
}

private fun addMatrixAccountStep(viewModel: AddMatrixAccountViewModel, i18n: I18nView): WizardStep {
    return WizardStep(
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
        nextButton = { WizardNavigationButton.None }, // user selects preferred login method directly
    )
}

private fun passwordLoginStep(viewModel: PasswordLoginViewModel, i18n: I18nView): WizardStep {
    return WizardStep(
        id = PASSWORD_LOGIN,
        title = { i18n.loginAt(viewModel.serverUrl) },
        content = {
            PasswordLogin(viewModel)
        },
        additionalButton = {
            ThemedButton(
                style = MaterialTheme.components.commonButton,
                onClick = viewModel::back,
            ) {
                Text(i18n.commonBack().capitalize(Locale.current))
            }
        },
        nextButton = {
            WizardNavigationButton.Custom {
                val state = viewModel.addMatrixAccountState.collectAsState().value
                val canLogin = viewModel.canLogin.collectAsState().value
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    enabled = canLogin && state !is AddMatrixAccountState.Connecting,
                    onClick = { viewModel.tryLogin() },
                ) {
                    Text(i18n.login())
                }
            }
        }
    )
}

private fun SSOLoginStep(viewModel: SSOLoginViewModel, i18n: I18nView): WizardStep {
    return WizardStep(
        id = SSO_LOGIN,
        title = { i18n.loginAt(viewModel.serverUrl) },
        content = {
            SSOLogin(viewModel)
        },
        additionalButton = {
            ThemedButton(
                style = MaterialTheme.components.commonButton,
                onClick = viewModel::back,
            ) {
                Text(i18n.commonBack().capitalize(Locale.current))
            }
        },
        nextButton = {
            WizardNavigationButton.Custom {
                val state = viewModel.addMatrixAccountState.collectAsState().value
                val waitForRedirect = viewModel.waitForRedirect.collectAsState().value
                val canLogin = !waitForRedirect && state !is AddMatrixAccountState.Connecting
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    enabled = canLogin,
                    onClick = { viewModel.tryLogin() },
                ) {
                    Text(i18n.login())
                }
            }
        },
    )
}

private fun registerMatrixAccountStep(viewModel: RegisterMatrixAccountViewModel, i18n: I18nView): WizardStep {
    return WizardStep(
        id = REGISTER_NEW_ACCOUNT,
        title = { i18n.registrationHeader() },
        content = {
            RegisterNewAccount(viewModel)
        },
        additionalButton = {
            ThemedButton(
                style = MaterialTheme.components.commonButton,
                onClick = viewModel::back,
            ) {
                Text(i18n.commonBack().capitalize(Locale.current))
            }
        },
        nextButton = {
            WizardNavigationButton.Custom {
                val canRegisterNewUser = viewModel.canRegisterNewUser.collectAsState().value
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    enabled = canRegisterNewUser,
                    onClick = { viewModel.register() },
                ) {
                    Text(i18n.register())
                }
            }
        },
    )
}

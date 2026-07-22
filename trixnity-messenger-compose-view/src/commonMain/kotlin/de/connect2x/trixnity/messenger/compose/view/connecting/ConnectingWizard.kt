package de.connect2x.trixnity.messenger.compose.view.connecting

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Wizard
import de.connect2x.trixnity.messenger.compose.view.common.WizardNavigationButton
import de.connect2x.trixnity.messenger.compose.view.common.WizardStep
import de.connect2x.trixnity.messenger.compose.view.form.LocalHiddenRegistrationForm
import de.connect2x.trixnity.messenger.compose.view.form.rememberHiddenRegistrationForm
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountState
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2AuthorizationCodeLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2DeviceAuthorizationLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterMatrixAccountViewModel
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
    val i18n = DI.get<I18nView>()
    val additionalConnectingWizardStep = DI.get<AdditionalConnectingWizardStep>()
    val hiddenRegistrationForm = rememberHiddenRegistrationForm()
    val wizardSteps =
        remember(viewModel, i18n, additionalConnectingWizardStep) {
            listOf(
                when (viewModel) {
                    is AddMatrixAccountViewModel -> AddMatrixAccountStep(viewModel, i18n)

                    is PasswordLoginViewModel -> LoginWithPasswordStep(viewModel, i18n)

                    is OAuth2AuthorizationCodeLoginViewModel -> LoginWithOAuth2AuthenticationCodeStep(viewModel, i18n)
                    is OAuth2DeviceAuthorizationLoginViewModel ->
                        LoginWithOAuth2DeviceAuthenticationStep(viewModel, i18n)

                    is SSOLoginViewModel -> LoginWithSSOStep(viewModel, i18n)

                    is RegisterMatrixAccountViewModel -> RegisterNewAccountStep(viewModel, i18n)

                    else -> additionalConnectingWizardStep.create(viewModel)
                }
            )
        }

    CompositionLocalProvider(LocalHiddenRegistrationForm provides hiddenRegistrationForm) {
        Wizard(wizardSteps = wizardSteps, wizardId = "ConnectingWizard")
    }
}

fun AddMatrixAccountStep(viewModel: AddMatrixAccountViewModel, i18n: I18nView): WizardStep {
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
        content = { AddMatrixAccount(viewModel) },
        nextButton = { WizardNavigationButton.None }, // user selects preferred login method directly
        backButton = {
            val hasOtherAccountsOrProfiles = viewModel.hasOtherAccountsOrProfiles.collectAsState().value
            if (hasOtherAccountsOrProfiles) {
                WizardNavigationButton.Custom {
                    ThemedButton(style = MaterialTheme.components.commonButton, onClick = { viewModel.cancel() }) {
                        Text(i18n.commonCancel())
                    }
                }
            } else WizardNavigationButton.None
        },
    )
}

fun LoginWithPasswordStep(viewModel: PasswordLoginViewModel, i18n: I18nView): WizardStep {
    return WizardStep(
        id = PASSWORD_LOGIN,
        title = { i18n.loginAt(viewModel.serverUrl) },
        content = { PasswordLogin(viewModel) },
        backButton = {
            WizardNavigationButton.Custom {
                ThemedButton(style = MaterialTheme.components.commonButton, onClick = viewModel::back) {
                    Text(i18n.commonBack().capitalize(Locale.current))
                }
            }
        },
        nextButton = {
            WizardNavigationButton.Custom {
                val state = viewModel.addMatrixAccountState.collectAsState().value
                val canLogin = viewModel.canLogin.collectAsState().value
                val hiddenRegistrationForm = LocalHiddenRegistrationForm.current
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    enabled = canLogin && state !is AddMatrixAccountState.Connecting,
                    onClick = {
                        hiddenRegistrationForm.submit(
                            username = viewModel.username.value.text,
                            password = viewModel.password.value.text,
                        )
                        viewModel.tryLogin()
                    },
                ) {
                    Text(i18n.login())
                }
            }
        },
    )
}

fun LoginWithOAuth2AuthenticationCodeStep(
    viewModel: OAuth2AuthorizationCodeLoginViewModel,
    i18n: I18nView,
): WizardStep {
    return WizardStep(
        id = SSO_LOGIN,
        title = {
            when (viewModel.type) {
                OAuth2AuthorizationCodeLoginViewModel.Type.LOGIN -> i18n.loginWithOAuth2()
                OAuth2AuthorizationCodeLoginViewModel.Type.REGISTER -> i18n.registerWithOAuth2()
            }
        },
        content = { OAuth2AuthorizationCodeLogin(viewModel) },
        additionalButton = {
            ThemedButton(style = MaterialTheme.components.commonButton, onClick = viewModel::back) {
                Text(i18n.commonBack().capitalize(Locale.current))
            }
        },
        nextButton = {
            WizardNavigationButton.Custom {
                val state = viewModel.state.collectAsState().value
                val canLogin = state is OAuth2AuthorizationCodeLoginViewModel.State.None
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    enabled = canLogin,
                    onClick = { viewModel.startLogin() },
                ) {
                    val text =
                        when (viewModel.type) {
                            OAuth2AuthorizationCodeLoginViewModel.Type.LOGIN -> i18n.login()
                            OAuth2AuthorizationCodeLoginViewModel.Type.REGISTER -> i18n.register()
                        }
                    Text(text)
                }
            }
        },
    )
}

fun LoginWithOAuth2DeviceAuthenticationStep(
    viewModel: OAuth2DeviceAuthorizationLoginViewModel,
    i18n: I18nView,
): WizardStep {
    return WizardStep(
        id = SSO_LOGIN,
        title = { i18n.loginWithOAuth2Device() },
        content = { OAuth2DeviceAuthorizationLogin(viewModel) },
        additionalButton = {
            ThemedButton(style = MaterialTheme.components.commonButton, onClick = viewModel::back) {
                Text(i18n.commonBack().capitalize(Locale.current))
            }
        },
        nextButton = {
            WizardNavigationButton.Custom {
                val state = viewModel.state.collectAsState().value
                val canOpen = state is OAuth2DeviceAuthorizationLoginViewModel.State.CheckCode
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    enabled = canOpen,
                    onClick = { viewModel.openLoginPage() },
                ) {
                    Text(i18n.openLoginPage())
                }
            }
        },
    )
}

fun LoginWithSSOStep(viewModel: SSOLoginViewModel, i18n: I18nView): WizardStep {
    return WizardStep(
        id = SSO_LOGIN,
        title = { i18n.loginAt(viewModel.serverUrl) },
        content = { SSOLogin(viewModel) },
        backButton = {
            WizardNavigationButton.Custom {
                ThemedButton(style = MaterialTheme.components.commonButton, onClick = viewModel::back) {
                    Text(i18n.commonBack().capitalize(Locale.current))
                }
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

fun RegisterNewAccountStep(viewModel: RegisterMatrixAccountViewModel, i18n: I18nView): WizardStep {
    return WizardStep(
        id = REGISTER_NEW_ACCOUNT,
        title = { i18n.registrationHeader() },
        content = { RegisterNewAccount(viewModel) },
        backButton = {
            WizardNavigationButton.Custom {
                ThemedButton(style = MaterialTheme.components.commonButton, onClick = viewModel::back) {
                    Text(i18n.commonBack().capitalize(Locale.current))
                }
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
                    Text(i18n.registerNewAccount())
                }
            }
        },
    )
}

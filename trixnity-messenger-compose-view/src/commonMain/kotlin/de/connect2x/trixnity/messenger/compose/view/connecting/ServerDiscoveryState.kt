package de.connect2x.trixnity.messenger.compose.view.connecting

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.ExpandableSection
import de.connect2x.trixnity.messenger.compose.view.files.toImageBitmap
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ButtonStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountMethod
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel.ServerDiscoveryState
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2AuthorizationCodeLoginViewModel

interface ServerDiscoveryStateView {
    @Composable fun create(addMatrixAccountViewModel: AddMatrixAccountViewModel)
}

@Composable
fun ServerDiscoveryState(addMatrixAccountViewModel: AddMatrixAccountViewModel) {
    DI.get<ServerDiscoveryStateView>().create(addMatrixAccountViewModel)
}

class ServerDiscoveryStateViewImpl : ServerDiscoveryStateView {
    @Composable
    override fun create(addMatrixAccountViewModel: AddMatrixAccountViewModel) {
        when (val serverDiscoveryState = addMatrixAccountViewModel.serverDiscoveryState.collectAsState().value) {
            is ServerDiscoveryState.None -> {}
            is ServerDiscoveryState.Loading ->
                ThemedProgressIndicator(Modifier.fillMaxWidth(), MaterialTheme.components.linearProgressIndicator)

            is ServerDiscoveryState.Success -> {
                val i18n = DI.get<I18nView>()
                val addMatrixAccountMethods =
                    remember(serverDiscoveryState.addMatrixAccountMethods) {
                        serverDiscoveryState.addMatrixAccountMethods.sortedBy {
                            when (it) {
                                is AddMatrixAccountMethod.OAuth2AuthorizationCode ->
                                    when (it.type) {
                                        OAuth2AuthorizationCodeLoginViewModel.Type.LOGIN -> 1
                                        OAuth2AuthorizationCodeLoginViewModel.Type.REGISTER -> 3
                                    }
                                is AddMatrixAccountMethod.OAuth2DeviceAuthorization -> 2
                                is AddMatrixAccountMethod.Password -> 3
                                is AddMatrixAccountMethod.SSO -> 4
                                is AddMatrixAccountMethod.Register -> 99
                            }
                        }
                    }
                OAuth2LoginItems(addMatrixAccountMethods, i18n, addMatrixAccountViewModel)
                val hasOAuth2AuthorizationCodeLogin = addMatrixAccountMethods.any {
                    it is AddMatrixAccountMethod.OAuth2AuthorizationCode ||
                        it is AddMatrixAccountMethod.OAuth2DeviceAuthorization
                }
                if (hasOAuth2AuthorizationCodeLogin) {
                    val nonOauth2AuthorizationCodeAddMatrixAccountMethods = addMatrixAccountMethods.filter {
                        it !is AddMatrixAccountMethod.OAuth2AuthorizationCode &&
                            it !is AddMatrixAccountMethod.OAuth2DeviceAuthorization
                    }
                    if (nonOauth2AuthorizationCodeAddMatrixAccountMethods.isNotEmpty())
                        ExpandableSection(heading = i18n.loginWithMoreClassic(), icon = Icons.Outlined.AlternateEmail) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.middle)
                            ) {
                                ClassicLoginItems(addMatrixAccountMethods, i18n, addMatrixAccountViewModel)
                            }
                        }
                } else ClassicLoginItems(addMatrixAccountMethods, i18n, addMatrixAccountViewModel)
            }

            is ServerDiscoveryState.Failure -> {
                Text(serverDiscoveryState.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun OAuth2LoginItems(
    addMatrixAccountMethods: List<AddMatrixAccountMethod>,
    i18n: I18nView,
    addMatrixAccountViewModel: AddMatrixAccountViewModel,
) {
    for (type in addMatrixAccountMethods) {
        when (type) {
            is AddMatrixAccountMethod.OAuth2AuthorizationCode -> {
                when (type.type) {
                    OAuth2AuthorizationCodeLoginViewModel.Type.LOGIN -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            LoginButton(
                                onClick = { addMatrixAccountViewModel.selectAddMatrixAccountMethod(type) },
                                isPrimary = true,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Login,
                                    i18n.loginWithOAuth2(),
                                    Modifier.fillMaxHeight(),
                                )
                                Text(i18n.loginWithOAuth2())
                            }
                        }
                    }

                    OAuth2AuthorizationCodeLoginViewModel.Type.REGISTER -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            LoginButton(
                                onClick = { addMatrixAccountViewModel.selectAddMatrixAccountMethod(type) },
                                isPrimary = false,
                            ) {
                                Icon(Icons.Outlined.PersonAdd, i18n.registerWithOAuth2(), Modifier.fillMaxHeight())
                                Text(i18n.registerWithOAuth2())
                            }
                        }
                    }
                }
            }
            is AddMatrixAccountMethod.OAuth2DeviceAuthorization -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LoginButton(
                        onClick = { addMatrixAccountViewModel.selectAddMatrixAccountMethod(type) },
                        isPrimary = false,
                    ) {
                        Icon(Icons.Outlined.DevicesOther, i18n.loginWithOAuth2Device(), Modifier.fillMaxHeight())
                        Text(i18n.loginWithOAuth2Device())
                    }
                }
            }

            is AddMatrixAccountMethod.Password,
            is AddMatrixAccountMethod.Register,
            is AddMatrixAccountMethod.SSO -> {}
        }
    }
}

@Composable
private fun ClassicLoginItems(
    addMatrixAccountMethods: List<AddMatrixAccountMethod>,
    i18n: I18nView,
    addMatrixAccountViewModel: AddMatrixAccountViewModel,
) {
    for (type in addMatrixAccountMethods) {
        when (type) {
            is AddMatrixAccountMethod.Password -> {
                LoginButton(
                    onClick = { addMatrixAccountViewModel.selectAddMatrixAccountMethod(type) },
                    isPrimary = true,
                ) {
                    Icon(Icons.Outlined.Password, i18n.loginWithPassword(), Modifier.fillMaxHeight())
                    Text(i18n.loginWithPassword())
                }
            }

            is AddMatrixAccountMethod.SSO -> {
                val providerName = type.identityProvider?.name ?: "SSO"
                LoginButton(
                    onClick = { addMatrixAccountViewModel.selectAddMatrixAccountMethod(type) },
                    isPrimary = false,
                ) {
                    val icon = type.icon?.toImageBitmap()
                    if (icon != null) Image(icon, i18n.loginWithSSO(providerName), Modifier.fillMaxHeight())
                    else Icon(Icons.Default.Web, i18n.loginWithSSO(providerName), Modifier.fillMaxHeight())
                    Text(i18n.loginWithSSO(providerName))
                }
            }

            is AddMatrixAccountMethod.Register -> {
                LoginButton(
                    onClick = { addMatrixAccountViewModel.selectAddMatrixAccountMethod(type) },
                    isPrimary = false,
                ) {
                    Icon(Icons.Outlined.PersonAdd, i18n.registerNewAccount(), Modifier.fillMaxHeight())
                    Text(i18n.registerNewAccount())
                }
            }

            is AddMatrixAccountMethod.OAuth2AuthorizationCode,
            is AddMatrixAccountMethod.OAuth2DeviceAuthorization -> {}
        }
    }
}

@Composable
private fun LoginButton(onClick: () -> Unit, isPrimary: Boolean, content: @Composable () -> Unit) {
    ThemedButton(
        onClick = onClick,
        style = if (isPrimary) MaterialTheme.components.primaryButton else ButtonStyle.outlined(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.middle),
        ) {
            content()
        }
    }
}

package de.connect2x.trixnity.messenger.compose.view.connecting

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.common.ExpandableSection
import de.connect2x.trixnity.messenger.compose.view.files.toImageBitmap
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItem
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountMethod
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel.ServerDiscoveryState
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2LoginViewModel


interface ServerDiscoveryStateView {
    @Composable
    fun create(addMatrixAccountViewModel: AddMatrixAccountViewModel)
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
            is ServerDiscoveryState.Loading -> ThemedProgressIndicator(
                Modifier.fillMaxWidth(),
                MaterialTheme.components.linearProgressIndicator
            )

            is ServerDiscoveryState.Success -> {
                val i18n = DI.get<I18nView>()
                val addMatrixAccountMethods = remember(serverDiscoveryState.addMatrixAccountMethods) {
                    serverDiscoveryState.addMatrixAccountMethods.sortedBy {
                        when (it) {
                            is AddMatrixAccountMethod.OAuth2 -> 0
                            is AddMatrixAccountMethod.Password -> 1
                            is AddMatrixAccountMethod.SSO -> 2
                            is AddMatrixAccountMethod.Register -> 99
                        }
                    }
                }
                OAuth2LoginItems(addMatrixAccountMethods, i18n, addMatrixAccountViewModel)
                val hasOAuth2Login = addMatrixAccountMethods.any { it is AddMatrixAccountMethod.OAuth2 }
                if (hasOAuth2Login) {
                    val nonOauth2AddMatrixAccountMethods =
                        addMatrixAccountMethods.filter { it !is AddMatrixAccountMethod.OAuth2 }
                    if (nonOauth2AddMatrixAccountMethods.isNotEmpty()) ExpandableSection(
                        heading = i18n.loginWithMoreClassic(),
                        icon = Icons.Outlined.AlternateEmail,
                    ) {
                        ClassicLoginItems(addMatrixAccountMethods, i18n, addMatrixAccountViewModel)
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
    addMatrixAccountViewModel: AddMatrixAccountViewModel
) {
    for (type in addMatrixAccountMethods) {
        when (type) {
            is AddMatrixAccountMethod.OAuth2 -> {
                when (type.type) {
                    OAuth2LoginViewModel.Type.LOGIN -> {
                        ThemedListItem(
                            headlineContent = { Text(i18n.loginWithOAuth2()) },
                            leadingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Login,
                                    i18n.loginWithOAuth2(),
                                    Modifier.fillMaxHeight(),
                                )
                            },
                            modifier = Modifier.clickable {
                                addMatrixAccountViewModel.selectAddMatrixAccountMethod(type)
                            }.buttonPointerModifier()
                        )
                    }

                    OAuth2LoginViewModel.Type.REGISTER -> {
                        ThemedListItem(
                            headlineContent = { Text(i18n.registerWithOAuth2()) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    i18n.registerWithOAuth2(),
                                    Modifier.fillMaxHeight(),
                                )
                            },
                            modifier = Modifier.clickable {
                                addMatrixAccountViewModel.selectAddMatrixAccountMethod(type)
                            }.buttonPointerModifier()
                        )
                    }
                }
            }

            is AddMatrixAccountMethod.Password,
            is AddMatrixAccountMethod.Register,
            is AddMatrixAccountMethod.SSO -> {
            }
        }
    }
}


@Composable
private fun ClassicLoginItems(
    addMatrixAccountMethods: List<AddMatrixAccountMethod>,
    i18n: I18nView,
    addMatrixAccountViewModel: AddMatrixAccountViewModel
) {
    for (type in addMatrixAccountMethods) {
        when (type) {
            is AddMatrixAccountMethod.Password -> {
                ThemedListItem(
                    headlineContent = { Text(i18n.loginWithPassword()) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Password,
                            i18n.loginWithPassword(),
                            Modifier.fillMaxHeight(),
                        )
                    },
                    modifier = Modifier.clickable {
                        addMatrixAccountViewModel.selectAddMatrixAccountMethod(type)
                    }.buttonPointerModifier()
                )
            }

            is AddMatrixAccountMethod.SSO -> {
                val providerName = type.identityProvider?.name ?: "SSO"
                ThemedListItem(
                    headlineContent = { Text(i18n.loginWithSSO(providerName)) },
                    leadingContent = {
                        val icon = type.icon?.toImageBitmap()
                        if (icon != null)
                            Image(
                                icon,
                                i18n.loginWithSSO(providerName),
                                Modifier.fillMaxHeight(),
                            )
                        else
                            Icon(
                                Icons.Default.Web,
                                i18n.loginWithSSO(providerName),
                                Modifier.fillMaxHeight(),
                            )
                    },
                    modifier = Modifier.clickable {
                        addMatrixAccountViewModel.selectAddMatrixAccountMethod(type)
                    }.buttonPointerModifier()
                )
            }

            is AddMatrixAccountMethod.Register -> {
                HorizontalDivider()
                ThemedListItem(
                    headlineContent = { Text(i18n.registerNewAccount()) },
                    leadingContent = {
                        Icon(
                            Icons.Default.PersonAdd,
                            i18n.registerNewAccount(),
                            Modifier.fillMaxHeight(),
                        )
                    },
                    modifier = Modifier.clickable {
                        addMatrixAccountViewModel.selectAddMatrixAccountMethod(type)
                    }.buttonPointerModifier()
                )
            }

            is AddMatrixAccountMethod.OAuth2 -> {}
        }
    }
}

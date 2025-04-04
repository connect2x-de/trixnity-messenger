package de.connect2x.messenger.compose.view.connecting

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.files.toImageBitmap
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountMethod
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel.ServerDiscoveryState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


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
        val i18n = DI.get<I18nView>()
        val serverDiscoveryState = addMatrixAccountViewModel.serverDiscoveryState.collectAsState().value
        var showLoading by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(serverDiscoveryState) {
            showLoading = when (serverDiscoveryState) {
                is ServerDiscoveryState.Loading -> {
                    delay(120.milliseconds)
                    true
                }
                is ServerDiscoveryState.Success, is ServerDiscoveryState.None, is ServerDiscoveryState.Failure ->
                    false
            }
        }

        when (serverDiscoveryState) {
            is ServerDiscoveryState.None -> {}
            is ServerDiscoveryState.Loading -> {
                if (showLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }

            is ServerDiscoveryState.Success -> {
                val addMatrixAccountMethods = remember {
                    serverDiscoveryState.addMatrixAccountMethods.sortedBy {
                        when (it) {
                            is AddMatrixAccountMethod.Password -> 0
                            is AddMatrixAccountMethod.SSO -> 1
                            is AddMatrixAccountMethod.Register -> 99
                        }
                    }
                }
                for (type in addMatrixAccountMethods) {
                    when (type) {
                        is AddMatrixAccountMethod.Password -> {
                            ListItem(
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
                                })
                        }

                        is AddMatrixAccountMethod.SSO -> {
                            val providerName = type.identityProvider?.name ?: "SSO"
                            ListItem(
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
                                })
                        }

                        is AddMatrixAccountMethod.Register -> {
                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text(i18n.register()) },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.PersonAdd,
                                        i18n.register(),
                                        Modifier.fillMaxHeight(),
                                    )
                                },
                                modifier = Modifier.clickable {
                                    addMatrixAccountViewModel.selectAddMatrixAccountMethod(type)
                                })
                        }
                    }
                }
            }

            is ServerDiscoveryState.Failure -> {
                Text(serverDiscoveryState.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

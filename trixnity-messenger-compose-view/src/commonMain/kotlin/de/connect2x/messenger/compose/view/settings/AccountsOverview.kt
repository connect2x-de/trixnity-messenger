package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedFloatingActionButton
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.messenger.compose.view.theme.components.ThemedSelectableText
import de.connect2x.trixnity.messenger.viewmodel.AccountInfo
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountsOverviewViewModel

interface AccountsOverviewView {
    @Composable
    fun create(accountsOverviewViewModel: AccountsOverviewViewModel)
}

@Composable
fun AccountsOverview(accountsOverviewViewModel: AccountsOverviewViewModel) {
    DI.get<AccountsOverviewView>().create(accountsOverviewViewModel)
}

class AccountsOverviewViewImpl : AccountsOverviewView {
    @Composable
    override fun create(accountsOverviewViewModel: AccountsOverviewViewModel) {
        val i18n = DI.get<I18nView>()
        val accounts = remember { accountsOverviewViewModel.accounts }.collectAsState().value
        val scrollState = rememberScrollState()
        var showLogoutWarning by remember { mutableStateOf<AccountInfo?>(null) }

        Box(Modifier.fillMaxSize()) {
            Column {
                Header(accountsOverviewViewModel::close, i18n.accountYourAccounts().capitalize(Locale.current))
                Column(Modifier.padding(10.dp).fillMaxWidth()) {
                    Column(
                        Modifier.scrollable(scrollState, Orientation.Vertical).weight(1.0f, fill = true),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        accounts.map { accountInfo ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .height(IntrinsicSize.Max),
                            ) {
                                val displayColor = accountInfo.displayColor
                                Box(
                                    Modifier
                                        .fillMaxHeight()
                                        .width(5.dp)
                                        .background(if (displayColor != null) Color(displayColor) else Color.Transparent)
                                        .padding(end = 5.dp)
                                )
                                Column(Modifier.weight(1.0f, fill = true)) {
                                    Tooltip(
                                        tooltip = {
                                            TooltipText { accountInfo.displayName }
                                        },
                                        content = {
                                            Text(
                                                accountInfo.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(horizontal = 10.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    )

                                    ThemedSelectableText(
                                        accountInfo.userId.full,
                                        MaterialTheme.components.selectionOnSurface,
                                        selectionModifier = Modifier.padding(horizontal = 10.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }

                                Tooltip({ Text(i18n.actionDelete()) }) {
                                    ThemedIconButton(
                                        style = MaterialTheme.components.destructiveIconButton,
                                        onClick = { showLogoutWarning = accountInfo }
                                    ) {
                                        Icon(Icons.AutoMirrored.Default.Logout, i18n.actionDelete())
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.size(20.dp))
                    ThemedFloatingActionButton(
                        expanded = true,
                        onClick = accountsOverviewViewModel::createNewAccount,
                        modifier = Modifier.buttonPointerModifier().align(Alignment.End).padding(all = 10.dp),
                        text = { Text(i18n.accountsOverviewCreateNewAccount()) },
                        icon = { Icon(Icons.Default.AddCircle, i18n.accountsOverviewCreateNewAccount()) },
                    )
                }
            }
        }
        if (showLogoutWarning != null) {
            ThemedModalDialog({ showLogoutWarning = null }) {
                ModalDialogHeader {
                    Text(i18n.accountsOverviewLogoutWarning(showLogoutWarning?.userId?.full ?: i18n.commonUnknown()))
                }
                ModalDialogContent {
                    Text(i18n.accountsOverviewLogoutWarningExplanation())
                }
                ModalDialogFooter {
                    ThemedButton(
                        style = MaterialTheme.components.commonButton,
                        onClick = { showLogoutWarning = null },
                    ) {
                        Text(i18n.actionCancel())
                    }
                    ThemedButton(
                        style = MaterialTheme.components.destructiveButton,
                        onClick = {
                            showLogoutWarning?.userId?.let { accountsOverviewViewModel.removeAccount(it) }
                            showLogoutWarning = null
                        },
                    ) {
                        Text(i18n.accountsOverviewLogout())
                    }
                }
            }
        }
    }
}

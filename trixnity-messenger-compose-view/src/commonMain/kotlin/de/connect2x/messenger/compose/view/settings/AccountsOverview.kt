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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.MessengerModal
import de.connect2x.messenger.compose.view.common.MessengerModalButtonRow
import de.connect2x.messenger.compose.view.common.MessengerModalContent
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
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
                                Text(
                                    accountInfo.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(10.dp).weight(1.0f, fill = true),
                                )
                                Text(
                                    accountInfo.userId.full,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(10.dp).weight(1.0f, fill = true),
                                )
                                OutlinedButton(onClick = { showLogoutWarning = accountInfo }) {
                                    Icon(Icons.Default.DeleteForever, i18n.commonDelete())
                                }
                            }
                        }
                    }
                    Spacer(Modifier.size(20.dp))
                    FloatingActionButton(
                        onClick = accountsOverviewViewModel::createNewAccount,
                        modifier = Modifier.buttonPointerModifier().align(Alignment.End).padding(all = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(all = 10.dp)) {
                            Icon(Icons.Default.AddCircle, i18n.commonCreate())
                            Spacer(Modifier.size(10.dp))
                            Text(i18n.accountsOverviewCreateNewAccount())
                        }
                    }
                }
            }
        }
        if (showLogoutWarning != null) {
            MessengerModal(
                onDismiss = { showLogoutWarning = null },
                i18n.accountsOverviewLogoutWarning(showLogoutWarning?.userId?.full ?: i18n.commonUnknown()),
            ) {
                MessengerModalContent {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, i18n.commonWarning())
                        Spacer(Modifier.size(10.dp))
                        Text(i18n.accountsOverviewLogoutWarningExplanation())
                    }
                }
                MessengerModalButtonRow(
                    button1 = {
                        OutlinedButton(
                            onClick = {
                                showLogoutWarning?.userId?.let { accountsOverviewViewModel.removeAccount(it) }
                                showLogoutWarning = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(i18n.accountsOverviewLogout())
                        }
                    },
                    button2 = {
                        Button(
                            onClick = { showLogoutWarning = null },
                        ) {
                            Text(i18n.commonCancel().capitalize(Locale.current))
                        }
                    })
            }
        }
    }
}

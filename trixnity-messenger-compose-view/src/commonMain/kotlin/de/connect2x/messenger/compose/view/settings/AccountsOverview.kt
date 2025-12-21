package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.common.modifier.focusHighlighting
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedFloatingActionButton
import de.connect2x.messenger.compose.view.theme.components.ThemedHorizontalDivider
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.messenger.compose.view.common.modifier.rovingFocusContainer
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
        val accounts by remember { accountsOverviewViewModel.accounts }.collectAsState()
        val scrollState = rememberScrollState()
        var showLogoutWarning by remember { mutableStateOf<AccountInfo?>(null) }

        var focusedItem by remember(accounts) { mutableStateOf(accounts.map { it.userId }.firstOrNull()) }

        Column {
            Header(accountsOverviewViewModel::close, i18n.accountYourAccounts().capitalize(Locale.current))
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .rovingFocusContainer()
                        .verticalScroll(scrollState)
                        .semantics {
                            collectionInfo = CollectionInfo(rowCount = accounts.size, columnCount = 1)
                        }
                ) {
                    accounts.mapIndexed { index, accountInfo ->
                        val displayColor = accountInfo.displayColor?.let { Color(it) }

                        ListItem(
                            headlineContent = {
                                Tooltip({ Text(accountInfo.displayName) }) {
                                    Text(
                                        accountInfo.displayName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                            supportingContent = { Text(accountInfo.userId.full) },
                            trailingContent = {
                                Tooltip({ Text(i18n.actionDelete()) }) {
                                    ThemedIconButton(
                                        style = MaterialTheme.components.destructiveIconButton,
                                        onClick = { showLogoutWarning = accountInfo },
                                    ) {
                                        Icon(Icons.AutoMirrored.Default.Logout, i18n.actionDelete())
                                    }
                                }
                            },
                            modifier = Modifier
                                .drawWithContent {
                                    drawContent()
                                    displayColor?.let {
                                        drawRect(displayColor, Offset.Zero, Size(5.dp.toPx(), size.height))
                                    }
                                }
                                .rovingFocusItem(
                                    isFocused = focusedItem == accountInfo.userId,
                                    onFocus = { focusedItem = accountInfo.userId }
                                )
                                .focusHighlighting()
                                .semantics {
                                    collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                                }
                        )
                        ThemedHorizontalDivider(style = MaterialTheme.components.roomListDivider)
                    }
                }
                VerticalScrollbar(
                    Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    scrollState,
                )
                Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp)) {
                    ThemedFloatingActionButton(
                        expanded = true,
                        onClick = accountsOverviewViewModel::createNewAccount,
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

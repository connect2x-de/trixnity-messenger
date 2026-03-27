package de.connect2x.trixnity.messenger.compose.view.roomlist.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.NotificationAndUnreadMarker
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.modifier.expandable
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedDropdownMenu
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedDropdownMenuItem
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.viewmodel.AccountInfo
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import kotlinx.coroutines.flow.map

interface AccountAvatarView {
    @Composable
    fun RowScope.create(accountViewModel: AccountViewModel)
}

@Composable
fun RowScope.AccountAvatar(accountViewModel: AccountViewModel) {
    with(DI.get<AccountAvatarView>()) { create(accountViewModel) }
}

class AccountAvatarViewImpl : AccountAvatarView {
    @Composable
    override fun RowScope.create(accountViewModel: AccountViewModel) {
        val activeAccount = accountViewModel.activeAccount.collectAsState().value
        if (activeAccount != null) {
            ActiveAccountData(activeAccount, accountViewModel)
        } else {
            NoAccountActiveAccountData(accountViewModel)
        }
    }
}

@Composable
fun RowScope.ActiveAccountData(activeAccount: UserId, accountViewModel: AccountViewModel) {
    val i18n = DI.get<I18nView>()
    val accounts = accountViewModel.accounts.collectAsState().value
    val accountSelectionOpen = remember { mutableStateOf(false) }
    val isSingleAccount = accountViewModel.isSingleAccount.collectAsState().value

    val activeAccountInfo = remember(accounts, activeAccount) {
        accountViewModel.accounts.map { accounts -> accounts.find { it.userId == activeAccount } }
    }.collectAsState(null).value

    val globalNotificationCount = accountViewModel.globalNotificationCount.collectAsState().value
    val accountNotificationCounts = accountViewModel.accountNotificationCounts.collectAsState().value

    if (activeAccountInfo != null) {
        Box(Modifier.weight(1.0f, false).fillMaxWidth()) {
            ThemedButton(
                style = MaterialTheme.components.accountSelector,
                onClick = {
                    if (isSingleAccount) accountViewModel.openUserAccounts()
                    else accountSelectionOpen.value = accountSelectionOpen.value.not()
                },
                modifier = Modifier.expandable(accountSelectionOpen),
            ) {
                AvatarArea(activeAccountInfo, accountNotificationCounts[activeAccount])
                if (isSingleAccount.not()) {
                    ThemedDropdownMenu(
                        expanded = accountSelectionOpen.value,
                        onDismissRequest = { accountSelectionOpen.value = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    ) {
                        SelectAccountHeader(i18n.accountChangeAccount())
                        AllAccountsMenuItem(
                            selectAction = {
                                accountViewModel.selectActiveAccount(null)
                                accountSelectionOpen.value = false
                            },
                            iconOverlay = {
                                if (globalNotificationCount == null) return@AllAccountsMenuItem
                                AccountNotificationCount(globalNotificationCount)
                            }
                        )
                        accounts
                            .filterNot { account -> account.userId == activeAccount }
                            .forEach { account ->
                                AccountMenuItem(
                                    accountInfo = account,
                                    selectAction = { userId ->
                                        accountViewModel.selectActiveAccount(userId)
                                        accountSelectionOpen.value = false
                                    },
                                    iconOverlay = {
                                        AccountNotificationCount(
                                            accountNotificationCounts[account.userId] ?: return@AccountMenuItem
                                        )
                                    }
                                )
                            }
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarArea(
    accountInfo: AccountInfo,
    notificationCount: String?
) {
    val i18n = DI.get<I18nView>()
    Row(
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                text =
                    AnnotatedString("${i18n.commonAccount()}: ${accountInfo.displayName}, ${accountInfo.userId.full}")
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemedUserAvatar(accountInfo.initials, accountInfo.avatar, overlay = {
            AccountNotificationCount(notificationCount ?: return@ThemedUserAvatar)
        })
        Spacer(Modifier.size(10.dp))
        Column {
            Tooltip({ Text(accountInfo.displayName) }) {
                Text(
                    accountInfo.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Tooltip({ Text(accountInfo.userId.full) }) {
                Text(
                    accountInfo.userId.full,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun RowScope.NoAccountActiveAccountData(accountViewModel: AccountViewModel) {
    val i18n = DI.get<I18nView>()
    val accounts = accountViewModel.accounts.collectAsState().value
    val accountSelectionOpen = remember { mutableStateOf(false) }

    val globalNotificationCount = accountViewModel.globalNotificationCount.collectAsState().value
    val accountNotificationCounts = accountViewModel.accountNotificationCounts.collectAsState().value

    Box(Modifier.weight(1.0f, false).fillMaxWidth()) {
        ThemedButton(
            style = MaterialTheme.components.accountSelector,
            onClick = { accountSelectionOpen.value = accountSelectionOpen.value.not() },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {
                        text = AnnotatedString(i18n.accountAllAccounts())
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemedUserAvatar("*", null, overlay = {
                    AccountNotificationCount(globalNotificationCount ?: return@ThemedUserAvatar)
                })
                Spacer(Modifier.size(10.dp))
                Tooltip({ Text(accounts.joinToString { account -> account.displayName }) }) {
                    Column {
                        Text(
                            i18n.accountAllAccounts(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            accounts.joinToString { account -> account.displayName },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        ThemedDropdownMenu(
            expanded = accountSelectionOpen.value,
            onDismissRequest = { accountSelectionOpen.value = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
        ) {
            SelectAccountHeader(i18n.accountChangeAccount())
            accounts.forEach { account ->
                AccountMenuItem(
                    accountInfo = account,
                    selectAction = { user ->
                        accountViewModel.selectActiveAccount(user)
                        accountSelectionOpen.value = false
                    },
                    iconOverlay = {
                        AccountNotificationCount(accountNotificationCounts[account.userId] ?: return@AccountMenuItem)
                    }
                )
            }
        }
    }
}

@Composable
fun BoxScope.AccountNotificationCount(count: String) {
    NotificationAndUnreadMarker(
        count = count,
        modifier = Modifier.align(Alignment.TopEnd).offset(2.dp, -(2.dp))
    )
}

@Composable
fun AllAccountsMenuItem(
    selectAction: () -> Unit,
    iconOverlay: @Composable BoxScope.() -> Unit = {}
) {
    val i18n = DI.get<I18nView>()
    ThemedDropdownMenuItem(
        leadingIcon = {
            ThemedUserAvatar("*", null, overlay = iconOverlay)
        },
        text = { Text(i18n.accountAllAccounts()) },
        onClick = selectAction,
    )
}

@Composable
fun AccountMenuItem(
    accountInfo: AccountInfo,
    selectAction: (UserId) -> Unit,
    iconOverlay: @Composable BoxScope.() -> Unit = {}
) {
    ThemedDropdownMenuItem(
        leadingIcon = {
            ThemedUserAvatar(accountInfo.initials, accountInfo.avatar, overlay = iconOverlay)
        },
        text = {
            Column {
                Tooltip({ Text(accountInfo.displayName) }) {
                    Text(
                        accountInfo.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    accountInfo.userId.full,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        onClick = { selectAction(accountInfo.userId) },
    )
}

@Composable
fun SelectAccountHeader(header: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            header,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .semantics {
                    heading()
                },
            style = MaterialTheme.typography.titleLarge,
        )
    }
    HorizontalDivider(Modifier.fillMaxWidth().padding(vertical = 5.dp))
}

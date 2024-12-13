package de.connect2x.messenger.compose.view.roomlist.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.AccountInfo
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.core.model.UserId

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
    val accountSelectionOpen = remember { mutableStateOf(false) }
    val isSingleAccount = accountViewModel.isSingleAccount.collectAsState().value
    val activeAccountInfo = remember(accountViewModel.accounts.value, activeAccount) {
        accountViewModel.accounts.map { accounts -> accounts.find { it.userId == activeAccount } }
    }.collectAsState(null).value

    if (activeAccountInfo != null) {
        Box(Modifier.Companion.weight(1.0f, false).fillMaxWidth()) {
            Button(
                onClick = {
                    if (isSingleAccount) accountViewModel.openUserProfile()
                    else accountSelectionOpen.value = accountSelectionOpen.value.not()
                },
                modifier = Modifier.buttonPointerModifier(),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                AvatarArea(activeAccountInfo)
                if (isSingleAccount.not()) DropdownMenu(
                    expanded = accountSelectionOpen.value,
                    onDismissRequest = { accountSelectionOpen.value = false },
                    offset = DpOffset(0.dp, 0.dp),
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                ) {
                    SelectAccountHeader(i18n.accountChangeAccount())
                    AllAccountsMenuItem(accountViewModel)
                    accountViewModel.accounts.value
                        .filterNot { account -> account.userId == activeAccount }
                        .forEach { account ->
                            AccountMenuItem(account, accountViewModel::selectActiveAccount)
                        }
                }
            }
        }
    }
}

@Composable
fun AvatarArea(
    accountInfo: AccountInfo,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Avatar(accountInfo.avatar, accountInfo.initials)
        Spacer(Modifier.size(10.dp))
        Column {
            Text(
                accountInfo.displayName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            )
            Text(
                accountInfo.userId.full,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            )
        }
    }
}

@Composable
fun RowScope.NoAccountActiveAccountData(accountViewModel: AccountViewModel) {
    val i18n = DI.get<I18nView>()
    val accounts = accountViewModel.accounts.collectAsState().value
    val accountSelectionOpen = remember { mutableStateOf(false) }

    Box(Modifier.Companion.weight(1.0f, false).fillMaxWidth()) {
        Button(
            onClick = { accountSelectionOpen.value = accountSelectionOpen.value.not() },
            modifier = Modifier.buttonPointerModifier(),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Avatar(null, "*")
                Spacer(Modifier.size(10.dp))
                Tooltip({
                    TooltipText {
                        accounts.joinToString { account -> account.displayName }
                    }
                },
                    onClick = { accountSelectionOpen.value = accountSelectionOpen.value.not() }
                ) {
                    Column {
                        Text(
                            i18n.accountAllAccounts(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        )
                        Text(
                            accounts.joinToString { account -> account.displayName },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = accountSelectionOpen.value,
            onDismissRequest = { accountSelectionOpen.value = false },
            offset = DpOffset(0.dp, 0.dp),
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
        ) {
            SelectAccountHeader(i18n.accountChangeAccount())
            accounts.forEach { account ->
                AccountMenuItem(account, accountViewModel::selectActiveAccount)
            }
        }
    }
}


@Composable
fun AllAccountsMenuItem(accountViewModel: AccountViewModel) {
    val i18n = DI.get<I18nView>()
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 10.dp), // to make up for missing account name space
            ) {
                Avatar(null, "*")
                Spacer(Modifier.size(10.dp))
                Text(
                    i18n.accountAllAccounts(),
                    Modifier.buttonPointerModifier(),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        onClick = { accountViewModel.selectActiveAccount(null) },
        contentPadding = PaddingValues(horizontal = 20.dp)
    )
}

@Composable
fun AccountMenuItem(
    accountInfo: AccountInfo,
    selectAction: (UserId) -> Unit,
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.buttonPointerModifier()) {
                Avatar(accountInfo.avatar, accountInfo.initials)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        accountInfo.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        accountInfo.userId.full,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
        onClick = { selectAction(accountInfo.userId) },
        contentPadding = PaddingValues(all = 20.dp)
    )
}

@Composable
fun SelectAccountHeader(header: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            header,
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
        )
    }
    HorizontalDivider(Modifier.fillMaxWidth().padding(vertical = 5.dp))
}

package de.connect2x.messenger.compose.view.roomlist.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel

interface AccountOptionsView {
    @Composable
    fun create(accountViewModel: AccountViewModel, roomListViewModel: RoomListViewModel)
}

@Composable
fun AccountOptions(accountViewModel: AccountViewModel, roomListViewModel: RoomListViewModel) {
    DI.current.get<AccountOptionsView>().create(accountViewModel, roomListViewModel)
}

class AccountOptionsViewImpl : AccountOptionsView {
    @Composable
    override fun create(accountViewModel: AccountViewModel, roomListViewModel: RoomListViewModel) {
        val i18n = DI.current.get<I18nView>()
        val config = DI.current.get<MatrixMessengerConfiguration>()
        val menuOpen = remember { mutableStateOf(false) }
        IconButton({ menuOpen.value = menuOpen.value.not() }, Modifier.buttonPointerModifier()) {
            Icon(Icons.Default.MoreVert, i18n.accountMoreSettings())
            DropdownMenu(
                menuOpen.value,
                { menuOpen.value = menuOpen.value.not() },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            i18n.commonSettings().capitalize(Locale.current),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    accountViewModel::userSettings,
                    Modifier.buttonPointerModifier(),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                )
                DropdownMenuItem(
                    text = {
                        Text(i18n.accountYourAccounts(), color = MaterialTheme.colorScheme.onBackground)
                    },
                    roomListViewModel::openAccountsOverview,
                    Modifier.buttonPointerModifier(),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            i18n.accountAboutTheApp(config.appName),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    accountViewModel::appInfo,
                    Modifier.buttonPointerModifier(),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                )

                config.sendLogsEmailAddress?.let {
                    DropdownMenuItem(
                        text = {
                            Text(i18n.accountSendErrorLogs(), color = MaterialTheme.colorScheme.onBackground)
                        },
                        roomListViewModel::sendLogs,
                        Modifier.buttonPointerModifier(),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    )
                }
            }
        }
    }
}

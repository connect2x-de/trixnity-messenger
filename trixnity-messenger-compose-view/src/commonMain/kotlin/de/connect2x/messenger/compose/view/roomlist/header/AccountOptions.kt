package de.connect2x.messenger.compose.view.roomlist.header

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.common.modifier.expandable
import de.connect2x.messenger.compose.view.common.modifier.focusOnFirstRender
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedDropdownMenu
import de.connect2x.messenger.compose.view.theme.components.ThemedDropdownMenuItem
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel

interface AccountOptionsView {
    @Composable
    fun create(accountViewModel: AccountViewModel, roomListViewModel: RoomListViewModel)
}

@Composable
fun AccountOptions(accountViewModel: AccountViewModel, roomListViewModel: RoomListViewModel) {
    DI.get<AccountOptionsView>().create(accountViewModel, roomListViewModel)
}

class AccountOptionsViewImpl : AccountOptionsView {
    @Composable
    override fun create(accountViewModel: AccountViewModel, roomListViewModel: RoomListViewModel) {
        val i18n = DI.get<I18nView>()
        val config = DI.get<MatrixMessengerConfiguration>()
        val menuOpen = remember { mutableStateOf(false) }
        Tooltip(
            tooltip = { Text(i18n.accountMoreSettings()) }
        ) {
            ThemedIconButton(
                style = MaterialTheme.components.commonIconButton,
                onClick = { menuOpen.value = menuOpen.value.not() },
                modifier = Modifier.expandable(menuOpen),
            ) {
                Icon(Icons.Default.MoreVert, i18n.accountMoreSettings())
                ThemedDropdownMenu(
                    menuOpen.value,
                    { menuOpen.value = menuOpen.value.not() },
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                ) {
                    ThemedDropdownMenuItem(
                        text = { Text(i18n.commonSettings().capitalize(Locale.current)) },
                        accountViewModel::openUserSettings,
                        modifier = Modifier.focusOnFirstRender(),
                    )
                    ThemedDropdownMenuItem(
                        text = { Text(i18n.accountAboutTheApp(config.appName)) },
                        accountViewModel::openAppInfo,
                    )

                    config.sendLogsEmailAddress?.let {
                        ThemedDropdownMenuItem(
                            text = { Text(i18n.accountSendErrorLogs()) },
                            roomListViewModel::sendLogs,
                        )
                    }
                }
            }
        }
    }
}

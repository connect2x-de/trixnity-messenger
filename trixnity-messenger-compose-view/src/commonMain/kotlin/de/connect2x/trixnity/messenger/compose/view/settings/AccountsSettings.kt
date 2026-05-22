package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.ErrorView
import de.connect2x.trixnity.messenger.compose.view.common.FilePickerType
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.files.LoadFileDialog
import de.connect2x.trixnity.messenger.compose.view.files.filterFilePickerOptionsByAvailability
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedFloatingActionButton
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountsViewModel

interface AccountsSettingsView {
    @Composable fun create(accountsViewModel: AccountsViewModel)
}

@Composable
fun AccountsSettings(accountsViewModel: AccountsViewModel) {
    DI.get<AccountsSettingsView>().create(accountsViewModel)
}

class AccountsSettingsViewImpl : AccountsSettingsView {
    @Composable
    override fun create(accountsViewModel: AccountsViewModel) {
        val openAvatarCutter = accountsViewModel.openAvatarCutter.collectAsState().value
        if (openAvatarCutter != null)
            LoadFileDialog(
                filterFilePickerOptionsByAvailability(FilePickerType.IMAGE_FILE, FilePickerType.PHOTO_CAPTURE),
                { accountsViewModel.openAvatarCutter(openAvatarCutter, it) },
                { accountsViewModel.closeAvatarCutter() },
            )
        AccountsOverview(accountsViewModel)
    }
}

@Composable
fun AccountsOverview(accountsViewModel: AccountsViewModel) {
    val i18n = DI.get<I18nView>()
    val error = accountsViewModel.error.collectAsState().value
    val accountSingleViewModels = accountsViewModel.accountSingleViewModels.collectAsState().value
    val scroll = rememberScrollState()

    Box(Modifier.fillMaxSize()) {
        Column {
            Header(accountsViewModel::close, i18n.accountYourAccounts())
            error?.let { ErrorView(it) }

            Box {
                Box {
                    Column(Modifier.padding(10.dp).verticalScroll(scroll)) {
                        accountSingleViewModels.forEach { accountSingleViewModel ->
                            AccountSingleSettings(accountSingleViewModel, accountsViewModel)
                        }
                        // leave space so that the floating action button does not cover up other elements
                        Spacer(Modifier.height(MaterialTheme.components.floatingActionButton.size + 2 * 20.dp))
                    }
                    VerticalScrollbar(Modifier.align(Alignment.CenterEnd).fillMaxHeight(), scroll)
                    Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp)) {
                        ThemedFloatingActionButton(
                            expanded = true,
                            onClick = { accountsViewModel.createNewAccount() },
                            text = { Text(i18n.accountsOverviewCreateNewAccount()) },
                            icon = { Icon(Icons.Default.AddCircle, null) },
                        )
                    }
                }
            }
        }
    }
}

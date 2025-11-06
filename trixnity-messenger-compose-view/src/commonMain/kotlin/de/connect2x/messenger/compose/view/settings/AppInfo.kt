package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Wysiwyg
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components.ThemedListItemButton
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.settings.AppInfoViewModel

interface AppInfoView {
    @Composable
    fun create(appInfoViewModel: AppInfoViewModel)
}

@Composable
fun AppInfo(appInfoViewModel: AppInfoViewModel) {
    DI.get<AppInfoView>().create(appInfoViewModel)
}

@Composable
expect fun PlatformAppInfo()

class AppInfoViewImpl : AppInfoView {
    @Composable
    override fun create(appInfoViewModel: AppInfoViewModel) {
        val i18n = DI.get<I18nView>()
        val showPrivacy = appInfoViewModel.showPrivacy.collectAsState().value
        val showImprint = appInfoViewModel.showImprint.collectAsState().value
        val showLicenses = appInfoViewModel.showLicenses.collectAsState().value

        Box(Modifier.fillMaxSize()) {
            Column {
                Header(
                    appInfoViewModel::close,
                    i18n.accountAboutTheApp(DI.get<MatrixMessengerConfiguration>().appName)
                        .capitalize(Locale.current),
                )
                AppInfoVersion(appInfoViewModel)
                PrivacyLink(appInfoViewModel)
                ImprintLink(appInfoViewModel)
                LicensesLink(appInfoViewModel)
                Spacer(Modifier.weight(1F))
                PlatformAppInfo()
            }
        }

        if (showPrivacy) AppInfoPrivacy(appInfoViewModel)
        if (showImprint) AppInfoImprint(appInfoViewModel)
        if (showLicenses) AppInfoLicenses(appInfoViewModel)
    }
}

@Composable
fun PrivacyLink(appInfoViewModel: AppInfoViewModel) {
    val i18n = DI.get<I18nView>()
    ThemedListItemButton(
        leadingContent = { Icon(Icons.Outlined.PrivacyTip, "") },
        headlineContent = { Text(i18n.appInfoPrivacy()) },
        onClick = { appInfoViewModel.showPrivacy.value = true },
        modifier = Modifier.heightIn(min = 72.dp),
    )
}

@Composable
fun ImprintLink(appInfoViewModel: AppInfoViewModel) {
    val i18n = DI.get<I18nView>()
    ThemedListItemButton(
        leadingContent = { Icon(Icons.Outlined.Email, "") },
        headlineContent = { Text(i18n.appInfoImprint()) },
        onClick = { appInfoViewModel.showImprint.value = true },
        modifier = Modifier.heightIn(min = 72.dp),
    )
}

@Composable
fun LicensesLink(appInfoViewModel: AppInfoViewModel) {
    val i18n = DI.get<I18nView>()
    ThemedListItemButton(
        leadingContent = { Icon(Icons.AutoMirrored.Outlined.Wysiwyg, "") },
        headlineContent = { Text(i18n.appInfoLicenses()) },
        onClick = { appInfoViewModel.showLicenses.value = true },
        modifier = Modifier.heightIn(min = 72.dp),
    )
}

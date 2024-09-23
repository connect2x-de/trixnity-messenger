package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Wysiwyg
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
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
    Item(i18n.appInfoPrivacy(), { appInfoViewModel.showPrivacy.value = true }) { Icon(Icons.Outlined.PrivacyTip, "") }
}

@Composable
fun ImprintLink(appInfoViewModel: AppInfoViewModel) {
    val i18n = DI.get<I18nView>()
    Item(i18n.appInfoImprint(), { appInfoViewModel.showImprint.value = true }) { Icon(Icons.Outlined.Email, "") }
}

@Composable
fun LicensesLink(appInfoViewModel: AppInfoViewModel) {
    val i18n = DI.get<I18nView>()
    Item(
        i18n.appInfoLicenses(),
        { appInfoViewModel.showLicenses.value = true }) { Icon(Icons.AutoMirrored.Outlined.Wysiwyg, "") }
}

@Composable
private fun Item(text: String, action: (() -> Unit)? = null, icon: @Composable () -> Unit) {
    Item({ Text(text) }, action, icon)
}

@Composable
fun Item(content: @Composable () -> Unit, action: (() -> Unit)? = null, icon: @Composable () -> Unit) {
    Box(Modifier
        .fillMaxWidth()
        .clickable {
            if (action != null) {
                action()
            }
        }
        .buttonPointerModifier()
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.size(20.dp))
            content()
        }
    }
}

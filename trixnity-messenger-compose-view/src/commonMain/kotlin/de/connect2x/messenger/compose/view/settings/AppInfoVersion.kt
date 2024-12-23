package de.connect2x.messenger.compose.view.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.settings.AppInfoViewModel
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

interface AppInfoVersionView {
    @Composable
    fun create(appInfoViewModel: AppInfoViewModel)
}

@Composable
fun AppInfoVersion(appInfoViewModel: AppInfoViewModel) {
    DI.get<AppInfoVersionView>().create(appInfoViewModel)
}

class AppInfoVersionViewImpl : AppInfoVersionView {
    @Composable
    override fun create(appInfoViewModel: AppInfoViewModel) {
        appInfoViewModel.version?.let { version ->
            val i18n = DI.get<I18nView>()
            Item(i18n.appInfoVersion(version)) { Icon(Icons.Outlined.Info, "") }
        } ?: log.warn { "No version found. To display a version here, override AppVersion and register it in the DI." }
    }
}

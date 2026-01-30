package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItem
import de.connect2x.trixnity.messenger.viewmodel.settings.AppInfoViewModel

private val log: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.settings.AppInfoViewKt")

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
            ThemedListItem(
                leadingContent = { Icon(Icons.Outlined.Info, "") },
                headlineContent = { Text(i18n.appInfoVersion(version)) },
                modifier = Modifier.heightIn(min = 72.dp),
            )
        } ?: log.warn { "No version found. To display a version here, override AppVersion and register it in the DI." }
    }
}

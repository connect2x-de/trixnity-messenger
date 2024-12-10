package de.connect2x.messenger.compose.view.settings

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.settings.AppInfoViewModel

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
        // FIXME version
    }
}

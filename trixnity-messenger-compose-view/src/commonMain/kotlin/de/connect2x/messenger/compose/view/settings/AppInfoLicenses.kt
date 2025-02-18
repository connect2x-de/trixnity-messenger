package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.MessengerModal
import de.connect2x.messenger.compose.view.common.MessengerModalButtonRow
import de.connect2x.messenger.compose.view.common.NextButton
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.settings.AppInfoViewModel

interface AppInfoLicensesView {
    @Composable
    fun create(appInfoViewModel: AppInfoViewModel)
}

@Composable
fun AppInfoLicenses(appInfoViewModel: AppInfoViewModel) {
    DI.get<AppInfoLicensesView>().create(appInfoViewModel)
}

class AppInfoLicensesViewImpl : AppInfoLicensesView {
    @Composable
    override fun create(appInfoViewModel: AppInfoViewModel) {
        val i18n = DI.get<I18nView>()
        val licences = DI.get<MatrixMessengerConfiguration>().licenses
        if (licences != null) {
            val lazyListState = rememberLazyListState()
            MessengerModal(onDismiss = { appInfoViewModel.showLicenses.value = false }, i18n.appInfoLicenses()) {
                Box(Modifier.fillMaxSize()) {
                    LibrariesContainer(
                        libraries = Libs.Builder().withJson(licences).build(),
                        lazyListState = lazyListState,
                        textStyles = LibraryDefaults.libraryTextStyles(
                            defaultOverflow = TextOverflow.Visible
                        )
                    )
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        lazyListState = lazyListState,
                        false,
                    )
                }
                MessengerModalButtonRow({
                    NextButton(text = i18n.commonBack()) { appInfoViewModel.showLicenses.value = false }
                })
            }
        }
    }
}

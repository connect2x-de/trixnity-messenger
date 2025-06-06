package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogHeader
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogScrollContent
import de.connect2x.messenger.compose.view.theme.components.ThemedAdaptiveDialog
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
            var openLibrary by remember { mutableStateOf<Library?>(null) }

            ThemedAdaptiveDialog({ appInfoViewModel.showLicenses.value = false },) {
                AdaptiveDialogHeader(onClose = { appInfoViewModel.showLicenses.value = false }) {
                    Text(i18n.appInfoLicenses())
                }
                AdaptiveDialogScrollContent(scrollState = lazyListState) {
                    LibrariesContainer(
                        colors = LibraryDefaults.libraryColors(backgroundColor = Color.Transparent),
                        libraries = Libs.Builder().withJson(licences).build(),
                        lazyListState = lazyListState,
                        textStyles = LibraryDefaults.libraryTextStyles(
                            defaultOverflow = TextOverflow.Ellipsis,
                            nameMaxLines = 10,
                            versionMaxLines = 2
                        ),
                        licenseDialogBody = { library ->
                            Text(library.licenses.firstOrNull()?.licenseContent ?: "")
                        },
                        onLibraryClick = { openLibrary = it}
                    )
                }
            }
            openLibrary?.let { library ->
                ThemedAdaptiveDialog({ openLibrary = null }) {
                    AdaptiveDialogHeader(onClose = { openLibrary = null }) {
                        Text(library.name)
                    }
                    AdaptiveDialogScrollContent {
                        Text(library.licenses.firstOrNull()?.licenseContent ?: "")
                    }
                }
            }
        }
    }
}

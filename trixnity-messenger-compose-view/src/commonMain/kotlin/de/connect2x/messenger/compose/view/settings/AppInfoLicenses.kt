package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.modifier.focusHighlighting
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogHeader
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogScrollContent
import de.connect2x.messenger.compose.view.theme.components.ThemedAdaptiveDialog
import de.connect2x.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
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
        Licenses { appInfoViewModel.showLicenses.value = false }
    }
}

@Composable
internal fun Licenses(onClose: () -> Unit) {
    val style = MaterialTheme.components.library

    val i18n = DI.get<I18nView>()
    val licences = DI.get<MatrixMessengerBaseConfiguration>().licenses
    if (licences != null) {
        val lazyListState = rememberLazyListState()
        val libraries = remember(licences) { Libs.Builder().withJson(licences).build() }
        var openLibrary by remember { mutableStateOf<Library?>(null) }

        var focusedItem by remember(libraries) { mutableStateOf(libraries.libraries.map { it.uniqueId }.firstOrNull()) }

        ThemedAdaptiveDialog(onClose) {
            AdaptiveDialogHeader(onClose = onClose) {
                Text(i18n.appInfoLicenses())
            }

            AdaptiveDialogScrollContent(scrollState = lazyListState) {
                LazyColumn(
                    modifier = Modifier.rovingFocusContainer(),
                    verticalArrangement = Arrangement.spacedBy(style.dimensions.itemSpacing),
                    state = lazyListState,
                ) {
                    items(libraries.libraries) { library ->
                        val interactionSource = remember { MutableInteractionSource() }
                        LibraryItem(
                            library = library,
                            modifier = Modifier
                                .focusHighlighting(interactionSource)
                                .rovingFocusItem(
                                    isFocused = focusedItem == library.uniqueId,
                                    onFocus = { focusedItem = library.uniqueId },
                                )
                                .clickable(interactionSource, LocalIndication.current) {
                                    openLibrary = library
                                }
                                .buttonPointerModifier(),
                            style = style,
                        )
                    }
                }
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

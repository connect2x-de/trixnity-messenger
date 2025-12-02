package de.connect2x.messenger.compose.view.roomlist.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.ExpandableSection
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.roomlist.search.SearchUserProviderSettings
import de.connect2x.messenger.compose.view.roomlist.search.searchResults
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatNewSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModel
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

class CreateNewChatNewSearchViewImpl : CreateNewChatView {
    @Composable
    override fun create(createNewChatViewModel: CreateNewChatViewModel) {
        if (createNewChatViewModel is CreateNewChatNewSearchViewModel) {
            val i18n = DI.get<I18nView>()
            val isCreating by createNewChatViewModel.isCreating.collectAsState()
            val error = createNewChatViewModel.error.collectAsState().value
            val errorDetails = createNewChatViewModel.errorDetails.collectAsState().value
            var searchTerm by createNewChatViewModel.searchUserViewModel.searchTerm.collectAsTextFieldValueState()
            val searchResultList = createNewChatViewModel.searchUserViewModel.searchResultList.collectAsState().value
            val providerSearchActive =
                createNewChatViewModel.searchUserViewModel.providerSearchActive.collectAsState().value
            val providerSettings = createNewChatViewModel.searchUserViewModel.providerSettings.collectAsState().value
            val listState = rememberLazyListState()

            val searchUserProviderSettings = remember { mutableStateOf(false) }

            Box(Modifier.fillMaxSize()) {
                Box {
                    if (error != null) {
                        ThemedModalDialog({ createNewChatViewModel.errorDismiss() }) {
                            ModalDialogHeader {
                                Text(i18n.anErrorHasOccurred())
                            }
                            ModalDialogContent {
                                Text(error)
                                if (errorDetails != null) {
                                    ExpandableSection(
                                        heading = i18n.errorDetails(),
                                    ) {
                                        Text(errorDetails, modifier = Modifier.padding(20.dp))
                                    }
                                }
                            }
                            ModalDialogFooter {
                                ThemedButton(
                                    style = MaterialTheme.components.primaryButton,
                                    onClick = { createNewChatViewModel.errorDismiss() },
                                ) {
                                    Text(i18n.actionOk())
                                }
                            }
                        }
                    }

                    Column {
                        Header(createNewChatViewModel::cancel, i18n.createNewChatTitle())
                        Box(Modifier.fillMaxSize()) {
                            LazyColumn(state = listState) {
                                item(key = "CreatingIndicator") {
                                    if (isCreating) {
                                        ThemedProgressIndicator(
                                            Modifier.fillMaxWidth(),
                                            MaterialTheme.components.linearProgressIndicator
                                        )
                                    }
                                }
                                item(key = "AddOrSearchGroup") {
                                    AddOrSearchGroup(createNewChatViewModel)
                                    HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
                                }
                                stickyHeader("searchTerm") {
                                    Surface(
                                        Modifier.fillMaxWidth()

                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = searchTerm,
                                                    onValueChange = { searchTerm = it },
                                                    modifier = Modifier.weight(1.0f, fill = true),
                                                )
                                                if (createNewChatViewModel.searchUserViewModel.searchUserProviders.any { searchUserProvider -> searchUserProvider.settings.isNotEmpty() }) {
                                                    ThemedIconButton(onClick = {
                                                        searchUserProviderSettings.value = true
                                                    }) {
                                                        Icon(Icons.Default.Settings, "Settings")
                                                    }
                                                }
                                            }
                                            SmallSpacer()
                                            providerSettings?.let { settings ->
                                                if (settings.isNotBlank()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Default.Settings,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                        )
                                                        SmallSpacer()
                                                        Text(settings, style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                searchResults(
                                    searchUserProviders = createNewChatViewModel.searchUserViewModel.searchUserProviders,
                                    createNewChatViewModel = createNewChatViewModel,
                                    providerSearchActive = providerSearchActive,
                                    providerSearchSetActive = { index, active ->
                                        createNewChatViewModel.searchUserViewModel.setProvider(index, active)
                                    },
                                    searchResultList = searchResultList,
                                )
                            }
                            VerticalScrollbar(
                                Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                lazyListState = listState,
                                reverseLayout = false,
                            )
                        }
                    }
                }
            }
            if (searchUserProviderSettings.value) {
                SearchUserProviderSettings(createNewChatViewModel.searchUserViewModel.searchUserProviders) {
                    searchUserProviderSettings.value = false
                }
            }
        } else {
            log.warn { "Cannot show CreateNewChatNewSearchView, since the viewmodel does not conform to CreateNewChatNewSearchViewModel" }
        }
    }
}

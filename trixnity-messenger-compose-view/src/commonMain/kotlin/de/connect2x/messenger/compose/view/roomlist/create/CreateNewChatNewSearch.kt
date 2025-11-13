package de.connect2x.messenger.compose.view.roomlist.create

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
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
import de.connect2x.messenger.compose.view.roomlist.search.searchResults
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
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
            val searchResults = createNewChatViewModel.searchUserViewModel.searchResult.collectAsState().value
            val providerSearchActive =
                createNewChatViewModel.searchUserViewModel.providerSearchActive.collectAsState().value
            val providerSettings = createNewChatViewModel.searchUserViewModel.providerSettings.collectAsState().value
            val listState = rememberLazyListState()

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
                        val expanded = rememberSaveable(searchResults) {
                            searchResults?.map { 3 }?.toMutableStateList() ?: SnapshotStateList()
                        }
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
                                                .padding(20.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = searchTerm,
                                                onValueChange = { searchTerm = it },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            providerSettings?.let { settings ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Settings", style = MaterialTheme.typography.bodyLarge)
                                                    SmallSpacer()
                                                    Text(settings, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                                searchResults(
                                    searchUserProviders = createNewChatViewModel.searchUserViewModel.searchUserProviders,
                                    createNewChatViewModel = createNewChatViewModel,
                                    searchTerm = searchTerm.text,
                                    providerSearchActive = providerSearchActive,
                                    providerSearchSetActive = { index, active ->
                                        createNewChatViewModel.searchUserViewModel.setProvider(index, active)
                                    },
                                    searchResults = searchResults,
                                    listState = listState,
                                    expanded = expanded,
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
        } else {
            log.warn { "Cannot show CreateNewChatNewSearchView, since the viewmodel does not conform to CreateNewChatNewSearchViewModel" }
        }
    }
}

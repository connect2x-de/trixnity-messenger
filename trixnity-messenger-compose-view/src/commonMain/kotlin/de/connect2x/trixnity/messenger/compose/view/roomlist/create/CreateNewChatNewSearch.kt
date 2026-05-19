package de.connect2x.trixnity.messenger.compose.view.roomlist.create

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.roomlist.search.SearchUserProviderSettings
import de.connect2x.trixnity.messenger.compose.view.roomlist.search.searchResults
import de.connect2x.trixnity.messenger.compose.view.search.searchTerm
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatNewSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModel

class CreateNewChatNewSearchViewImpl : CreateNewChatView {
    private val log =
        Logger("de.connect2x.trixnity.messenger.compose.view.roomlist.create.CreateNewChatNewSearchViewImpl")

    @Composable
    override fun create(createNewChatViewModel: CreateNewChatViewModel) {
        println("------- WOWO =====")
        if (createNewChatViewModel is CreateNewChatNewSearchViewModel) {
            val i18n = DI.get<I18nView>()
            val isCreating by createNewChatViewModel.isCreating.collectAsState()
            val error = createNewChatViewModel.error.collectAsState().value
            val errorDetails = createNewChatViewModel.errorDetails.collectAsState().value
            val searchResultList = createNewChatViewModel.searchUserViewModel.searchResultList.collectAsState().value
            val providerSearchActive =
                createNewChatViewModel.searchUserViewModel.providerSearchActive.collectAsState().value
            val listState = rememberLazyListState()

            val searchUserProviderSettings = remember { mutableStateOf(false) }

            Box(Modifier.fillMaxSize()) {
                Box {
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
                                searchTerm(createNewChatViewModel.searchUserViewModel) {
                                    searchUserProviderSettings.value = true
                                }
                                searchResults(
                                    searchUserProviders = createNewChatViewModel.searchUserViewModel.searchUserProviders,
                                    onUserClick = createNewChatViewModel::onUserClick,
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

                    CreateNewRoomErrorDialog(error, errorDetails, onDismiss = { createNewChatViewModel.errorDismiss() })
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

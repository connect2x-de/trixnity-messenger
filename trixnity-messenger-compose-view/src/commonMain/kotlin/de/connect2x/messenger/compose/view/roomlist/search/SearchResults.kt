package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatNewSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.SearchResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.ProviderSearchResult

// FIXME make extensible?

fun LazyListScope.searchResults(
    createNewChatViewModel: CreateNewChatViewModel,
    searchTerm: String,
    searchResults: List<SearchResult>?,
    listState: LazyListState,
    expanded: SnapshotStateList<Boolean>,
) {
    if (createNewChatViewModel is CreateNewChatNewSearchViewModel) {
        if (searchResults == null) {
            item("searchIn") {
                Text("Search in ... ") // FIXME every provider could contribute a location!
            }
        } else {
            searchResults.forEachIndexed { index, searchResult ->
                if (searchResults.size > 1) {
                    stickyHeader(searchResult.id) {
                        val stickyHeaderActive = rememberStickyHeaderActive(listState, searchResult.id)
                        Surface {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ThemedIconButton(
                                    onClick = { expanded[index] = expanded[index].not() },
                                    enabled = ((searchResult.providerSearchResult as? ProviderSearchResult.Success)
                                        ?.result?.size ?: 0) > 5,
                                ) {
                                    if (expanded[index]) {
                                        Icon(Icons.Default.ExpandLess, "Show less")
                                    } else {
                                        Icon(Icons.Default.ExpandMore, "Show more")
                                    }
                                }
                                Text(
                                    text =
                                        if (stickyHeaderActive.value) "${searchResult.providerDisplayName} ($searchTerm)"
                                        else searchResult.providerDisplayName,
                                    modifier = Modifier.padding(20.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                        if (expanded[index].not() && searchResult.isLoading.not()) {
                            when (val providerSearchResult = searchResult.providerSearchResult) {
                                null -> {}

                                is ProviderSearchResult.Success -> {
                                    providerSearchResult.result.take(5)
                                        .forEach { providerIndividualSearchResult ->
                                            Box(Modifier.padding(horizontal = 20.dp)) {
                                                SearchResultSelector(
                                                    userSearchResult = providerIndividualSearchResult,
                                                    onClick = {
                                                        createNewChatViewModel.onUserClick(it)
                                                    }
                                                )
                                            }
                                        }
                                }

                                is ProviderSearchResult.Failure -> {
                                    Text("${searchResult.providerDisplayName}: failure")
                                }
                            }
                        }
                    }
                }
                if (searchResult.isLoading) {
                    item("${searchResult.id}-loading") {
                        LoadingSpinner()
                    }
                } else {
                    when (val providerSearchResult = searchResult.providerSearchResult) {
                        null -> {}

                        is ProviderSearchResult.Success -> {
                            if (expanded[index]) {
                                providerSearchResult.result.forEach { providerIndividualSearchResult ->
                                    item("${searchResult.id}-${providerIndividualSearchResult.id}") {
                                        Box(Modifier.padding(horizontal = 20.dp)) {
                                            SearchResultSelector(
                                                userSearchResult = providerIndividualSearchResult,
                                                onClick = {
                                                    createNewChatViewModel.onUserClick(it)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is ProviderSearchResult.Failure -> {
                            item("${searchResult.id}-error") {
                                Text("${searchResult.providerDisplayName}: failure")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberStickyHeaderActive(state: LazyListState, key: Any): androidx.compose.runtime.State<Boolean> =
    remember(state) {
        derivedStateOf {
            val items = state.layoutInfo.visibleItemsInfo
            val header = items.getOrNull(0) ?: return@derivedStateOf false
            val item = items.getOrNull(1) ?: return@derivedStateOf false

            header.key == key && item.offset < header.size
        }
    }

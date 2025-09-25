package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.modifier.customClickable
import de.connect2x.messenger.compose.view.theme.components.ThemedFilterChip
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatNewSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.SearchResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.ProviderSearchResult

// FIXME make extensible?

fun LazyListScope.searchResults(
    createNewChatViewModel: CreateNewChatViewModel,
    searchTerm: String,
    providerSearchActive: List<Boolean>,
    providerSearchSetActive: (Int, Boolean) -> Unit,
    searchResults: List<SearchResult>?,
    listState: LazyListState,
    expanded: SnapshotStateList<Int>,
) {
    if (createNewChatViewModel is CreateNewChatNewSearchViewModel) {
        if (searchResults == null) {
            item("searchIn") {
                Text("Search in ... ") // FIXME every provider could contribute a location!
            }
        } else {
            optionsHeader(providerSearchActive, providerSearchSetActive, searchResults)
            searchResults.forEachIndexed { index, searchResult ->
                if (searchResults.size > 1) {
                    multipleSearchResultsHeader(
                        searchResult,
                        listState,
                        searchTerm,
                    )
                }
                if (searchResult.isLoading) {
                    item("${searchResult.id}-loading") {
                        LoadingSpinner()
                    }
                } else {
                    when (val providerSearchResult = searchResult.providerSearchResult) {
                        null -> {}

                        is ProviderSearchResult.Success -> {
                            allSearchResults(
                                providerSearchResult,
                                searchResult,
                                createNewChatViewModel,
                                expanded,
                                index,
                            )
                        }

                        is ProviderSearchResult.Failure -> {
                            item("${searchResult.id}-error") {
                                Text("${searchResult.providerDisplayName.prettyPrint()}: failure")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.optionsHeader(
    providerSearchActive: List<Boolean>,
    providerSearchSetActive: (Int, Boolean) -> Unit,
    searchResults: List<SearchResult>,
) {
    stickyHeader("searchOptions") {
        FlowRow(
            Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            searchResults.forEachIndexed { index, searchResult ->
                Row(
                    Modifier
                        .border(1.dp, Color.LightGray, shape = RoundedCornerShape(10.dp))
                        .padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    ThemedFilterChip(
                        selected = providerSearchActive[index],
                        onClick = { providerSearchSetActive(index, providerSearchActive[index].not()) },
                        label = {
                            Text(searchResult.providerDisplayName)
                        }
                    )
                    ThemedIconButton(onClick = { }) {
                        Icon(Icons.Default.Settings, "Settings for ${searchResult.providerDisplayName.prettyPrint()}")
                    }
                }
            }
        }
    }
}

private fun LazyListScope.multipleSearchResultsHeader(
    searchResult: SearchResult,
    listState: LazyListState,
    searchTerm: String
) {
    if (searchResult.active && (searchResult.providerSearchResult != null || searchResult.isLoading)) {
        stickyHeader(searchResult.id) {
            val stickyHeaderActive = rememberStickyHeaderActive(listState, searchResult.id)
            Surface {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            if (stickyHeaderActive.value) "${searchResult.providerDisplayName.prettyPrint()} (\"$searchTerm\")"
                            else searchResult.providerDisplayName.prettyPrint(),
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.allSearchResults(
    providerSearchResult: ProviderSearchResult.Success,
    searchResult: SearchResult,
    createNewChatViewModel: CreateNewChatNewSearchViewModel,
    expanded: SnapshotStateList<Int>,
    index: Int
) {
    if (searchResult.active) {
        if (providerSearchResult.result.isEmpty()) {
            item("${searchResult.id}-empty") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No search results found.") // FIXME i18n
                }
            }
        }
        providerSearchResult.result.take(expanded[index]).forEach { providerIndividualSearchResult ->
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
        if (providerSearchResult.result.size > expanded[index]) {
            item("more-${providerSearchResult.result.hashCode()}") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        "Load more...",
                        modifier = Modifier
                            .customClickable(
                                onClick = {
                                    expanded[index] = expanded[index] + 5
                                }
                            )
                            .align(Alignment.Center),
                    )
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

private fun String.prettyPrint() = this.replace("\n", " ")

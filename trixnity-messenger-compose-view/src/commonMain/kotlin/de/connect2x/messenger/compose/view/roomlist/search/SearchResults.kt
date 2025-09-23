package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.modifier.customClickable
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
    expanded: SnapshotStateList<Int>,
) {
    if (createNewChatViewModel is CreateNewChatNewSearchViewModel) {
        if (searchResults == null) {
            item("searchIn") {
                Text("Search in ... ") // FIXME every provider could contribute a location!
            }
        } else {
            optionsHeader(searchResults)
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
                                Text("${searchResult.providerDisplayName}: failure")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.optionsHeader(
    searchResults: List<SearchResult>,
) {
    stickyHeader("searchOptions") {
        FlowRow(Modifier.padding(horizontal = 20.dp)) {
            searchResults.forEach { searchResult ->
                val checked = remember { mutableStateOf(true) }
                MultiChoiceSegmentedButtonRow {
                    SegmentedButton(
                        checked = checked.value,
                        onCheckedChange = { checked.value = it },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            SegmentedButtonDefaults.Icon(checked.value)
                        },
                        label = {
                            Text(searchResult.providerDisplayName)
                        }
                    )
                    SegmentedButton(
                        checked = false,
                        onCheckedChange = { },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            SegmentedButtonDefaults.Icon(false)
                        },
                        label = {
                            Icon(Icons.Default.Settings, "Settings for ${searchResult.providerDisplayName}")
                        },
                        modifier = Modifier.requiredWidth(ButtonDefaults.MinWidth).weight(1.4f)
                    )
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
    stickyHeader(searchResult.id) {
        val stickyHeaderActive = rememberStickyHeaderActive(listState, searchResult.id)
        Surface {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        if (stickyHeaderActive.value) "${searchResult.providerDisplayName} (\"$searchTerm\")"
                        else searchResult.providerDisplayName,
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
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

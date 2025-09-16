package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatNewSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.SearchResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.ProviderSearchResult

// FIXME make extensible?

fun LazyListScope.searchResults(createNewChatViewModel: CreateNewChatViewModel, searchResults: List<SearchResult>?) {
    if (createNewChatViewModel is CreateNewChatNewSearchViewModel) {
        if (searchResults == null) {
            item("searchIn") {
                Text("Search in ... ") // FIXME every provider could contribute a location!
            }
        } else {
            searchResults.forEach { searchResult ->
                if (searchResults.size > 1) {
                    stickyHeader(searchResult.id) {
                        Text(
                            searchResult.providerDisplayName,
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                if (searchResult.isLoading) {
                    item("${searchResult.id}-loading") {
                        LoadingSpinner()
                    }
                } else {
                    when (val providerSearchResult = searchResult.providerSearchResult) {
                        null -> {

                        }

                        is ProviderSearchResult.Success -> {
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

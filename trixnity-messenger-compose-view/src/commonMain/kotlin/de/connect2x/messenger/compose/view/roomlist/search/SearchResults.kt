package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatNewSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.provider.ProviderSearchResult

// FIXME make extensible?

@Composable
fun SearchResults(createNewChatViewModel: CreateNewChatViewModel) {
    if (createNewChatViewModel is CreateNewChatNewSearchViewModel) {
        val searchResults = createNewChatViewModel.searchUserViewModel.searchResult.collectAsState().value
        if (searchResults == null) {
            Text("Search in ... ") // FIXME every provider could contribute a location!
        } else {
            // FIXME loading, etc
            searchResults.forEach { searchResult ->
                if (searchResult.isLoading) {
                    LoadingSpinner()
                } else {
                    when (val providerSearchResult = searchResult.providerSearchResult) {
                        null -> {

                        }

                        is ProviderSearchResult.Success -> {
                            providerSearchResult.result.forEach { providerIndividualSearchResult ->
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
    }
}

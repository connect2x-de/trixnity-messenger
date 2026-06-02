package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProviderId
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProviderResult

// used internally in [UserSearchViewModel]
internal data class SearchResult(
    val id: SearchProviderId,
    val enabled: Boolean,
    val providerDisplayName: String,
    val providerSearchResult: SearchProviderResult?,
    val isSearching: Boolean,
)

package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.viewmodel.search.provider.ProviderSearchResult

data class SearchResult(
    val providerDisplayName: String,
    val providerSearchResult: ProviderSearchResult,
    val isLoading: Boolean,
)

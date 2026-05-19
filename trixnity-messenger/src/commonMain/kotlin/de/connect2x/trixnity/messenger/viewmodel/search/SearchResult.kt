package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.viewmodel.search.provider.ProviderSearchResult

/**
 * A [SearchResult] is a search result from a [de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider].
 */
data class SearchResult(
    /**
     * The [de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider]'s id.
     */
    val id: String,
    /**
     * Indicates whether the search should be performed at all.
     */
    val active: Boolean,
    /**
     * The [de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider]'s display name.
     */
    val providerDisplayName: String,
    /**
     * The [de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider]'s search result.
     */
    val providerSearchResult: ProviderSearchResult?,
    /**
     * Indicates whether the search for the
     * [de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider] is currently running.
     */
    val isSearching: Boolean,
)

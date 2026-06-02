package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProviderId
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProviderResult

/** A [SearchResult] is a search result from a [SearchProvider]. */
data class SearchResult(
    /** The [SearchProvider]'s id. */
    val id: SearchProviderId,
    /** Indicates whether the search should be performed at all. */
    val enabled: Boolean,
    /** The [SearchProvider]'s display name. */
    val providerDisplayName: String,
    /** The [SearchProvider]'s search result. */
    val providerSearchResult: SearchProviderResult?,
    /** Indicates whether the search for the [SearchProvider] is currently running. */
    val isSearching: Boolean,
)

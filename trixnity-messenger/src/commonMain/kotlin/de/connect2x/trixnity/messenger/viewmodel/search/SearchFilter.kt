package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchFilterValue
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProvider

data class SearchFilter(
    val sources: List<SearchProvider<*>>,
    val searchFilterValueKeys: List<SearchFilterValue.Key<*>>,
    val isEnabled: Boolean,
)

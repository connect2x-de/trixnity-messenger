package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchFilter
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProviderResult
import kotlinx.coroutines.flow.StateFlow

interface Search<T : SearchProviderResult> {
    fun setFilter(filter: SearchFilter)

    fun removeFilter(filter: SearchFilter.Key<*>)

    val filters: StateFlow<List<SearchFilter>>

    val result: StateFlow<List<T>?>
}

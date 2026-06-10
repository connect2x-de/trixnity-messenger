package de.connect2x.trixnity.messenger.search.provider

import de.connect2x.trixnity.messenger.search.SearchResult

/**
 * Sorts the [SearchProvider]s by a criteria. The standard implementation ([SearchProviderSorterImpl]) uses the
 * [SearchProvider.priority] to do so, but the behavior can be overridden.
 */
interface SearchProviderSorter {
    fun <SR : SearchResult, SC : SearchContext> sort(list: List<SearchProvider<SR, SC>>): List<SearchProvider<SR, SC>>
}

class SearchProviderSorterImpl : SearchProviderSorter {
    override fun <SR : SearchResult, SC : SearchContext> sort(
        list: List<SearchProvider<SR, SC>>
    ): List<SearchProvider<SR, SC>> {
        return list.sortedBy { it.priority }
    }
}

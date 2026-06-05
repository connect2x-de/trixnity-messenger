package de.connect2x.trixnity.messenger.viewmodel.search.provider

/**
 * Sorts the [SearchProvider]s by a criteria. The standard implementation ([SearchProviderSorterImpl]) uses the
 * [SearchProvider.priority] to do so, but the behavior can be overridden.
 */
interface SearchProviderSorter {
    fun sort(list: List<SearchProvider<*>>): List<SearchProvider<*>>
}

class SearchProviderSorterImpl : SearchProviderSorter {
    override fun sort(list: List<SearchProvider<*>>): List<SearchProvider<*>> {
        return list.sortedBy { it.priority }
    }
}

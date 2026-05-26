package de.connect2x.trixnity.messenger.viewmodel.search.provider

/**
 * Sorts the [SearchUserProvider]s by a criteria. The standard implementation ([SearchUserProviderSorterImpl]) uses the
 * [SearchUserProvider.priority] to do so, but the behavior can be overridden.
 */
interface SearchUserProviderSorter {
    fun sort(list: List<SearchUserProvider>): List<SearchUserProvider>
}

class SearchUserProviderSorterImpl : SearchUserProviderSorter {
    override fun sort(list: List<SearchUserProvider>): List<SearchUserProvider> {
        return list.sortedBy { it.priority }
    }
}

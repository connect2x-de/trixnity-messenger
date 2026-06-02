package de.connect2x.trixnity.messenger.viewmodel.search.provider

/**
 * Sorts the [SearchProvider]s by a criteria. The standard implementation ([UserSearchProviderSorterImpl]) uses the
 * [SearchProvider.priority] to do so, but the behavior can be overridden.
 */
interface UserSearchProviderSorter {
    fun sort(list: List<SearchProvider<*>>): List<SearchProvider<*>>
}

class UserSearchProviderSorterImpl : UserSearchProviderSorter {
    override fun sort(list: List<SearchProvider<*>>): List<SearchProvider<*>> {
        return list.sortedBy { it.priority }
    }
}

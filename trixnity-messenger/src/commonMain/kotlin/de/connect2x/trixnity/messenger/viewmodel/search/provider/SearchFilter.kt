package de.connect2x.trixnity.messenger.viewmodel.search.provider

/**
 * Holds information on a special search filters, e.g., "city" -> "Berlin". The value itself is delegated to the
 * provider to perform the search.
 */
interface SearchFilter {
    val key: Key<*>

    interface Key<T : SearchFilter>
}

data class FulltextSearchFilter(val value: String) : SearchFilter {
    override val key = Key

    companion object Key : SearchFilter.Key<FulltextSearchFilter> {}
}

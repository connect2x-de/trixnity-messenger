package de.connect2x.trixnity.messenger.viewmodel.search.provider

/**
 * Holds information on a special search filters, e.g., "city" -> "Berlin". The value itself is delegated to the
 * provider to perform the search.
 */
interface SearchFilterValue {
    val key: Key<*>

    interface Key<T : SearchFilterValue>

    fun isEmpty(): Boolean

    fun displayValue(): String
}

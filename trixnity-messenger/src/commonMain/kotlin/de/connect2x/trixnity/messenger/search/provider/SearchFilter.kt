package de.connect2x.trixnity.messenger.search.provider

/**
 * Holds information on a special search filters, e.g., "city" -> "Berlin". The value itself is delegated to the
 * provider to perform the search. The search provider itself has to decide whether the filter value is relevant for its
 * search or not.
 */
interface SearchFilter {
    /**
     * The unique key of the filter. Implementations should use something like
     *
     * ```kotlin
     * class MySearchFilter(val value: String): SearchFilter {
     *   override val key = Key
     *   companion object Key: SearchFilterValue.Key<MySearchFilter>
     *  // ...
     * }
     * ```
     */
    val key: Key<*>

    /** A key to identify different [SearchFilter]s. */
    interface Key<T : SearchFilter>

    /**
     * Signifies that this filter is enabled and should be considered for the search. A filter should be considered
     * disabled if it is empty in case of a String.
     *
     * If it is enabled, it could lead to search providers being disabled when they do not support the
     * [SearchFilter.Key].
     */
    val isEnabled: Boolean

    /** The value to display in the UI. If the filter wraps a complex object, this can be more elaborate. */
    val displayValue: String
}

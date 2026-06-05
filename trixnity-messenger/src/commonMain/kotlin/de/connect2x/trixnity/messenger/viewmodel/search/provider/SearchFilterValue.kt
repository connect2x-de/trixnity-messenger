package de.connect2x.trixnity.messenger.viewmodel.search.provider

/**
 * Holds information on a special search filters, e.g., "city" -> "Berlin". The value itself is delegated to the
 * provider to perform the search. The search provider itself has to decide whether the filter value is relevant for its
 * search or not.
 */
interface SearchFilterValue {
    /**
     * The unique key of the filter. Implementations should use something like
     *
     * ```kotlin
     * class MySearchFilterValue(val value: String): SearchFilterValue {
     *   override val key = Key
     *   companion object Key: SearchFilterValue.Key<MySearchFilterValue>
     *  // ...
     * }
     * ```
     */
    val key: Key<*>

    /** A key to identify different [SearchFilterValue]s. */
    interface Key<T : SearchFilterValue>

    /**
     * Signifies that this filter is active and should be considered for the search. A filter should be considered
     * inactive if it is empty in case of a String.
     *
     * If it is active, it could lead to search providers being disabled when they do not support the
     * [SearchFilterValue.Key].
     */
    val isActive: Boolean

    /** The value to display in the UI. If the filter wraps a complex object, this can be more elaborate. */
    val displayValue: String
}

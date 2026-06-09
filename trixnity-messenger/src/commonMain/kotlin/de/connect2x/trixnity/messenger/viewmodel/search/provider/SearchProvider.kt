package de.connect2x.trixnity.messenger.viewmodel.search.provider

import de.connect2x.trixnity.core.model.UserId
import kotlinx.coroutines.CoroutineScope

/**
 * An extension point of the search functionality. The core search framework organizes [SearchProvider]s and their
 * filters and passes the search query to them.
 *
 * A [SearchProvider] declares which kind of filters it supports ([supportedFilters]). When a search is initiated by the
 * search framework, [search] is called with the current search term and all the currently active filter values,
 * including those of other [SearchProvider]s. It is the [SearchProvider]'s job to use only those filters it is
 * interested in. [search] returns a [SearchProviderResult].
 *
 * ### User Search
 * In case of the user search, this represents a place to search for users that at least have a UserId. For standard
 * Matrix clients this is the homeserver search which is already included by default.
 *
 * Other places could be an LDAP or central registry for users.
 */
interface SearchProvider<T : SearchProviderResult> {
    interface Key<T : SearchProvider<*>>

    /** A unique identifier for the provider. */
    val key: Key<*>

    /** A display name, e.g. "homeserver", "LDAP", etc. */
    val displayName: String

    /**
     * Determines the order with which the search providers and their settings are presented. This does _not_ include
     * the results as those are mixed randomly together to make all types appear near the top of the search result list.
     *
     * A lower number means a higher priority. The homeserver search default priority is 100.
     */
    val priority: Int

    /**
     * If `false`, the UI will show the settings inside a collapsed element and the provider is disabled initially (but
     * can be enabled by user interaction).
     */
    val disabledByDefault: Boolean

    /**
     * A list of [SearchFilter.Key]s the [SearchProvider] supports. Filters are used in addition to the search term and
     * the core search framework's UI implementation displays filters in addition to the search term text field.
     *
     * It is possible to share the same [SearchFilter.Key] between different [SearchProvider]s. The core search
     * framework merges those values and presents them as one filter that influences all declaring [SearchProvider]s.
     *
     * When a setting has a value that is not blank, all providers that do not have the setting are automatically
     * disabled (as searching and filtering for the setting does not make sense in this provider).
     */
    val supportedFilters: List<SearchFilter.Key<*>>

    /**
     * Do the actual search in the search provider. The provider is responsible to retrieve any [filters] it might have
     * defined (e.g., "city" -> "Berlin" and thus results only from Berlin should be returned).
     *
     * ```kotlin
     * override suspend fun search(
     *   searchTerm: String,
     *   filters: List<SearchFilterValue>,
     *   activeAccount: UserId,
     *   coroutineScope: CoroutineScope,
     * ): SearchProviderResult {
     * val myFilterValue = filters.filterIsInstance<MySearchFilterValue>().firstOrNull() ?: MySearchFilterValue("")
     *   // do something with myFilterValue
     * }
     * ```
     */
    suspend fun search(
        searchTerm: String,
        filters: List<SearchFilter>,
        activeAccount: UserId,
        coroutineScope: CoroutineScope,
    ): T
}

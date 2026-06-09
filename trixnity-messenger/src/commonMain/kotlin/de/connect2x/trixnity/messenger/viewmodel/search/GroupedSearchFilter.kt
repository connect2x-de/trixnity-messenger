package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchFilter
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProvider

/**
 * A filter combined of all registered [SearchProvider]s.
 *
 * Can have multiple [sources], i.e., [SearchProvider]s that declare the filter (via [SearchProvider.supportedFilters]).
 *
 * Can have multiple filters ([searchFilterKeys]) that are declared by the [sources].
 *
 * [isEnabled] depends on the [sources] being enabled and whether the [searchFilterKeys] of other, incompatible filters
 * are set.
 *
 * ### Example
 *
 * ```
 * [SearchProvider]1 and [SearchProvider]2 both support a filter with [SearchFilter.Key]A.
 * [SearchProvider]1 also supports [SearchFilter.Key]B and [SearchFilter.Key]C.
 * This results in:
 * [SearchFilter]1: sources = listOf([SearchProvider]1, [SearchProvider]2), searchFilterKeys = listOf([SearchFilter.Key]A)
 * [SearchFilter]2: sources = listOf([SearchProvider]1), searchFilterKeys = listOf([SearchFilter.Key]B, [SearchFilter.Key]C)
 * ```
 */
data class GroupedSearchFilter(
    val sources: List<SearchProvider<*>>,
    val searchFilterKeys: List<SearchFilter.Key<*>>,
    val isEnabled: Boolean,
)

package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchFilterValue
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProvider

/**
 * A filter combined of all registered [SearchProvider]s.
 *
 * Can have multiple [sources], i.e., [SearchProvider]s that declare the filter (via [SearchProvider.supportedFilters]).
 *
 * Can have multiple filters ([searchFilterValueKeys]) that are declared by the [sources].
 *
 * [isEnabled] depends on the [sources] being enabled and whether the [searchFilterValueKeys] of other, incompatible
 * filters are set.
 *
 * ### Example
 *
 * ```
 * [SearchProvider]1 and [SearchProvider]2 both support a filter with [SearchFilterValue.Key]A.
 * [SearchProvider]1 also supports [SearchFilterValue.Key]B and [SearchFilterValue.Key]C.
 * This results in:
 * [SearchFilter]1: sources = listOf([SearchProvider]1, [SearchProvider]2), searchFilterValueKeys = listOf([SearchFilterValue.Key]A)
 * [SearchFilter]2: sources = listOf([SearchProvider]1), searchFilterValueKeys = listOf([SearchFilterValue.Key]B, [SearchFilterValue.Key]C)
 * ```
 */
data class SearchFilter(
    val sources: List<SearchProvider<*>>,
    val searchFilterValueKeys: List<SearchFilterValue.Key<*>>,
    val isEnabled: Boolean,
)

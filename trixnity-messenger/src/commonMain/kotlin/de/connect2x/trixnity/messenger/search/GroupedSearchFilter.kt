package de.connect2x.trixnity.messenger.search

import de.connect2x.trixnity.messenger.search.provider.SearchFilter
import de.connect2x.trixnity.messenger.search.provider.SearchProvider

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
    val sources: List<SearchProvider<*, *>>,
    val searchFilterKeys: List<SearchFilter.Key<*>>,
    val isEnabled: Boolean,
)

fun List<SearchProvider<*, *>>.getGroupedSearchFilters(
    enabled: Map<SearchProvider.Key<*>, Boolean>
): List<GroupedSearchFilter> {
    return flatMap { searchProvider ->
            searchProvider.supportedFilters.map { Triple(it, searchProvider, enabled[searchProvider.key] == true) }
        }
        .fold(emptyList<GroupedSearchFilter>()) { acc, (key, provider, enabled) ->
            val existingKey = acc.find { it.searchFilterKeys.contains(key) }
            if (existingKey != null) { // the search filter is already present
                val withoutCurrentKey =
                    existingKey.copy(searchFilterKeys = existingKey.searchFilterKeys.filter { it != key })
                val alreadyCombined = acc.find { it.sources == existingKey.sources + provider }
                // we already combined with another provider -> we need to
                if (alreadyCombined != null) {
                    acc - existingKey + withoutCurrentKey - alreadyCombined +
                        alreadyCombined.copy(
                            sources = alreadyCombined.sources,
                            searchFilterKeys = alreadyCombined.searchFilterKeys + key,
                            isEnabled = alreadyCombined.isEnabled || enabled,
                        )
                } else { // no need to split, just add to the list of sources
                    acc - existingKey +
                        withoutCurrentKey +
                        GroupedSearchFilter(
                            sources = existingKey.sources + provider,
                            searchFilterKeys = listOf(key),
                            isEnabled = existingKey.isEnabled || enabled,
                        )
                }
            } else {
                val existing = acc.find { it.sources.size == 1 && it.sources.first() == provider }
                if (existing != null) { // the search provider already has registered a filter
                    acc - existing + existing.copy(searchFilterKeys = existing.searchFilterKeys + key)
                } else {
                    // completely new filter
                    acc +
                        GroupedSearchFilter(
                            sources = listOf(provider),
                            searchFilterKeys = listOf(key),
                            isEnabled = enabled,
                        )
                }
            }
        }
        .sortedByDescending { it.sources.size }
}

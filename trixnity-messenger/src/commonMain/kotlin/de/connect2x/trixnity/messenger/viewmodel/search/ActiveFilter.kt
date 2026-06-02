package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchFilterValue

/**
 * Representation of an active filter in the search. The [value] is the display value of the [SearchFilterValue].
 * Provides a possibility to [remove] the filter.
 */
data class ActiveFilter(val value: String, val remove: () -> Unit)

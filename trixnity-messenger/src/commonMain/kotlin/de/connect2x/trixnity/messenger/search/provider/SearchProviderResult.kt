package de.connect2x.trixnity.messenger.search.provider

import de.connect2x.trixnity.messenger.search.SearchResult

/** Result of a search a [SearchProvider] has performed. */
sealed interface SearchProviderResult<SR : SearchResult> {
    data class Success<SR : SearchResult>(val result: List<SR>) : SearchProviderResult<SR>

    data class Failure<SR : SearchResult>(val error: String) : SearchProviderResult<SR>
}

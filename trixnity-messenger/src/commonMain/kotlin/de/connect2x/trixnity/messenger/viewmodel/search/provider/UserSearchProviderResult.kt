package de.connect2x.trixnity.messenger.viewmodel.search.provider

import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult

/**
 * Represents a result of a [SearchProvider] that searches for [UserSearchResult]s. Can either be a [Success] or
 * [Failure].
 */
sealed interface UserSearchProviderResult : SearchProviderResult {
    data class Success(val result: List<UserSearchResult>) : UserSearchProviderResult

    data class Failure(val error: String) : UserSearchProviderResult
}

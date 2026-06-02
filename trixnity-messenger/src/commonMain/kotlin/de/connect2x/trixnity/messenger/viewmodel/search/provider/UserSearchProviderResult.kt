package de.connect2x.trixnity.messenger.viewmodel.search.provider

import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult

sealed interface UserSearchProviderResult : SearchProviderResult {
    data class Success(val result: List<UserSearchResult>) : UserSearchProviderResult

    data class Failure(val error: String) : UserSearchProviderResult
}

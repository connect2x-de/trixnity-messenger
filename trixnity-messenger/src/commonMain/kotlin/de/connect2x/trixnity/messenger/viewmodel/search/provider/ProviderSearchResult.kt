package de.connect2x.trixnity.messenger.viewmodel.search.provider

import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult

sealed interface ProviderSearchResult {
    data class Success(val result: List<UserSearchResult>) : ProviderSearchResult

    data class Failure(val error: String) : ProviderSearchResult
}

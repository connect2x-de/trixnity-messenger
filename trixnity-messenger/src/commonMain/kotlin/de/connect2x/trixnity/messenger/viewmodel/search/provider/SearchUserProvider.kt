package de.connect2x.trixnity.messenger.viewmodel.search.provider

import kotlinx.coroutines.CoroutineScope
import net.folivo.trixnity.core.model.UserId

// FIXME add filters?
interface SearchUserProvider {
    val providerId: String
    val providerDisplayName: String
    suspend fun search(
        searchTerm: String,
        activeAccount: UserId,
        coroutineScope: CoroutineScope,
    ): ProviderSearchResult
    // internal state for filters (we cannot define here)
}

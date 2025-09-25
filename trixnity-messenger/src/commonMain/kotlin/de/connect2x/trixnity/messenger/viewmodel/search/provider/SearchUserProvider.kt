package de.connect2x.trixnity.messenger.viewmodel.search.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.core.model.UserId

// FIXME add filters?
interface SearchUserProvider {
    val active: MutableStateFlow<Boolean>
    val providerId: String
    val providerDisplayName: String
    suspend fun search(
        searchTerm: String,
        activeAccount: UserId,
        coroutineScope: CoroutineScope,
    ): ProviderSearchResult
    // internal state for filters (we cannot define here)
}

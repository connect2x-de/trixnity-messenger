package de.connect2x.trixnity.messenger.search.provider

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.search.SearchResult
import kotlin.reflect.KClass

/** For registration in the DI, to create [SearchProvider]s. */
interface SearchProviderFactory<SR : SearchResult, SC : SearchContext> {
    /** Only [SearchProvider]s for the same context can and should be grouped. */
    val supports: KClass<SC>

    /**
     * Based on the account, the factory can decide to create the provider or not. Useful for providers that are only
     * valid in certain contexts.
     */
    fun create(account: UserId): SearchProvider<SR, SC>?
}

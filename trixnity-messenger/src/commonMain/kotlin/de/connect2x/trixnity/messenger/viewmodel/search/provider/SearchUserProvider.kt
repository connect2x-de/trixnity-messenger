package de.connect2x.trixnity.messenger.viewmodel.search.provider

import de.connect2x.trixnity.core.model.UserId
import kotlinx.coroutines.CoroutineScope

typealias SearchUserProviderId = String

interface SettingsId

/**
 * A place to search for users that at least have a UserId. For standard Matrix clients this is the homeserver search
 * which is already included by default.
 *
 * Other places could be an LDAP or central registry for users.
 */
interface SearchUserProvider {
    /** A unique identifier for the provider. */
    val providerId: SearchUserProviderId

    /** A display name, e.g. "homeserver", "LDAP", etc. */
    val providerDisplayName: String

    /**
     * Determines the order with which the search providers and their settings are presented. This does _not_ include
     * the results as those are mixed randomly together to make all types appear near the top of the search result list.
     *
     * A lower number means a higher priority. The homeserver search default priority is 100.
     */
    val priority: Int

    /**
     * If `false`, the UI will show the settings inside a collapsed element and the provider is disabled initially (but
     * can be enabled by user interaction).
     */
    val disabledByDefault: Boolean

    /**
     * The [SettingsId] allows the usage of settings/filters in multiple providers. E.g., a setting could be a filter
     * for "city" in multiple providers.
     *
     * When a setting has a value that is not blank, all providers that do not have the setting are automatically
     * disabled (as searching and filtering for the setting does not make sense in this provider).
     */
    val settings: Map<SettingsId, SearchSetting>

    /**
     * Do the actual search in the search provider. The provider is responsible to include any [settings] it might have
     * defined (e.g., "city" -> "Berlin" and thus results only from Berlin should be returned).
     */
    suspend fun search(searchTerm: String, activeAccount: UserId, coroutineScope: CoroutineScope): ProviderSearchResult
}

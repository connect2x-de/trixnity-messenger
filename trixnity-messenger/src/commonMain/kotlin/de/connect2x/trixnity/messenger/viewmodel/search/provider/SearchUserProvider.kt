package de.connect2x.trixnity.messenger.viewmodel.search.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.UserId

typealias SearchUserProviderId = String
typealias SettingsId = String

/**
 * Holds information on a special search setting, e.g., "city" -> "Berlin".
 */
data class SearchSetting(
    /**
     * The name of the setting for the UI. Should be i18nized.
     */
    val name: String,
    /**
     * The setting's value. Can be `null` or blank to be interpreted as 'not set'.
     */
    val value: String? = null,
)

/**
 * A place to search for users that at least have a UserId. For standard Matrix clients this is the homeserver search
 * which is already included by default.
 *
 * Other places could be an LDAP or central registry for users.
 */
interface SearchUserProvider {
    /**
     * A unique identifier for the provider.
     */
    val providerId: SearchUserProviderId

    /**
     * A display name. Can contain line breaks (\n).
     */
    val providerDisplayName: String

    /**
     * The [SettingsId] allows the usage of settings/filters in multiple providers. E.g., a setting could be a filter
     * for "city" in multiple providers.
     *
     * When a setting has a value that is not blank, all providers that do not have the setting are automatically
     * disabled (as searching and filtering for the setting does not make sense in this provider).
     */
    val settings: Map<SettingsId, StateFlow<SearchSetting>>

    /**
     * Although [SearchSetting] in [settings] are Flows, it should be updated lazily, e.g., by pressing "apply" in a
     * popup or similar.
     */
    fun applySettings()

    /**
     * Do the actual search in the search provider. The provider is responsible to include any [settings] it might have
     * defined (e.g., "city" -> "Berlin" and thus results only from Berlin should be returned).
     */
    suspend fun search(
        searchTerm: String,
        activeAccount: UserId,
        coroutineScope: CoroutineScope,
    ): ProviderSearchResult

    // internal state for filters (use in extensions of this interface and in the UI cast to the appropriate subtype)
}

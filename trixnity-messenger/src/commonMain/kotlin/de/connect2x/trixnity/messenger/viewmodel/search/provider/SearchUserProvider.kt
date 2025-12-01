package de.connect2x.trixnity.messenger.viewmodel.search.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.UserId

typealias SettingsId = String

data class SearchSetting(
    val name: String, // language for i18n can change
    val value: String? = null, // value can change
)

/**
 * A place to search for users that at least have a UserId. For standard Matrix clients this is the homeserver search
 * which is already included by default.
 *
 * Other places could be an LDAP or central registry for users.
 */
interface SearchUserProvider {
    /**
     * Whether the Provider is enabled. If disabled, it will not be used in the search.
     */
    val enabled: MutableStateFlow<Boolean>

    /**
     * A unique identifier for the provider.
     */
    val providerId: String

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

    suspend fun search(
        searchTerm: String,
        activeAccount: UserId,
        coroutineScope: CoroutineScope,
    ): ProviderSearchResult

    // internal state for filters (use in extensions of this interface and in the UI cast to the appropriate subtype)
}

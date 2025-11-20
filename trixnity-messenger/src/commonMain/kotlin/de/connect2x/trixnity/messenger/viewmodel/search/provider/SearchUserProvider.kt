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
     * Whether the Provider is enabled.
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
     * Whether the Provider can be configured to filter or alter the search results.
     */
    val hasSettings: Boolean

    /**
     * When settings are active, those should be displayed in the search UI to not lose the context that is not visible
     * anymore.
     *
     * Examples: "city: Berlin", "title: Duke"
     */
    val settingsDisplay: StateFlow<String?>

    /**
     * Although [settingsDisplay] is a Flow, it could be updated lazily, e.g., by pressing "apply" in a popup.
     */
    fun applySettings()

    /// ------

    /**
     * If empty, no settings
     */
    val settings: Map<SettingsId, StateFlow<SearchSetting>>

    suspend fun search(
        searchTerm: String,
        activeAccount: UserId,
        coroutineScope: CoroutineScope,
    ): ProviderSearchResult

    // internal state for filters (use in extensions of this interface and in the UI cast to the appropriate subtype)
}

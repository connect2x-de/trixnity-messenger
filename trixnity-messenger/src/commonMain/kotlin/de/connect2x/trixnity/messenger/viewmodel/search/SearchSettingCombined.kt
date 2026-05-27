package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchSetting
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SettingsId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Combines multiple [SearchSetting]s with the same [SettingsId] into one setting.
 *
 * Also holds the [value] of the setting and distributes it to every [SearchSetting].
 */
data class SearchSettingCombined(
    /** The id of the setting. */
    val id: SettingsId,
    /** The name of the setting for the UI. Should be i18nized. */
    val name: String,
    /** The display names of the providers that provide this setting. Are used to group the filter options. */
    val sourceDisplayNames: List<String>,
    /** When at least one [SearchUserProvider] that contributes to this setting is active, this returns `true`. */
    val enabled: StateFlow<Boolean>,
    internal val getDisplayValue: (String) -> String,
    internal val setValue: List<(String?) -> Unit>,
) {
    private val _value = MutableStateFlow<String?>(null)

    /** The current value of the setting. Can only be updated via [setValue]. */
    val value: StateFlow<String?> = _value.asStateFlow()

    /** Updates the [value] and distributes it to every [SearchSetting]. */
    fun setValue(newValue: String?) {
        setValue.forEach { function -> function(newValue) }
        _value.value = newValue
    }
}

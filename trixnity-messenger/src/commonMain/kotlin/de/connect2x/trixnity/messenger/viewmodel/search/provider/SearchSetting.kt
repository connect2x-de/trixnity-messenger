package de.connect2x.trixnity.messenger.viewmodel.search.provider

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

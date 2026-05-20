package de.connect2x.trixnity.messenger.viewmodel.search.provider

/**
 * Holds information on a special search setting, e.g., "city" -> "Berlin". The value itself is delegated to the
 * provider to perform the search.
 */
data class SearchSetting(
    /**
     * The name of the setting for the UI. Should be i18nized.
     */
    val name: String,
    /**
     * In case the value of the search setting has a different representation than the value.
     */
    val getDisplayValue: (String) -> String = { it },
    /**
     * The update method to transfer the current value to the [SearchUserProvider].
     */
    val setValue: (String?) -> Unit,
)

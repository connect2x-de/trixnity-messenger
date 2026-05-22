package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.search.SearchSettingCombined
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SettingsId
import kotlin.reflect.KClass

interface SearchSettingInputView<S : SettingsId> {
    val supports: KClass<out S>

    @Composable fun create(searchSettingCombined: SearchSettingCombined)
}

object EmptySearchSettingInputView : SearchSettingInputView<SettingsId> {
    override val supports: KClass<SettingsId>
        get() = SettingsId::class

    @Composable override fun create(searchSettingCombined: SearchSettingCombined) {}
}

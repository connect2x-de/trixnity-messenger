package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.search.SearchSettingCombined
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SettingsId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

interface SearchSettingInputSelector {
    @Composable
    fun create(searchSettingCombined: SearchSettingCombined) =
        rememberFactory(searchSettingCombined).create(searchSettingCombined)


    @Composable
    private fun rememberFactory(element: SearchSettingCombined): SearchSettingInputView<*> =
        remember(element) { selectFactory(element) }

    fun selectFactory(element: SearchSettingCombined): SearchSettingInputView<*>
}

@Composable
fun SearchSettingInputSelector(searchSettingCombined: SearchSettingCombined) {
    with(DI.get<SearchSettingInputSelector>()) { create(searchSettingCombined) }
}

class SearchSettingInputSelectorImpl(
    private val factories: List<SearchSettingInputView<*>>,
) : SearchSettingInputSelector {
    private val log =
        Logger("de.connect2x.trixnity.messenger.compose.view.roomlist.search.SearchSettingInputSelectorImpl")

    private val factoryMapping =
        MutableStateFlow<Map<KClass<out SettingsId>, SearchSettingInputView<*>>>(emptyMap())

    override fun selectFactory(element: SearchSettingCombined): SearchSettingInputView<*> {
        val target = element.id
        val searchSettingInputClass = target::class
        return factoryMapping.value[searchSettingInputClass]
            ?: run {
                val foundFactory =
                    factories.firstOrNull { it.supports.isInstance(target) }
                        ?: run {
                            log.warn {
                                "There are no registered view for ${target::class.simpleName}. " +
                                        "This can be a missing view in the DI."
                            }
                            EmptySearchSettingInputView
                        }
                factoryMapping.update { it + (searchSettingInputClass to foundFactory) }
                foundFactory
            }
    }
}



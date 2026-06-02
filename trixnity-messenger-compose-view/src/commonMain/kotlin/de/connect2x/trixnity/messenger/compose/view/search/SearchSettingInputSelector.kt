package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchFilterValue
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface SearchSettingInputSelector {
    @Composable
    fun create(userSearchViewModel: UserSearchViewModel, searchFilterValueKey: SearchFilterValue.Key<*>) =
        rememberFactory(searchFilterValueKey).create(userSearchViewModel, searchFilterValueKey)

    @Composable
    private fun rememberFactory(element: SearchFilterValue.Key<*>): SearchSettingInputView<*> =
        remember(element) { selectFactory(element) }

    fun selectFactory(element: SearchFilterValue.Key<*>): SearchSettingInputView<*>
}

@Composable
fun SearchSettingInputSelector(
    userSearchViewModel: UserSearchViewModel,
    searchFilterValueKey: SearchFilterValue.Key<*>,
) {
    with(DI.get<SearchSettingInputSelector>()) { create(userSearchViewModel, searchFilterValueKey) }
}

class SearchSettingInputSelectorImpl(private val factories: List<SearchSettingInputView<*>>) :
    SearchSettingInputSelector {
    private val log =
        Logger("de.connect2x.trixnity.messenger.compose.view.roomlist.search.SearchSettingInputSelectorImpl")

    private val factoryMapping =
        MutableStateFlow<Map<KClass<out SearchFilterValue.Key<*>>, SearchSettingInputView<*>>>(emptyMap())

    override fun selectFactory(element: SearchFilterValue.Key<*>): SearchSettingInputView<*> {
        val searchSettingInputClass = element::class
        return factoryMapping.value[searchSettingInputClass]
            ?: run {
                val foundFactory =
                    factories.firstOrNull { it.supports.isInstance(element) }
                        ?: run {
                            log.warn {
                                "There are no registered view for ${element::class.simpleName}. " +
                                    "This can be a missing view in the DI."
                            }
                            EmptySearchSettingInputView
                        }
                factoryMapping.update { it + (searchSettingInputClass to foundFactory) }
                foundFactory
            }
    }
}

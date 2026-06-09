package de.connect2x.trixnity.messenger.compose.view.search.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchFilter
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface SearchFilterInputSelector {
    @Composable
    fun create(userSearchViewModel: UserSearchViewModel, searchFilterKey: SearchFilter.Key<*>) =
        rememberFactory(searchFilterKey).create(userSearchViewModel, searchFilterKey)

    @Composable
    private fun rememberFactory(element: SearchFilter.Key<*>): SearchFilterInputView<*> =
        remember(element) { selectFactory(element) }

    fun selectFactory(element: SearchFilter.Key<*>): SearchFilterInputView<*>
}

@Composable
fun SearchFilterInputSelector(
    userSearchViewModel: UserSearchViewModel,
    searchFilterKey: SearchFilter.Key<*>,
) {
    with(DI.get<SearchFilterInputSelector>()) { create(userSearchViewModel, searchFilterKey) }
}

class SearchFilterInputSelectorImpl(private val factories: List<SearchFilterInputView<*>>) : SearchFilterInputSelector {

    private val log =
        Logger("de.connect2x.trixnity.messenger.compose.view.roomlist.search.SearchFilterInputSelectorImpl")

    private val factoryMapping =
        MutableStateFlow<Map<KClass<out SearchFilter.Key<*>>, SearchFilterInputView<*>>>(emptyMap())

    override fun selectFactory(element: SearchFilter.Key<*>): SearchFilterInputView<*> {
        return factoryMapping.value[element::class]
            ?: run {
                val foundFactory =
                    factories.firstOrNull { it.supports.isInstance(element) }
                        ?: run {
                            log.warn {
                                "There are no registered view for ${element::class.simpleName}. " +
                                    "This can be a missing view in the DI."
                            }
                            EmptySearchFilterInputView
                        }
                factoryMapping.update { it + (element::class to foundFactory) }
                foundFactory
            }
    }
}

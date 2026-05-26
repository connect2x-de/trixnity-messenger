package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface SearchUserProviderToggleViewSelector {
    @Composable
    fun create(
        searchUserProvider: SearchUserProvider,
        providerSearchCanBeActivated: Boolean,
        active: Boolean,
        setActive: () -> Unit,
    ) = rememberFactory(searchUserProvider).create(searchUserProvider, providerSearchCanBeActivated, active, setActive)

    @Composable
    private fun rememberFactory(element: SearchUserProvider): SearchUserProviderToggleView<*> =
        remember(element) { selectFactory(element) }

    fun selectFactory(element: SearchUserProvider): SearchUserProviderToggleView<*>
}

@Composable
fun SearchUserProviderToggleSelector(
    searchUserProvider: SearchUserProvider,
    providerSearchCanBeActivated: Boolean,
    active: Boolean,
    setActive: () -> Unit,
) {
    with(DI.get<SearchUserProviderToggleViewSelector>()) {
        create(searchUserProvider, providerSearchCanBeActivated, active, setActive)
    }
}

class SearchUserProviderToggleViewSelectorImpl(private val factories: List<SearchUserProviderToggleView<*>>) :
    SearchUserProviderToggleViewSelector {
    private val log =
        Logger("de.connect2x.trixnity.messenger.compose.view.roomlist.search.SearchUserProviderToggleViewSelectorImpl")

    private val factoryMapping =
        MutableStateFlow<Map<KClass<out SearchUserProvider>, SearchUserProviderToggleView<*>>>(emptyMap())

    override fun selectFactory(element: SearchUserProvider): SearchUserProviderToggleView<*> {
        val searchUserProviderClass = element::class
        return factoryMapping.value[searchUserProviderClass]
            ?: run {
                val foundFactory =
                    factories.firstOrNull { it.supports.isInstance(element) }
                        ?: run {
                            log.warn {
                                "There are no registered view for ${element::class.simpleName}. " +
                                    "This can be a missing view in the DI."
                            }
                            EmptySearchUserProviderToggleView
                        }
                factoryMapping.update { it + (searchUserProviderClass to foundFactory) }
                foundFactory
            }
    }
}

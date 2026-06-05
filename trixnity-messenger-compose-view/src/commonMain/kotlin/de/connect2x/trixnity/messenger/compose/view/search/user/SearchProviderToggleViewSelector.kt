package de.connect2x.trixnity.messenger.compose.view.search.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProvider
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface SearchProviderToggleViewSelector {
    @Composable
    fun create(
        searchProvider: SearchProvider<*>,
        providerSearchCanBeActivated: Boolean,
        active: Boolean,
        setActive: () -> Unit,
    ) = rememberFactory(searchProvider).create(searchProvider, providerSearchCanBeActivated, active, setActive)

    @Composable
    private fun rememberFactory(element: SearchProvider<*>): SearchProviderToggleView<*> =
        remember(element) { selectFactory(element) }

    fun selectFactory(element: SearchProvider<*>): SearchProviderToggleView<*>
}

@Composable
fun SearchProviderToggleSelector(
    searchProvider: SearchProvider<*>,
    providerSearchCanBeActivated: Boolean,
    active: Boolean,
    setActive: () -> Unit,
) {
    with(DI.get<SearchProviderToggleViewSelector>()) {
        create(searchProvider, providerSearchCanBeActivated, active, setActive)
    }
}

class SearchProviderToggleViewSelectorImpl(private val factories: List<SearchProviderToggleView<*>>) :
    SearchProviderToggleViewSelector {
    private val log =
        Logger("de.connect2x.trixnity.messenger.compose.view.roomlist.search.SearchProviderToggleViewSelectorImpl")

    private val factoryMapping =
        MutableStateFlow<Map<KClass<out SearchProvider<*>>, SearchProviderToggleView<*>>>(emptyMap())

    override fun selectFactory(element: SearchProvider<*>): SearchProviderToggleView<*> {
        return factoryMapping.value[element::class]
            ?: run {
                val foundFactory =
                    factories.firstOrNull { it.supports.isInstance(element) }
                        ?: run {
                            log.warn {
                                "There are no registered view for ${element::class.simpleName}. " +
                                    "This can be a missing view in the DI."
                            }
                            EmptySearchProviderToggleView
                        }
                factoryMapping.update { it + (element::class to foundFactory) }
                foundFactory
            }
    }
}

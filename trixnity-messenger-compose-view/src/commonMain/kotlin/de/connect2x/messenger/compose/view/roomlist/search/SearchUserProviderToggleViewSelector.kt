package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass


private val log = KotlinLogging.logger {}

interface SearchUserProviderToggleViewSelector {
    @Composable
    fun create(searchUserProvider: SearchUserProvider, active: Boolean, setActive: () -> Unit) =
        rememberFactory(searchUserProvider).create(searchUserProvider, active, setActive)


    @Composable
    private fun rememberFactory(element: SearchUserProvider): SearchUserProviderToggleView<*> =
        remember(element) { selectFactory(element) }

    fun selectFactory(element: SearchUserProvider): SearchUserProviderToggleView<*>
}

@Composable
fun SearchUserProviderToggleSelector(
    searchUserProvider: SearchUserProvider,
    active: Boolean,
    setActive: () -> Unit,
) {
    with(DI.get<SearchUserProviderToggleViewSelector>()) { create(searchUserProvider, active, setActive) }
}

class SearchUserProviderToggleViewSelectorImpl(
    private val factories: List<SearchUserProviderToggleView<*>>,
) : SearchUserProviderToggleViewSelector {
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

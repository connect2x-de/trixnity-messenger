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

interface SearchUserProviderSettingsViewSelector {
    @Composable
    fun create(searchUserProvider: SearchUserProvider) =
        rememberFactory(searchUserProvider).create(searchUserProvider)


    @Composable
    private fun rememberFactory(element: SearchUserProvider): SearchUserProviderSettingsView<*> =
        remember(element) { selectFactory(element) }

    fun selectFactory(element: SearchUserProvider): SearchUserProviderSettingsView<*>
}

@Composable
fun SearchUserProviderSettingsSelector(
    searchUserProvider: SearchUserProvider,
) {
    with(DI.get<SearchUserProviderSettingsViewSelector>()) { create(searchUserProvider) }
}

class SearchUserProviderSettingsViewSelectorImpl(
    private val factories: List<SearchUserProviderSettingsView<*>>,
) : SearchUserProviderSettingsViewSelector {
    private val factoryMapping =
        MutableStateFlow<Map<KClass<out SearchUserProvider>, SearchUserProviderSettingsView<*>>>(emptyMap())

    override fun selectFactory(element: SearchUserProvider): SearchUserProviderSettingsView<*> {
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
                            EmptySearchUserProviderSettingsView
                        }
                factoryMapping.update { it + (searchUserProviderClass to foundFactory) }
                foundFactory
            }
    }
}

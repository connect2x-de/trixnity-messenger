package de.connect2x.trixnity.messenger.compose.view.roomlist.search

import androidx.compose.runtime.Composable
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Composable
fun SearchResultSelector(userSearchResult: UserSearchResult, showOrigin: Boolean, onClick: (UserSearchResult) -> Unit) {
    with(DI.get<SearchResultViewSelector>()) { create(userSearchResult, showOrigin, onClick) }
}

interface SearchResultViewSelector : SearchResultViewFactorySelector<SearchResultView<UserSearchResult>> {
    @Composable
    fun create(userSearchResult: UserSearchResult, showOrigin: Boolean, onClick: (UserSearchResult) -> Unit) =
        rememberFactory(userSearchResult).create(userSearchResult, showOrigin, onClick)
}

class SearchResultViewSelectorImpl(private val factories: List<SearchResultView<*>>) : SearchResultViewSelector {
    private val log =
        Logger("de.connect2x.trixnity.messenger.compose.view.roomlist.search.SearchResultViewSelectorImpl")

    private val factoryMapping =
        MutableStateFlow<Map<KClass<out UserSearchResult>, SearchResultView<UserSearchResult>>>(emptyMap())

    override fun selectFactory(element: UserSearchResult): SearchResultView<UserSearchResult> {
        val userSearchResultClass = element::class
        return factoryMapping.value[userSearchResultClass]
            ?: run {
                @Suppress("UNCHECKED_CAST")
                val foundFactory =
                    factories.firstOrNull { it.supports.isInstance(element) } as SearchResultView<UserSearchResult>?
                        ?: run {
                            log.warn {
                                "There are no registered views for ${element::class.simpleName}. " +
                                    "This can be a missing view in the DI or might be an element that should not be " +
                                    "visible in the timeline."
                            }
                            EmptySearchResultView
                        }
                factoryMapping.update { it + (userSearchResultClass to foundFactory) }
                foundFactory
            }
    }
}

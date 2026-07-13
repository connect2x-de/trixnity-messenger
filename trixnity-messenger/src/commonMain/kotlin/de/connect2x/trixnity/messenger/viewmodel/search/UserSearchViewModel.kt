package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.search.GroupedSearchFilter
import de.connect2x.trixnity.messenger.search.provider.SearchFilter
import de.connect2x.trixnity.messenger.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.search.user.UserSearchContext
import de.connect2x.trixnity.messenger.search.user.UserSearchResult
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import kotlinx.coroutines.flow.MutableStateFlow

interface UserSearchViewModelFactory {
    fun create(
        matrixClientViewModelContext: MatrixClientViewModelContext,
        searchViewModel: SearchViewModel<UserSearchResult, UserSearchContext>,
    ): UserSearchViewModel {
        return UserSearchViewModelImpl(matrixClientViewModelContext, searchViewModel)
    }

    companion object : UserSearchViewModelFactory
}

interface UserSearchViewModel : SearchViewModel<UserSearchResult, UserSearchContext>

class UserSearchViewModelImpl(
    matrixClientViewModelContext: MatrixClientViewModelContext,
    private val searchViewModel: SearchViewModel<UserSearchResult, UserSearchContext>,
) :
    UserSearchViewModel,
    SearchViewModel<UserSearchResult, UserSearchContext> by searchViewModel,
    MatrixClientViewModelContext by matrixClientViewModelContext

class PreviewUserSearchViewModel : UserSearchViewModel {
    override val searchTerm: TextFieldViewModel = TextFieldViewModelImpl(255)
    override val searchProviders: List<SearchProvider<UserSearchResult, UserSearchContext>> = emptyList()
    override val availableFilters: MutableStateFlow<List<GroupedSearchFilter>> = MutableStateFlow(emptyList())
    override val searchResultList: MutableStateFlow<List<UserSearchResult>> = MutableStateFlow(emptyList())
    override val searchProviderEnabled: MutableStateFlow<Map<SearchProvider.Key<*>, Boolean>> =
        MutableStateFlow(emptyMap())
    override val isSearching: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val noResultsFound: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    override val searchProviderCanBeEnabled: MutableStateFlow<Map<SearchProvider.Key<*>, Boolean>> =
        MutableStateFlow(emptyMap())
    override val errors: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())
    override val searchFilters: MutableStateFlow<List<SearchFilter>> = MutableStateFlow(emptyList())

    override fun setProvider(providerId: SearchProvider.Key<*>, enabled: Boolean) {}

    override fun filterNotSearchResult(searchResult: UserSearchResult) {}

    override fun unfilterNotSearchResult(searchResult: UserSearchResult) {}

    override fun setSearchFilter(searchFilter: SearchFilter) {}

    override fun removeSearchFilter(key: SearchFilter.Key<*>) {}
}

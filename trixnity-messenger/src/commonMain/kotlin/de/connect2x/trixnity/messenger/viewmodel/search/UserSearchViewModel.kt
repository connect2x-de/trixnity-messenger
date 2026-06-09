package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchFilter
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProviderResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProviderSorter
import de.connect2x.trixnity.messenger.viewmodel.search.provider.UserSearchProviderResult
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get

interface UserSearchViewModelFactory {
    fun create(matrixClientViewModelContext: MatrixClientViewModelContext): UserSearchViewModel {
        return UserSearchViewModelImpl(matrixClientViewModelContext)
    }

    companion object : UserSearchViewModelFactory
}

/**
 * Searches for users with a [searchTerm] and current values of the [availableFilters] in different [searchProviders]
 * and provides a combined [searchResultList].
 */
interface UserSearchViewModel {
    /**
     * Global search term for every [SearchProvider]. When changed, all providers use this term to initiate a search and
     * update their respective results.
     */
    val searchTerm: TextFieldViewModel

    /** A list of all [SearchProvider]s. Obtained from the DI. */
    val searchProviders: List<SearchProvider<*>>

    /**
     * Accumulated (and possibly merged by the same [SearchFilter.Key]) list of all [SearchFilter]s supported by all
     * [SearchProvider]s.
     */
    val availableFilters: StateFlow<List<GroupedSearchFilter>>

    /**
     * The current list of set [SearchFilter]s. UI elements for filters can query this list and filter for a potentially
     * set value (`searchFilters.filterIsInstance<MySearchFilter>() ?: MySearchFilter("")`).
     *
     * In order to manipulate the values, use [setSearchFilter].
     */
    val searchFilters: StateFlow<List<SearchFilter>>

    /**
     * A combined list of search results from different search providers. The list is sorted by relevance by the used
     * [SearchProvider]s, but interlaced from different providers. Is updated as soon as any [SearchProvider] has a
     * result list. If you do not want your presentation to jump on search results popping up, try to `debounce` this
     * list.
     */
    val searchResultList: StateFlow<List<UserSearchResult>>

    /** Indicates whether a search is currently running for any [SearchProvider]. */
    val isSearching: StateFlow<Boolean>

    /**
     * Indicates whether the search was not successful for _all_ [SearchProvider]s. If there is at least one
     * [SearchProvider] still searching, this is undetermined (`null`).
     */
    val noResultsFound: StateFlow<Boolean?>

    /** Indicates whether the search provider is enabled at the moment. Can be set disabled via [setProvider]. */
    val searchProviderEnabled: StateFlow<List<Boolean>>

    /** Indicates whether a provider can be activated. */
    val searchProviderCanBeEnabled: StateFlow<List<Boolean>>

    /** (Dis-)able a [SearchProvider] by its [SearchProvider.Key]. */
    fun setProvider(providerId: SearchProvider.Key<*>, enabled: Boolean)

    fun filterUserSearchResult(userSearchResult: UserSearchResult)

    fun filterNotUserSearchResult(userSearchResult: UserSearchResult)

    /**
     * Manipulate the filters of all [SearchProvider]s. If [SearchFilter.isActive], the filter is removed from the list.
     */
    fun setSearchFilter(searchFilter: SearchFilter)

    /**
     * [SearchProvider]s that support [SearchFilter]s have to remove a set value by its [SearchFilter.Key] if the value
     * is empty (i.e., an empty String or similar).
     */
    fun removeFilterValue(key: SearchFilter.Key<*>)
}

class UserSearchViewModelImpl(
    matrixClientViewModelContext: MatrixClientViewModelContext,
    private val debounceDuration: Duration = 300.milliseconds,
) : UserSearchViewModel, MatrixClientViewModelContext by matrixClientViewModelContext {
    override val searchProviders: List<SearchProvider<*>> =
        get<SearchProviderSorter>().sort(getKoin().getAll<SearchProvider<*>>())
    override val searchFilters: MutableStateFlow<List<SearchFilter>> = MutableStateFlow(emptyList())
    override val searchTerm = TextFieldViewModelImpl(maxLength = 1_000)

    private val _searchProviderEnabled = MutableStateFlow(searchProviders.map { it.disabledByDefault.not() })
    override val searchProviderCanBeEnabled: StateFlow<List<Boolean>> =
        searchFilters
            .map { searchFilters ->
                if (searchFilters.isEmpty()) {
                    searchProviders.map { true }
                } else {
                    searchFilters
                        .fold(searchProviders.map { false }) { acc, searchFilter ->
                            val inActive = searchProviders.map { searchProvider ->
                                searchProvider.supportedFilters.none { it == searchFilter.key }
                            }
                            acc.zip(inActive).map { (v1, v2) -> v1 || v2 }
                        }
                        .map { it.not() }
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), searchProviders.map { true })

    override val searchProviderEnabled =
        combine(_searchProviderEnabled, searchProviderCanBeEnabled) { enabledList, canBeEnabledList ->
                enabledList.zip(canBeEnabledList).map { (enabled, canBeEnabled) -> enabled && canBeEnabled }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), _searchProviderEnabled.value)

    override val availableFilters: StateFlow<List<GroupedSearchFilter>> =
        searchProviderEnabled
            .map { enabled ->
                searchProviders
                    .flatMapIndexed { index, searchProvider ->
                        searchProvider.supportedFilters.map { Triple(it, searchProvider, enabled[index]) }
                    }
                    .fold(emptyList<GroupedSearchFilter>()) { acc, (key, provider, enabled) ->
                        val existingKey = acc.find { it.searchFilterKeys.contains(key) }
                        if (existingKey != null) { // the search filter is already present
                            val withoutCurrentKey =
                                existingKey.copy(searchFilterKeys = existingKey.searchFilterKeys.filter { it != key })
                            val alreadyCombined = acc.find { it.sources == existingKey.sources + provider }
                            // we already combined with another provider -> we need to
                            if (alreadyCombined != null) {
                                acc - existingKey + withoutCurrentKey - alreadyCombined +
                                    alreadyCombined.copy(
                                        sources = alreadyCombined.sources,
                                        searchFilterKeys = alreadyCombined.searchFilterKeys + key,
                                        isEnabled = alreadyCombined.isEnabled || enabled,
                                    )
                            } else { // no need to split, just add to the list of sources
                                acc - existingKey +
                                    withoutCurrentKey +
                                    GroupedSearchFilter(
                                        sources = existingKey.sources + provider,
                                        searchFilterKeys = listOf(key),
                                        isEnabled = existingKey.isEnabled || enabled,
                                    )
                            }
                        } else {
                            val existing = acc.find { it.sources.size == 1 && it.sources.first() == provider }
                            if (existing != null) { // the search provider already has registered a filter
                                acc - existing + existing.copy(searchFilterKeys = existing.searchFilterKeys + key)
                            } else {
                                // completely new filter
                                acc +
                                    GroupedSearchFilter(
                                        sources = listOf(provider),
                                        searchFilterKeys = listOf(key),
                                        isEnabled = enabled,
                                    )
                            }
                        }
                    }
                    .sortedByDescending { it.sources.size }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    private val triggerSearch = MutableStateFlow<Unit?>(null)
    private val searchProvidersResult = searchProviders.map { MutableStateFlow<SearchProviderResult?>(null) }
    private val searchProvidersLoading = searchProviders.map { MutableStateFlow(false) }
    private val filteredUserSearchResults = MutableStateFlow<List<UserSearchResult>>(emptyList())

    override fun filterUserSearchResult(userSearchResult: UserSearchResult) {
        filteredUserSearchResults.value += userSearchResult
    }

    override fun filterNotUserSearchResult(userSearchResult: UserSearchResult) {
        filteredUserSearchResults.value -= userSearchResult
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val searchResult: StateFlow<List<SearchResult>?> =
        combine(
                combine(searchProvidersResult) { it },
                searchProviderEnabled,
                combine(searchProvidersLoading) { it },
                filteredUserSearchResults,
            ) { results, enabled, loading, filteredResults ->
                log.debug {
                    "searchResult=${results.joinToString { it?.toString() ?: "<none>" }}, enabled=$enabled, loading=${loading.contentToString()}, filteredResults=${filteredResults.joinToString { it.userId.full }}"
                }
                results.mapIndexed { index, result ->
                    SearchResult(
                        id = searchProviders[index].key,
                        enabled = enabled[index],
                        providerDisplayName = searchProviders[index].displayName,
                        providerSearchResult = result,
                        isSearching = loading[index],
                    )
                }
            }
            .mapLatest { it }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val searchResultList: StateFlow<List<UserSearchResult>> =
        combine(searchResult, filteredUserSearchResults) { results, filteredUserSearchResults ->
                if (results != null) {
                    randomSequence(results, filteredUserSearchResults)
                } else emptyList()
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun setProvider(providerId: SearchProvider.Key<*>, enabled: Boolean) {
        val index = searchProviders.indexOfFirst { it.key == providerId }
        val rest =
            if (index == searchProviderEnabled.value.size - 1) emptyList()
            else searchProviderEnabled.value.subList(index + 1, searchProviderEnabled.value.size)
        _searchProviderEnabled.value = _searchProviderEnabled.value.subList(0, index) + enabled + rest

        if (enabled) { // means: re-activating a search provider -> we need to initiate a new search
            emitTriggerSearch()
        }
    }

    private fun emitTriggerSearch() {
        triggerSearch.value =
            when (triggerSearch.value) {
                null -> Unit
                else -> null
            }
    }

    override val isSearching: StateFlow<Boolean> =
        combine(searchProvidersLoading) { loading -> loading.any { it } }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val noResultsFound: StateFlow<Boolean?> =
        combine(searchProvidersLoading) { loading ->
                if (loading.all { it.not() }) {
                    combine(searchProvidersResult) { providerSearchResults ->
                        providerSearchResults
                            .map {
                                when (it) {
                                    is UserSearchProviderResult.Success -> it.result.isEmpty()
                                    else -> null
                                }
                            }
                            .fold(null as Boolean?) { acc, ele -> if (acc == null) ele else acc && (ele ?: true) }
                    }
                } else {
                    flowOf(null)
                }
            }
            .flatMapLatest { it }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override fun setSearchFilter(searchFilter: SearchFilter) {
        if (searchFilter.isActive) {
            removeFilterValue(searchFilter.key)
        } else {
            val existing = searchFilters.value.find { it.key == searchFilter.key }
            if (existing != null) {
                searchFilters.value = searchFilters.value - existing + searchFilter
            } else {
                searchFilters.value += searchFilter
            }
        }
    }

    override fun removeFilterValue(key: SearchFilter.Key<*>) {
        searchFilters.value = searchFilters.value.filter { it.key != key }
    }

    init {
        log.debug { "searchUserProviders: $searchProviders" }
        coroutineScope.launch { search() }
    }

    @OptIn(FlowPreview::class)
    private suspend fun search() {
        combine(
                triggerSearch,
                searchFilters
                    .onEach { log.trace { "filtering for value **** (redacted for privacy)" } }
                    .debounce(debounceDuration),
                searchTerm
                    .onEach { log.trace { "Searching for user **** (redacted for privacy)" } }
                    .map { it.text }
                    .distinctUntilChanged()
                    .debounce(debounceDuration)
                    .map {
                        if (UserId.isValid(it.lowercase())) {
                            log.debug { "found matrix id" }
                            it.lowercase()
                        } else it
                    },
            ) { _, filterValues, searchTerm ->
                filterValues to searchTerm // we just need to react to provider enabled and providerSettings changes
            }
            .scopedCollectLatest { (filterValues, searchTerm) ->
                log.trace { "search for users in search providers" }
                searchProviders.mapIndexed { index, searchUserProvider ->
                    log.debug { " - in search provider ${searchUserProvider.displayName} (${searchUserProvider.key})" }
                    if (searchTerm.isNotBlank() || filterValues.isNotEmpty()) {
                        searchProvidersResult[index].value = null // reset old search results

                        if (searchProviderEnabled.value[index]) {
                            searchProvidersLoading[index].value = true
                            launch {
                                searchProvidersResult[index].value =
                                    searchUserProvider.search(searchTerm, filterValues, matrixClient.userId, this)
                                log.trace { " searchProvider ${searchUserProvider.key} finished search" }
                                searchProvidersLoading[index].value = false
                            }
                        } else {
                            log.debug { "searchProvider ${searchUserProvider.key} is not enabled -> no search" }
                        }
                    } else {
                        log.trace { "user search blank -> empty list" }
                        searchProvidersResult[index].value = null
                        searchProvidersLoading[index].value = false
                    }
                }
            }
    }

    private fun randomSequence(
        results: List<SearchResult>,
        filteredUserSearchResults: List<UserSearchResult>,
    ): List<UserSearchResult> {
        // without query
        val random = Random(results.hashCode())

        /**
         * Returns a list of values built from elements of all lists with same indexes using provided [transform].
         * Output has length of longest input list.
         */
        fun <T, V> zip(vararg lists: List<T>, transform: (List<T?>) -> V): List<V> {
            val maxSize = lists.maxOfOrNull(List<T>::size) ?: return emptyList()
            val list = ArrayList<V>(maxSize)

            val iterators = lists.map { it.iterator() }
            var i = 0
            while (i < maxSize) {
                list.add(transform(iterators.map { if (it.hasNext()) it.next() else null }))
                i++
            }

            return list
        }

        /**
         * Returns a list of lists, each built from elements of all lists with the same indexes. Output has length of
         * longest input list.
         */
        fun <T> zip(vararg lists: List<T>): List<List<T>> {
            return zip(*lists, transform = { a -> a.mapNotNull { it } })
        }

        fun <T> List<T>.splitIntoRandomChunks() =
            sequence {
                    var index = 0
                    while (index < size) {
                        val chunkSize = random.nextInt(3, 10)
                        val endIndex = minOf(index + chunkSize, size)

                        if (chunkSize > 0) {
                            yield(subList(index, endIndex))
                        }

                        index = endIndex
                    }
                }
                .toList()

        val userSearchResults = results.map { searchResult ->
            if (searchResult.enabled) {
                when (val providerSearchResult = searchResult.providerSearchResult) {
                    is UserSearchProviderResult.Success -> {
                        providerSearchResult.result
                            .filterNot { filteredUserSearchResults.contains(it) }
                            .splitIntoRandomChunks()
                    }

                    else -> emptyList()
                }
            } else emptyList()
        }

        return zip(*userSearchResults.toTypedArray()).flatten().flatten()
    }
}

// used in this class and in the tests where we do not want randomness of results
internal data class SearchResult(
    val id: SearchProvider.Key<*>,
    val enabled: Boolean,
    val providerDisplayName: String,
    val providerSearchResult: SearchProviderResult?,
    val isSearching: Boolean,
)

class PreviewUserSearchViewModel : UserSearchViewModel {
    override val searchTerm: TextFieldViewModel = TextFieldViewModelImpl(255)
    override val searchProviders: List<SearchProvider<*>> = emptyList()
    override val availableFilters: StateFlow<List<GroupedSearchFilter>> = MutableStateFlow(emptyList())
    override val searchResultList: MutableStateFlow<List<UserSearchResult>> = MutableStateFlow(emptyList())
    override val searchProviderEnabled: MutableStateFlow<List<Boolean>> = MutableStateFlow(emptyList())
    override val isSearching: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val noResultsFound: StateFlow<Boolean?> = MutableStateFlow(null)
    override val searchProviderCanBeEnabled: StateFlow<List<Boolean>> = MutableStateFlow(emptyList())
    override val searchFilters: StateFlow<List<SearchFilter>> = MutableStateFlow(emptyList())

    override fun setProvider(providerId: SearchProvider.Key<*>, enabled: Boolean) {}

    override fun filterUserSearchResult(userSearchResult: UserSearchResult) {}

    override fun filterNotUserSearchResult(userSearchResult: UserSearchResult) {}

    override fun setSearchFilter(searchFilter: SearchFilter) {}

    override fun removeFilterValue(key: SearchFilter.Key<*>) {}
}

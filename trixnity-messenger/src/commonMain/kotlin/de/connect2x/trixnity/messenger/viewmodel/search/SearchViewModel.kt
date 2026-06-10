package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.search.GroupedSearchFilter
import de.connect2x.trixnity.messenger.search.SearchResult
import de.connect2x.trixnity.messenger.search.getGroupedSearchFilters
import de.connect2x.trixnity.messenger.search.provider.SearchContext
import de.connect2x.trixnity.messenger.search.provider.SearchFilter
import de.connect2x.trixnity.messenger.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.search.provider.SearchProviderResult
import de.connect2x.trixnity.messenger.search.provider.SearchProviderSorter
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
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

interface SearchViewModelFactory {
    fun <SR : SearchResult, SC : SearchContext> create(
        matrixClientViewModelContext: MatrixClientViewModelContext,
        getSearchContext: () -> SC,
    ): SearchViewModel<SR, SC> {
        return SearchViewModelImpl(matrixClientViewModelContext, getSearchContext)
    }

    companion object : SearchViewModelFactory
}

/**
 * Searches for users with a [searchTerm] and current values of the [availableFilters] in different [searchProviders]
 * and provides a combined [searchResultList].
 */
interface SearchViewModel<SR : SearchResult, SC : SearchContext> {
    /**
     * Global search term for every [SearchProvider]. When changed, all providers use this term to initiate a search and
     * update their respective results.
     */
    val searchTerm: TextFieldViewModel

    /** A list of all [SearchProvider]s. Obtained from the DI. */
    val searchProviders: List<SearchProvider<*, *>>

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
    val searchResultList: StateFlow<List<SR>>

    /** Indicates whether a search is currently running for any [SearchProvider]. */
    val isSearching: StateFlow<Boolean>

    /**
     * Indicates whether the search was not successful for _all_ [SearchProvider]s. If there is at least one
     * [SearchProvider] still searching, this is undetermined (`null`).
     */
    val noResultsFound: StateFlow<Boolean?>

    /** Indicates whether the search provider is enabled at the moment. Can be set disabled via [setProvider]. */
    val searchProviderEnabled: StateFlow<Map<SearchProvider.Key<*>, Boolean>>

    /** Indicates whether a provider can be activated. */
    val searchProviderCanBeEnabled: StateFlow<Map<SearchProvider.Key<*>, Boolean>>

    /** (Dis-)able a [SearchProvider] by its [SearchProvider.Key]. */
    fun setProvider(providerId: SearchProvider.Key<*>, enabled: Boolean)

    /**
     * Manipulate the filters of all [SearchProvider]s. If [SearchFilter.isEnabled], the filter is removed from the
     * list.
     */
    fun setSearchFilter(searchFilter: SearchFilter)

    /**
     * [SearchProvider]s that support [SearchFilter]s have to remove a set value by its [SearchFilter.Key] if the value
     * is empty (i.e., an empty String or similar).
     */
    fun removeSearchFilter(key: SearchFilter.Key<*>)

    fun filterNotSearchResult(searchResult: SR)

    fun unfilterNotSearchResult(searchResult: SR)
}

// used in this class and in the tests where we do not want randomness of results
internal data class InternalSearchResult<SR : SearchResult>(
    val id: SearchProvider.Key<*>,
    val enabled: Boolean,
    val providerDisplayName: String,
    val providerSearchResult: SearchProviderResult<SR>?,
    val isSearching: Boolean,
)

class SearchViewModelImpl<SR : SearchResult, SC : SearchContext>(
    matrixClientViewModelContext: MatrixClientViewModelContext,
    private val getSearchContext: () -> SC,
    private val debounceDuration: Duration = 300.milliseconds,
) : SearchViewModel<SR, SC>, MatrixClientViewModelContext by matrixClientViewModelContext {
    override val searchProviders: List<SearchProvider<SR, SC>> =
        get<SearchProviderSorter>().sort(getKoin().getAll<SearchProvider<SR, SC>>())
    override val searchFilters: MutableStateFlow<List<SearchFilter>> = MutableStateFlow(emptyList())
    override val searchTerm = TextFieldViewModelImpl(maxLength = 1_000)

    private val _searchProviderEnabled =
        MutableStateFlow(searchProviders.associate { it.key to it.disabledByDefault.not() })
    override val searchProviderCanBeEnabled: StateFlow<Map<SearchProvider.Key<*>, Boolean>> =
        searchFilters
            .map { searchFilters ->
                if (searchFilters.isEmpty()) {
                    searchProviders.associate { it.key to true }
                } else {
                    searchFilters
                        .fold(searchProviders.associate { it.key to false }) { acc, searchFilter ->
                            val disabled = searchProviders.associate { searchProvider ->
                                searchProvider.key to searchProvider.supportedFilters.none { it == searchFilter.key }
                            }
                            acc.mapValues { (key, value) -> value || disabled[key] == true }
                        }
                        .mapValues { (_, value) -> value.not() }
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), searchProviders.associate { it.key to true })

    override val searchProviderEnabled =
        combine(_searchProviderEnabled, searchProviderCanBeEnabled) { enabledMap, canBeEnabledMap ->
                enabledMap.mapValues { (key, value) -> value && canBeEnabledMap[key] == true }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), _searchProviderEnabled.value)

    override val availableFilters: StateFlow<List<GroupedSearchFilter>> =
        searchProviderEnabled
            .map { enabled -> searchProviders.getGroupedSearchFilters(enabled) }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    private val triggerSearch = MutableStateFlow<Unit?>(null)
    private val searchProvidersResult = searchProviders.map { MutableStateFlow<SearchProviderResult<SR>?>(null) }
    private val searchProvidersLoading = searchProviders.map { MutableStateFlow(false) }

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val searchResult: StateFlow<List<InternalSearchResult<SR>>?> =
        combine(combine(searchProvidersResult) { it }, searchProviderEnabled, combine(searchProvidersLoading) { it }) {
                results,
                enabled,
                loading ->
                log.debug {
                    "searchResult=${results.joinToString { it?.toString() ?: "<none>" }}, enabled=$enabled, loading=${loading.contentToString()}}"
                }
                results.mapIndexed { index, result ->
                    InternalSearchResult(
                        id = searchProviders[index].key,
                        enabled = enabled[searchProviders[index].key] == true,
                        providerDisplayName = searchProviders[index].displayName,
                        providerSearchResult = result,
                        isSearching = loading[index],
                    )
                }
            }
            .mapLatest { it }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val filteredSearchResults = MutableStateFlow<List<SR>>(emptyList())

    override fun filterNotSearchResult(searchResult: SR) {
        filteredSearchResults.value += searchResult
    }

    override fun unfilterNotSearchResult(searchResult: SR) {
        filteredSearchResults.value -= searchResult
    }

    override val searchResultList: StateFlow<List<SR>> =
        combine(searchResult, filteredSearchResults) { results, filteredUserSearchResults ->
                if (results != null) {
                    randomSequence(results, filteredUserSearchResults)
                } else emptyList()
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun setProvider(providerId: SearchProvider.Key<*>, enabled: Boolean) {
        _searchProviderEnabled.value = _searchProviderEnabled.value - providerId + (providerId to enabled)

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
                                    is SearchProviderResult.Success<*> -> it.result.isEmpty()
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
        if (searchFilter.isEnabled) {
            removeSearchFilter(searchFilter.key)
        } else {
            val existing = searchFilters.value.find { it.key == searchFilter.key }
            if (existing != null) {
                searchFilters.value = searchFilters.value - existing + searchFilter
            } else {
                searchFilters.value += searchFilter
            }
        }
    }

    override fun removeSearchFilter(key: SearchFilter.Key<*>) {
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
                searchFilters
                    .onEach { log.trace { "filtering for value **** (redacted for privacy)" } }
                    .debounce(debounceDuration),
            ) { _, searchTerm, filters ->
                searchTerm to filters // we just need to react to provider enabled and providerSettings changes
            }
            .scopedCollectLatest { (searchTerm, filters) ->
                log.trace { "search for users in search providers" }
                searchProviders.mapIndexed { index, searchProvider ->
                    log.debug { " - in search provider ${searchProvider.displayName} (${searchProvider.key})" }
                    if (searchTerm.isNotBlank() || filters.isNotEmpty()) {
                        searchProvidersResult[index].value = null // reset old search results

                        if (searchProviderEnabled.value[searchProvider.key] == true) {
                            searchProvidersLoading[index].value = true
                            launch {
                                searchProvidersResult[index].value =
                                    searchProvider.search(searchTerm, filters, getSearchContext(), this)
                                log.trace { " searchProvider ${searchProvider.key} finished search" }
                                searchProvidersLoading[index].value = false
                            }
                        } else {
                            log.debug { "searchProvider ${searchProvider.key} is not enabled -> no search" }
                        }
                    } else {
                        log.trace { "user search blank -> empty list" }
                        searchProvidersResult[index].value = null
                        searchProvidersLoading[index].value = false
                    }
                }
            }
    }

    internal fun randomSequence(results: List<InternalSearchResult<SR>>, filteredSearchResults: List<SR>): List<SR> {
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

        val searchResults = results.map { searchResult ->
            if (searchResult.enabled) {
                when (val providerSearchResult = searchResult.providerSearchResult) {
                    is SearchProviderResult.Success<SR> -> {
                        providerSearchResult.result
                            .filterNot { filteredSearchResults.contains(it) }
                            .splitIntoRandomChunks()
                    }

                    else -> emptyList()
                }
            } else emptyList()
        }

        return zip(*searchResults.toTypedArray()).flatten().flatten()
    }
}

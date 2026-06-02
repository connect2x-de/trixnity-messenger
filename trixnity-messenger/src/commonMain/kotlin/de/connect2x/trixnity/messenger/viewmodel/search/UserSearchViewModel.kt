package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchFilterValue
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProviderId
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProviderResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.UserSearchProviderResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.UserSearchProviderSorter
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/** Searches for users in different [SearchProvider]s and provides a combined search result list. */
interface UserSearchViewModel {
    /**
     * Global search term for every [SearchProvider]. When changed, all providers use this term to initiate a search and
     * update their respective results.
     */
    val searchTerm: TextFieldViewModel

    /** A list of all [SearchProvider]s. Obtained from the DI. */
    val searchProviders: List<SearchProvider<*>>

    /**
     * Accumulated (and possibly merged by the same [SearchFilterValue.Key]) list of all [SearchFilterValue]s supported
     * by all [SearchProvider]s.
     */
    val providedFilters: StateFlow<List<SearchFilter>>

    /** Accumulation of all settings the search providers have, e.g., "Berlin, Germany". */
    val providerSettingsList: StateFlow<List<String>>

    /**
     * The current list of set [SearchFilterValue]s. UI elements for filters can query this list and filter for a
     * potentially set value (`searchFilterValues.filterIsInstance<MySearchFilterValue>() ?: MySearchFilterValue("")`).
     *
     * In order to manipulate the values, use [setSearchFilterValue].
     */
    val searchFilterValues: StateFlow<List<SearchFilterValue>>

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
    val providerSearchEnabled: StateFlow<List<Boolean>>

    /** Indicates whether a provider can be activated. */
    val providerSearchCanBeEnabled: StateFlow<List<Boolean>>

    /** (Dis-)able a [SearchProvider] by its [SearchProviderId]. */
    fun setProvider(providerId: SearchProviderId, enabled: Boolean)

    fun filterUserSearchResult(userSearchResult: UserSearchResult)

    fun unfilterUserSearchResult(userSearchResult: UserSearchResult)

    /**
     * Manipulate the filters of all [SearchProvider]s. If [SearchFilterValue.isEmpty], the filter is removed from the
     * list.
     */
    fun setSearchFilterValue(searchFilterValue: SearchFilterValue)
}

class UserSearchViewModelImpl(
    matrixClientViewModelContext: MatrixClientViewModelContext,
    private val debounceDuration: Duration = 300.milliseconds,
) : UserSearchViewModel, MatrixClientViewModelContext by matrixClientViewModelContext {
    override val searchProviders: List<SearchProvider<*>> =
        get<UserSearchProviderSorter>().sort(getKoin().getAll<SearchProvider<*>>())
    private val providerSearchResult = searchProviders.map { MutableStateFlow<SearchProviderResult?>(null) }
    private val providerSearchLoading = searchProviders.map { MutableStateFlow(false) }

    private val _providerSearchCanBeEnabled = MutableStateFlow(searchProviders.map { true })
    private val _providerSearchEnabled = MutableStateFlow(searchProviders.map { it.disabledByDefault.not() })
    override val providerSearchEnabled =
        combine(_providerSearchEnabled, _providerSearchCanBeEnabled) { enabledList, canBeEnabledList ->
                enabledList.zip(canBeEnabledList).map { (enabled, canBeEnabled) -> enabled && canBeEnabled }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), _providerSearchEnabled.value)
    override val providerSearchCanBeEnabled: StateFlow<List<Boolean>> = _providerSearchCanBeEnabled.asStateFlow()
    private val triggerSearch = MutableStateFlow<Unit?>(null)

    override val searchTerm = TextFieldViewModelImpl(maxLength = 1_000)

    private val filteredUserSearchResults = MutableStateFlow<List<UserSearchResult>>(emptyList())

    override val searchFilterValues: MutableStateFlow<List<SearchFilterValue>> = MutableStateFlow(emptyList())

    override fun filterUserSearchResult(userSearchResult: UserSearchResult) {
        filteredUserSearchResults.value += userSearchResult
    }

    override fun unfilterUserSearchResult(userSearchResult: UserSearchResult) {
        filteredUserSearchResults.value -= userSearchResult
    }

    override val providedFilters: StateFlow<List<SearchFilter>> =
        providerSearchEnabled
            .map { enabled ->
                searchProviders
                    .flatMapIndexed { index, searchProvider ->
                        searchProvider.supportedFilters.map { Triple(it, searchProvider, enabled[index]) }
                    }
                    .fold(emptyList<SearchFilter>()) { acc, (key, provider, enabled) ->
                        val existing = acc.find { it.searchFilterValueKeys.contains(key) }
                        if (existing != null) { // maybe the search filter is already present
                            val withoutCurrentKey =
                                existing.copy(
                                    searchFilterValueKeys = existing.searchFilterValueKeys.filter { it != key }
                                )
                            val alreadyCombined = acc.find { it.sources == existing.sources + provider }
                            if (alreadyCombined != null) {
                                acc - existing + withoutCurrentKey - alreadyCombined +
                                    alreadyCombined.copy(
                                        sources = alreadyCombined.sources,
                                        searchFilterValueKeys = alreadyCombined.searchFilterValueKeys + key,
                                        isEnabled = alreadyCombined.isEnabled || enabled,
                                    )
                            } else {
                                acc - existing +
                                    withoutCurrentKey +
                                    SearchFilter(
                                        sources = existing.sources + provider,
                                        searchFilterValueKeys = listOf(key),
                                        isEnabled = existing.isEnabled || enabled,
                                    )
                            }
                        } else {
                            val existing = acc.find { it.sources.size == 1 && it.sources.first() == provider }
                            if (existing != null) { // maybe the search provider already has registered a filter
                                acc - existing +
                                    existing.copy(searchFilterValueKeys = existing.searchFilterValueKeys + key)
                            } else {
                                // completely new filter
                                acc +
                                    SearchFilter(
                                        sources = listOf(provider),
                                        searchFilterValueKeys = listOf(key),
                                        isEnabled = enabled,
                                    )
                            }
                        }
                    }
                    .sortedByDescending { it.sources.size }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val searchResult: StateFlow<List<SearchResult>?> =
        combine(
                combine(providerSearchResult) { it },
                providerSearchEnabled,
                combine(providerSearchLoading) { it },
                filteredUserSearchResults,
            ) { results, enabled, loading, filteredResults ->
                log.debug {
                    "searchResult=${results.joinToString { it?.toString() ?: "<none>" }}, enabled=$enabled, loading=${loading.contentToString()}, filteredResults=${filteredResults.joinToString { it.userId.full }}"
                }
                results.mapIndexed { index, result ->
                    SearchResult(
                        id = searchProviders[index].id,
                        enabled = enabled[index],
                        providerDisplayName = searchProviders[index].displayName,
                        providerSearchResult = result,
                        isSearching = loading[index],
                    )
                }
            }
            .mapLatest { it } // optimization
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val searchResultList: StateFlow<List<UserSearchResult>> =
        combine(searchResult, filteredUserSearchResults) { results, filteredUserSearchResults ->
                if (results != null) {
                    randomSequence(results, filteredUserSearchResults)
                } else emptyList()
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    override val providerSettingsList: StateFlow<List<String>> =
        combine(providerSearchEnabled, searchFilterValues) { enabled, filterValues ->
                log.debug {
                    "provider settings: $enabled, ${filterValues.joinToString { "${it.key}: ${it.displayValue()}" }}"
                }
                filterValues
                    .filter {
                        it.displayValue().isNotBlank() &&
                            searchProviders
                                .mapIndexed { index, provider ->
                                    enabled[index] && provider.supportedFilters.contains(it.key)
                                }
                                .isNotEmpty()
                    }
                    .map { it.displayValue() }
                //   .map { "${it.key}: ${it.displayValue()}" } // FIXME who is responsible for i18n?
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun setProvider(providerId: SearchProviderId, enabled: Boolean) {
        val index = searchProviders.indexOfFirst { it.id == providerId }
        val rest =
            if (index == providerSearchEnabled.value.size - 1) emptyList()
            else providerSearchEnabled.value.subList(index + 1, providerSearchEnabled.value.size)
        _providerSearchEnabled.value = _providerSearchEnabled.value.subList(0, index) + enabled + rest

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
        combine(providerSearchLoading) { loading -> loading.any { it } }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val noResultsFound: StateFlow<Boolean?> =
        combine(providerSearchLoading) { loading ->
                if (loading.all { it.not() }) {
                    combine(providerSearchResult) { providerSearchResults ->
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

    override fun setSearchFilterValue(searchFilterValue: SearchFilterValue) {
        if (searchFilterValue.isEmpty()) {
            searchFilterValues.value = searchFilterValues.value.filter { it.key != searchFilterValue.key }
        } else {
            val existing = searchFilterValues.value.find { it.key == searchFilterValue.key }
            if (existing != null) {
                searchFilterValues.value = searchFilterValues.value - existing + searchFilterValue
            } else {
                searchFilterValues.value += searchFilterValue
            }
        }
    }

    init {
        log.debug { "searchUserProviders: $searchProviders" }
        coroutineScope.launch {
            searchFilterValues.collect { searchFilterValues ->
                _providerSearchCanBeEnabled.value =
                    if (searchFilterValues.isEmpty()) {
                        searchProviders.map { true }
                    } else {
                        searchFilterValues
                            .fold(searchProviders.map { false }) { acc, searchFilterValue ->
                                val inActive = searchProviders.map { searchProvider ->
                                    searchProvider.supportedFilters.none { it == searchFilterValue.key }
                                }
                                acc.zip(inActive).map { (v1, v2) -> v1 || v2 }
                            }
                            .map { it.not() }
                    }
            }
        }
        coroutineScope.launch { search() }
    }

    @OptIn(FlowPreview::class)
    private suspend fun search() {
        combine(
                triggerSearch,
                searchFilterValues,
                searchTerm
                    .onEach { log.debug { "Searching for user **** (redacted for privacy)" } }
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
                    log.debug { " - in search provider ${searchUserProvider.displayName} (${searchUserProvider.id})" }
                    if (searchTerm.isNotBlank() || filterValues.any { entry -> entry.isEmpty().not() }) {
                        providerSearchResult[index].value = null // reset old search results

                        if (providerSearchEnabled.value[index]) {
                            providerSearchLoading[index].value = true
                            launch {
                                providerSearchResult[index].value =
                                    searchUserProvider.search(searchTerm, filterValues, matrixClient.userId, this)
                                log.trace { " searchProvider ${searchUserProvider.id} finished search" }
                                providerSearchLoading[index].value = false
                            }
                        } else {
                            log.debug { "searchProvider ${searchUserProvider.id} is not enabled -> no search" }
                        }
                    } else {
                        log.trace { "user search blank -> empty list" }
                        providerSearchResult[index].value = null
                        providerSearchLoading[index].value = false
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

class PreviewUserSearchViewModel : UserSearchViewModel {
    override val searchTerm: TextFieldViewModel = TextFieldViewModelImpl(255)
    override val searchProviders: List<SearchProvider<*>> = emptyList()
    override val providedFilters: StateFlow<List<SearchFilter>> = MutableStateFlow(emptyList())
    override val searchResultList: MutableStateFlow<List<UserSearchResult>> = MutableStateFlow(emptyList())
    override val providerSearchEnabled: MutableStateFlow<List<Boolean>> = MutableStateFlow(emptyList())
    override val providerSettingsList: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    override val isSearching: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val noResultsFound: StateFlow<Boolean?> = MutableStateFlow(null)
    override val providerSearchCanBeEnabled: StateFlow<List<Boolean>> = MutableStateFlow(emptyList())
    override val searchFilterValues: StateFlow<List<SearchFilterValue>> = MutableStateFlow(emptyList())

    override fun setProvider(providerId: SearchProviderId, enabled: Boolean) {}

    override fun filterUserSearchResult(userSearchResult: UserSearchResult) {}

    override fun unfilterUserSearchResult(userSearchResult: UserSearchResult) {}

    override fun setSearchFilterValue(searchFilterValue: SearchFilterValue) {}
}

package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.search.provider.ProviderSearchResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProviderId
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface SearchUserViewModelFactory {
    fun create(
        matrixClientViewModelContext: MatrixClientViewModelContext,
    ): SearchUserViewModel {
        return SearchUserViewModelImpl(
            matrixClientViewModelContext,
        )
    }

    companion object : SearchUserViewModelFactory
}

/**
 * Searches for users in different [SearchUserProvider]s and provides a combined search result list.
 */
interface SearchUserViewModel {
    /**
     * Global search term for every [SearchUserProvider]. When changed, all providers use this term to initiate a search
     * and update their respective results.
     */
    val searchTerm: TextFieldViewModel

    /**
     * A list of all [SearchUserProvider]s. Obtained from the DI.
     */
    val searchUserProviders: List<SearchUserProvider>

    /**
     * A combined list of search results from different search providers. The list is sorted by relevance by the used
     * [SearchUserProvider]s, but interlaced from different providers.
     * Is updated as soon as any [SearchUserProvider] has a result list. If you do not want your presentation to jump
     * on search results popping up, try to `debounce` this list.
     */
    val searchResultList: StateFlow<List<UserSearchResult>?>

    /**
     * Indicates whether a search is currently running for each individual [SearchUserProvider].
     */
    val isSearching: StateFlow<Map<SearchUserProviderId, Boolean>>

    /**
     * Indicates whether the search provider is active at the moment. Can be set inactive via [setProvider].
     */
    val providerSearchActive: StateFlow<List<Boolean>>

    /**
     * Accumulation of all settings the search providers have, e.g., "city: Berlin, country: Germany".
     */
    val providerSettings: StateFlow<String?>

    /**
     * (De-)activate a [SearchUserProvider] by its [SearchUserProviderId].
     */
    fun setProvider(providerId: SearchUserProviderId, active: Boolean)

    fun filterUserSearchResult(userSearchResult: UserSearchResult)
    fun unfilterUserSearchResult(userSearchResult: UserSearchResult)
}

// FIXME change active should lead to new search being triggered

class SearchUserViewModelImpl(
    matrixClientViewModelContext: MatrixClientViewModelContext,
    private val debounceDuration: Duration = 300.milliseconds,
) : SearchUserViewModel, MatrixClientViewModelContext by matrixClientViewModelContext {
    override val searchUserProviders: List<SearchUserProvider> = getKoin().getAll<SearchUserProvider>()
    private val providerSearchResult = searchUserProviders.map { MutableStateFlow<ProviderSearchResult?>(null) }
    private val providerSearchLoading = searchUserProviders.map { MutableStateFlow(false) }

    private val providerSearchCanBeActive = MutableStateFlow(searchUserProviders.map { true })
    private val _providerSearchActive = MutableStateFlow(searchUserProviders.map { true })
    override val providerSearchActive =
        combine(_providerSearchActive, providerSearchCanBeActive) { activeList, canBeActiveList ->
            activeList.zip(canBeActiveList).map { (active, canBeActive) -> active && canBeActive }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), _providerSearchActive.value)
    override val searchTerm = TextFieldViewModelImpl(maxLength = 1_000)

    private val filteredUserSearchResults = MutableStateFlow<List<UserSearchResult>>(emptyList())

    override fun filterUserSearchResult(userSearchResult: UserSearchResult) {
        filteredUserSearchResults.value += userSearchResult
    }

    override fun unfilterUserSearchResult(userSearchResult: UserSearchResult) {
        filteredUserSearchResults.value -= userSearchResult
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val searchResult: StateFlow<List<SearchResult>?> = combine(
        combine(providerSearchResult) { it },
        providerSearchActive,
        combine(providerSearchLoading) { it },
        filteredUserSearchResults,
    ) { results, active, loading, filteredResults ->
        log.debug { "searchResult=${results.joinToString { it?.toString() ?: "<none>" }}, active=$active, loading=${loading.contentToString()}, filteredResults=${filteredResults.joinToString { it.userId.full }}" }
        results.mapIndexed { index, result ->
            SearchResult(
                id = searchUserProviders[index].providerId,
                active = active[index], // FIXME needed?
                providerDisplayName = searchUserProviders[index].providerDisplayName,
                providerSearchResult = result,
                isSearching = loading[index],
            )
        }
    }
        .mapLatest { it } // optimization
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val searchResultList: StateFlow<List<UserSearchResult>?> = combine(
        searchResult,
        filteredUserSearchResults,
    ) { results, filteredUserSearchResults ->
        if (results != null) {
            randomSequence(results, filteredUserSearchResults)
        } else null
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val providerSettings: StateFlow<String?> = combine(
        providerSearchActive,
        combine(searchUserProviders.flatMap { searchUserProvider ->
            searchUserProvider.settings.values
        }) { it }
    ) { active, settings ->
        log.trace { "provider settings: $active, ${settings.joinToString { it.value ?: "<none>" }}" }
        searchUserProviders.mapIndexed { index, searchUserProvider -> active[index] to searchUserProvider }
            .filter { (active, searchUserProvider) ->
                searchUserProvider.settings.any { setting ->
                    // look for other active search providers which might have the same setting
                    val consider =
                        active || searchUserProviders.any { otherSearchUserProvider ->
                            otherSearchUserProvider.providerId != searchUserProvider.providerId &&
                                    otherSearchUserProvider.settings.any { otherSetting -> otherSetting.key == setting.key }
                        }
                    val value = setting.value.value
                    consider && value.value != null && value.value.isNotBlank()
                }
            }
            .joinToString { (active, searchUserProvider) ->
                searchUserProvider.settings
                    .filter { setting ->
                        val value = setting.value.value.value
                        value != null && value.isNotBlank()
                    }
                    .values
                    .joinToString { setting ->
                        "${setting.value.name}: ${setting.value.value}"
                    }
            }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override fun setProvider(providerId: SearchUserProviderId, active: Boolean) {
        val index = searchUserProviders.indexOfFirst { it.providerId == providerId }
        val rest =
            if (index == providerSearchActive.value.size - 1) emptyList()
            else providerSearchActive.value.subList(index + 1, providerSearchActive.value.size)
        _providerSearchActive.value = _providerSearchActive.value.subList(0, index) + active + rest
    }

    override val isSearching: StateFlow<Map<SearchUserProviderId, Boolean>> =
        combine(providerSearchLoading) {
            it
        }.map { it.mapIndexed { index, loading -> searchUserProviders[index].providerId to loading }.toMap() }
            .stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(),
                searchUserProviders.associate { it.providerId to false }
            )

    init {
        log.debug { "searchUserProviders: $searchUserProviders" }

        coroutineScope.launch {
            val settings2SearchUserProviders = buildMap {
                searchUserProviders.forEachIndexed { index, searchUserProvider ->
                    searchUserProvider.settings.keys.forEach { setting ->
                        getOrPut(setting) { mutableListOf() }.add(index)
                    }
                }
            }
            combine(searchUserProviders.flatMap { searchUserProvider ->
                searchUserProvider.settings.values
            }) { it }.collectLatest { // anything changed in the settings
                searchUserProviders.forEach { searchUserProvider ->
                    searchUserProvider.settings.forEach { (settingsId, setting) ->
                        if (setting.value.value != null && setting.value.value?.isNotBlank() == true) {
                            settings2SearchUserProviders[settingsId]?.let { hasSetting ->
                                providerSearchCanBeActive.value = searchUserProviders.mapIndexed { index, _ ->
                                    hasSetting.contains(index)
                                }
                            }
                        }
                    }
                }
            }
        }
        coroutineScope.launch { search() }
    }

    @OptIn(FlowPreview::class)
    private suspend fun search() {
        combine(
            providerSettings,
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
        ) { _, searchTerm ->
            searchTerm // we just need to react to providerSettings changes
        }.scopedCollectLatest { searchTerm ->
            if (searchTerm.isNotBlank()) {
                log.trace { "search for users in search providers" }
                searchUserProviders.mapIndexed { index, searchUserProvider ->
                    log.debug { " - in search provider ${searchUserProvider.providerDisplayName} (${searchUserProvider.providerId})" }
                    if (providerSearchActive.value[index]) {
                        providerSearchLoading[index].value = true
                        providerSearchResult[index].value = null // reset old search results
                        launch {
                            providerSearchResult[index].value =
                                searchUserProvider.search(searchTerm, matrixClient.userId, this)
                            log.trace { " searchProvider ${searchUserProvider.providerId} finished search" }
                            providerSearchLoading[index].value = false
                        }
                    } else {
                        log.debug { "searchProvider ${searchUserProvider.providerId} is not active -> no search" }
                    }
                }
            } else {
                log.trace { "user search blank -> empty list" }
                providerSearchResult.map { it.value = null }
                providerSearchLoading.map { it.value = false }
            }
        }
    }

    private fun randomSequence(
        results: List<SearchResult>,
        filteredUserSearchResults: List<UserSearchResult>
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
         * Returns a list of lists, each built from elements of all lists with the same indexes.
         * Output has length of longest input list.
         */
        fun <T> zip(vararg lists: List<T>): List<List<T>> {
            return zip(*lists, transform = { a -> a.mapNotNull { it } })
        }

        fun <T> List<T>.splitIntoRandomChunks() = sequence {
            var index = 0
            while (index < size) {
                val chunkSize = random.nextInt(3, 10)
                val endIndex = minOf(index + chunkSize, size)

                if (chunkSize > 0) {
                    yield(subList(index, endIndex))
                }

                index = endIndex
            }
        }.toList()

        val userSearchResults = results.map { searchResult ->
            if (searchResult.active) {
                when (val providerSearchResult = searchResult.providerSearchResult) {
                    is ProviderSearchResult.Success -> {
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

class PreviewSearchUserViewModel() : SearchUserViewModel {
    override val searchTerm: TextFieldViewModel = TextFieldViewModelImpl(255)
    override val searchUserProviders: List<SearchUserProvider> = emptyList()
    override val searchResultList: MutableStateFlow<List<UserSearchResult>?> = MutableStateFlow(null)
    override val providerSearchActive: MutableStateFlow<List<Boolean>> = MutableStateFlow(emptyList())
    override val providerSettings: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isSearching: MutableStateFlow<Map<SearchUserProviderId, Boolean>> = MutableStateFlow(mapOf())
    override fun setProvider(providerId: SearchUserProviderId, active: Boolean) {}
    override fun filterUserSearchResult(userSearchResult: UserSearchResult) {}
    override fun unfilterUserSearchResult(userSearchResult: UserSearchResult) {}
}



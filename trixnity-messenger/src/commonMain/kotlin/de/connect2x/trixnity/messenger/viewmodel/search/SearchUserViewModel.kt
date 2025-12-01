package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.search.Analyzer
import de.connect2x.trixnity.messenger.search.BoolQuery
import de.connect2x.trixnity.messenger.search.Document
import de.connect2x.trixnity.messenger.search.DocumentIndex
import de.connect2x.trixnity.messenger.search.MatchQuery
import de.connect2x.trixnity.messenger.search.NgramTokenFilter
import de.connect2x.trixnity.messenger.search.RankingAlgorithm
import de.connect2x.trixnity.messenger.search.TextFieldIndex
import de.connect2x.trixnity.messenger.search.search
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.search.provider.ProviderSearchResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.UserId
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger {}

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

interface SearchUserViewModel {
    val searchTerm: TextFieldViewModel
    val searchUserProviders: List<SearchUserProvider>
    val searchResult: StateFlow<List<SearchResult>?>
    val searchResultList: StateFlow<List<UserSearchResult>?>
    val providerSearchActive: StateFlow<List<Boolean>>
    val providerSettings: StateFlow<String?>
    fun setProvider(provider: Int, active: Boolean)
}

// FIXME limit search to users not already selected in a group
// FIXME change active should lead to new search being triggered

class SearchUserViewModelImpl(
    matrixClientViewModelContext: MatrixClientViewModelContext,
    private val debounceDuration: Duration = 300.milliseconds,
) : SearchUserViewModel, MatrixClientViewModelContext by matrixClientViewModelContext {
    override val searchUserProviders: List<SearchUserProvider> = getKoin().getAll<SearchUserProvider>()
    private val providerSearchResult = searchUserProviders.map { MutableStateFlow<ProviderSearchResult?>(null) }
    private val providerSearchLoading = searchUserProviders.map { MutableStateFlow(false) }

    override val providerSearchActive = MutableStateFlow(searchUserProviders.map { true })
    override val searchTerm = TextFieldViewModelImpl(maxLength = 1_000)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val searchResult: StateFlow<List<SearchResult>?> = combine(
        combine(providerSearchResult) { it },
        providerSearchActive,
        combine(providerSearchLoading) { it },
    ) { results, active, loading ->
        log.debug { "searchResult=${results.joinToString { it?.toString() ?: "<none>" }}, active=$active, loading=$loading" }
        results.mapIndexed { index, result ->
            SearchResult(
                id = searchUserProviders[index].providerId,
                active = active[index], // FIXME needed?
                providerDisplayName = searchUserProviders[index].providerDisplayName,
                providerSearchResult = result,
                isLoading = loading[index],
            )
        }
    }
        .mapLatest { it } // optimization
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val searchResultList: StateFlow<List<UserSearchResult>?> =
        searchResult.map { results ->
            if (results != null) {
                randomSequence(results)
//                sequenceWithBM25(results)
            } else null
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val providerSettings: StateFlow<String?> = combine(
        providerSearchActive,
        combine(searchUserProviders.flatMap { searchUserProvider ->
            searchUserProvider.settings.values
        }) { it }
    ) { active, settings ->
        log.trace { "provider settings: $active, ${settings.joinToString { it.value ?: "<none>" }}" }
        searchUserProviders.mapIndexed { index, searchUserProvider -> active[index] to searchUserProvider.settings.values }
            .filter { (active, settings) ->
                active && settings.any { setting ->
                    val value = setting.value.value
                    value != null && value.isNotBlank()
                }
            }
            .joinToString { (_, settings) ->
                settings
                    .filter { setting ->
                        val value = setting.value.value
                        value != null && value.isNotBlank()
                    }
                    .joinToString { setting ->
                        "${setting.value.name}: ${setting.value.value}"
                    }
            }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    init {
        log.debug { "searchUserProviders: $searchUserProviders" }
        coroutineScope.launch { search() }
    }

    override fun setProvider(provider: Int, active: Boolean) {
        if (provider < 0 || provider > providerSearchActive.value.size - 1) {
            log.warn { "provider index wrong: $provider" }
        } else {
            val rest =
                if (provider == providerSearchActive.value.size - 1) emptyList()
                else providerSearchActive.value.subList(provider + 1, providerSearchActive.value.size)
            providerSearchActive.value = providerSearchActive.value.subList(0, provider) + active + rest
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun search() {
        combine(
            providerSettings,
            searchTerm
                .onEach { log.trace { "Searching for user $it" } }
                .map { it.text }
                .distinctUntilChanged()
                .debounce(debounceDuration)
                .map {
                    if (UserId.isValid(it.lowercase())) {
                        log.trace { "found matrix id" }
                        it.lowercase()
                    } else it
                },
        ) { _, searchTerm ->
            searchTerm // we just need to react to providerSettings changes
        }.scopedCollectLatest { searchTerm ->
            if (searchTerm.isNotBlank()) {
                log.trace { "search for users" }
                searchUserProviders.mapIndexed { index, searchUserProvider ->
                    log.trace { " - in search provider ${searchUserProvider.providerDisplayName} (${searchUserProvider.providerId})" }
                    if (providerSearchActive.value[index]) {
                        providerSearchLoading[index].value = true
                        launch {
                            providerSearchResult[index].value =
                                searchUserProvider.search(searchTerm, matrixClient.userId, this)
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

    private fun randomSequence(results: List<SearchResult>): List<UserSearchResult> {
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
                val chunkSize = random.nextInt(4)
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
                    is ProviderSearchResult.Success -> providerSearchResult.result.splitIntoRandomChunks()
                    else -> emptyList()
                }
            } else emptyList()
        }

        return zip(*userSearchResults.toTypedArray()).flatten().flatten()
    }

    private fun sequenceWithBM25(results: List<SearchResult>): List<UserSearchResult> {
        val userSearchResults = results.flatMap { searchResult ->
            if (searchResult.active) {
                when (val providerSearchResult = searchResult.providerSearchResult) {
                    is ProviderSearchResult.Success -> providerSearchResult.result
                    else -> emptyList()
                }
            } else emptyList()
        }
        val sortingFields = userSearchResults.flatMap { it.sortingFields.map { it.first } }.distinct()
        val documentIndex = DocumentIndex(
            mutableMapOf(
                "displayname" to TextFieldIndex(
                    rankingAlgorithm = RankingAlgorithm.BM25,
                    analyzer = Analyzer(tokenFilter = listOf(NgramTokenFilter(2)))
                ),
                "userId" to TextFieldIndex(
                    rankingAlgorithm = RankingAlgorithm.BM25,
                    analyzer = Analyzer(tokenFilter = listOf(NgramTokenFilter(2)))
                ),
                *sortingFields.map {
                    it to TextFieldIndex(
                        rankingAlgorithm = RankingAlgorithm.BM25,
                        analyzer = Analyzer(tokenFilter = listOf(NgramTokenFilter(2)))
                    )
                }
                    .toTypedArray(),
            )
        )
        log.debug { "documentIndex: ${documentIndex.mapping}" }
        userSearchResults.map { searchResult ->
            Document(
                searchResult.userId.full,
                mapOf(
                    "displayname" to listOf(searchResult.displayName ?: ""),
                    "userId" to listOf(searchResult.userId.full),
                    *searchResult.sortingFields.map { it.first to listOf(it.second) }.toTypedArray(),
                )
            )
        }.forEach(documentIndex::index)
        val result = documentIndex.search {
            query = BoolQuery(
                should = listOf(
                    MatchQuery("displayname", searchTerm.textValue, prefixMatch = true, boost = 1.5),
                    MatchQuery("userId", searchTerm.textValue, prefixMatch = true),
                ) + sortingFields.map { sortingField ->
                    MatchQuery(sortingField, searchTerm.textValue, prefixMatch = true)
                })
        }
            .sortedByDescending { hit -> hit.second }
            .onEach { hit -> println("hit: ${hit.first} (${hit.second}") }
            .mapNotNull { hit ->
                userSearchResults.find { it.userId.full == hit.first }
            }

        return result.ifEmpty { userSearchResults } // can be empty if every document matches
    }
}



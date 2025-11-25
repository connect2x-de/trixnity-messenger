package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.search.BoolQuery
import de.connect2x.trixnity.messenger.search.Document
import de.connect2x.trixnity.messenger.search.DocumentIndex
import de.connect2x.trixnity.messenger.search.MatchQuery
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
                // without query
//                val sizes = results.map { searchResult ->
//                    when (val providerSearchResult = searchResult.providerSearchResult) {
//                        is ProviderSearchResult.Success -> providerSearchResult.result.size
//                        else -> 0
//                    }
//                }
//                val maxSize = sizes.max()
//                (0..<maxSize).flatMap { i ->
//                    (0..<results.size).mapNotNull { j ->
//                        if (results[j].active) {
//                            when (val providerSearchResult = results[j].providerSearchResult) {
//                                is ProviderSearchResult.Success -> providerSearchResult.result.getOrNull(i)
//                                else -> null
//                            }
//                        } else null
//                    }

                // with query
                val documentIndex = DocumentIndex(
                    mutableMapOf(
                        "displayname" to TextFieldIndex(),
                        "userId" to TextFieldIndex(),
                    )
                )
                val userSearchResults = results.flatMap { searchResult ->
                    if (searchResult.active) {
                        when (val providerSearchResult = searchResult.providerSearchResult) {
                            is ProviderSearchResult.Success -> providerSearchResult.result
                            else -> emptyList()
                        }
                    } else emptyList()
                }
                userSearchResults.map { searchResult ->
                    Document(
                        searchResult.userId.full,
                        mapOf(
                            "displayname" to listOf(searchResult.displayName ?: ""),
                            "userId" to listOf(searchResult.userId.full),
                        )
                    )
                }.forEach(documentIndex::index)
                val result = documentIndex.search {
                    query = BoolQuery(
                        should = listOf(
                            MatchQuery("displayname", searchTerm.textValue, prefixMatch = true, boost = 1.5),
                            MatchQuery("userId", searchTerm.textValue, prefixMatch = true),
                        )
                    )
                }.mapNotNull { hit ->
                    userSearchResults.find { it.userId.full == hit.first }
                }
                result.ifEmpty { userSearchResults } // can be empty if every document matches
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
}



package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.search.provider.ProviderSearchResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.UserId
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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
    val searchResult: StateFlow<List<SearchResult>?>
}

// FIXME limit search to users not already selected in a group

class SearchUserViewModelImpl(
    matrixClientViewModelContext: MatrixClientViewModelContext,
    private val debounceDuration: Duration = 300.toDuration(DurationUnit.MILLISECONDS),
) : SearchUserViewModel, MatrixClientViewModelContext by matrixClientViewModelContext {
    private val searchUserProviders: List<SearchUserProvider> = getKoin().getAll<SearchUserProvider>()
    private val providerSearchResult = searchUserProviders.map { MutableStateFlow<ProviderSearchResult?>(null) }
    private val providerSearchLoading = searchUserProviders.map { MutableStateFlow(false) }

    override val searchTerm = TextFieldViewModelImpl(maxLength = 1_000)

    override val searchResult: StateFlow<List<SearchResult>?> = combine(
        combine(providerSearchResult) { it },
        combine(providerSearchLoading) { it },
    ) { results, loading ->
        results.mapIndexed { index, result ->
            SearchResult(
                providerDisplayName = searchUserProviders[index].providerDisplayName,
                providerSearchResult = result,
                isLoading = loading[index],
            )
        }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)


    init {
        log.debug { "searchUserProviders: $searchUserProviders" }
        coroutineScope.launch { search() }
    }

    @OptIn(FlowPreview::class)
    private suspend fun search() {
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
            }
            .scopedCollectLatest { searchTerm ->
                if (searchTerm.isNotBlank()) {
                    log.trace { "search for users" }
                    searchUserProviders.mapIndexed { index, searchUserProvider ->
                        log.trace { " - in search provider ${searchUserProvider.providerDisplayName} (${searchUserProvider.providerId})" }
                        providerSearchLoading[index].value = true
                        launch {
                            providerSearchResult[index].value =
                                searchUserProvider.search(searchTerm, matrixClient.userId, this)
                            providerSearchLoading[index].value = false
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



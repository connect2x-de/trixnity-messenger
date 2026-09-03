package de.connect2x.trixnity.messenger.viewmodel.search

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.search.SearchResult
import de.connect2x.trixnity.messenger.search.provider.EmptySearchContext
import de.connect2x.trixnity.messenger.search.provider.SearchFilter
import de.connect2x.trixnity.messenger.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.search.provider.SearchProviderFactory
import de.connect2x.trixnity.messenger.search.provider.SearchProviderResult
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class SearchViewModelTest {
    private val matrixClientMock = mock<MatrixClient>()
    private val userId = UserId("test", "server")

    @Test
    fun `isLoading is properly reset when new search is made too quickly`() = runTest {
        val cut = searchViewModel(listOf(SlowFailingSearchProviderFactory()))
        backgroundScope.launch { cut.isSearching.collect { println("isSearching: $it") } }
        cut.isSearching.value shouldBe false

        cut.searchTerm.update("Hi")
        delay(1.seconds) // this can't use eventually(1.seconds){...} because then the test will fail
        cut.isSearching.value shouldBe true

        cut.setProvider(SlowFailingKey, false)
        delay(200.milliseconds)
        cut.searchTerm.update("Hi2")

        delay(1.seconds)
        cut.isSearching.value shouldBe false
    }

    private fun TestScope.searchViewModel(
        searchProviderFactories: List<SearchProviderFactory<EmptySearchResult, EmptySearchContext>>
    ) =
        SearchViewModelFactory.create<EmptySearchResult, EmptySearchContext>(
            matrixClientViewModelContext =
                MatrixClientViewModelContextImpl(
                    componentContext = DefaultComponentContext(LifecycleRegistry()),
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(matrixClientMock, userId) +
                                        module {
                                            for (factory in searchProviderFactories) {
                                                single<SearchProviderFactory<EmptySearchResult, EmptySearchContext>> {
                                                    factory
                                                }
                                            }
                                        }
                                )
                            }
                            .koin,
                    userId = userId,
                    coroutineContext = backgroundScope.coroutineContext,
                    name = "SearchGroup",
                ),
            searchContext = EmptySearchContext,
        )

    private data object EmptySearchResult : SearchResult {
        override val id: String = "Dinosaur"
    }

    private companion object SlowFailingKey : SearchProvider.Key<SlowFailingSearchProvider>

    private class SlowFailingSearchProviderFactory : SearchProviderFactory<EmptySearchResult, EmptySearchContext> {
        override val supports: KClass<EmptySearchContext> = EmptySearchContext::class

        override fun create(account: UserId): SearchProvider<EmptySearchResult, EmptySearchContext> {
            return SlowFailingSearchProvider(SlowFailingKey)
        }
    }

    private class SlowFailingSearchProvider(override val key: SearchProvider.Key<*>) :
        SearchProvider<EmptySearchResult, EmptySearchContext> {
        override val displayName: String = "Sometimes I think but then I forget"
        override val priority: Int = 100
        override val disabledByDefault: Boolean = false
        override val supportedFilters: List<SearchFilter.Key<*>> = emptyList()

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilter>,
            searchContext: EmptySearchContext,
            coroutineScope: CoroutineScope,
        ): SearchProviderResult<EmptySearchResult> {
            delay(2.minutes)
            println("The SlowFailingSearchProvider has finished it's delay")
            throw IllegalStateException()
        }
    }
}

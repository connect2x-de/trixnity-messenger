package de.connect2x.trixnity.messenger.viewmodel.search

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.search.provider.SearchFilter
import de.connect2x.trixnity.messenger.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.search.provider.SearchProviderFactory
import de.connect2x.trixnity.messenger.search.provider.SearchProviderResult
import de.connect2x.trixnity.messenger.search.user.UserSearchContext
import de.connect2x.trixnity.messenger.search.user.homeserver.HomeserverSearchProviderFactory
import de.connect2x.trixnity.messenger.search.user.homeserver.HomeserverUserSearchResult
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module

private val log = Logger("de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModelTest")

private data class CitySearchFilter(val value: String) : SearchFilter {
    override val key: SearchFilter.Key<*> = Key

    companion object Key : SearchFilter.Key<CitySearchFilter>

    override val isDisabled: Boolean = value.isBlank()

    override val displayValue: String = value
}

private data class AddressSearchFilter(val value: String) : SearchFilter {
    override val key: SearchFilter.Key<*> = Key

    companion object Key : SearchFilter.Key<AddressSearchFilter>

    override val isDisabled: Boolean = value.isBlank()

    override val displayValue: String = value
}

private data class OptionsSearchFilter(val value: String) : SearchFilter {
    override val key: SearchFilter.Key<*> = Key

    companion object Key : SearchFilter.Key<OptionsSearchFilter>

    override val isDisabled: Boolean = value.isBlank()

    override val displayValue: String = value
}

private data class ColorSearchFilter(val value: Color?) : SearchFilter {
    override val key: SearchFilter.Key<*> = Key

    companion object Key : SearchFilter.Key<ColorSearchFilter>

    override val isDisabled: Boolean = value == null

    override val displayValue: String = value?.value ?: ""
}

private data class DifferentSearchFilter(val value: String) : SearchFilter {
    override val key: SearchFilter.Key<*> = Key

    companion object Key : SearchFilter.Key<DifferentSearchFilter>

    override val isDisabled: Boolean = value.isBlank()

    override val displayValue: String = value
}

private enum class Color(val value: String) {
    GREY("grey")
}

class UserSearchViewModelTest {

    private val matrixClientMock = mock<MatrixClient>()

    companion object {
        val user1 =
            HomeserverUserSearchResult(
                userId = UserId("user1", "server"),
                displayName = "User 1",
                initials = "U1",
                image = MutableStateFlow(null),
                presence = MutableStateFlow(null),
            )

        val user2 =
            HomeserverUserSearchResult(
                userId = UserId("user2", "server"),
                displayName = "User 2",
                initials = "U2",
                image = MutableStateFlow(null),
                presence = MutableStateFlow(null),
            )
        val user3 =
            HomeserverUserSearchResult(
                userId = UserId("user3", "server"),
                displayName = "User 3",
                initials = "U3",
                image = MutableStateFlow(null),
                presence = MutableStateFlow(null),
            )

        // displayname match
        val martin =
            HomeserverUserSearchResult(
                userId = UserId("supertester", "server"),
                displayName = "Martin ST",
                initials = "MS",
                image = MutableStateFlow(null),
                presence = MutableStateFlow(null),
            )

        // displayname match
        val alex =
            HomeserverUserSearchResult(
                userId = UserId("native", "server"),
                displayName = "Alex ST",
                initials = "AS",
                image = MutableStateFlow(null),
                presence = MutableStateFlow(null),
            )

        // userId match
        val merlin =
            HomeserverUserSearchResult(
                userId = UserId("star merlin", "server"),
                displayName = "Merlin",
                initials = "M",
                image = MutableStateFlow(null),
                presence = MutableStateFlow(null),
            )
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()

        resetMocks(matrixClientMock)
        every { matrixClientMock.userId } returns UserId("test", "server")
    }

    @Test
    fun `should select all user results`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(user1, user2, user3)
    }

    @Test
    fun `should search in the search providers`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)

        cut.searchResultList.value shouldNotBeNull
            {
                shouldContainAll(
                    listOf(user1, user2, user3) // SP1: user1, SP2: user2, user3
                )
            }
    }

    @Test
    fun `should search for term in displayname and userId`() = runTest {
        val cut = searchUserViewModel(listOf(SearchProvider3.Factory(SearchProvider1())))
        cut.searchTerm.update("st")
        delay(10.milliseconds)

        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(martin, alex, merlin)
    }

    @Test
    fun `should search in search provider when search term is blank but a filter setting is set`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("")
        cut.setSearchFilter(CitySearchFilter("Berlin"))
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull { shouldContainAll(listOf(user1)) } // SP1: user1, SP2: empty
    }

    @Test
    fun `should react to setting changes in search providers`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)

        cut.setSearchFilter(CitySearchFilter("Berlin Ost"))
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull
            {
                shouldContainAll(
                    listOf(user2, user3) // SP1: empty (user1 is not in Berlin Ost), SP2: user2, user3
                )
            }

        cut.setSearchFilter(CitySearchFilter("Berlin"))
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull
            {
                shouldContainAll(
                    listOf(user1, user2, user3) // SP1: user1 (user1 is in Berlin), SP2: user2, user3
                )
            }
    }

    @Test
    fun `should display the correct search options from the providers`() = runTest {
        val cut = searchUserViewModel()
        cut.setSearchFilter(CitySearchFilter("Berlin"))
        cut.setSearchFilter(OptionsSearchFilter("loud"))
        cut.setSearchFilter(ColorSearchFilter(Color.GREY))
        delay(10.milliseconds)

        cut.searchFilters.value.map { it.displayValue } shouldContain
            "loud" shouldContain
            "grey" shouldContain
            "Berlin" shouldNotContain
            "null" // address is not set
    }

    @Test
    fun `should allow to filter by search providers`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(user1, user2, user3)
        cut.setProvider(SearchProvider1, false)
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(user2, user3)
    }

    @Test
    fun `should disable search provider which does not have the selected filter`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        // both custom providers have a city filter
        cut.setSearchFilter(CitySearchFilter("Berlin"))
        delay(10.milliseconds)
        cut.searchProviderEnabled.value shouldBe
            mapOf(SearchProvider1 to true, SearchProvider2 to true, TestHomeserverSearchProvider to false)
        // only provider 1 has an address
        cut.setSearchFilter(AddressSearchFilter("somewhere"))
        delay(10.milliseconds)
        cut.searchProviderEnabled.value shouldBe
            mapOf(SearchProvider1 to true, SearchProvider2 to false, TestHomeserverSearchProvider to false)
        // reset address
        cut.setSearchFilter(AddressSearchFilter(""))
        delay(10.milliseconds)
        cut.searchProviderEnabled.value shouldBe
            mapOf(SearchProvider1 to true, SearchProvider2 to true, TestHomeserverSearchProvider to false)
    }

    @Test
    fun `should disable activating search provider which does not have the selected filter`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)
        cut.searchProviders.map { it.key } shouldBe
            listOf(TestHomeserverSearchProvider, SearchProvider1, SearchProvider2)
        cut.searchProviderCanBeEnabled.value shouldBe
            mapOf(SearchProvider1 to true, SearchProvider2 to true, TestHomeserverSearchProvider to true)

        // only provider 1 has an address
        cut.setSearchFilter(AddressSearchFilter("somewhere"))
        delay(10.milliseconds)

        cut.searchProviderCanBeEnabled.value shouldBe
            mapOf(SearchProvider1 to true, SearchProvider2 to false, TestHomeserverSearchProvider to false)
    }

    @Test
    fun `should disable filter settings that are not compatible with current settings`() = runTest {
        val cut = searchUserViewModel(listOf(SearchProvider5.Factory()))
        delay(10.milliseconds)
        cut.availableFilters.value.map { it.searchFilterKeys to it.isEnabled } shouldContainAll
            listOf(
                listOf(CitySearchFilter) to true, // SP1 + SP2
                listOf(AddressSearchFilter) to true, // SP1
                listOf(OptionsSearchFilter, ColorSearchFilter) to true, // SP2
                listOf(DifferentSearchFilter) to true, // SP5
            )

        cut.setSearchFilter(AddressSearchFilter("Somewhere"))
        delay(10.milliseconds)
        cut.availableFilters.value.map { it.searchFilterKeys to it.isEnabled } shouldContainAll
            listOf(
                listOf(CitySearchFilter) to true, // SP1 + SP2
                listOf(AddressSearchFilter) to true, // SP1
                listOf(OptionsSearchFilter, ColorSearchFilter) to false, // SP2
                listOf(DifferentSearchFilter) to false, // SP5
            )

        cut.setSearchFilter(AddressSearchFilter(""))
        delay(10.milliseconds)
        cut.availableFilters.value.map { it.searchFilterKeys to it.isEnabled } shouldContainAll
            listOf(
                listOf(CitySearchFilter) to true, // SP1 + SP2
                listOf(AddressSearchFilter) to true, // SP1
                listOf(OptionsSearchFilter, ColorSearchFilter) to true, // SP2
                listOf(DifferentSearchFilter) to true, // SP5
            )

        cut.setSearchFilter(DifferentSearchFilter("Oh no!"))
        delay(10.milliseconds)
        cut.availableFilters.value.map { it.searchFilterKeys to it.isEnabled } shouldContainAll
            listOf(
                listOf(CitySearchFilter) to false, // SP1 + SP2
                listOf(AddressSearchFilter) to false, // SP1
                listOf(OptionsSearchFilter, ColorSearchFilter) to false, // SP2
                listOf(DifferentSearchFilter) to true, // SP5
            )
    }

    @Test
    fun `should display the provider's setting if the setting is set in another deactivated provider`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        cut.setSearchFilter(CitySearchFilter("Berlin"))
        cut.setProvider(SearchProvider1, false) // provider2 still has city
        delay(10.milliseconds)
        cut.searchFilters.value.map { it.displayValue } shouldBe listOf("Berlin")
    }

    @Test
    fun `should show searching for provider when search is ongoing`() = runTest {
        val factory = SearchProviderWithResumedSearch.Factory()
        val searchProviderWithResumedSearch = factory.create(UserId(""))
        val cut = searchUserViewModel(listOf(factory))
        cut.searchTerm.update("onlyResumedReturnsUser1")
        delay(10.milliseconds)
        cut.isSearching.value shouldBe true
        cut.searchResultList.value shouldNotBeNull {} shouldBe emptyList()
        searchProviderWithResumedSearch.resumeSearch()
        delay(10.milliseconds)
        cut.isSearching.value shouldBe false
        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(user1)

        cut.searchTerm.update("changedAgain")
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldBe emptyList() // result is reset until search finishes
    }

    @Test
    fun `should indicate that no results have been found after a search is conducted`() = runTest {
        val factory = SearchProviderWithResumedSearch.Factory()
        val searchProviderWithResumedSearch = factory.create(UserId(""))
        val cut = searchUserViewModel(listOf(factory))

        delay(10.milliseconds)
        cut.noResultsFound.value shouldBe null // undetermined

        cut.searchTerm.update("onlyResumedReturnsUser1")
        delay(10.milliseconds)
        cut.noResultsFound.value shouldBe null // still undetermined

        searchProviderWithResumedSearch.resumeSearch()
        delay(10.milliseconds)
        cut.noResultsFound.value shouldBe false // found 1 user

        cut.searchTerm.update("emptyList")
        searchProviderWithResumedSearch.resumeSearch()
        delay(10.milliseconds)
        cut.noResultsFound.value shouldBe true // should not find anything
    }

    @Test
    fun `should do a new search if a provider is re-activated with another search term`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(user1, user2, user3)

        cut.setProvider(SearchProvider1, enabled = false)
        cut.searchTerm.update("martinInProvider1")
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldBe listOf()

        cut.setProvider(SearchProvider1, enabled = true)
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldBe listOf(martin)
    }

    @Test
    fun `should set a not enabled by default search provider to disabled initially but can be activated afterwards`() =
        runTest {
            val cut = searchUserViewModel(listOf(SearchProvider4.Factory(SearchProvider1())))
            delay(10.milliseconds)
            cut.searchProviders.map { it.key } shouldBe
                listOf(TestHomeserverSearchProvider, SearchProvider1, SearchProvider2, SearchProvider4)
            cut.searchProviderEnabled.value shouldBe
                mapOf(
                    SearchProvider1 to true,
                    SearchProvider2 to true,
                    SearchProvider4 to false,
                    TestHomeserverSearchProvider to true,
                )
            cut.searchProviderCanBeEnabled.value shouldBe
                mapOf(
                    SearchProvider1 to true,
                    SearchProvider2 to true,
                    SearchProvider4 to true,
                    TestHomeserverSearchProvider to true,
                )

            cut.setProvider(SearchProvider4, enabled = true)
            delay(10.milliseconds)
            cut.searchProviderEnabled.value shouldBe
                mapOf(
                    SearchProvider1 to true,
                    SearchProvider2 to true,
                    SearchProvider4 to true,
                    TestHomeserverSearchProvider to true,
                )
            cut.searchProviderCanBeEnabled.value shouldBe
                mapOf(
                    SearchProvider1 to true,
                    SearchProvider2 to true,
                    SearchProvider4 to true,
                    TestHomeserverSearchProvider to true,
                )
        }

    @Test
    fun `should enable combined setting when one search provider is enabled`() = runTest {
        val cut = searchUserViewModel(listOf(SearchProvider4.Factory(SearchProvider1())))
        delay(10.milliseconds)
        cut.availableFilters.value.map {
            Triple(it.sources.map { it.key }, it.searchFilterKeys, it.isEnabled)
        } shouldContainAll
            listOf(
                Triple(listOf(SearchProvider1, SearchProvider2, SearchProvider4), listOf(CitySearchFilter), true),
                Triple(listOf(SearchProvider1, SearchProvider4), listOf(AddressSearchFilter), true),
                Triple(listOf(SearchProvider2), listOf(OptionsSearchFilter, ColorSearchFilter), true),
            )
    }

    @Test
    fun `should combine filters from different providers correctly`() = runTest {
        val cut =
            searchUserViewModel(
                listOf(SearchProvider4.Factory(SearchProvider1()), SearchProvider5.Factory(), SearchProvider6.Factory())
            )
        delay(10.milliseconds)
        cut.availableFilters.value.map {
            Triple(it.sources.map { it.key }, it.searchFilterKeys, it.isEnabled)
        } shouldContainAll
            listOf(
                Triple(
                    listOf(SearchProvider1, SearchProvider2, SearchProvider6, SearchProvider4),
                    listOf(CitySearchFilter),
                    true,
                ),
                Triple(listOf(SearchProvider1, SearchProvider4), listOf(AddressSearchFilter), true),
                Triple(listOf(SearchProvider2), listOf(OptionsSearchFilter, ColorSearchFilter), true),
                Triple(listOf(SearchProvider5, SearchProvider6), listOf(DifferentSearchFilter), true),
            )
    }

    private fun TestScope.searchUserViewModel(): UserSearchViewModelImpl =
        searchUserViewModel<SearchProviderFactory<HomeserverUserSearchResult, UserSearchContext>>(null)

    private inline fun <reified T : SearchProviderFactory<HomeserverUserSearchResult, UserSearchContext>> TestScope
        .searchUserViewModel(additionalSearchUserProviderFactories: List<T>? = null): UserSearchViewModelImpl {

        val matrixClientViewModelContext =
            MatrixClientViewModelContextImpl(
                di =
                    koinApplication {
                            modules(
                                createTestDefaultTrixnityMessengerModules(
                                    mapOf(UserId("test", "server") to matrixClientMock)
                                ) +
                                    module {
                                        single<SearchProviderFactory<*, *>>(named("sp1")) { SearchProvider1.Factory() }
                                        single<SearchProviderFactory<*, *>>(named("sp2")) { SearchProvider2.Factory() }
                                        // dummy implementation to avoid mocking the standard impl
                                        single<SearchProviderFactory<*, *>>(named<HomeserverSearchProviderFactory>()) {
                                            TestHomeserverSearchProvider.Factory()
                                        }
                                        additionalSearchUserProviderFactories?.forEachIndexed {
                                            index,
                                            searchUserProviderFactory ->
                                            single<SearchProviderFactory<*, *>>(
                                                named("${searchUserProviderFactory::class.simpleName}$index")
                                            ) {
                                                searchUserProviderFactory
                                            }
                                        }
                                    }
                            )
                        }
                        .koin,
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                userId = UserId("test", "server"),
                coroutineContext = backgroundScope.coroutineContext,
                name = "test",
            )
        val searchUserViewModelImpl =
            UserSearchViewModelImpl(
                matrixClientViewModelContext,
                searchViewModel =
                    SearchViewModelImpl(
                        matrixClientViewModelContext,
                        debounceDuration = 0.milliseconds,
                        searchContext = UserSearchContext(UserId("@me:local")),
                    ),
            )
        backgroundScope.launch { searchUserViewModelImpl.availableFilters.collect() }
        backgroundScope.launch { searchUserViewModelImpl.searchResultList.collect() }
        backgroundScope.launch { searchUserViewModelImpl.isSearching.collect() }
        backgroundScope.launch { searchUserViewModelImpl.noResultsFound.collect() }
        backgroundScope.launch { searchUserViewModelImpl.searchProviderEnabled.collect() }
        backgroundScope.launch { searchUserViewModelImpl.searchProviderCanBeEnabled.collect() }
        return searchUserViewModelImpl
    }

    class TestHomeserverSearchProvider : SearchProvider<HomeserverUserSearchResult, UserSearchContext> {
        companion object Key : SearchProvider.Key<TestHomeserverSearchProvider>

        override val key: Key = Key
        override val displayName: String = "Homeserver"
        override val priority: Int = 100
        override val disabledByDefault: Boolean = false

        override val supportedFilters: List<SearchFilter.Key<*>> = emptyList()

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilter>,
            searchContext: UserSearchContext,
            coroutineScope: CoroutineScope,
        ): SearchProviderResult<HomeserverUserSearchResult> {
            log.debug { "homeserver search" }
            return SearchProviderResult.Success(listOf())
        }

        class Factory : SearchProviderFactory<HomeserverUserSearchResult, UserSearchContext> {
            override val supports = UserSearchContext::class

            override fun create(account: UserId): SearchProvider<HomeserverUserSearchResult, UserSearchContext> =
                TestHomeserverSearchProvider()
        }
    }

    class SearchProvider1 : SearchProvider<HomeserverUserSearchResult, UserSearchContext> {
        companion object Key : SearchProvider.Key<SearchProvider1>

        override val key: Key = Key
        override val displayName: String = "Test 1"
        override val priority: Int = 150
        override val disabledByDefault: Boolean = false

        override val supportedFilters: List<SearchFilter.Key<*>> = listOf(CitySearchFilter, AddressSearchFilter)

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilter>,
            searchContext: UserSearchContext,
            coroutineScope: CoroutineScope,
        ): SearchProviderResult<HomeserverUserSearchResult> {
            log.debug { "test-1 search" }
            return when (searchTerm) {
                "u",
                "" -> { // "" for testing filter settings
                    val city = filters.filterIsInstance<CitySearchFilter>().firstOrNull() ?: CitySearchFilter("")
                    if (city.value.isBlank() || city.value == "Berlin") {
                        SearchProviderResult.Success(listOf(user1))
                    } else {
                        SearchProviderResult.Success(listOf())
                    }
                }
                "martinInProvider1" -> {
                    SearchProviderResult.Success(listOf(martin))
                }
                else -> {
                    SearchProviderResult.Success(listOf())
                }
            }
        }

        class Factory : SearchProviderFactory<HomeserverUserSearchResult, UserSearchContext> {
            private val instance: SearchProvider1 = SearchProvider1()
            override val supports = UserSearchContext::class

            override fun create(account: UserId): SearchProvider1 = instance
        }
    }

    class SearchProvider2 : SearchProvider<HomeserverUserSearchResult, UserSearchContext> {
        companion object Key : SearchProvider.Key<SearchProvider2>

        override val key: Key = Key
        override val displayName: String = "Test 2"
        override val priority: Int = 151
        override val disabledByDefault: Boolean = false

        override val supportedFilters: List<SearchFilter.Key<*>> =
            listOf(CitySearchFilter, OptionsSearchFilter, ColorSearchFilter)

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilter>,
            searchContext: UserSearchContext,
            coroutineScope: CoroutineScope,
        ): SearchProviderResult<HomeserverUserSearchResult> {
            log.debug { "test-2 search" }
            return if (searchTerm == "u") {
                SearchProviderResult.Success(listOf(user2, user3))
            } else {
                SearchProviderResult.Success(listOf())
            }
        }

        class Factory : SearchProviderFactory<HomeserverUserSearchResult, UserSearchContext> {
            private val instance: SearchProvider2 = SearchProvider2()
            override val supports = UserSearchContext::class

            override fun create(account: UserId): SearchProvider2 = instance
        }
    }

    class SearchProvider3(searchProvider: SearchProvider<HomeserverUserSearchResult, UserSearchContext>) :
        SearchProvider<HomeserverUserSearchResult, UserSearchContext> by searchProvider {
        companion object Key : SearchProvider.Key<SearchProvider3>

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilter>,
            searchContext: UserSearchContext,
            coroutineScope: CoroutineScope,
        ): SearchProviderResult<HomeserverUserSearchResult> {
            log.debug { "test-2' search" }
            return SearchProviderResult.Success(listOf(martin, alex, merlin))
        }

        class Factory(searchProvider: SearchProvider<HomeserverUserSearchResult, UserSearchContext>) :
            SearchProviderFactory<HomeserverUserSearchResult, UserSearchContext> {
            private val instance: SearchProvider3 = SearchProvider3(searchProvider)
            override val supports = UserSearchContext::class

            override fun create(account: UserId): SearchProvider3 = instance
        }
    }

    class SearchProvider4(searchProvider: SearchProvider<HomeserverUserSearchResult, UserSearchContext>) :
        SearchProvider<HomeserverUserSearchResult, UserSearchContext> by searchProvider {
        companion object Key : SearchProvider.Key<SearchProvider4>

        override val key: Key = Key
        override val displayName: String = "Test 4"
        override val priority: Int = 1_000
        override val disabledByDefault: Boolean = true

        class Factory(searchProvider: SearchProvider<HomeserverUserSearchResult, UserSearchContext>) :
            SearchProviderFactory<HomeserverUserSearchResult, UserSearchContext> {
            private val instance: SearchProvider4 = SearchProvider4(searchProvider)
            override val supports = UserSearchContext::class

            override fun create(account: UserId): SearchProvider4 = instance
        }
    }

    class SearchProvider5 : SearchProvider<HomeserverUserSearchResult, UserSearchContext> {
        companion object Key : SearchProvider.Key<SearchProvider5>

        override val key: Key = Key
        override val displayName: String = "Test 5"
        override val priority: Int = 152
        override val disabledByDefault: Boolean = false

        override val supportedFilters: List<SearchFilter.Key<*>> = listOf(DifferentSearchFilter)

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilter>,
            searchContext: UserSearchContext,
            coroutineScope: CoroutineScope,
        ): SearchProviderResult<HomeserverUserSearchResult> {
            return SearchProviderResult.Success(listOf())
        }

        class Factory : SearchProviderFactory<HomeserverUserSearchResult, UserSearchContext> {
            private val instance: SearchProvider5 = SearchProvider5()
            override val supports = UserSearchContext::class

            override fun create(account: UserId): SearchProvider5 = instance
        }
    }

    class SearchProvider6 : SearchProvider<HomeserverUserSearchResult, UserSearchContext> {
        companion object Key : SearchProvider.Key<SearchProvider6>

        override val key: Key = Key
        override val displayName: String = "Test 6"
        override val priority: Int = 153
        override val disabledByDefault: Boolean = false

        override val supportedFilters: List<SearchFilter.Key<*>> = listOf(CitySearchFilter, DifferentSearchFilter)

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilter>,
            searchContext: UserSearchContext,
            coroutineScope: CoroutineScope,
        ): SearchProviderResult<HomeserverUserSearchResult> {
            return SearchProviderResult.Success(listOf())
        }

        class Factory : SearchProviderFactory<HomeserverUserSearchResult, UserSearchContext> {
            private val instance: SearchProvider6 = SearchProvider6()
            override val supports = UserSearchContext::class

            override fun create(account: UserId): SearchProvider6 = instance
        }
    }

    class SearchProviderWithResumedSearch : SearchProvider<HomeserverUserSearchResult, UserSearchContext> {
        companion object Key : SearchProvider.Key<SearchProviderWithResumedSearch>

        override val key: Key = Key
        override val displayName: String = "Test 99"
        override val priority: Int = 500
        override val disabledByDefault: Boolean = false
        override val supportedFilters: List<SearchFilter.Key<*>> = emptyList()

        private val resumeSearch = MutableStateFlow(false)

        fun resumeSearch() {
            resumeSearch.value = true
        }

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilter>,
            searchContext: UserSearchContext,
            coroutineScope: CoroutineScope,
        ): SearchProviderResult<HomeserverUserSearchResult> {
            log.debug { "test-99 search" }

            resumeSearch.first { it }
            resumeSearch.value = false

            return if (searchTerm == "emptyList") {
                SearchProviderResult.Success(listOf())
            } else {
                SearchProviderResult.Success(listOf(user1))
            }
        }

        class Factory : SearchProviderFactory<HomeserverUserSearchResult, UserSearchContext> {
            private val instance: SearchProviderWithResumedSearch = SearchProviderWithResumedSearch()
            override val supports = UserSearchContext::class

            override fun create(account: UserId): SearchProviderWithResumedSearch = instance
        }
    }
}

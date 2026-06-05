package de.connect2x.trixnity.messenger.viewmodel.search

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.searchProvider
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchFilterValue
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.viewmodel.search.provider.UserSearchProviderResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.homeserver.HomeserverSearchProvider
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
import kotlin.time.Duration
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

private data class CitySearchFilterValue(val value: String) : SearchFilterValue {
    override val key: SearchFilterValue.Key<*> = Key

    companion object Key : SearchFilterValue.Key<CitySearchFilterValue>

    override val isActive: Boolean = value.isBlank()

    override val displayValue: String = value
}

private data class AddressSearchFilterValue(val value: String) : SearchFilterValue {
    override val key: SearchFilterValue.Key<*> = Key

    companion object Key : SearchFilterValue.Key<AddressSearchFilterValue>

    override val isActive: Boolean = value.isBlank()

    override val displayValue: String = value
}

private data class OptionsSearchFilterValue(val value: String) : SearchFilterValue {
    override val key: SearchFilterValue.Key<*> = Key

    companion object Key : SearchFilterValue.Key<OptionsSearchFilterValue>

    override val isActive: Boolean = value.isBlank()

    override val displayValue: String = value
}

private data class ColorSearchFilterValue(val value: Color?) : SearchFilterValue {
    override val key: SearchFilterValue.Key<*> = Key

    companion object Key : SearchFilterValue.Key<ColorSearchFilterValue>

    override val isActive: Boolean = value == null

    override val displayValue: String = value?.value ?: ""
}

private data class DifferentSearchFilterValue(val value: String) : SearchFilterValue {
    override val key: SearchFilterValue.Key<*> = Key

    companion object Key : SearchFilterValue.Key<DifferentSearchFilterValue>

    override val isActive: Boolean = value.isBlank()

    override val displayValue: String = value
}

private enum class Color(val value: String) {
    GREY("grey")
}

class UserSearchViewModelTest {

    private val matrixClientMock = mock<MatrixClient>()

    companion object {
        val user1 =
            object : UserSearchResult {
                override val id: String = "user-1"
                override val userId: UserId = UserId("user1", "server")
                override val displayName: String = "User 1"
                override val initials: String = "U1"
                override val image: MutableStateFlow<ByteArray?> = MutableStateFlow(null)

                override fun toString(): String {
                    return "(id='$id', userId=$userId, displayName='$displayName')"
                }
            }

        val user2 =
            object : UserSearchResult {
                override val id: String = "user-2"
                override val userId: UserId = UserId("user2", "server")
                override val displayName: String = "User 2"
                override val initials: String = "U2"
                override val image: MutableStateFlow<ByteArray?> = MutableStateFlow(null)

                override fun toString(): String {
                    return "(id='$id', userId=$userId, displayName='$displayName')"
                }
            }
        val user3 =
            object : UserSearchResult {
                override val id: String = "user-3"
                override val userId: UserId = UserId("user3", "server")
                override val displayName: String = "User 3"
                override val initials: String = "U3"
                override val image: MutableStateFlow<ByteArray?> = MutableStateFlow(null)

                override fun toString(): String {
                    return "(id='$id', userId=$userId, displayName='$displayName')"
                }
            }

        // displayname match
        val martin =
            object : UserSearchResult {
                override val id: String = "martin"
                override val userId: UserId = UserId("supertester", "server")
                override val displayName: String = "Martin ST"
                override val initials: String = "MS"
                override val image: MutableStateFlow<ByteArray?> = MutableStateFlow(null)

                override fun toString(): String {
                    return "(id='$id', userId=$userId, displayName='$displayName')"
                }
            }

        // displayname match
        val alex =
            object : UserSearchResult {
                override val id: String = "alex"
                override val userId: UserId = UserId("native", "server")
                override val displayName: String = "Alex ST"
                override val initials: String = "AS"
                override val image: MutableStateFlow<ByteArray?> = MutableStateFlow(null)

                override fun toString(): String {
                    return "(id='$id', userId=$userId, displayName='$displayName')"
                }
            }

        // userId match
        val merlin =
            object : UserSearchResult {
                override val id: String = "merlin"
                override val userId: UserId = UserId("star merlin", "server")
                override val displayName: String = "Merlin"
                override val initials: String = "M"
                override val image: MutableStateFlow<ByteArray?> = MutableStateFlow(null)

                override fun toString(): String {
                    return "(id='$id', userId=$userId, displayName='$displayName')"
                }
            }
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

        cut.searchResult.value shouldNotBeNull
            {
                shouldContainAll(
                    listOf(
                        SearchResult(
                            id = SearchProvider1.Key,
                            enabled = true,
                            providerDisplayName = "Test 1",
                            isSearching = false,
                            providerSearchResult = UserSearchProviderResult.Success(listOf(user1)),
                        ),
                        SearchResult(
                            id = SearchProvider2.Key,
                            enabled = true,
                            providerDisplayName = "Test 2",
                            isSearching = false,
                            providerSearchResult = UserSearchProviderResult.Success(listOf(user2, user3)),
                        ),
                    )
                )
            }
    }

    @Test
    fun `should search for term in displayname and userId`() = runTest {
        val cut = searchUserViewModel(listOf(SearchProvider3(SearchProvider1())))
        cut.searchTerm.update("st")
        delay(10.milliseconds)

        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(martin, alex, merlin)
    }

    @Test
    fun `should search in search provider when search term is blank but a filter setting is set`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("")
        cut.setSearchFilterValue(CitySearchFilterValue("Berlin"))
        delay(10.milliseconds)
        cut.searchResult.value shouldNotBeNull
            {
                shouldContainAll(
                    listOf(
                        SearchResult(
                            id = SearchProvider1.Key,
                            enabled = true,
                            providerDisplayName = "Test 1",
                            isSearching = false,
                            providerSearchResult = UserSearchProviderResult.Success(listOf(user1)),
                        ),
                        SearchResult(
                            id = SearchProvider2.Key,
                            enabled = true,
                            providerDisplayName = "Test 2",
                            isSearching = false,
                            providerSearchResult = UserSearchProviderResult.Success(emptyList()),
                        ),
                    )
                )
            }
    }

    @Test
    fun `should react to setting changes in search providers`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)

        cut.setSearchFilterValue(CitySearchFilterValue("Berlin Ost"))
        delay(10.milliseconds)
        cut.searchResult.value shouldNotBeNull
            {
                shouldContainAll(
                    listOf(
                        SearchResult(
                            id = SearchProvider1.Key,
                            enabled = true,
                            providerDisplayName = "Test 1",
                            isSearching = false,
                            providerSearchResult =
                                UserSearchProviderResult.Success(listOf()), // user1 is not in Berlin Ost
                        ),
                        SearchResult(
                            id = SearchProvider2.Key,
                            enabled = true,
                            providerDisplayName = "Test 2",
                            isSearching = false,
                            providerSearchResult = UserSearchProviderResult.Success(listOf(user2, user3)),
                        ),
                    )
                )
            }

        cut.setSearchFilterValue(CitySearchFilterValue("Berlin"))
        delay(10.milliseconds)
        cut.searchResult.value shouldNotBeNull
            {
                shouldContainAll(
                    listOf(
                        SearchResult(
                            id = SearchProvider1.Key,
                            enabled = true,
                            providerDisplayName = "Test 1",
                            isSearching = false,
                            providerSearchResult = UserSearchProviderResult.Success(listOf(user1)), // user1 is in Berlin
                        ),
                        SearchResult(
                            id = SearchProvider2.Key,
                            enabled = true,
                            providerDisplayName = "Test 2",
                            isSearching = false,
                            providerSearchResult = UserSearchProviderResult.Success(listOf(user2, user3)),
                        ),
                    )
                )
            }
    }

    @Test
    fun `should display the correct search options from the providers`() = runTest {
        val cut = searchUserViewModel()
        cut.setSearchFilterValue(CitySearchFilterValue("Berlin"))
        cut.setSearchFilterValue(OptionsSearchFilterValue("loud"))
        cut.setSearchFilterValue(ColorSearchFilterValue(Color.GREY))
        delay(10.milliseconds)

        cut.searchFilterValues.value.map { it.displayValue } shouldContain
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
        cut.setProvider(SearchProvider1.Key, false)
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(user2, user3)
    }

    @Test
    fun `should disable search provider which does not have the selected filter`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        // both custom providers have a city filter
        cut.setSearchFilterValue(CitySearchFilterValue("Berlin"))
        delay(10.milliseconds)
        cut.searchProviderEnabled.value shouldBe
            cut.searchProviders.map { searchUserProvider ->
                searchUserProvider is SearchProvider1 || searchUserProvider is SearchProvider2
            }
        // only provider 1 has an address
        cut.setSearchFilterValue(AddressSearchFilterValue("somewhere"))
        delay(10.milliseconds)
        cut.searchProviderEnabled.value shouldBe
            cut.searchProviders.map { searchUserProvider -> searchUserProvider is SearchProvider1 }
        // reset address
        cut.setSearchFilterValue(AddressSearchFilterValue(""))
        delay(10.milliseconds)
        cut.searchProviderEnabled.value shouldBe
            cut.searchProviders.map { searchUserProvider ->
                searchUserProvider is SearchProvider1 || searchUserProvider is SearchProvider2
            }
    }

    @Test
    fun `should disable activating search provider which does not have the selected filter`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)
        cut.searchProviders.map { it.key } shouldBe
            listOf(TestHomeserverSearchProvider.Key, SearchProvider1.Key, SearchProvider2.Key)
        cut.searchProviderCanBeEnabled.value shouldBe listOf(true, true, true)

        // only provider 1 has an address
        cut.setSearchFilterValue(AddressSearchFilterValue("somewhere"))
        delay(10.milliseconds)

        cut.searchProviderCanBeEnabled.value shouldBe listOf(false, true, false)
    }

    @Test
    fun `should disable filter settings that are not compatible with current settings`() = runTest {
        val cut = searchUserViewModel(listOf(SearchProvider5()))
        delay(10.milliseconds)
        cut.availableFilters.value.map { it.searchFilterValueKeys to it.isEnabled } shouldContainAll
            listOf(
                listOf(CitySearchFilterValue.Key) to true, // SP1 + SP2
                listOf(AddressSearchFilterValue.Key) to true, // SP1
                listOf(OptionsSearchFilterValue.Key, ColorSearchFilterValue.Key) to true, // SP2
                listOf(DifferentSearchFilterValue.Key) to true, // SP5
            )

        cut.setSearchFilterValue(AddressSearchFilterValue("Somewhere"))
        delay(10.milliseconds)
        cut.availableFilters.value.map { it.searchFilterValueKeys to it.isEnabled } shouldContainAll
            listOf(
                listOf(CitySearchFilterValue.Key) to true, // SP1 + SP2
                listOf(AddressSearchFilterValue.Key) to true, // SP1
                listOf(OptionsSearchFilterValue.Key, ColorSearchFilterValue.Key) to false, // SP2
                listOf(DifferentSearchFilterValue.Key) to false, // SP5
            )

        cut.setSearchFilterValue(AddressSearchFilterValue(""))
        delay(10.milliseconds)
        cut.availableFilters.value.map { it.searchFilterValueKeys to it.isEnabled } shouldContainAll
            listOf(
                listOf(CitySearchFilterValue.Key) to true, // SP1 + SP2
                listOf(AddressSearchFilterValue.Key) to true, // SP1
                listOf(OptionsSearchFilterValue.Key, ColorSearchFilterValue.Key) to true, // SP2
                listOf(DifferentSearchFilterValue.Key) to true, // SP5
            )

        cut.setSearchFilterValue(DifferentSearchFilterValue("Oh no!"))
        delay(10.milliseconds)
        cut.availableFilters.value.map { it.searchFilterValueKeys to it.isEnabled } shouldContainAll
            listOf(
                listOf(CitySearchFilterValue.Key) to false, // SP1 + SP2
                listOf(AddressSearchFilterValue.Key) to false, // SP1
                listOf(OptionsSearchFilterValue.Key, ColorSearchFilterValue.Key) to false, // SP2
                listOf(DifferentSearchFilterValue.Key) to true, // SP5
            )
    }

    @Test
    fun `should display the provider's setting if the setting is set in another deactivated provider`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        cut.setSearchFilterValue(CitySearchFilterValue("Berlin"))
        cut.setProvider(SearchProvider1.Key, false) // provider2 still has city
        delay(10.milliseconds)
        cut.searchFilterValues.value.map { it.displayValue } shouldBe listOf("Berlin")
    }

    @Test
    fun `should show searching for provider when search is ongoing`() = runTest {
        val searchProviderWithResumedSearch = SearchProviderWithResumedSearch()
        val cut = searchUserViewModel(listOf(searchProviderWithResumedSearch))
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
        val searchProviderWithResumedSearch = SearchProviderWithResumedSearch()
        val cut = searchUserViewModel(listOf(searchProviderWithResumedSearch))

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

        cut.setProvider(SearchProvider1.Key, enabled = false)
        cut.searchTerm.update("martinInProvider1")
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldBe listOf()

        cut.setProvider(SearchProvider1.Key, enabled = true)
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldBe listOf(martin)
    }

    @Test
    fun `should set a not enabled by default search provider to disabled initially but can be activated afterwards`() =
        runTest {
            val cut = searchUserViewModel(listOf(SearchProvider4(SearchProvider1())))
            delay(10.milliseconds)
            cut.searchProviders.map { it.key } shouldBe
                listOf(TestHomeserverSearchProvider.Key, SearchProvider1.Key, SearchProvider2.Key, SearchProvider4.Key)
            cut.searchProviderEnabled.value shouldBe listOf(true, true, true, false)
            cut.searchProviderCanBeEnabled.value shouldBe listOf(true, true, true, true)

            cut.setProvider(SearchProvider4.Key, enabled = true)
            delay(10.milliseconds)
            cut.searchProviderEnabled.value shouldBe listOf(true, true, true, true)
            cut.searchProviderCanBeEnabled.value shouldBe listOf(true, true, true, true)
        }

    @Test
    fun `should enable combined setting when one search provider is enabled`() = runTest {
        val cut = searchUserViewModel(listOf(SearchProvider4(SearchProvider1())))
        delay(10.milliseconds)
        cut.availableFilters.value.map {
            Triple(it.sources.map { it.key }, it.searchFilterValueKeys, it.isEnabled)
        } shouldContainAll
            listOf(
                Triple(
                    listOf(SearchProvider1.Key, SearchProvider2.Key, SearchProvider4.Key),
                    listOf(CitySearchFilterValue.Key),
                    true,
                ),
                Triple(listOf(SearchProvider1.Key, SearchProvider4.Key), listOf(AddressSearchFilterValue.Key), true),
                Triple(
                    listOf(SearchProvider2.Key),
                    listOf(OptionsSearchFilterValue.Key, ColorSearchFilterValue.Key),
                    true,
                ),
            )
    }

    @Test
    fun `should combine filters from different providers correctly`() = runTest {
        val cut = searchUserViewModel(listOf(SearchProvider4(SearchProvider1()), SearchProvider5(), SearchProvider6()))
        delay(10.milliseconds)
        cut.availableFilters.value.map {
            Triple(it.sources.map { it.key }, it.searchFilterValueKeys, it.isEnabled)
        } shouldContainAll
            listOf(
                Triple(
                    listOf(SearchProvider1.Key, SearchProvider2.Key, SearchProvider6.Key, SearchProvider4.Key),
                    listOf(CitySearchFilterValue.Key),
                    true,
                ),
                Triple(listOf(SearchProvider1.Key, SearchProvider4.Key), listOf(AddressSearchFilterValue.Key), true),
                Triple(
                    listOf(SearchProvider2.Key),
                    listOf(OptionsSearchFilterValue.Key, ColorSearchFilterValue.Key),
                    true,
                ),
                Triple(listOf(SearchProvider5.Key, SearchProvider6.Key), listOf(DifferentSearchFilterValue.Key), true),
            )
    }

    private fun TestScope.searchUserViewModel(): UserSearchViewModelImpl = searchUserViewModel<SearchProvider<*>>(null)

    private inline fun <reified T : SearchProvider<*>> TestScope.searchUserViewModel(
        additionalSearchUserProviders: List<T>? = null
    ): UserSearchViewModelImpl {
        val searchUserViewModelImpl =
            UserSearchViewModelImpl(
                MatrixClientViewModelContextImpl(
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(
                                        mapOf(UserId("test", "server") to matrixClientMock)
                                    ) +
                                        module {
                                            searchProvider<SearchProvider1> { SearchProvider1() }
                                            searchProvider<SearchProvider2> { SearchProvider2() }
                                            // dummy implementation to avoid mocking the standard impl
                                            single<SearchProvider<*>>(named<HomeserverSearchProvider>()) {
                                                TestHomeserverSearchProvider()
                                            }
                                            additionalSearchUserProviders?.forEach { additionalSearchUserProvider ->
                                                single<SearchProvider<*>>(
                                                    named(additionalSearchUserProvider::class.simpleName ?: "")
                                                ) {
                                                    additionalSearchUserProvider
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
                ),
                debounceDuration = Duration.ZERO,
            )
        backgroundScope.launch { searchUserViewModelImpl.availableFilters.collect() }
        backgroundScope.launch { searchUserViewModelImpl.searchResult.collect() }
        backgroundScope.launch { searchUserViewModelImpl.searchResultList.collect() }
        backgroundScope.launch { searchUserViewModelImpl.isSearching.collect() }
        backgroundScope.launch { searchUserViewModelImpl.noResultsFound.collect() }
        backgroundScope.launch { searchUserViewModelImpl.searchProviderEnabled.collect() }
        backgroundScope.launch { searchUserViewModelImpl.searchProviderCanBeEnabled.collect() }
        return searchUserViewModelImpl
    }

    class TestHomeserverSearchProvider : SearchProvider<UserSearchProviderResult> {
        companion object Key : SearchProvider.Key<TestHomeserverSearchProvider>

        override val key: Key = Key
        override val displayName: String = "Homeserver"
        override val priority: Int = 100
        override val disabledByDefault: Boolean = false

        override val supportedFilters: List<SearchFilterValue.Key<*>> = emptyList()

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilterValue>,
            activeAccount: UserId,
            coroutineScope: CoroutineScope,
        ): UserSearchProviderResult {
            log.debug { "homeserver search" }
            return UserSearchProviderResult.Success(listOf())
        }
    }

    class SearchProvider1 : SearchProvider<UserSearchProviderResult> {
        companion object Key : SearchProvider.Key<SearchProvider1>

        override val key: Key = Key
        override val displayName: String = "Test 1"
        override val priority: Int = 150
        override val disabledByDefault: Boolean = false

        override val supportedFilters: List<SearchFilterValue.Key<*>> =
            listOf(CitySearchFilterValue.Key, AddressSearchFilterValue.Key)

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilterValue>,
            activeAccount: UserId,
            coroutineScope: CoroutineScope,
        ): UserSearchProviderResult {
            log.debug { "test-1 search" }
            return when (searchTerm) {
                "u",
                "" -> { // "" for testing filter settings
                    val city =
                        filters.filterIsInstance<CitySearchFilterValue>().firstOrNull() ?: CitySearchFilterValue("")
                    if (city.value.isBlank() || city.value == "Berlin") {
                        UserSearchProviderResult.Success(listOf(user1))
                    } else {
                        UserSearchProviderResult.Success(listOf())
                    }
                }
                "martinInProvider1" -> {
                    UserSearchProviderResult.Success(listOf(martin))
                }
                else -> {
                    UserSearchProviderResult.Success(listOf())
                }
            }
        }
    }

    class SearchProvider2 : SearchProvider<UserSearchProviderResult> {
        companion object Key : SearchProvider.Key<SearchProvider2>

        override val key: Key = Key
        override val displayName: String = "Test 2"
        override val priority: Int = 151
        override val disabledByDefault: Boolean = false

        override val supportedFilters: List<SearchFilterValue.Key<*>> =
            listOf(CitySearchFilterValue.Key, OptionsSearchFilterValue.Key, ColorSearchFilterValue.Key)

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilterValue>,
            activeAccount: UserId,
            coroutineScope: CoroutineScope,
        ): UserSearchProviderResult {
            log.debug { "test-2 search" }
            return if (searchTerm == "u") {
                UserSearchProviderResult.Success(listOf(user2, user3))
            } else {
                UserSearchProviderResult.Success(listOf())
            }
        }
    }

    class SearchProvider3(searchProvider: SearchProvider<UserSearchProviderResult>) :
        SearchProvider<UserSearchProviderResult> by searchProvider {
        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilterValue>,
            activeAccount: UserId,
            coroutineScope: CoroutineScope,
        ): UserSearchProviderResult {
            log.debug { "test-2' search" }
            return UserSearchProviderResult.Success(listOf(martin, alex, merlin))
        }
    }

    class SearchProvider4(searchProvider: SearchProvider<UserSearchProviderResult>) :
        SearchProvider<UserSearchProviderResult> by searchProvider {
        companion object Key : SearchProvider.Key<SearchProvider4>

        override val key: Key = Key
        override val displayName: String = "Test 4"
        override val priority: Int = 1_000
        override val disabledByDefault: Boolean = true
    }

    class SearchProvider5 : SearchProvider<UserSearchProviderResult> {
        companion object Key : SearchProvider.Key<SearchProvider5>

        override val key: Key = Key
        override val displayName: String = "Test 5"
        override val priority: Int = 152
        override val disabledByDefault: Boolean = false

        override val supportedFilters: List<SearchFilterValue.Key<*>> = listOf(DifferentSearchFilterValue.Key)

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilterValue>,
            activeAccount: UserId,
            coroutineScope: CoroutineScope,
        ): UserSearchProviderResult {
            return UserSearchProviderResult.Success(listOf())
        }
    }

    class SearchProvider6 : SearchProvider<UserSearchProviderResult> {
        companion object Key : SearchProvider.Key<SearchProvider6>

        override val key: Key = Key
        override val displayName: String = "Test 6"
        override val priority: Int = 153
        override val disabledByDefault: Boolean = false

        override val supportedFilters: List<SearchFilterValue.Key<*>> =
            listOf(CitySearchFilterValue.Key, DifferentSearchFilterValue.Key)

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilterValue>,
            activeAccount: UserId,
            coroutineScope: CoroutineScope,
        ): UserSearchProviderResult {
            return UserSearchProviderResult.Success(listOf())
        }
    }

    class SearchProviderWithResumedSearch : SearchProvider<UserSearchProviderResult> {
        companion object Key : SearchProvider.Key<SearchProviderWithResumedSearch>

        override val key: Key = Key
        override val displayName: String = "Test 99"
        override val priority: Int = 500
        override val disabledByDefault: Boolean = false
        override val supportedFilters: List<SearchFilterValue.Key<*>> = emptyList()

        private val resumeSearch = MutableStateFlow(false)

        fun resumeSearch() {
            resumeSearch.value = true
        }

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilterValue>,
            activeAccount: UserId,
            coroutineScope: CoroutineScope,
        ): UserSearchProviderResult {
            log.debug { "test-99 search" }

            resumeSearch.first { it }
            resumeSearch.value = false

            return if (searchTerm == "emptyList") {
                UserSearchProviderResult.Success(listOf())
            } else {
                UserSearchProviderResult.Success(listOf(user1))
            }
        }
    }
}

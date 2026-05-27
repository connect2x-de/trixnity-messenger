package de.connect2x.trixnity.messenger.viewmodel.search

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.searchUserProvider
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.search.provider.ProviderSearchResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchSetting
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProviderId
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SettingsId
import de.connect2x.trixnity.messenger.viewmodel.search.provider.homeserver.HomeserverSearchUserProvider
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.shouldContainAll
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

private val log = Logger("de.connect2x.trixnity.messenger.viewmodel.search.SearchUserViewModelTest")

private object SettingsIdCity : SettingsId

private object SettingsIdAddress : SettingsId

private object SettingsIdOptions : SettingsId

private object SettingsIdColor : SettingsId

private object SettingsIdDifferent : SettingsId

private enum class Color(val value: String) {
    BLACK("black"),
    WHITE("white"),
    GREY("grey");

    companion object {
        fun fromValue(value: String): Color? = entries.find { it.value == value }
    }
}

class SearchUserViewModelTest {

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
                            id = "test-1",
                            enabled = true,
                            providerDisplayName = "Test 1",
                            isSearching = false,
                            providerSearchResult = ProviderSearchResult.Success(listOf(user1)),
                        ),
                        SearchResult(
                            id = "test-2",
                            enabled = true,
                            providerDisplayName = "Test 2",
                            isSearching = false,
                            providerSearchResult = ProviderSearchResult.Success(listOf(user2, user3)),
                        ),
                    )
                )
            }
    }

    @Test
    fun `should search for term in displayname and userId`() = runTest {
        val cut = searchUserViewModel(SearchUserProvider3(SearchUserProvider1()))
        cut.searchTerm.update("st")
        delay(10.milliseconds)

        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(martin, alex, merlin)
    }

    @Test
    fun `should search in search provider when search term is blank but a filter setting is set`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("")
        cut.providerSettings[SettingsIdCity]?.setValue("Berlin")
        delay(10.milliseconds)
        cut.searchResult.value shouldNotBeNull
            {
                shouldContainAll(
                    listOf(
                        SearchResult(
                            id = "test-1",
                            enabled = true,
                            providerDisplayName = "Test 1",
                            isSearching = false,
                            providerSearchResult = ProviderSearchResult.Success(listOf(user1)),
                        ),
                        SearchResult(
                            id = "test-2",
                            enabled = true,
                            providerDisplayName = "Test 2",
                            isSearching = false,
                            providerSearchResult = ProviderSearchResult.Success(emptyList()),
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

        cut.providerSettings[SettingsIdCity]?.setValue("Berlin Ost")
        delay(10.milliseconds)
        cut.searchResult.value shouldNotBeNull
            {
                shouldContainAll(
                    listOf(
                        SearchResult(
                            id = "test-1",
                            enabled = true,
                            providerDisplayName = "Test 1",
                            isSearching = false,
                            providerSearchResult = ProviderSearchResult.Success(listOf()), // user1 is not in Berlin Ost
                        ),
                        SearchResult(
                            id = "test-2",
                            enabled = true,
                            providerDisplayName = "Test 2",
                            isSearching = false,
                            providerSearchResult = ProviderSearchResult.Success(listOf(user2, user3)),
                        ),
                    )
                )
            }

        cut.providerSettings[SettingsIdCity]?.setValue("Berlin")
        delay(10.milliseconds)
        cut.searchResult.value shouldNotBeNull
            {
                shouldContainAll(
                    listOf(
                        SearchResult(
                            id = "test-1",
                            enabled = true,
                            providerDisplayName = "Test 1",
                            isSearching = false,
                            providerSearchResult = ProviderSearchResult.Success(listOf(user1)), // user1 is in Berlin
                        ),
                        SearchResult(
                            id = "test-2",
                            enabled = true,
                            providerDisplayName = "Test 2",
                            isSearching = false,
                            providerSearchResult = ProviderSearchResult.Success(listOf(user2, user3)),
                        ),
                    )
                )
            }
    }

    @Test
    fun `should display the correct search options from the providers`() = runTest {
        val cut = searchUserViewModel()
        cut.providerSettings[SettingsIdCity]?.setValue("Berlin")
        cut.providerSettings[SettingsIdOptions]?.setValue("loud")
        cut.providerSettings[SettingsIdColor]?.setValue("grey")
        delay(10.milliseconds)

        cut.providerSettingsList.value shouldContain
            "options: loud" shouldContain
            "color: grey (color)" shouldContain
            "city: Berlin" shouldNotContain
            "address: null"
    }

    @Test
    fun `should allow to filter by search providers`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(user1, user2, user3)
        cut.setProvider("test-1", false)
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(user2, user3)
    }

    @Test
    fun `should disable search provider which does not have the selected filter`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        // both custom providers have a city filter
        cut.providerSettings[SettingsIdCity]?.setValue("Berlin")
        delay(10.milliseconds)
        cut.providerSearchEnabled.value shouldBe
            cut.searchUserProviders.map { searchUserProvider ->
                searchUserProvider is SearchUserProvider1 || searchUserProvider is SearchUserProvider2
            }
        // only provider 1 has an address
        cut.providerSettings[SettingsIdAddress]?.setValue("somewhere")
        delay(10.milliseconds)
        cut.providerSearchEnabled.value shouldBe
            cut.searchUserProviders.map { searchUserProvider -> searchUserProvider is SearchUserProvider1 }
        // reset address
        cut.providerSettings[SettingsIdAddress]?.setValue(null)
        delay(10.milliseconds)
        cut.providerSearchEnabled.value shouldBe
            cut.searchUserProviders.map { searchUserProvider ->
                searchUserProvider is SearchUserProvider1 || searchUserProvider is SearchUserProvider2
            }
        // reset address to empty String -> same as null
        cut.providerSettings[SettingsIdAddress]?.setValue("")
        delay(10.milliseconds)
        cut.providerSearchEnabled.value shouldBe
            cut.searchUserProviders.map { searchUserProvider ->
                searchUserProvider is SearchUserProvider1 || searchUserProvider is SearchUserProvider2
            }
    }

    @Test
    fun `should disable activating search provider which does not have the selected filter`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)
        cut.searchUserProviders.map { it.providerId } shouldBe listOf("homeserver", "test-1", "test-2")
        cut.providerSearchCanBeEnabled.value shouldBe listOf(true, true, true)

        // only provider 1 has an address
        cut.providerSettings[SettingsIdAddress]?.setValue("somewhere")
        delay(10.milliseconds)

        cut.providerSearchCanBeEnabled.value shouldBe listOf(false, true, false)
    }

    @Test
    fun `should disable filter settings that are not compatible with current settings`() = runTest {
        val cut = searchUserViewModel(SearchUserProvider5())
        delay(10.milliseconds)
        cut.providerSettings.entries.map { (key, value) -> key to value.enabled.value } shouldContainOnly
            listOf(
                SettingsIdCity to true,
                SettingsIdAddress to true,
                SettingsIdOptions to true,
                SettingsIdColor to true,
                SettingsIdDifferent to true,
            )

        cut.providerSettings[SettingsIdAddress]?.setValue("Somewhere")
        delay(10.milliseconds)
        // only Provider1 has city and address
        cut.providerSettings.entries.map { (key, value) -> key to value.enabled.value } shouldContainOnly
            listOf(
                SettingsIdCity to true,
                SettingsIdAddress to true,
                SettingsIdOptions to false,
                SettingsIdColor to false,
                SettingsIdDifferent to false,
            )

        cut.providerSettings[SettingsIdAddress]?.setValue(null)
        delay(10.milliseconds)
        cut.providerSettings.entries.map { (key, value) -> key to value.enabled.value } shouldContainOnly
            listOf(
                SettingsIdCity to true,
                SettingsIdAddress to true,
                SettingsIdOptions to true,
                SettingsIdColor to true,
                SettingsIdDifferent to true,
            )

        cut.providerSettings[SettingsIdDifferent]?.setValue("Oh no!")
        delay(10.milliseconds)
        cut.providerSettings.entries.map { (key, value) -> key to value.enabled.value } shouldContainOnly
            listOf(
                SettingsIdCity to false,
                SettingsIdOptions to false,
                SettingsIdAddress to false,
                SettingsIdColor to false,
                SettingsIdDifferent to true,
            )
    }

    @Test
    fun `should display the provider's setting if the setting is set in another deactivated provider`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        cut.providerSettings[SettingsIdCity]?.setValue("Berlin")
        cut.setProvider("test-1", false) // provider2 still has city
        delay(10.milliseconds)
        cut.providerSettingsList.value shouldBe listOf("city: Berlin")
    }

    @Test
    fun `should show searching for provider when search is ongoing`() = runTest {
        val searchUserProviderWithResumedSearch = SearchUserProviderWithResumedSearch()
        val cut = searchUserViewModel(searchUserProviderWithResumedSearch)
        cut.searchTerm.update("onlyResumedReturnsUser1")
        delay(10.milliseconds)
        cut.isSearching.value shouldContainAll
            mapOf("test-1" to false, "test-2" to false, searchUserProviderWithResumedSearch.providerId to true)
        cut.searchResultList.value shouldNotBeNull {} shouldBe emptyList()
        searchUserProviderWithResumedSearch.resumeSearch()
        delay(10.milliseconds)
        cut.isSearching.value shouldContainAll
            mapOf("test-1" to false, "test-2" to false, searchUserProviderWithResumedSearch.providerId to false)
        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(user1)

        cut.searchTerm.update("changedAgain")
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldBe emptyList() // result is reset until search finishes
    }

    @Test
    fun `should do a new search if a provider is re-activated with another search term`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(user1, user2, user3)

        cut.setProvider("test-1", enabled = false)
        cut.searchTerm.update("martinInProvider1")
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldBe listOf()

        cut.setProvider("test-1", enabled = true)
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldBe listOf(martin)
    }

    @Test
    fun `should set a not enabled by default search provider to disabled initially, but can be activated afterwards`() =
        runTest {
            val cut = searchUserViewModel(SearchUserProvider4(SearchUserProvider1()))
            delay(10.milliseconds)
            cut.searchUserProviders.map { it.providerId } shouldBe listOf("homeserver", "test-1", "test-2", "test-4")
            cut.providerSearchEnabled.value shouldBe listOf(true, true, true, false)
            cut.providerSearchCanBeEnabled.value shouldBe listOf(true, true, true, true)

            cut.setProvider("test-4", enabled = true)
            delay(10.milliseconds)
            cut.providerSearchEnabled.value shouldBe listOf(true, true, true, true)
            cut.providerSearchCanBeEnabled.value shouldBe listOf(true, true, true, true)
        }

    @Test
    fun `should enable combined setting when one search provider is enabled`() = runTest {
        val cut = searchUserViewModel(SearchUserProvider4(SearchUserProvider1()))
        delay(10.milliseconds)
        cut.providerSettings[SettingsIdCity]?.enabled?.value shouldBe true
    }

    private fun TestScope.searchUserViewModel(): SearchUserViewModelImpl = searchUserViewModel(null)

    private inline fun <reified T : SearchUserProvider> TestScope.searchUserViewModel(
        additionalSearchUserProvider: T?
    ): SearchUserViewModelImpl {
        val searchUserViewModelImpl =
            SearchUserViewModelImpl(
                MatrixClientViewModelContextImpl(
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(
                                        mapOf(UserId("test", "server") to matrixClientMock)
                                    ) +
                                        module {
                                            searchUserProvider<SearchUserProvider1> { SearchUserProvider1() }
                                            searchUserProvider<SearchUserProvider2> { SearchUserProvider2() }
                                            // dummy implementation to avoid mocking the standard impl
                                            single<SearchUserProvider>(named<HomeserverSearchUserProvider>()) {
                                                object : SearchUserProvider {
                                                    override val providerId: String = "homeserver"
                                                    override val providerDisplayName: String = "Homeserver"
                                                    override val priority: Int = 100
                                                    override val disabledByDefault: Boolean = false

                                                    override val settings: Map<SettingsId, SearchSetting> = emptyMap()

                                                    override suspend fun search(
                                                        searchTerm: String,
                                                        activeAccount: UserId,
                                                        coroutineScope: CoroutineScope,
                                                    ): ProviderSearchResult {
                                                        log.debug { "homeserver search" }
                                                        return ProviderSearchResult.Success(listOf())
                                                    }
                                                }
                                            }
                                            if (additionalSearchUserProvider != null) {
                                                searchUserProvider<T> { additionalSearchUserProvider }
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
        backgroundScope.launch { searchUserViewModelImpl.searchResult.collect() }
        backgroundScope.launch { searchUserViewModelImpl.searchResultList.collect() }
        backgroundScope.launch { searchUserViewModelImpl.providerSettingsList.collect() }
        backgroundScope.launch { searchUserViewModelImpl.isSearching.collect() }
        backgroundScope.launch { searchUserViewModelImpl.providerSearchEnabled.collect() }
        backgroundScope.launch { searchUserViewModelImpl.providerSearchCanBeEnabled.collect() }
        return searchUserViewModelImpl
    }

    class SearchUserProvider1 : SearchUserProvider {
        override val providerId: String = "test-1"
        override val providerDisplayName: String = "Test 1"
        override val priority: Int = 150
        override val disabledByDefault: Boolean = false

        private var city: String? = null
        private var address: String? = null

        val citySetting = SearchSetting("city") { city = it }
        val addressSetting = SearchSetting("address") { address = it }

        override val settings: Map<SettingsId, SearchSetting> =
            mapOf(SettingsIdCity to citySetting, SettingsIdAddress to addressSetting)

        override suspend fun search(
            searchTerm: String,
            activeAccount: UserId,
            coroutineScope: CoroutineScope,
        ): ProviderSearchResult {
            log.debug { "test-1 search" }
            return when (searchTerm) {
                "u",
                "" -> { // "" for testing filter settings
                    if (city == null || city == "Berlin") {
                        ProviderSearchResult.Success(listOf(user1))
                    } else {
                        ProviderSearchResult.Success(listOf())
                    }
                }
                "martinInProvider1" -> {
                    ProviderSearchResult.Success(listOf(martin))
                }
                else -> {
                    ProviderSearchResult.Success(listOf())
                }
            }
        }
    }

    class SearchUserProvider2 : SearchUserProvider {
        override val providerId: String = "test-2"
        override val providerDisplayName: String = "Test 2"
        override val priority: Int = 151
        override val disabledByDefault: Boolean = false

        private var city: String? = null
        private var options: String? = null
        private var color: Color? = null

        val citySetting = SearchSetting("city") { city = it }
        val optionsSetting = SearchSetting("options") { options = it }
        val colorSetting =
            SearchSetting("color", getDisplayValue = { stringValue -> "$stringValue (color)" }) { stringValue ->
                color = stringValue?.let { Color.fromValue(it) }
            }

        override val settings: Map<SettingsId, SearchSetting> =
            mapOf(SettingsIdCity to citySetting, SettingsIdOptions to optionsSetting, SettingsIdColor to colorSetting)

        override suspend fun search(
            searchTerm: String,
            activeAccount: UserId,
            coroutineScope: CoroutineScope,
        ): ProviderSearchResult {
            log.debug { "test-2 search" }
            return if (searchTerm == "u") {
                ProviderSearchResult.Success(listOf(user2, user3))
            } else {
                ProviderSearchResult.Success(listOf())
            }
        }
    }

    class SearchUserProvider3(searchUserProvider: SearchUserProvider) : SearchUserProvider by searchUserProvider {
        override suspend fun search(
            searchTerm: String,
            activeAccount: UserId,
            coroutineScope: CoroutineScope,
        ): ProviderSearchResult {
            log.debug { "test-2' search" }
            return ProviderSearchResult.Success(listOf(martin, alex, merlin))
        }
    }

    class SearchUserProvider4(searchUserProvider: SearchUserProvider) : SearchUserProvider by searchUserProvider {
        override val providerId: SearchUserProviderId = "test-4"
        override val providerDisplayName: String = "Test 4"
        override val priority: Int = 1_000
        override val disabledByDefault: Boolean = true
    }

    class SearchUserProvider5 : SearchUserProvider {
        override val providerId: String = "test-5"
        override val providerDisplayName: String = "Test 5"
        override val priority: Int = 152
        override val disabledByDefault: Boolean = false

        private var different: String? = null

        val differentSetting = SearchSetting("different") { different = it }

        override val settings: Map<SettingsId, SearchSetting> = mapOf(SettingsIdDifferent to differentSetting)

        override suspend fun search(
            searchTerm: String,
            activeAccount: UserId,
            coroutineScope: CoroutineScope,
        ): ProviderSearchResult {
            return ProviderSearchResult.Success(listOf())
        }
    }

    class SearchUserProviderWithResumedSearch : SearchUserProvider {
        override val providerId: String = "test-99"
        override val providerDisplayName: String = "Test 99"
        override val priority: Int = 500
        override val disabledByDefault: Boolean = false
        override val settings: Map<SettingsId, SearchSetting> = emptyMap()

        private val resumeSearch = MutableStateFlow(false)

        fun resumeSearch() {
            resumeSearch.value = true
        }

        override suspend fun search(
            searchTerm: String,
            activeAccount: UserId,
            coroutineScope: CoroutineScope,
        ): ProviderSearchResult {
            log.debug { "test-99 search" }

            resumeSearch.first { it }
            resumeSearch.value = false

            return ProviderSearchResult.Success(listOf(user1))
        }
    }
}

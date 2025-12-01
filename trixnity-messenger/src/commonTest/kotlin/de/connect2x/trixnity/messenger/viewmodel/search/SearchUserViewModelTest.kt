package de.connect2x.trixnity.messenger.viewmodel.search

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.searchUserProvider
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.util.ImmediateDispatcherElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.search.provider.ProviderSearchResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchSetting
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SettingsId
import de.connect2x.trixnity.messenger.viewmodel.search.provider.homeserver.HomeserverSearchUserProvider
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.UserId
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger {}

class SearchUserViewModelTest {

    private val matrixClientMock = mock<MatrixClient>()

    private lateinit var searchUserProvider1: SearchUserProvider1
    private lateinit var searchUserProvider2: SearchUserProvider2

    companion object {
        val user1 = object : UserSearchResult {
            override val id: String = "user-1"
            override val userId: UserId = UserId("user1", "server")
            override val displayName: String = "User 1"
            override val initials: String = "U1"
            override val image: ByteArray? = null
            override val sortingFields: List<Pair<String, String>> = emptyList()
            override fun toString(): String {
                return "(id='$id', userId=$userId, displayName='$displayName')"
            }

        }

        val user2 = object : UserSearchResult {
            override val id: String = "user-2"
            override val userId: UserId = UserId("user2", "server")
            override val displayName: String = "User 2"
            override val initials: String = "U2"
            override val image: ByteArray? = null
            override val sortingFields: List<Pair<String, String>> = emptyList()
            override fun toString(): String {
                return "(id='$id', userId=$userId, displayName='$displayName')"
            }
        }
        val user3 = object : UserSearchResult {
            override val id: String = "user-3"
            override val userId: UserId = UserId("user3", "server")
            override val displayName: String = "User 3"
            override val initials: String = "U3"
            override val image: ByteArray? = null
            override val sortingFields: List<Pair<String, String>> = emptyList()
            override fun toString(): String {
                return "(id='$id', userId=$userId, displayName='$displayName')"
            }
        }

        // displayname match
        val martin = object : UserSearchResult {
            override val id: String = "martin"
            override val userId: UserId = UserId("supertester", "server")
            override val displayName: String = "Martin ST"
            override val initials: String = "MS"
            override val image: ByteArray? = null
            override val sortingFields: List<Pair<String, String>> = emptyList()
            override fun toString(): String {
                return "(id='$id', userId=$userId, displayName='$displayName')"
            }
        }

        // displayname match
        val alex = object : UserSearchResult {
            override val id: String = "alex"
            override val userId: UserId = UserId("native", "server")
            override val displayName: String = "Alex ST"
            override val initials: String = "AS"
            override val image: ByteArray? = null
            override val sortingFields: List<Pair<String, String>> = emptyList()
            override fun toString(): String {
                return "(id='$id', userId=$userId, displayName='$displayName')"
            }
        }

        // userId match
        val merlin = object : UserSearchResult {
            override val id: String = "merlin"
            override val userId: UserId = UserId("star merlin", "server")
            override val displayName: String = "Merlin"
            override val initials: String = "M"
            override val image: ByteArray? = null
            override val sortingFields: List<Pair<String, String>> = emptyList()
            override fun toString(): String {
                return "(id='$id', userId=$userId, displayName='$displayName')"
            }
        }

        // with custom field
        val martinCustom = CustomUserSearchResult(
            id = "martin",
            userId = UserId("martin", "server"),
            displayName = "Martin",
            initials = "M",
            image = null,
            myCustomField = "SuperTester",
        )
    }

    @BeforeTest
    fun setup() {
        searchUserProvider1 = SearchUserProvider1()
        searchUserProvider2 = SearchUserProvider2()
        every { matrixClientMock.userId } returns UserId("test", "server")
    }

    @Test
    fun `should display the correct search options from the providers`() = runTest {
        val cut = searchUserViewModel()
        searchUserProvider1.cityFlow.value = SearchSetting("city", "Berlin")
        searchUserProvider2.optionsFlow.value = SearchSetting("options", "loud")
        searchUserProvider2.colorFlow.value = SearchSetting("color", "grey")
        delay(10.milliseconds)

        cut.providerSettings.value shouldBe "options: loud, color: grey, city: Berlin"
    }

    @Test
    fun `should display the same options from different providers as one`() = runTest {
        val cut = searchUserViewModel()
        searchUserProvider1.cityFlow.value = SearchSetting("city", "Berlin")
        // FIXME
    }

    @Test
    fun `should select all user results`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(user1, user2, user3)
    }

    @Test
    fun `should allow to filter by search providers`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(user1, user2, user3)
        val index = cut.searchUserProviders.indexOf(searchUserProvider1)
        cut.providerSearchActive.value = List(3, { i -> i != index })
        delay(10.milliseconds)
        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(user2, user3)
    }

    @Test
    fun `should search in the search providers`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)

        cut.searchResult.value shouldNotBeNull {
            shouldContainAll(
                listOf(
                    SearchResult(
                        id = "test-1",
                        active = true,
                        providerDisplayName = "Test 1",
                        isLoading = false,
                        providerSearchResult = ProviderSearchResult.Success(
                            listOf(user1)
                        )
                    ),
                    SearchResult(
                        id = "test-2",
                        active = true,
                        providerDisplayName = "Test 2",
                        isLoading = false,
                        providerSearchResult = ProviderSearchResult.Success(
                            listOf(user2, user3)
                        )
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
    fun `should react to setting changes in search providers`() = runTest {
        val cut = searchUserViewModel()
        cut.searchTerm.update("u")
        delay(10.milliseconds)

        searchUserProvider1.cityFlow.value = SearchSetting("city", "Berlin Ost")
        delay(10.milliseconds)
        cut.searchResult.value shouldNotBeNull {
            shouldContainAll(
                listOf(
                    SearchResult(
                        id = "test-1",
                        active = true,
                        providerDisplayName = "Test 1",
                        isLoading = false,
                        providerSearchResult = ProviderSearchResult.Success(listOf()) // user1 is not in Berlin Ost
                    ),
                    SearchResult(
                        id = "test-2",
                        active = true,
                        providerDisplayName = "Test 2",
                        isLoading = false,
                        providerSearchResult = ProviderSearchResult.Success(
                            listOf(user2, user3)
                        )
                    ),
                )
            )
        }

        searchUserProvider1.cityFlow.value = SearchSetting("city", "Berlin")
        delay(10.milliseconds)
        cut.searchResult.value shouldNotBeNull {
            shouldContainAll(
                listOf(
                    SearchResult(
                        id = "test-1",
                        active = true,
                        providerDisplayName = "Test 1",
                        isLoading = false,
                        providerSearchResult = ProviderSearchResult.Success(listOf(user1)) // user1 is in Berlin
                    ),
                    SearchResult(
                        id = "test-2",
                        active = true,
                        providerDisplayName = "Test 2",
                        isLoading = false,
                        providerSearchResult = ProviderSearchResult.Success(
                            listOf(user2, user3)
                        )
                    ),
                )
            )
        }
    }

    @Test
    fun `should consider custom UserSearchResult's sorting fields`() = runTest {
        val additionalSearchUserProvider = SearchUserProvider4(SearchUserProvider1())
        val cut = searchUserViewModel(additionalSearchUserProvider)
        cut.searchTerm.update("super")
        delay(10.milliseconds)

        cut.searchResultList.value shouldNotBeNull {} shouldContainOnly listOf(martinCustom, martin)
    }

    private fun TestScope.searchUserViewModel(): SearchUserViewModelImpl = searchUserViewModel(null)

    private inline fun <reified T : SearchUserProvider> TestScope.searchUserViewModel(additionalSearchUserProvider: T?): SearchUserViewModelImpl {
        val searchUserViewModelImpl = SearchUserViewModelImpl(
            MatrixClientViewModelContextImpl(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        ) + module {
                            searchUserProvider<SearchUserProvider1> { searchUserProvider1 }
                            searchUserProvider<SearchUserProvider2> { searchUserProvider2 }
                            // dummy implementation to avoid mocking the standard impl
                            single<SearchUserProvider>(named<HomeserverSearchUserProvider>()) {
                                object : SearchUserProvider {
                                    override val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
                                    override val providerId: String = "homeserver"
                                    override val providerDisplayName: String = "Homeserver"

                                    override val settings: Map<SettingsId, StateFlow<SearchSetting>> = emptyMap()
                                    override fun applySettings() {}

                                    override suspend fun search(
                                        searchTerm: String,
                                        activeAccount: UserId,
                                        coroutineScope: CoroutineScope
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
                }.koin,
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                userId = UserId("test", "server"),
                coroutineContext = backgroundScope.coroutineContext + ImmediateDispatcherElement(testDispatcher),
            ),
            debounceDuration = Duration.ZERO,
        )
        backgroundScope.launch { searchUserViewModelImpl.searchResult.collect() }
        backgroundScope.launch { searchUserViewModelImpl.searchResultList.collect() }
        backgroundScope.launch { searchUserViewModelImpl.providerSettings.collect() }
        return searchUserViewModelImpl
    }

    class SearchUserProvider1 : SearchUserProvider {
        override val enabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
        override val providerId: String = "test-1"
        override val providerDisplayName: String = "Test 1"

        val cityFlow = MutableStateFlow(SearchSetting("city", null))
        val addressFlow = MutableStateFlow(SearchSetting("address", null))

        override val settings: Map<SettingsId, StateFlow<SearchSetting>> = mapOf(
            "city" to cityFlow,
            "address" to addressFlow,
        )

        override fun applySettings() {}

        override suspend fun search(
            searchTerm: String,
            activeAccount: UserId,
            coroutineScope: CoroutineScope
        ): ProviderSearchResult {
            log.debug { "test-1 search" }
            return if (searchTerm == "u") {
                if (cityFlow.value.value == null || cityFlow.value.value == "Berlin") {
                    ProviderSearchResult.Success(listOf(user1))
                } else {
                    ProviderSearchResult.Success(listOf())
                }
            } else {
                ProviderSearchResult.Success(listOf())
            }
        }
    }

    class SearchUserProvider2 : SearchUserProvider {
        override val enabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
        override val providerId: String = "test-2"
        override val providerDisplayName: String = "Test 2"

        val cityFlow = MutableStateFlow(SearchSetting("city", null))
        val optionsFlow = MutableStateFlow(SearchSetting("address", null))
        val colorFlow = MutableStateFlow(SearchSetting("color", null))

        override val settings: Map<SettingsId, StateFlow<SearchSetting>> = mapOf(
            "city" to cityFlow,
            "options" to optionsFlow,
            "color" to colorFlow,
        )

        override fun applySettings() {}

        override suspend fun search(
            searchTerm: String,
            activeAccount: UserId,
            coroutineScope: CoroutineScope
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
            coroutineScope: CoroutineScope
        ): ProviderSearchResult {
            log.debug { "test-2' search" }
            return ProviderSearchResult.Success(listOf(martin, alex, merlin))
        }
    }

    data class CustomUserSearchResult(
        override val id: String,
        override val userId: UserId,
        override val displayName: String?,
        override val initials: String,
        override val image: ByteArray?,
        val myCustomField: String,
    ) : UserSearchResult {
        override val sortingFields: List<Pair<String, String>> = listOf(::myCustomField.name to myCustomField)
    }

    class SearchUserProvider4(searchUserProvider: SearchUserProvider) : SearchUserProvider by searchUserProvider {
        override suspend fun search(
            searchTerm: String,
            activeAccount: UserId,
            coroutineScope: CoroutineScope
        ): ProviderSearchResult {
            log.debug { "test-2' search" }
            return ProviderSearchResult.Success(listOf(martinCustom, martin))
        }
    }
}

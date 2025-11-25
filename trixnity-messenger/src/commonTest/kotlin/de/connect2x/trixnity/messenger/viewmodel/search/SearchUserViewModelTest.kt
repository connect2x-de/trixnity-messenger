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
            override val userId: UserId = UserId("user1")
            override val displayName: String = "User 1"
            override val initials: String = "U1"
            override val image: ByteArray? = null
        }

        val user2 = object : UserSearchResult {
            override val id: String = "user-2"
            override val userId: UserId = UserId("user2")
            override val displayName: String = "User 2"
            override val initials: String = "U2"
            override val image: ByteArray? = null
        }
        val user3 = object : UserSearchResult {
            override val id: String = "user-3"
            override val userId: UserId = UserId("user3")
            override val displayName: String = "User 3"
            override val initials: String = "U3"
            override val image: ByteArray? = null
        }
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

    private fun TestScope.searchUserViewModel(): SearchUserViewModelImpl {
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
                                    override val hasSettings: Boolean = false
                                    override val settingsDisplay: StateFlow<String?> = MutableStateFlow(null)

                                    override fun applySettings() {}

                                    override val settings: List<Pair<SettingsId, StateFlow<SearchSetting>>> =
                                        emptyList()

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
        backgroundScope.launch { searchUserViewModelImpl.providerSettings.collect() }
        return searchUserViewModelImpl
    }

    class SearchUserProvider1 : SearchUserProvider {
        override val enabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
        override val providerId: String = "test-1"
        override val providerDisplayName: String = "Test 1"
        override val hasSettings: Boolean = false
        override val settingsDisplay: StateFlow<String?> = MutableStateFlow(null)

        override fun applySettings() {}

        val cityFlow = MutableStateFlow(SearchSetting("city", null))
        val addressFlow = MutableStateFlow(SearchSetting("address", null))

        override val settings: List<Pair<SettingsId, StateFlow<SearchSetting>>> = listOf(
            "city" to cityFlow,
            "address" to addressFlow,
        )

        override suspend fun search(
            searchTerm: String,
            activeAccount: UserId,
            coroutineScope: CoroutineScope
        ): ProviderSearchResult {
            log.debug { "test-1 search" }
            return if (cityFlow.value.value == null || cityFlow.value.value == "Berlin") {
                ProviderSearchResult.Success(listOf(user1))
            } else {
                ProviderSearchResult.Success(listOf())
            }
        }
    }

    class SearchUserProvider2 : SearchUserProvider {
        override val enabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
        override val providerId: String = "test-2"
        override val providerDisplayName: String = "Test 2"
        override val hasSettings: Boolean = false
        override val settingsDisplay: StateFlow<String?> = MutableStateFlow(null)

        val cityFlow = MutableStateFlow(SearchSetting("city", null))
        val optionsFlow = MutableStateFlow(SearchSetting("address", null))
        val colorFlow = MutableStateFlow(SearchSetting("color", null))

        override fun applySettings() {}

        override val settings: List<Pair<SettingsId, StateFlow<SearchSetting>>> = listOf(
            "city" to cityFlow,
            "options" to optionsFlow,
            "color" to colorFlow,
        )

        override suspend fun search(
            searchTerm: String,
            activeAccount: UserId,
            coroutineScope: CoroutineScope
        ): ProviderSearchResult {
            log.debug { "test-2 search" }
            return ProviderSearchResult.Success(listOf(user2, user3))
        }
    }
}

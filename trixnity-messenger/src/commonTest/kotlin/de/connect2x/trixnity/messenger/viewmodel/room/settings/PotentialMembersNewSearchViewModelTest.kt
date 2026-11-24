package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.search.provider.SearchFilter
import de.connect2x.trixnity.messenger.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.search.provider.SearchProviderFactory
import de.connect2x.trixnity.messenger.search.provider.SearchProviderResult
import de.connect2x.trixnity.messenger.search.user.UserSearchContext
import de.connect2x.trixnity.messenger.search.user.homeserver.HomeserverSearchProviderFactory
import de.connect2x.trixnity.messenger.search.user.homeserver.HomeserverUserSearchResult
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class PotentialMembersNewSearchViewModelTest {
    private val roomId = RoomId("!room")
    private val me = UserId("test", "server")

    private val joinedUser = searchResult(UserId("joined", "server"))
    private val invitedUser = searchResult(UserId("invited", "server"))
    private val leftUser = searchResult(UserId("left", "server"))
    private val otherUser = searchResult(UserId("other", "server"))

    private val userServiceMock = mock<UserService>()
    private val matrixClientMock =
        mock<MatrixClient> {
            every { di } returns koinApplication { modules(module { single { userServiceMock } }) }.koin
            every { userId } returns me
            every { syncState } returns MutableStateFlow(SyncState.STARTED)
        }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `should not show users who are already in the room`() = runTest {
        val members = MutableStateFlow(roomUsers(Membership.JOIN to joinedUser, Membership.INVITE to invitedUser))
        every { userServiceMock.getAll(roomId) } returns members
        val cut = createPotentialMembersNewSearchViewModel(listOf(joinedUser, invitedUser, leftUser, otherUser))

        cut.userSearchViewModel.searchTerm.update("u")

        cut.userSearchViewModel.searchResultList.first { it == listOf(leftUser, otherUser) }
    }

    @Test
    fun `should show users again after they left the room`() = runTest {
        val members = MutableStateFlow(roomUsers(Membership.JOIN to joinedUser))
        every { userServiceMock.getAll(roomId) } returns members
        val cut = createPotentialMembersNewSearchViewModel(listOf(joinedUser, otherUser))

        cut.userSearchViewModel.searchTerm.update("u")
        cut.userSearchViewModel.searchResultList.first { it == listOf(otherUser) }

        members.value = roomUsers(Membership.LEAVE to joinedUser)

        cut.userSearchViewModel.searchResultList.first { it == listOf(joinedUser, otherUser) }
    }

    @Test
    fun `should report no results when all found users are already in the room`() = runTest {
        every { userServiceMock.getAll(roomId) } returns MutableStateFlow(roomUsers(Membership.JOIN to joinedUser))
        val cut = createPotentialMembersNewSearchViewModel(listOf(joinedUser))
        backgroundScope.launch { cut.userSearchViewModel.searchResultList.collect() }

        cut.userSearchViewModel.searchTerm.update("u")

        cut.userSearchViewModel.noResultsFound.first { it == true }
    }

    @Test
    fun `should not report no results when the only found user was selected`() = runTest {
        every { userServiceMock.getAll(roomId) } returns MutableStateFlow(emptyMap())
        val cut = createPotentialMembersNewSearchViewModel(listOf(otherUser))
        backgroundScope.launch { cut.userSearchViewModel.searchResultList.collect() }

        cut.userSearchViewModel.searchTerm.update("u")
        cut.userSearchViewModel.noResultsFound.first { it == false }

        cut.userSearchViewModel.filterNotSearchResult(otherUser)
        cut.userSearchViewModel.searchResultList.first { it.isEmpty() }

        cut.userSearchViewModel.noResultsFound.value shouldBe false
    }

    private fun roomUsers(vararg users: Pair<Membership, HomeserverUserSearchResult>): Map<UserId, Flow<RoomUser?>> {
        val roomUsers = users.map { (membership, user) ->
            val event =
                StateEvent(
                    MemberEventContent(membership = membership),
                    EventId("\$${user.userId.localpart}"),
                    user.userId,
                    roomId,
                    0,
                    stateKey = user.userId.full,
                )
            RoomUser(roomId, user.userId, user.userId.localpart, event)
        }
        return roomUsers.associate { it.userId to flowOf(it) }
    }

    private fun TestScope.createPotentialMembersNewSearchViewModel(
        searchResults: List<HomeserverUserSearchResult>
    ): PotentialMembersNewSearchViewModel {
        val viewModelContext =
            testMatrixClientViewModelContext(
                di =
                    koinApplication {
                            modules(
                                createTestDefaultTrixnityMessengerModules(mapOf(me to matrixClientMock)) +
                                    module {
                                        single<SearchProviderFactory<*, *>>(named<HomeserverSearchProviderFactory>()) {
                                            FixedSearchProvider.Factory(searchResults)
                                        }
                                    }
                            )
                        }
                        .koin,
                userId = me,
            )
        return PotentialMembersNewSearchViewModelImpl(
            viewModelContext = viewModelContext,
            roomId = roomId,
            potentialMembersViewModel = PotentialMembersViewModelImpl(viewModelContext, roomId),
        )
    }

    private fun searchResult(userId: UserId): HomeserverUserSearchResult {
        return HomeserverUserSearchResult(
            userId = userId,
            displayName = userId.localpart,
            initials = "U",
            image = MutableStateFlow(null),
            presence = MutableStateFlow(null),
        )
    }

    private class FixedSearchProvider(private val results: List<HomeserverUserSearchResult>) :
        SearchProvider<HomeserverUserSearchResult, UserSearchContext> {
        companion object Key : SearchProvider.Key<FixedSearchProvider>

        override val key: Key = Key
        override val displayName: String = "Fixed"
        override val priority: Int = 100
        override val disabledByDefault: Boolean = false
        override val supportedFilters: List<SearchFilter.Key<*>> = emptyList()

        override suspend fun search(
            searchTerm: String,
            filters: List<SearchFilter>,
            searchContext: UserSearchContext,
            coroutineScope: CoroutineScope,
        ): SearchProviderResult<HomeserverUserSearchResult> {
            return SearchProviderResult.Success(results)
        }

        class Factory(private val results: List<HomeserverUserSearchResult>) :
            SearchProviderFactory<HomeserverUserSearchResult, UserSearchContext> {
            override val supports = UserSearchContext::class

            override fun create(account: UserId): SearchProvider<HomeserverUserSearchResult, UserSearchContext> {
                return FixedSearchProvider(results)
            }
        }
    }
}

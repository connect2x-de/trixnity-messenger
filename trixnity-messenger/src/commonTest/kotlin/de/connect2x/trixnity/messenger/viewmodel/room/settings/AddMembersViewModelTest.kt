package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.clientserverapi.client.UserApiClient
import de.connect2x.trixnity.clientserverapi.model.user.SearchUsers
import de.connect2x.trixnity.core.ErrorResponse
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class AddMembersViewModelTest {
    private val roomId = RoomId("!room")

    private val userId1 = UserId("user1", "localhost")
    private val userId2 = UserId("user2", "localhost")
    private val userId3 = UserId("user3", "localhost")
    private val userDisplayName3 = "Peter Hase"

    val matrixClientMock = mock<MatrixClient>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val usersApiClientMock = mock<UserApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    val userServiceMock = mock<UserService>()

    val roomServiceMock = mock<RoomService>()

    private val onBackMock = mock<Function0<Unit>>()

    init {
        resetMocks(
            matrixClientMock,
            matrixClientServerApiClientMock,
            usersApiClientMock,
            roomsApiClientMock,
            userServiceMock,
            onBackMock,
        )
        every { matrixClientMock.di } returns
            koinApplication { modules(module { single { userServiceMock } }, module { single { roomServiceMock } }) }
                .koin
        every { matrixClientMock.syncState } returns MutableStateFlow(SyncState.STARTED)
        every { matrixClientMock.userId } returns userId1
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.user } returns usersApiClientMock
        every { matrixClientServerApiClientMock.room } returns roomsApiClientMock
        every { userServiceMock.getAll(roomId) } returns MutableStateFlow(emptyMap())
        every { userServiceMock.getPresence(any()) } returns flowOf(null)
        every { roomServiceMock.getById(roomId) } returns flowOf(null)
        every { roomServiceMock.getState<HistoryVisibilityEventContent>(roomId, any(), any()) } returns
            flowOf(
                ClientEvent.StrippedStateEvent(
                    HistoryVisibilityEventContent(HistoryVisibilityEventContent.HistoryVisibility.INVITED),
                    sender = UserId("user", "server"),
                    stateKey = "stateKey",
                )
            )
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `remove user - should add user to group list when selected and remove from list when deselected`() = runTest {
        everySuspend { usersApiClientMock.searchUsers("u", any(), any()) } returns
            Result.success(
                SearchUsers.Response(
                    false,
                    listOf(
                        SearchUsers.Response.SearchUser(userId = userId1),
                        SearchUsers.Response.SearchUser(userId = userId2),
                        SearchUsers.Response.SearchUser(userId = userId3),
                    ),
                )
            )
        val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
        val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userDisplayName3, initials = "U")

        val cut = createAddMembersViewModel()
        backgroundScope.launch { cut.canAddMembers.collect {} }
        val searchHandler = cut.potentialMembersViewModel.searchHandler
        backgroundScope.launch { searchHandler.foundUsers.collect {} }
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first { it == listOf(user2, user3) }
        cut.canAddMembers.value shouldBe false
        cut.onUserClick(user2)

        delay(300.milliseconds)
        cut.canAddMembers.value shouldBe true
        cut.groupUsers.value shouldContainExactly listOf(user2)
        searchHandler.foundUsers.value shouldNotContain user2

        cut.removeUserFromGroup(user2)

        delay(300.milliseconds)
        cut.canAddMembers.value shouldBe false
        cut.groupUsers.value shouldBe emptyList()
        searchHandler.foundUsers.value shouldContain user2
    }

    @Test
    fun `select user - should add Members with all selected users and go back to room settings`() = runTest {
        every { onBackMock.invoke() } returns Unit

        everySuspend { roomsApiClientMock.inviteUser(roomId, userId2, null) } returns Result.success(Unit)
        everySuspend { roomsApiClientMock.inviteUser(roomId, userId3, null) } returns Result.success(Unit)
        everySuspend { usersApiClientMock.searchUsers("u", any(), any()) } returns
            Result.success(
                SearchUsers.Response(
                    false,
                    listOf(
                        SearchUsers.Response.SearchUser(userId = userId1),
                        SearchUsers.Response.SearchUser(userId = userId2),
                        SearchUsers.Response.SearchUser(userId = userId3),
                    ),
                )
            )
        val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
        val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userDisplayName3, initials = "U")

        val cut = createAddMembersViewModel()
        val searchHandler = cut.potentialMembersViewModel.searchHandler
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first { it == listOf(user2, user3) }
        cut.onUserClick(user2)
        cut.onUserClick(user3)

        cut.addMembers()

        delay(300.milliseconds)
        verify { onBackMock.invoke() }
        cut.error.value shouldBe null
    }

    @Test
    fun `select user - should show error message when a user cannot be added`() = runTest {
        var onBackWasCalled = false
        every { onBackMock.invoke() } calls { onBackWasCalled = true }

        everySuspend { roomsApiClientMock.inviteUser(roomId, userId2, null) } returns
            Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("403")))
        everySuspend { roomsApiClientMock.inviteUser(roomId, userId3, null) } returns
            Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("403")))
        everySuspend { usersApiClientMock.searchUsers("u", any(), any()) } returns
            Result.success(
                SearchUsers.Response(
                    false,
                    listOf(
                        SearchUsers.Response.SearchUser(userId = userId1),
                        SearchUsers.Response.SearchUser(userId = userId2),
                        SearchUsers.Response.SearchUser(userId = userId3),
                    ),
                )
            )
        val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
        val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userDisplayName3, initials = "U")

        val cut = createAddMembersViewModel()

        val searchHandler = cut.potentialMembersViewModel.searchHandler
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first { it == listOf(user2, user3) }

        cut.onUserClick(user2)
        cut.onUserClick(user3)

        cut.addMembers()
        yield() // uses launch -> need to yield on js

        cut.error.value shouldNotBe null
        onBackWasCalled shouldBe false
    }

    @Test
    fun `undecryptable history info - should show specific undecryptable history info when room has history visibility INVITED`() =
        runTest {
            every { roomServiceMock.getById(roomId) } returns flowOf(Room(RoomId(""), encrypted = true))
            every { roomServiceMock.getState<HistoryVisibilityEventContent>(roomId, any(), any()) } returns
                flowOf(
                    ClientEvent.StrippedStateEvent(
                        HistoryVisibilityEventContent(HistoryVisibilityEventContent.HistoryVisibility.INVITED),
                        sender = UserId("user", "server"),
                        stateKey = "stateKey",
                    )
                )

            val cut = createAddMembersViewModel()
            backgroundScope.launch { cut.undecryptableHistoryInfo.collect {} }
            delay(1.seconds)

            cut.undecryptableHistoryInfo.value shouldBe "Added members cannot see messages from before you invited them"
        }

    @Test
    fun `undecryptable history info - should show specific undecryptable history info when room has history visibility JOINED`() =
        runTest {
            every { roomServiceMock.getById(roomId) } returns flowOf(Room(RoomId(""), encrypted = true))
            every { roomServiceMock.getState<HistoryVisibilityEventContent>(roomId, any(), any()) } returns
                flowOf(
                    ClientEvent.StrippedStateEvent(
                        HistoryVisibilityEventContent(HistoryVisibilityEventContent.HistoryVisibility.JOINED),
                        sender = UserId("user", "server"),
                        stateKey = "stateKey",
                    )
                )

            val cut = createAddMembersViewModel()
            backgroundScope.launch { cut.undecryptableHistoryInfo.collect {} }
            delay(1.seconds)

            cut.undecryptableHistoryInfo.value shouldBe "Added members cannot see messages from before they joined"
        }

    @Test
    fun `undecryptable history info - should show specific undecryptable history info when room has history visibility SHARED`() =
        runTest {
            every { roomServiceMock.getById(roomId) } returns flowOf(Room(RoomId(""), encrypted = true))
            every { roomServiceMock.getState<HistoryVisibilityEventContent>(roomId, any(), any()) } returns
                flowOf(
                    ClientEvent.StrippedStateEvent(
                        HistoryVisibilityEventContent(HistoryVisibilityEventContent.HistoryVisibility.SHARED),
                        sender = UserId("user", "server"),
                        stateKey = "stateKey",
                    )
                )

            val cut = createAddMembersViewModel()
            backgroundScope.launch { cut.undecryptableHistoryInfo.collect {} }
            delay(1.seconds)

            cut.undecryptableHistoryInfo.value shouldBe "Added members cannot see messages from before you invited them"
        }

    @Test
    fun `undecryptable history info - should not show undecryptable history info when room is unencrypted`() = runTest {
        every { roomServiceMock.getById(roomId) } returns flowOf(Room(RoomId(""), encrypted = false))

        val cut = createAddMembersViewModel()
        backgroundScope.launch { cut.undecryptableHistoryInfo.collect {} }
        delay(1.seconds)

        cut.undecryptableHistoryInfo.value shouldBe null
    }

    private fun TestScope.createAddMembersViewModel(): AddMembersViewModel {
        return AddMembersViewModelImpl(
            viewModelContext =
                MatrixClientViewModelContextImpl(
                    componentContext = DefaultComponentContext(LifecycleRegistry()),
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(
                                        mapOf(UserId("test", "server") to matrixClientMock)
                                    )
                                )
                            }
                            .koin,
                    userId = UserId("test", "server"),
                    coroutineContext = backgroundScope.coroutineContext,
                    name = "AddMembers",
                ),
            potentialMembersViewModel = createPotentialMembersViewModel(),
            onBack = onBackMock,
            roomId = roomId,
        )
    }

    private fun TestScope.createPotentialMembersViewModel(): PotentialMembersViewModel {
        return PotentialMembersViewModelImpl(
            viewModelContext =
                MatrixClientViewModelContextImpl(
                    componentContext = DefaultComponentContext(LifecycleRegistry()),
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(
                                        mapOf(UserId("test", "server") to matrixClientMock)
                                    )
                                )
                            }
                            .koin,
                    userId = UserId("test", "server"),
                    coroutineContext = backgroundScope.coroutineContext,
                    name = "PotentialMembers",
                ),
            roomId,
        )
    }
}

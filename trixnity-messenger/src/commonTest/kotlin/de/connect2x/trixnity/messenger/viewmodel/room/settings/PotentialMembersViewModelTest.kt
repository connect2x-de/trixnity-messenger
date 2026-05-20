package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.clientserverapi.client.UserApiClient
import de.connect2x.trixnity.clientserverapi.model.user.SearchUsers
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class PotentialMembersViewModelTest {
    val roomId = RoomId("!room")

    val userId1 = UserId("user1", "localhost")
    val userId2 = UserId("user2", "localhost")
    val userId3 = UserId("user3", "localhost")
    val userId4 = UserId("user1a", "localhost")
    val userId5 = UserId("user5", "localhost")
    val userId6 = UserId("user6", "localhost")

    private val matrixClientMock = mock<MatrixClient>()
    private val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()
    private val usersApiClientMock = mock<UserApiClient>()
    private val roomsApiClientMock = mock<RoomApiClient>()
    private val userServiceMock = mock<UserService>()

    private var syncStateMocker: BlockingAnsweringScope<StateFlow<SyncState>>

    init {
        resetMocks(
            matrixClientMock,
            matrixClientServerApiClientMock,
            usersApiClientMock,
            roomsApiClientMock,
            userServiceMock,
        )
        every { matrixClientMock.di } returns koinApplication { modules(module { single { userServiceMock } }) }.koin
        syncStateMocker = every { matrixClientMock.syncState }
        syncStateMocker returns MutableStateFlow(SyncState.STARTED)
        every { matrixClientMock.userId } returns userId1
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.user } returns usersApiClientMock
        every { matrixClientServerApiClientMock.room } returns roomsApiClientMock
        every { userServiceMock.getPresence(any()) } returns flowOf(null)
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `room has no members » filter users by search term`() = runTest {
        every { userServiceMock.getAll(roomId) } returns MutableStateFlow(emptyMap())
        everySuspend { usersApiClientMock.searchUsers("user1", any(), any()) } returns
            Result.success(SearchUsers.Response(false, listOf(SearchUsers.Response.SearchUser(userId = userId1))))
        everySuspend { usersApiClientMock.searchUsers("us", any(), any()) } returns
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
        everySuspend { usersApiClientMock.searchUsers("user3", any(), any()) } returns
            Result.success(SearchUsers.Response(false, listOf(SearchUsers.Response.SearchUser(userId = userId3))))

        val cut = createPotentialMembersViewModel()
        val searchHandler = cut.searchHandler

        searchHandler.searchTerm.update("us")
        searchHandler.foundUsers.first {
            it ==
                listOf(
                    Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U"),
                    Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U"),
                )
        }
        searchHandler.searchTerm.update("user3")
        searchHandler.foundUsers.first {
            it == listOf(Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U"))
        }
        searchHandler.searchTerm.update("user1")
        searchHandler.foundUsers.first { it == emptyList<SearchUserElement>() }
    }

    fun setupRoomHasMembers() {
        val memberEvent1 =
            StateEvent(
                MemberEventContent(membership = Membership.JOIN),
                EventId("event1"),
                userId1,
                roomId,
                123,
                null,
                "",
            )
        val memberEvent2 =
            StateEvent(
                MemberEventContent(membership = Membership.INVITE),
                EventId("event2"),
                userId1,
                roomId,
                123,
                null,
                "",
            )
        val memberEvent3 =
            StateEvent(
                MemberEventContent(membership = Membership.LEAVE),
                EventId("event3"),
                userId1,
                roomId,
                123,
                null,
                "",
            )

        val roomUser4 = RoomUser(roomId, userId4, "user1a", memberEvent1)
        val roomUser5 = RoomUser(roomId, userId5, "user5", memberEvent2)
        val roomUser6 = RoomUser(roomId, userId6, "user6", memberEvent3)

        every { userServiceMock.getAll(roomId) } returns
            MutableStateFlow(
                mapOf(
                    roomUser4.userId to flowOf(roomUser4),
                    roomUser5.userId to flowOf(roomUser5),
                    roomUser6.userId to flowOf(roomUser6),
                )
            )
    }

    @Test
    fun `room has members » filter users by search term + do not show users who are already in the room`() = runTest {
        setupRoomHasMembers()
        everySuspend { usersApiClientMock.searchUsers("us", any(), any()) } returns
            Result.success(
                SearchUsers.Response(
                    false,
                    listOf(
                        SearchUsers.Response.SearchUser(userId = userId1),
                        SearchUsers.Response.SearchUser(userId = userId2),
                        SearchUsers.Response.SearchUser(userId = userId3),
                        SearchUsers.Response.SearchUser(userId = userId4),
                        SearchUsers.Response.SearchUser(userId = userId5),
                        SearchUsers.Response.SearchUser(userId = userId6),
                    ),
                )
            )
        val cut = createPotentialMembersViewModel()
        val searchHandler = cut.searchHandler

        searchHandler.searchTerm.update("us")
        searchHandler.foundUsers.first {
            it ==
                listOf(
                    Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U"),
                    Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U"),
                    Search.SearchUserElementImpl(userId = userId6, displayName = userId6.full, initials = "U"),
                )
        }
    }

    private fun TestScope.createPotentialMembersViewModel(): PotentialMembersViewModel {
        return PotentialMembersViewModelImpl(
            viewModelContext =
                testMatrixClientViewModelContext(
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
                ),
            roomId = roomId,
        )
    }
}

package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules

import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.Search
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifyNoMoreCalls
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.UserApiClient
import net.folivo.trixnity.clientserverapi.model.users.SearchUsers
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.InitialStateEvent
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test

class CreateNewChatViewModelTest {

    private val userId1 = UserId("user1", "localhost")
    private val userId2 = UserId("user2", "localhost")
    private val userId3 = UserId("user3", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val usersApiClientMock = mock<UserApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    val userServiceMock = mock<UserService>()

    private val onCancelMock = mock<Function0<Unit>>()
    private val onRoomCreatedMock = mock<(UserId, RoomId) -> Unit>()

    init {
        resetMocks(
            matrixClientMock,
            roomServiceMock,
            matrixClientServerApiClientMock,
            usersApiClientMock,
            roomsApiClientMock,
            userServiceMock,
            onCancelMock,
            onRoomCreatedMock,
        )
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { userServiceMock }
                    single { roomServiceMock }
                })
        }.koin
        every { matrixClientMock.userId } returns userId1
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.user } returns usersApiClientMock
        every { matrixClientServerApiClientMock.room } returns roomsApiClientMock
        every { userServiceMock.getPresence(any()) } returns flowOf(null)

        every { onRoomCreatedMock.invoke(any(), any()) } returns Unit
    }

    @Test
    fun `jump to room for users which I have already a direct conversation with`() = runTest {
        var createRoomCalled = false
        everySuspend {
            roomsApiClientMock.createRoom(
                visibility = any(),
                roomAliasId = any(),
                name = any(),
                topic = any(),
                invite = any(),
                inviteThirdPid = any(),
                roomVersion = any(),
                creationContent = any(),
                initialState = any(),
                preset = any(),
                isDirect = any(),
                powerLevelContentOverride = any(),
                asUserId = any(),
            )
        } calls {
            createRoomCalled = true
            Result.failure(RuntimeException("Should not happen!"))
        }
        everySuspend {
            usersApiClientMock.searchUsers(
                searchTerm = "u",
                acceptLanguage = any(),
                limit = any(),
                asUserId = null,
            )
        } returns Result.success(
            SearchUsers.Response(
                false, listOf(
                    SearchUsers.Response.SearchUser(userId = userId1),
                    SearchUsers.Response.SearchUser(userId = userId2),
                    SearchUsers.Response.SearchUser(userId = userId3),
                )
            )
        )
        val roomId = RoomId("!room1")
        every {
            userServiceMock.getAccountData(DirectEventContent::class, any())
        } returns MutableStateFlow(
            DirectEventContent(mapOf(userId2 to setOf(roomId)))
        )
        every {
            userServiceMock.getById(roomId, userId2)
        } returns MutableStateFlow(
            RoomUser(
                roomId, userId2, name = "User2", ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(
                        membership = Membership.JOIN,
                    ),
                    id = EventId("1"),
                    sender = userId1,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
        )
        every { userServiceMock.getAll(roomId) } returns MutableStateFlow(
            mapOf(
                userId1 to flowOf(
                    RoomUser(
                        roomId,
                        userId1,
                        "User1",
                        ClientEvent.RoomEvent.StateEvent(
                            content = MemberEventContent(membership = Membership.JOIN),
                            id = EventId("2"),
                            sender = userId1,
                            roomId = roomId,
                            originTimestamp = 0L,
                            stateKey = userId1.full,
                        )
                    )
                ),
                userId2 to flowOf(
                    RoomUser(
                        roomId,
                        userId2,
                        "User2",
                        ClientEvent.RoomEvent.StateEvent(
                            content = MemberEventContent(membership = Membership.JOIN),
                            id = EventId("3"),
                            sender = userId2,
                            roomId = roomId,
                            originTimestamp = 0L,
                            stateKey = userId2.full,
                        )
                    )
                ),
            )
        )
        every { roomServiceMock.getById(any()) } returns MutableStateFlow(Room(roomId))

        val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
        val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

        val cut = createNewChatViewModel()
        val searchHandler = cut.createNewRoomViewModel.searchHandler
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first {
            it == listOf(user2, user3)
        }

        cut.onUserClick(user2)
        delay(10)
        verify { onRoomCreatedMock.invoke(userId1, roomId) }
        createRoomCalled shouldBe false
    }

    @Test
    fun `create a direct chat with selected user and go to new room`() = runTest {
        every {
            userServiceMock.getAccountData(DirectEventContent::class, any())
        } returns MutableStateFlow(DirectEventContent(emptyMap()))

        everySuspend { usersApiClientMock.searchUsers(any(), any(), any(), null) } returns Result.success(
            SearchUsers.Response(false, listOf())
        )
        val roomId = RoomId("!room1")
        everySuspend {
            roomsApiClientMock.createRoom(
                visibility = any(),
                roomAliasId = any(),
                name = any(),
                topic = any(),
                invite = setOf(userId2),
                inviteThirdPid = any(),
                roomVersion = any(),
                creationContent = any(),
                initialState = listOf(InitialStateEvent(EncryptionEventContent(), "")),
                preset = any(),
                isDirect = true,
                powerLevelContentOverride = any(),
                asUserId = null,
            )
        } returns Result.success(roomId)
        every { roomServiceMock.getById(any()) } returns MutableStateFlow(Room(roomId))

        val cut = createNewChatViewModel()

        val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
        cut.onUserClick(user2)
        delay(10)
        verify { onRoomCreatedMock.invoke(userId1, roomId) }
    }

    @Test
    fun `create a new room if a direct room can be found but not in the room list`() = runTest {
        val roomId = RoomId("!room1")
        var createRoomCalled = false
        everySuspend {
            roomsApiClientMock.createRoom(
                visibility = any(),
                roomAliasId = any(),
                name = any(),
                topic = any(),
                invite = any(),
                inviteThirdPid = any(),
                roomVersion = any(),
                creationContent = any(),
                initialState = any(),
                preset = any(),
                isDirect = any(),
                powerLevelContentOverride = any(),
                asUserId = any(),
            )
        } calls {
            createRoomCalled = true
            Result.success(roomId)
        }
        everySuspend {
            usersApiClientMock.searchUsers(
                searchTerm = "u",
                acceptLanguage = any(),
                limit = any(),
                asUserId = null,
            )
        } returns Result.success(
            SearchUsers.Response(
                false, listOf(
                    SearchUsers.Response.SearchUser(userId = userId1),
                    SearchUsers.Response.SearchUser(userId = userId2),
                    SearchUsers.Response.SearchUser(userId = userId3),
                )
            )
        )
        every {
            userServiceMock.getAccountData(DirectEventContent::class, any())
        } returns MutableStateFlow(
            DirectEventContent(mapOf(userId2 to setOf(roomId)))
        )
        every {
            userServiceMock.getById(roomId, userId2)
        } returns MutableStateFlow(
            RoomUser(
                roomId, userId2, name = "User2", ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(
                        membership = Membership.JOIN,
                    ),
                    id = EventId("1"),
                    sender = userId1,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
        )
        every { roomServiceMock.getById(any()) } returns MutableStateFlow(null) // no local room!

        val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
        val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

        val cut = createNewChatViewModel()
        val searchHandler = cut.createNewRoomViewModel.searchHandler
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first {
            it == listOf(user2, user3)
        }

        cut.onUserClick(user2)
        delay(10)
        createRoomCalled shouldBe true
    }

    @Test
    fun `create a new room if a direct room can be found but other user already left`() = runTest {
        val roomId = RoomId("!room1")
        val existingRoomId = RoomId("!existingRoom")
        everySuspend {
            roomsApiClientMock.createRoom(
                visibility = any(),
                roomAliasId = any(),
                name = any(),
                topic = any(),
                invite = any(),
                inviteThirdPid = any(),
                roomVersion = any(),
                creationContent = any(),
                initialState = any(),
                preset = any(),
                isDirect = any(),
                powerLevelContentOverride = any(),
                asUserId = any(),
            )
        } calls {
            Result.success(roomId)
        }
        everySuspend {
            usersApiClientMock.searchUsers(
                searchTerm = "u",
                acceptLanguage = any(),
                limit = any(),
                asUserId = null,
            )
        } returns Result.success(
            SearchUsers.Response(
                false, listOf(
                    SearchUsers.Response.SearchUser(userId = userId1),
                    SearchUsers.Response.SearchUser(userId = userId2),
                    SearchUsers.Response.SearchUser(userId = userId3),
                )
            )
        )
        every {
            userServiceMock.getAccountData(DirectEventContent::class, any())
        } returns MutableStateFlow(
            DirectEventContent(mapOf(userId2 to setOf(existingRoomId)))
        )
        every {
            userServiceMock.getById(existingRoomId, userId2)
        } returns MutableStateFlow(
            RoomUser(
                existingRoomId, userId2, name = "User2", ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(
                        membership = Membership.LEAVE,
                    ),
                    id = EventId("1"),
                    sender = userId1,
                    roomId = existingRoomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
        )
        every { roomServiceMock.getById(any()) } returns MutableStateFlow(Room(roomId))

        val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
        val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

        val cut = createNewChatViewModel()
        val searchHandler = cut.createNewRoomViewModel.searchHandler
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first {
            it == listOf(user2, user3)
        }

        cut.onUserClick(user2)
        delay(10)
        verifySuspend {
            roomsApiClientMock.createRoom(
                visibility = any(),
                roomAliasId = any(),
                name = any(),
                topic = any(),
                invite = any(),
                inviteThirdPid = any(),
                roomVersion = any(),
                creationContent = any(),
                initialState = any(),
                preset = any(),
                isDirect = any(),
                powerLevelContentOverride = any(),
                asUserId = any(),
            )
        }
        verify { onRoomCreatedMock.invoke(userId1, roomId) }
        verifyNoMoreCalls(onRoomCreatedMock)
    }

    @Test
    fun `create a new room if the users are in a direct chat already but another user is also part of it`() = runTest {
        val roomId = RoomId("!room1")
        val existingRoomId = RoomId("!existingRoom")
        everySuspend {
            roomsApiClientMock.createRoom(
                visibility = any(),
                roomAliasId = any(),
                name = any(),
                topic = any(),
                invite = any(),
                inviteThirdPid = any(),
                roomVersion = any(),
                creationContent = any(),
                initialState = any(),
                preset = any(),
                isDirect = any(),
                powerLevelContentOverride = any(),
                asUserId = any(),
            )
        } calls {
            Result.success(roomId)
        }
        everySuspend {
            usersApiClientMock.searchUsers(
                searchTerm = "u",
                acceptLanguage = any(),
                limit = any(),
                asUserId = null,
            )
        } returns Result.success(
            SearchUsers.Response(
                false, listOf(
                    SearchUsers.Response.SearchUser(userId = userId1),
                    SearchUsers.Response.SearchUser(userId = userId2),
                    SearchUsers.Response.SearchUser(userId = userId3),
                )
            )
        )
        every {
            userServiceMock.getAccountData(DirectEventContent::class, any())
        } returns MutableStateFlow(
            DirectEventContent(mapOf(userId2 to setOf(existingRoomId)))
        )
        every {
            userServiceMock.getById(existingRoomId, userId2)
        } returns MutableStateFlow(
            RoomUser(
                existingRoomId, userId2, name = "User2", ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(
                        membership = Membership.JOIN,
                    ),
                    id = EventId("1"),
                    sender = userId1,
                    roomId = existingRoomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
        )
        every { userServiceMock.getAll(existingRoomId) } returns MutableStateFlow(
            mapOf(
                userId1 to flowOf(
                    RoomUser(
                        existingRoomId,
                        userId1,
                        "User1",
                        ClientEvent.RoomEvent.StateEvent(
                            content = MemberEventContent(membership = Membership.JOIN),
                            id = EventId("2"),
                            sender = userId1,
                            roomId = existingRoomId,
                            originTimestamp = 0L,
                            stateKey = userId1.full,
                        )
                    )
                ),
                userId2 to flowOf(
                    RoomUser(
                        existingRoomId,
                        userId2,
                        "User2",
                        ClientEvent.RoomEvent.StateEvent(
                            content = MemberEventContent(membership = Membership.JOIN),
                            id = EventId("3"),
                            sender = userId2,
                            roomId = existingRoomId,
                            originTimestamp = 0L,
                            stateKey = userId2.full,
                        )
                    )
                ),
                userId3 to flowOf(
                    RoomUser(
                        existingRoomId,
                        userId3,
                        "User3",
                        ClientEvent.RoomEvent.StateEvent(
                            content = MemberEventContent(membership = Membership.JOIN),
                            id = EventId("4"),
                            sender = userId3,
                            roomId = existingRoomId,
                            originTimestamp = 0L,
                            stateKey = userId3.full,
                        )
                    )
                )
            )
        )
        every { roomServiceMock.getById(any()) } returns MutableStateFlow(Room(roomId))

        val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
        val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

        val cut = createNewChatViewModel()
        val searchHandler = cut.createNewRoomViewModel.searchHandler
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first {
            it == listOf(user2, user3)
        }

        cut.onUserClick(user2)
        delay(10)
        verifySuspend {
            roomsApiClientMock.createRoom(
                visibility = any(),
                roomAliasId = any(),
                name = any(),
                topic = any(),
                invite = any(),
                inviteThirdPid = any(),
                roomVersion = any(),
                creationContent = any(),
                initialState = any(),
                preset = any(),
                isDirect = any(),
                powerLevelContentOverride = any(),
                asUserId = any(),
            )
        }
        verify { onRoomCreatedMock.invoke(userId1, roomId) }
        verifyNoMoreCalls(onRoomCreatedMock)
    }

    @Test
    fun `multiple direct rooms exist and one has only the other user so directly jump to this room`() = runTest {
        var createRoomCalled = false
        val roomId = RoomId("!room1")
        val existingRoom1Id = RoomId("!existingRoom1")
        val existingRoom2Id = RoomId("!existingRoom2")
        everySuspend {
            roomsApiClientMock.createRoom(
                visibility = any(),
                roomAliasId = any(),
                name = any(),
                topic = any(),
                invite = any(),
                inviteThirdPid = any(),
                roomVersion = any(),
                creationContent = any(),
                initialState = any(),
                preset = any(),
                isDirect = any(),
                powerLevelContentOverride = any(),
                asUserId = any(),
            )
        } calls {
            createRoomCalled = true
            Result.failure(RuntimeException("Should not happen!"))
        }
        everySuspend {
            usersApiClientMock.searchUsers(
                searchTerm = "u",
                acceptLanguage = any(),
                limit = any(),
                asUserId = null,
            )
        } returns Result.success(
            SearchUsers.Response(
                false, listOf(
                    SearchUsers.Response.SearchUser(userId = userId1),
                    SearchUsers.Response.SearchUser(userId = userId2),
                    SearchUsers.Response.SearchUser(userId = userId3),
                )
            )
        )
        every {
            userServiceMock.getAccountData(DirectEventContent::class, any())
        } returns MutableStateFlow(
            DirectEventContent(mapOf(userId2 to setOf(existingRoom1Id, existingRoom2Id)))
        )
        every {
            userServiceMock.getById(existingRoom1Id, userId2)
        } returns MutableStateFlow(
            RoomUser(
                existingRoom1Id, userId2, name = "User2", ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(
                        membership = Membership.JOIN,
                    ),
                    id = EventId("1"),
                    sender = userId1,
                    roomId = existingRoom1Id,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
        )
        every {
            userServiceMock.getById(existingRoom2Id, userId2)
        } returns MutableStateFlow(
            RoomUser(
                existingRoom1Id, userId2, name = "User2", ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(
                        membership = Membership.JOIN,
                    ),
                    id = EventId("1"),
                    sender = userId1,
                    roomId = existingRoom2Id,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
        )
        every { userServiceMock.getAll(existingRoom1Id) } returns MutableStateFlow(
            mapOf(
                userId1 to flowOf(
                    RoomUser(
                        existingRoom1Id,
                        userId1,
                        "User1",
                        ClientEvent.RoomEvent.StateEvent(
                            content = MemberEventContent(membership = Membership.JOIN),
                            id = EventId("2"),
                            sender = userId1,
                            roomId = existingRoom1Id,
                            originTimestamp = 0L,
                            stateKey = userId1.full,
                        )
                    )
                ),
                userId2 to flowOf(
                    RoomUser(
                        existingRoom1Id,
                        userId2,
                        "User2",
                        ClientEvent.RoomEvent.StateEvent(
                            content = MemberEventContent(membership = Membership.JOIN),
                            id = EventId("3"),
                            sender = userId2,
                            roomId = existingRoom1Id,
                            originTimestamp = 0L,
                            stateKey = userId2.full,
                        )
                    )
                ),
                userId3 to flowOf(
                    RoomUser(
                        existingRoom1Id,
                        userId3,
                        "User3",
                        ClientEvent.RoomEvent.StateEvent(
                            content = MemberEventContent(membership = Membership.JOIN),
                            id = EventId("4"),
                            sender = userId3,
                            roomId = existingRoom1Id,
                            originTimestamp = 0L,
                            stateKey = userId3.full,
                        )
                    )
                )
            )
        )
        every { userServiceMock.getAll(existingRoom2Id) } returns MutableStateFlow(
            mapOf(
                userId1 to flowOf(
                    RoomUser(
                        existingRoom2Id,
                        userId1,
                        "User1",
                        ClientEvent.RoomEvent.StateEvent(
                            content = MemberEventContent(membership = Membership.JOIN),
                            id = EventId("2"),
                            sender = userId1,
                            roomId = existingRoom2Id,
                            originTimestamp = 0L,
                            stateKey = userId1.full,
                        )
                    )
                ),
                userId2 to flowOf(
                    RoomUser(
                        existingRoom2Id,
                        userId2,
                        "User2",
                        ClientEvent.RoomEvent.StateEvent(
                            content = MemberEventContent(membership = Membership.JOIN),
                            id = EventId("3"),
                            sender = userId2,
                            roomId = existingRoom2Id,
                            originTimestamp = 0L,
                            stateKey = userId2.full,
                        )
                    )
                ),
            )
        )
        every { roomServiceMock.getById(any()) } returns MutableStateFlow(Room(roomId))

        val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
        val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

        val cut = createNewChatViewModel()
        val searchHandler = cut.createNewRoomViewModel.searchHandler
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first {
            it == listOf(user2, user3)
        }

        cut.onUserClick(user2)
        delay(10)
        verify { onRoomCreatedMock.invoke(userId1, existingRoom2Id) }
        createRoomCalled shouldBe false
    }

    @Test
    fun `do not reuse existing direct room with other user if this user is not part of it anymore but there are still 2 users in it`() =
        runTest {
            val roomId = RoomId("!room1")
            val existingRoomId = RoomId("!existingRoom")
            everySuspend {
                roomsApiClientMock.createRoom(
                    visibility = any(),
                    roomAliasId = any(),
                    name = any(),
                    topic = any(),
                    invite = any(),
                    inviteThirdPid = any(),
                    roomVersion = any(),
                    creationContent = any(),
                    initialState = any(),
                    preset = any(),
                    isDirect = any(),
                    powerLevelContentOverride = any(),
                    asUserId = any(),
                )
            } calls {
                Result.success(roomId)
            }
            everySuspend {
                usersApiClientMock.searchUsers(
                    searchTerm = "u",
                    acceptLanguage = any(),
                    limit = any(),
                    asUserId = null,
                )
            } returns Result.success(
                SearchUsers.Response(
                    false, listOf(
                        SearchUsers.Response.SearchUser(userId = userId1),
                        SearchUsers.Response.SearchUser(userId = userId2),
                        SearchUsers.Response.SearchUser(userId = userId3),
                    )
                )
            )
            every {
                userServiceMock.getAccountData(DirectEventContent::class, any())
            } returns MutableStateFlow(
                DirectEventContent(mapOf(userId2 to setOf(existingRoomId)))
            )
            every {
                userServiceMock.getById(existingRoomId, userId2)
            } returns MutableStateFlow(
                RoomUser(
                    existingRoomId, userId2, name = "User2", ClientEvent.RoomEvent.StateEvent(
                        content = MemberEventContent(
                            membership = Membership.JOIN,
                        ),
                        id = EventId("1"),
                        sender = userId1,
                        roomId = existingRoomId,
                        originTimestamp = 0L,
                        stateKey = "",
                    )
                )
            )
            every { userServiceMock.getAll(existingRoomId) } returns MutableStateFlow(
                mapOf(
                    userId1 to flowOf(
                        RoomUser(
                            existingRoomId,
                            userId1,
                            "User1",
                            ClientEvent.RoomEvent.StateEvent(
                                content = MemberEventContent(membership = Membership.JOIN),
                                id = EventId("2"),
                                sender = userId1,
                                roomId = existingRoomId,
                                originTimestamp = 0L,
                                stateKey = userId1.full,
                            )
                        )
                    ),
                    userId2 to flowOf(
                        RoomUser(
                            existingRoomId,
                            userId2,
                            "User2",
                            ClientEvent.RoomEvent.StateEvent(
                                content = MemberEventContent(membership = Membership.LEAVE), // <--
                                id = EventId("3"),
                                sender = userId2,
                                roomId = existingRoomId,
                                originTimestamp = 0L,
                                stateKey = userId2.full,
                            )
                        )
                    ),
                    userId3 to flowOf(
                        RoomUser(
                            existingRoomId,
                            userId3,
                            "User3",
                            ClientEvent.RoomEvent.StateEvent(
                                content = MemberEventContent(membership = Membership.JOIN),
                                id = EventId("4"),
                                sender = userId3,
                                roomId = existingRoomId,
                                originTimestamp = 0L,
                                stateKey = userId3.full,
                            )
                        )
                    )
                )
            )
            every { roomServiceMock.getById(any()) } returns MutableStateFlow(Room(roomId))

            val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
            val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

            val cut = createNewChatViewModel()
            val searchHandler = cut.createNewRoomViewModel.searchHandler
            searchHandler.searchTerm.update("u")
            searchHandler.foundUsers.first {
                it == listOf(user2, user3)
            }

            cut.onUserClick(user2)
            delay(10)
            verifySuspend {
                roomsApiClientMock.createRoom(
                    visibility = any(),
                    roomAliasId = any(),
                    name = any(),
                    topic = any(),
                    invite = any(),
                    inviteThirdPid = any(),
                    roomVersion = any(),
                    creationContent = any(),
                    initialState = any(),
                    preset = any(),
                    isDirect = any(),
                    powerLevelContentOverride = any(),
                    asUserId = any(),
                )
            }
            verify { onRoomCreatedMock.invoke(userId1, roomId) }
            verifyNoMoreCalls(onRoomCreatedMock)
        }

    @Test
    fun `ignore direct rooms we have left`() = runTest {
        val roomId = RoomId("!room1")
        val existingRoomId = RoomId("!existingRoom")
        everySuspend {
            roomsApiClientMock.createRoom(
                visibility = any(),
                roomAliasId = any(),
                name = any(),
                topic = any(),
                invite = any(),
                inviteThirdPid = any(),
                roomVersion = any(),
                creationContent = any(),
                initialState = any(),
                preset = any(),
                isDirect = any(),
                powerLevelContentOverride = any(),
                asUserId = any(),
            )
        } calls {
            Result.success(roomId)
        }
        everySuspend {
            usersApiClientMock.searchUsers(
                searchTerm = "u",
                acceptLanguage = any(),
                limit = any(),
                asUserId = null,
            )
        } returns Result.success(
            SearchUsers.Response(
                false, listOf(
                    SearchUsers.Response.SearchUser(userId = userId1),
                    SearchUsers.Response.SearchUser(userId = userId2),
                    SearchUsers.Response.SearchUser(userId = userId3),
                )
            )
        )
        every {
            userServiceMock.getAccountData(DirectEventContent::class, any())
        } returns MutableStateFlow(
            DirectEventContent(mapOf(userId2 to setOf(existingRoomId)))
        )
        every {
            userServiceMock.getById(existingRoomId, userId2)
        } returns MutableStateFlow(
            RoomUser(
                existingRoomId, userId2, name = "User2", ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(
                        membership = Membership.JOIN,
                    ),
                    id = EventId("1"),
                    sender = userId1,
                    roomId = existingRoomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
        )
        every { roomServiceMock.getById(any()) } returns MutableStateFlow(
            Room(roomId, membership = Membership.LEAVE) // <--
        )

        val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
        val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

        val cut = createNewChatViewModel()
        val searchHandler = cut.createNewRoomViewModel.searchHandler
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first {
            it == listOf(user2, user3)
        }

        cut.onUserClick(user2)
        delay(10)
        verifySuspend {
            roomsApiClientMock.createRoom(
                visibility = any(),
                roomAliasId = any(),
                name = any(),
                topic = any(),
                invite = any(),
                inviteThirdPid = any(),
                roomVersion = any(),
                creationContent = any(),
                initialState = any(),
                preset = any(),
                isDirect = any(),
                powerLevelContentOverride = any(),
                asUserId = any(),
            )
        }
        verify { onRoomCreatedMock.invoke(userId1, roomId) }
        verifyNoMoreCalls(onRoomCreatedMock)
    }

    @Test
    fun `display error message when direct message could not be created`() = runTest {
        var cancelWasCalled = false
        every { onCancelMock.invoke() } calls {
            cancelWasCalled = true
        }
        every {
            userServiceMock.getAccountData(DirectEventContent::class, any())
        } returns MutableStateFlow(DirectEventContent(emptyMap()))

        everySuspend { usersApiClientMock.searchUsers(any(), any(), any(), null) } returns Result.success(
            SearchUsers.Response(false, listOf())
        )
        everySuspend {
            roomsApiClientMock.createRoom(
                visibility = any(),
                roomAliasId = any(),
                name = any(),
                topic = any(),
                invite = setOf(userId2),
                inviteThirdPid = any(),
                roomVersion = any(),
                creationContent = any(),
                initialState = listOf(InitialStateEvent(EncryptionEventContent(), "")),
                preset = any(),
                isDirect = true,
                powerLevelContentOverride = any(),
                asUserId = null,
            )
        } returns Result.failure(
            MatrixServerException(
                HttpStatusCode.Forbidden, ErrorResponse.Forbidden("403")
            )
        )
        every { roomServiceMock.getById(any()) } returns MutableStateFlow(Room(RoomId("!a")))

        val cut = createNewChatViewModel()
        val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
        cut.onUserClick(user2)
        delay(10)
        cut.error.value shouldNotBe null
        cancelWasCalled shouldBe false
    }

    private fun TestScope.createNewChatViewModel(): CreateNewChatViewModelImpl {
        return CreateNewChatViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        )
                    )
                }.koin,
                userId = UserId("test", "server"),
            ),
            createNewRoomViewModel = createNewRoomViewModel(),
            onCreateGroup = mock(),
            onSearchGroup = mock(),
            onCancel = onCancelMock,
        )
    }

    private fun TestScope.createNewRoomViewModel(): CreateNewRoomViewModel {
        return CreateNewRoomViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        )
                    )
                }.koin,
                userId = UserId("test", "server"),
            ),
            onRoomCreated = onRoomCreatedMock,
        )
    }
}

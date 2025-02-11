package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifyNoMoreCalls
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.setMain
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

@OptIn(ExperimentalCoroutinesApi::class)
class CreateNewChatViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

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
        Dispatchers.setMain(Dispatchers.Unconfined)
        beforeTest {
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
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns userId1
            every { matrixClientMock.api } returns matrixClientServerApiClientMock
            every { matrixClientServerApiClientMock.user } returns usersApiClientMock
            every { matrixClientServerApiClientMock.room } returns roomsApiClientMock

            every { onRoomCreatedMock.invoke(any(), any()) } returns Unit

        }

        should("jump to room for users which I have already a direct conversation with") {
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
                    searchTerm = eq("u"),
                    acceptLanguage = any(),
                    limit = any(),
                    asUserId = eqNull(),
                )
            } returns
                    Result.success(
                        SearchUsers.Response(
                            false,
                            listOf(
                                SearchUsers.Response.SearchUser(userId = userId1),
                                SearchUsers.Response.SearchUser(userId = userId2),
                                SearchUsers.Response.SearchUser(userId = userId3),
                            )
                        )
                    )
            val roomId = RoomId("room1", "localhost")
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
            verify { onRoomCreatedMock.invoke(userId1, roomId) }
            createRoomCalled shouldBe false
        }

        should("create a direct chat with selected user and go to new room") {
            every {
                userServiceMock.getAccountData(eq(DirectEventContent::class), any())
            } returns MutableStateFlow(DirectEventContent(emptyMap()))

            everySuspend { usersApiClientMock.searchUsers(any(), any(), any(), eqNull()) } returns
                    Result.success(SearchUsers.Response(false, listOf()))
            val roomId = RoomId("room1", "localhost")
            everySuspend {
                roomsApiClientMock.createRoom(
                    visibility = any(),
                    roomAliasId = any(),
                    name = any(),
                    topic = any(),
                    invite = eq(setOf(userId2)),
                    inviteThirdPid = any(),
                    roomVersion = any(),
                    creationContent = any(),
                    initialState = eq(listOf(InitialStateEvent(EncryptionEventContent(), ""))),
                    preset = any(),
                    isDirect = eq(true),
                    powerLevelContentOverride = any(),
                    asUserId = eqNull(),
                )
            } returns Result.success(roomId)
            every { roomServiceMock.getById(any()) } returns MutableStateFlow(Room(roomId))

            val cut = createNewChatViewModel()

            val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
            cut.onUserClick(user2)
            verify { onRoomCreatedMock.invoke(userId1, roomId) }
        }

        should("create a new room if a direct room can be found, but not in the room list") {
            val roomId = RoomId("room1", "localhost")
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
                    searchTerm = eq("u"),
                    acceptLanguage = any(),
                    limit = any(),
                    asUserId = eqNull(),
                )
            } returns
                    Result.success(
                        SearchUsers.Response(
                            false,
                            listOf(
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
            createRoomCalled shouldBe true
        }

        should("create a new room if a direct room can be found, but other user already left") {
            val roomId = RoomId("room1", "localhost")
            val existingRoomId = RoomId("existingRoom", "localhost")
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
                    searchTerm = eq("u"),
                    acceptLanguage = any(),
                    limit = any(),
                    asUserId = eqNull(),
                )
            } returns
                    Result.success(
                        SearchUsers.Response(
                            false,
                            listOf(
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

        should("display error message when direct message could not be created") {
            var cancelWasCalled = false
            every { onCancelMock.invoke() } calls {
                cancelWasCalled = true
            }
            every {
                userServiceMock.getAccountData(eq(DirectEventContent::class), any())
            } returns MutableStateFlow(DirectEventContent(emptyMap()))

            everySuspend { usersApiClientMock.searchUsers(any(), any(), any(), eqNull()) } returns
                    Result.success(SearchUsers.Response(false, listOf()))
            everySuspend {
                roomsApiClientMock.createRoom(
                    visibility = any(),
                    roomAliasId = any(),
                    name = any(),
                    topic = any(),
                    invite = eq(setOf(userId2)),
                    inviteThirdPid = any(),
                    roomVersion = any(),
                    creationContent = any(),
                    initialState = eq(listOf(InitialStateEvent(EncryptionEventContent(), ""))),
                    preset = any(),
                    isDirect = eq(true),
                    powerLevelContentOverride = any(),
                    asUserId = eqNull(),
                )
            } returns Result.failure(
                MatrixServerException(
                    HttpStatusCode.Forbidden,
                    ErrorResponse.Forbidden("403")
                )
            )
            every { roomServiceMock.getById(any()) } returns MutableStateFlow(Room(RoomId("a", "local")))

            val cut = createNewChatViewModel()
            val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
            cut.onUserClick(user2)
            cut.error.value shouldNotBe null
            cancelWasCalled shouldBe false
        }
    }

    private fun createNewChatViewModel(): CreateNewChatViewModelImpl {
        return CreateNewChatViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = Dispatchers.Unconfined
            ),
            createNewRoomViewModel = createNewRoomViewModel(),
            onCreateGroup = mock(),
            onSearchGroup = mock(),
            onCancel = onCancelMock,
        )
    }

    private fun createNewRoomViewModel(): CreateNewRoomViewModel {
        return CreateNewRoomViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = Dispatchers.Unconfined,
            ),
            onRoomCreated = onRoomCreatedMock,
        )
    }
}

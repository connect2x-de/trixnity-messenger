package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
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
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.UserApiClient
import net.folivo.trixnity.clientserverapi.model.users.SearchUsers
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.InitialStateEvent
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.kodein.mock.mockFunction1
import org.kodein.mock.mockFunction2
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class CreateNewChatViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    private val userId1 = UserId("user1", "localhost")
    private val userId2 = UserId("user2", "localhost")
    private val userId3 = UserId("user3", "localhost")

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var matrixClientServerApiClientMock: MatrixClientServerApiClient

    @Mock
    lateinit var usersApiClientMock: UserApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomApiClient

    @Mock
    lateinit var userServiceMock: UserService

    private val onCancelMock = mockFunction0<Unit>(mocker)
    private val goToRoomMock = mockFunction2<Unit, UserId, RoomId>(mocker)

    init {
        Dispatchers.setMain(Dispatchers.Unconfined)
        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
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

                every { goToRoomMock.invoke(isAny(), isAny()) } returns Unit
            }
        }

        should("jump to room for users which I have already a direct conversation with") {
            var createRoomCalled = false
            mocker.everySuspending {
                roomsApiClientMock.createRoom(
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny()
                )
            } runs {
                createRoomCalled = true
                Result.failure(RuntimeException("Should not happen!"))
            }
            mocker.everySuspending {
                usersApiClientMock.searchUsers(
                    isEqual("u"),
                    isAny(),
                    isAny(),
                    isNull()
                )
            } returns
                    Result.success(
                        SearchUsers.Response(
                            false,
                            listOf(
                                SearchUsers.Response.SearchUser(userId = userId1),
                                SearchUsers.Response.SearchUser(userId = userId2),
                                SearchUsers.Response.SearchUser(userId = userId3)
                            )
                        )
                    )
            val roomId = RoomId("room1", "localhost")
            mocker.every {
                userServiceMock.getAccountData<DirectEventContent>(isAny(), isAny())
            } returns MutableStateFlow(
                DirectEventContent(mapOf(userId2 to setOf(roomId)))
            )
            mocker.every { roomServiceMock.getById(isAny()) } returns MutableStateFlow(Room(roomId))

            val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
            val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

            val cut = createNewChatViewModel()
            cut.createNewRoomViewModel.userSearchTerm.value = "u"
            cut.createNewRoomViewModel.foundUsers.first {
                it == listOf(user2, user3)
            }

            cut.onUserClick(user2)
            mocker.verify(exhaustive = false) { goToRoomMock.invoke(userId1, roomId) }
            createRoomCalled shouldBe false
        }

        should("create a direct chat with selected user and go to new room") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(DirectEventContent::class), isAny())
            } returns MutableStateFlow(DirectEventContent(emptyMap()))

            mocker.everySuspending { usersApiClientMock.searchUsers(isAny(), isAny(), isAny(), isNull()) } returns
                    Result.success(SearchUsers.Response(false, listOf()))
            val roomId = RoomId("room1", "localhost")
            mocker.everySuspending {
                roomsApiClientMock.createRoom(
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isEqual(setOf(userId2)),
                    isAny(),
                    isAny(),
                    isAny(),
                    isEqual(listOf(InitialStateEvent(EncryptionEventContent(), ""))),
                    isAny(),
                    isEqual(true),
                    isAny(),
                    isNull(),
                )
            } returns Result.success(roomId)
            mocker.every { roomServiceMock.getById(isAny()) } returns MutableStateFlow(Room(roomId))

            val cut = createNewChatViewModel()

            val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
            cut.onUserClick(user2)
            mocker.verify(exhaustive = false) { goToRoomMock.invoke(userId1, roomId) }
        }

        should("create a new room if a direct room can be found, but not in the room list") {
            val roomId = RoomId("room1", "localhost")
            var createRoomCalled = false
            mocker.everySuspending {
                roomsApiClientMock.createRoom(
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny()
                )
            } runs {
                createRoomCalled = true
                Result.success(roomId)
            }
            mocker.everySuspending {
                usersApiClientMock.searchUsers(
                    isEqual("u"),
                    isAny(),
                    isAny(),
                    isNull()
                )
            } returns
                    Result.success(
                        SearchUsers.Response(
                            false,
                            listOf(
                                SearchUsers.Response.SearchUser(userId = userId1),
                                SearchUsers.Response.SearchUser(userId = userId2),
                                SearchUsers.Response.SearchUser(userId = userId3)
                            )
                        )
                    )
            mocker.every {
                userServiceMock.getAccountData<DirectEventContent>(isAny(), isAny())
            } returns MutableStateFlow(
                DirectEventContent(mapOf(userId2 to setOf(roomId)))
            )
            mocker.every { roomServiceMock.getById(isAny()) } returns MutableStateFlow(null) // no local room!

            val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
            val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

            val cut = createNewChatViewModel()
            cut.createNewRoomViewModel.userSearchTerm.value = "u"
            cut.createNewRoomViewModel.foundUsers.first {
                it == listOf(user2, user3)
            }

            cut.onUserClick(user2)
            createRoomCalled shouldBe true
        }

        should("display error message when direct message could not be created") {
            var cancelWasCalled = false
            mocker.every { onCancelMock.invoke() } runs {
                cancelWasCalled = true
            }
            mocker.every {
                userServiceMock.getAccountData(isEqual(DirectEventContent::class), isAny())
            } returns MutableStateFlow(DirectEventContent(emptyMap()))

            mocker.everySuspending { usersApiClientMock.searchUsers(isAny(), isAny(), isAny(), isNull()) } returns
                    Result.success(SearchUsers.Response(false, listOf()))
            mocker.everySuspending {
                roomsApiClientMock.createRoom(
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isEqual(setOf(userId2)),
                    isAny(),
                    isAny(),
                    isAny(),
                    isEqual(listOf(InitialStateEvent(EncryptionEventContent(), ""))),
                    isAny(),
                    isEqual(true),
                    isAny(),
                    isNull(),
                )
            } returns Result.failure(
                MatrixServerException(
                    HttpStatusCode.Forbidden,
                    ErrorResponse.Forbidden("403")
                )
            )
            mocker.every { roomServiceMock.getById(isAny()) } returns MutableStateFlow(Room(RoomId("a", "local")))

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
            onCreateGroup = mockFunction1(mocker),
            onSearchGroup = mockFunction1(mocker),
            onCancel = onCancelMock,
            goToRoom = goToRoomMock,
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
                coroutineContext = Dispatchers.Unconfined
            ),
            HistoryVisibilityEventContent.HistoryVisibility.entries
                .filterNot { it == HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE }
        )
    }
}

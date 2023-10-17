package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
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
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomsApiClient
import net.folivo.trixnity.clientserverapi.client.UsersApiClient
import net.folivo.trixnity.clientserverapi.model.users.SearchUsers
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.InitialStateEvent
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.kodein.mock.mockFunction1
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
    lateinit var matrixClientServerApiClientMock: MatrixClientServerApiClient

    @Mock
    lateinit var usersApiClientMock: UsersApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomsApiClient

    @Mock
    lateinit var userServiceMock: UserService

    private val onCancelMock = mockFunction0<Unit>(mocker)
    private val goToRoomMock = mockFunction1<Unit, RoomId>(mocker)

    init {
        Dispatchers.setMain(testMainDispatcher)
        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { userServiceMock }
                        }
                    )
                }.koin
                every { matrixClientMock.userId } returns userId1
                every { matrixClientMock.api } returns matrixClientServerApiClientMock
                every { matrixClientServerApiClientMock.users } returns usersApiClientMock
                every { matrixClientServerApiClientMock.rooms } returns roomsApiClientMock

                every { goToRoomMock.invoke(isAny()) } returns Unit
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

            val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
            val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

            val cut = createNewChatViewModel()
            cut.createNewRoomViewModel.userSearchTerm.value = "u"
            cut.createNewRoomViewModel.foundUsers.first {
                it == listOf(user2, user3)
            }

            cut.onUserClick(user2)
            mocker.verify(exhaustive = false) { goToRoomMock.invoke(roomId) }
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

            val cut = createNewChatViewModel()

            val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
            cut.onUserClick(user2)
            mocker.verify(exhaustive = false) { goToRoomMock.invoke(roomId) }
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
                        trixnityMessengerModule(),
                        testMatrixClientModule(matrixClientMock),
                    )
                }.koin,
                accountName = "test",
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
                        trixnityMessengerModule(),
                        testMatrixClientModule(matrixClientMock),
                    )
                }.koin,
                accountName = "test",
                coroutineContext = Dispatchers.Unconfined
            ),
        )
    }
}
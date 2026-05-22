package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.clientserverapi.client.UserApiClient
import de.connect2x.trixnity.clientserverapi.model.user.SearchUsers
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class CreateNewRoomViewModelTest {
    private val userId1 = UserId("user1", "localhost")
    private val userId2 = UserId("user2", "localhost")
    private val userId3 = UserId("user3", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val usersApiClientMock = mock<UserApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    val userServiceMock = mock<UserService>()
    val onRoomCreatedMock = mock<(UserId, RoomId) -> Unit>()

    init {
        resetMocks(
            matrixClientMock,
            matrixClientServerApiClientMock,
            userServiceMock,
            usersApiClientMock,
            roomsApiClientMock,
            onRoomCreatedMock,
        )
        every { matrixClientMock.di } returns koinApplication { modules(module { single { userServiceMock } }) }.koin
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
    fun `filter users by search term`() = runTest {
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

        val cut = createNewRoomViewModel()
        val searchHandler = cut.searchHandler

        println("search: 'us'")
        searchHandler.searchTerm.update("us")
        searchHandler.foundUsers.first {
            it ==
                listOf(
                    Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U"),
                    Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U"),
                )
        }
        println("search: 'user3'")
        searchHandler.searchTerm.update("user3")
        searchHandler.foundUsers.first {
            it == listOf(Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U"))
        }
        println("search: 'user1'")
        searchHandler.searchTerm.update("user1")
        searchHandler.foundUsers.first { it == emptyList<SearchUserElement>() }
    }

    private fun TestScope.createNewRoomViewModel(): CreateNewRoomViewModel {
        return CreateNewRoomViewModelImpl(
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
            onRoomCreated = onRoomCreatedMock,
        )
    }
}

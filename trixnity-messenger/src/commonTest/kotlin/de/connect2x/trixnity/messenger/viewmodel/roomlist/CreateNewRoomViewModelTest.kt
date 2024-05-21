package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.core.spec.style.ShouldSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.UserApiClient
import net.folivo.trixnity.clientserverapi.model.users.SearchUsers
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class CreateNewRoomViewModelTest : ShouldSpec() {
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
    lateinit var usersApiClientMock: UserApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomApiClient

    @Mock
    lateinit var userServiceMock: UserService

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
                        }
                    )
                }.koin
                every { matrixClientMock.userId } returns userId1
                every { matrixClientMock.api } returns matrixClientServerApiClientMock
                every { matrixClientServerApiClientMock.users } returns usersApiClientMock
                every { matrixClientServerApiClientMock.room } returns roomsApiClientMock
            }
        }

        should("filter users by search term") {
            mocker.everySuspending {
                usersApiClientMock.searchUsers(
                    isEqual("user1"),
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
                            )
                        )
                    )
            mocker.everySuspending {
                usersApiClientMock.searchUsers(
                    isEqual("us"),
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
            mocker.everySuspending {
                usersApiClientMock.searchUsers(
                    isEqual("user3"),
                    isAny(),
                    isAny(),
                    isNull()
                )
            } returns
                    Result.success(
                        SearchUsers.Response(
                            false,
                            listOf(SearchUsers.Response.SearchUser(userId = userId3))
                        )
                    )

            val cut = createNewRoomViewModel()

            println("search: 'us'")
            cut.userSearchTerm.value = "us"
            cut.foundUsers.first {
                it == listOf(
                    Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U"),
                    Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")
                )
            }
            println("search: 'user3'")
            cut.userSearchTerm.value = "user3"
            cut.foundUsers.first {
                it == listOf(
                    Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")
                )
            }
            println("search: 'user1'")
            cut.userSearchTerm.value = "user1"
            cut.foundUsers.first {
                it == emptyList<SearchUserElement>()
            }
        }
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
        )
    }
}

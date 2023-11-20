package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.clientserverapi.client.UserApiClient
import net.folivo.trixnity.clientserverapi.model.users.SearchUsers
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AddMembersViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 4_000

    val mocker = Mocker()

    private val roomId = RoomId("room", "localhost")

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

    private val onBackMock = mockFunction0<Unit>(mocker)

    private lateinit var syncStateMocker: Mocker.Every<StateFlow<SyncState>>

    init {
        beforeTest {
            Dispatchers.setMain(testMainDispatcher)
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
                syncStateMocker = every { matrixClientMock.syncState }
                syncStateMocker returns MutableStateFlow(SyncState.STARTED)

                every { matrixClientMock.userId } returns userId1
                every { matrixClientMock.api } returns matrixClientServerApiClientMock
                every { matrixClientServerApiClientMock.users } returns usersApiClientMock
                every { matrixClientServerApiClientMock.rooms } returns roomsApiClientMock
                every { userServiceMock.getAll(roomId) } returns MutableStateFlow(emptyMap())
            }
        }

        should("add user to group list when selected and remove from list when deselected") {
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
            val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
            val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

            val cut = createNewAddMembersViewmodel()
            val subscriberJob = launch { cut.canAddMembers.collect {} }
            cut.potentialMembersViewModel.userSearchTerm.value = "u"
            cut.potentialMembersViewModel.foundUsers.first {
                it == listOf(user2, user3)
            }
            cut.canAddMembers.value shouldBe false
            cut.onUserClick(user2)

            eventually(3.seconds) {
                cut.canAddMembers.value shouldBe true
                cut.groupUsers.value shouldContainExactly listOf(user2)
                cut.potentialMembersViewModel.foundUsers.value shouldNotContain user2
            }

            cut.removeUserFromGroup(user2)

            eventually(3.seconds) {
                cut.canAddMembers.value shouldBe false
                cut.groupUsers.value shouldBe emptyList()
                cut.potentialMembersViewModel.foundUsers.value shouldContain user2
            }
            subscriberJob.cancel()
        }

        should("add Members with all selected users and go back to room settings") {
            mocker.every { onBackMock.invoke() } returns Unit

            mocker.everySuspending {
                roomsApiClientMock.inviteUser(
                    isEqual(roomId),
                    isEqual(userId2),
                    isNull(),
                    isNull()
                )
            } returns Result.success(Unit)
            mocker.everySuspending {
                roomsApiClientMock.inviteUser(
                    isEqual(roomId),
                    isEqual(userId3),
                    isNull(),
                    isNull()
                )
            } returns Result.success(Unit)
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
            val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
            val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

            val cut = createNewAddMembersViewmodel()
            cut.potentialMembersViewModel.userSearchTerm.value = "u"
            cut.potentialMembersViewModel.foundUsers.first {
                it == listOf(user2, user3)
            }
            cut.onUserClick(user2)
            cut.onUserClick(user3)

            cut.addMembers()

            eventually(3.seconds) {
                mocker.verify(exhaustive = false) { onBackMock.invoke() }
                cut.error.value shouldBe null
            }
        }

        should("show error message when a user cannot be added") {
            var onBackWasCalled = false
            mocker.every { onBackMock.invoke() } runs {
                onBackWasCalled = true
            }

            mocker.everySuspending {
                roomsApiClientMock.inviteUser(
                    isEqual(roomId),
                    isEqual(userId2),
                    isNull(),
                    isNull()
                )
            } returns Result.failure(
                MatrixServerException(
                    HttpStatusCode.Forbidden,
                    ErrorResponse.Forbidden("403")
                )
            )
            mocker.everySuspending {
                roomsApiClientMock.inviteUser(
                    isEqual(roomId),
                    isEqual(userId3),
                    isNull(),
                    isNull()
                )
            } returns Result.failure(
                MatrixServerException(
                    HttpStatusCode.Forbidden,
                    ErrorResponse.Forbidden("403")
                )
            )
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
            val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
            val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

            val cut = createNewAddMembersViewmodel()
            cut.potentialMembersViewModel.userSearchTerm.value = "u"
            cut.potentialMembersViewModel.foundUsers.first {
                it == listOf(user2, user3)
            }
            cut.onUserClick(user2)
            cut.onUserClick(user3)

            cut.addMembers()

            cut.error.value shouldNotBe null
            onBackWasCalled shouldBe false
        }
    }

    private fun createNewAddMembersViewmodel(): AddMembersViewModel {
        return AddMembersViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        trixnityMessengerModule(),
                        testMatrixClientModule(matrixClientMock),
                    )
                }.koin,
                accountName = "test",
                coroutineContext = Dispatchers.Unconfined,
            ),
            potentialMembersViewModel = potentialMembersViewModel(),
            onBack = onBackMock,
            roomId = roomId
        )
    }


    private fun potentialMembersViewModel(): PotentialMembersViewModel {
        return PotentialMembersViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        trixnityMessengerModule(),
                        testMatrixClientModule(matrixClientMock),
                    )
                }.koin,
                accountName = "test",
                coroutineContext = Dispatchers.Unconfined,
            ),
            roomId
        )
    }
}
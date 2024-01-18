package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.UserApiClient
import net.folivo.trixnity.clientserverapi.model.rooms.CreateRoom
import net.folivo.trixnity.clientserverapi.model.rooms.DirectoryVisibility
import net.folivo.trixnity.clientserverapi.model.users.SearchUsers
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.InitialStateEvent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import org.kodein.mock.*
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CreateNewGroupViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 10_000

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

    private val onBackMock = mockFunction0<Unit>(mocker)
    private val onGroupCreatedMock = mockFunction2<Unit, UserId, RoomId>(mocker)

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
                every { matrixClientServerApiClientMock.rooms } returns roomsApiClientMock
            }
        }

        should("create a room for public + encrypted") {
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
            } returns Result.success(RoomId("room1", "localhost"))
            val cut = createNewGroupViewModel()
            cut.isPrivate.value = false
            cut.isEncrypted.value = true
            eventually(2.seconds) {
                cut.canCreateNewGroup.value shouldBe true
            }
            cut.createNewGroup()

            mocker.verifyWithSuspend(exhaustive = false) {
                roomsApiClientMock.createRoom(
                    isAny(),
                    isAny(),
                    isNull(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isAny(),
                    isEqual(listOf(InitialStateEvent(EncryptionEventContent(), ""))),
                    isEqual(CreateRoom.Request.Preset.PUBLIC),
                    isAny(),
                    isAny(),
                    isNull(),
                )
            }
        }

        should("not allow creation of room when private & unencrypted") {

            val cut = createNewGroupViewModel()
            cut.isPrivate.value = true
            cut.isEncrypted.value = false
            eventually(2.seconds) {
                cut.canCreateNewGroup.value shouldBe false
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

            val cut = createNewGroupViewModel()
            val subscriberJob = launch { cut.canCreateNewGroup.collect {} }
            cut.createNewRoomViewModel.userSearchTerm.value = "u"
            cut.foundUsers.first {
                it == listOf(user2, user3)
            }
            cut.onUserClick(user2)

            eventually(3.seconds) {
                cut.groupUsers.value shouldContainExactly listOf(user2)
                cut.foundUsers.value shouldNotContain user2
            }

            cut.removeUserFromGroup(user2)

            eventually(3.seconds) {
                cut.groupUsers.value shouldBe emptyList()
                cut.foundUsers.value shouldContain user2
            }

            subscriberJob.cancel()
        }

        should("create group with all selected users") {
            mocker.every { onGroupCreatedMock.invoke(isAny(), isAny()) } returns Unit

            val roomId = RoomId("room1", "localhost")
            mocker.everySuspending {
                roomsApiClientMock.createRoom(
                    isEqual(DirectoryVisibility.PRIVATE),
                    isAny(),
                    isAny(),
                    isAny(),
                    isEqual(setOf(userId2, userId3)),
                    isAny(),
                    isAny(),
                    isAny(),
                    isEqual(listOf(InitialStateEvent(EncryptionEventContent(), ""))),
                    isAny(),
                    isEqual(false),
                    isAny(),
                    isNull(),
                )
            } returns Result.success(roomId)
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

            val cut = createNewGroupViewModel()
            cut.createNewRoomViewModel.userSearchTerm.value = "u"
            cut.foundUsers.first {
                it == listOf(user2, user3)
            }
            cut.onUserClick(user2)
            cut.onUserClick(user3)

            cut.createNewGroup()

            eventually(3.seconds) {
                mocker.verify(exhaustive = false) { onGroupCreatedMock.invoke(UserId("test", "server"), roomId) }
            }
        }

        should("show error message when group cannot be created") {
            var groupCreatedWasCalled = false
            mocker.every { onGroupCreatedMock.invoke(isAny(), isAny()) } runs {
                groupCreatedWasCalled = true
            }

            mocker.everySuspending {
                roomsApiClientMock.createRoom(
                    isEqual(DirectoryVisibility.PRIVATE),
                    isAny(),
                    isAny(),
                    isAny(),
                    isEqual(setOf(userId2, userId3)),
                    isAny(),
                    isAny(),
                    isAny(),
                    isEqual(listOf(InitialStateEvent(EncryptionEventContent(), ""))),
                    isAny(),
                    isEqual(false),
                    isAny(),
                    isNull(),
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

            val cut = createNewGroupViewModel()
            cut.createNewRoomViewModel.userSearchTerm.value = "u"
            cut.foundUsers.first {
                it == listOf(user2, user3)
            }
            cut.onUserClick(user2)
            cut.onUserClick(user3)

            cut.createNewGroup()

            cut.error.value shouldNotBe null
            groupCreatedWasCalled shouldBe false
        }
    }

    private fun createNewGroupViewModel(): CreateNewGroupViewModelImpl {
        val createNewGroupViewModelImpl = CreateNewGroupViewModelImpl(
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
            onBack = onBackMock,
            onGroupCreated = onGroupCreatedMock,
        )
        return createNewGroupViewModelImpl
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
        )
    }
}
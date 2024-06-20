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
import dev.mokkery.verifySuspend
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CreateNewGroupViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 10_000

    private val userId1 = UserId("user1", "localhost")
    private val userId2 = UserId("user2", "localhost")
    private val userId3 = UserId("user3", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val usersApiClientMock = mock<UserApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    val userServiceMock = mock<UserService>()

    private val onBackMock = mock<Function0<Unit>>()
    private val onGroupCreatedMock = mock<Function2<UserId, RoomId, Unit>>()

    init {
        Dispatchers.setMain(Dispatchers.Unconfined)
        beforeTest {
            resetMocks(
                matrixClientMock,
                matrixClientServerApiClientMock,
                usersApiClientMock,
                roomsApiClientMock,
                userServiceMock,
                onBackMock,
                onGroupCreatedMock
            )
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { userServiceMock }
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns userId1
            every { matrixClientMock.api } returns matrixClientServerApiClientMock
            every { matrixClientServerApiClientMock.user } returns usersApiClientMock
            every { matrixClientServerApiClientMock.room } returns roomsApiClientMock
        }

        should("create a room for public + encrypted") {
            everySuspend {
                roomsApiClientMock.createRoom(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns Result.success(RoomId("room1", "localhost"))
            val cut = createNewGroupViewModel()
            cut.isPrivate.value = false
            cut.isEncrypted.value = true
            eventually(2.seconds) {
                cut.canCreateNewGroup.value shouldBe true
            }
            cut.createNewGroup()

            verifySuspend {
                roomsApiClientMock.createRoom(
                    any(),
                    any(),
                    eqNull(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    eq(
                        listOf(
                            InitialStateEvent(EncryptionEventContent(), "")
                        )
                    ),
                    eq(CreateRoom.Request.Preset.PUBLIC),
                    any(),
                    any(),
                    eqNull(),
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
            everySuspend {
                usersApiClientMock.searchUsers(
                    eq("u"),
                    any(),
                    any(),
                    eqNull()
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
            val roomId = RoomId("room1", "localhost")
            every {
                onGroupCreatedMock.invoke(any(), any())
            } returns Unit
            everySuspend {
                roomsApiClientMock.createRoom(
                    eq(DirectoryVisibility.PRIVATE),
                    any(),
                    any(),
                    any(),
                    eq(setOf(userId2, userId3)),
                    any(),
                    any(),
                    any(),
                    eq(
                        listOf(
                            InitialStateEvent(EncryptionEventContent(), "")
                        )
                    ),
                    any(),
                    eq(false),
                    any(),
                    eqNull(),
                )
            } returns Result.success(roomId)
            everySuspend {
                usersApiClientMock.searchUsers(
                    eq("u"),
                    any(),
                    any(),
                    eqNull(),
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
                verify {
                    onGroupCreatedMock.invoke(UserId("test", "server"), roomId)
                }
            }
        }

        should("create group with a custom topic") {
            val roomId = RoomId("room1", "localhost")
            val topicText = "This is a room for testing!"
            every {
                onGroupCreatedMock.invoke(any(), any())
            } returns Unit
            everySuspend {
                roomsApiClientMock.createRoom(
                    eq(DirectoryVisibility.PRIVATE),
                    any(),
                    any(),
                    eq(topicText),
                    any(),
                    any(),
                    any(),
                    any(),
                    eq(
                        listOf(
                            InitialStateEvent(EncryptionEventContent(), "")
                        )
                    ),
                    any(),
                    eq(false),
                    any(),
                    eqNull(),
                )
            } returns Result.success(roomId)

            val cut = createNewGroupViewModel()
            cut.optionalGroupTopic.value = topicText
            cut.createNewGroup()

            eventually(3.seconds) {
                verify {
                    onGroupCreatedMock.invoke(UserId("test", "server"), roomId)
                }
            }
        }

        should("show error message when group cannot be created") {
            var groupCreatedWasCalled = false
            every { onGroupCreatedMock.invoke(any(), any()) } calls {
                groupCreatedWasCalled = true
            }

            everySuspend {
                roomsApiClientMock.createRoom(
                    eq(DirectoryVisibility.PRIVATE),
                    any(),
                    any(),
                    any(),
                    eq(setOf(userId2, userId3)),
                    any(),
                    any(),
                    any(),
                    eq(
                        listOf(
                            InitialStateEvent(EncryptionEventContent(), "")
                        )
                    ),
                    any(),
                    eq(false),
                    any(),
                    eqNull(),
                )
            } returns Result.failure(
                MatrixServerException(
                    HttpStatusCode.Forbidden,
                    ErrorResponse.Forbidden("403")
                )
            )
            everySuspend {
                usersApiClientMock.searchUsers(
                    eq("u"),
                    any(),
                    any(),
                    eqNull()
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
            )
        )
    }
}

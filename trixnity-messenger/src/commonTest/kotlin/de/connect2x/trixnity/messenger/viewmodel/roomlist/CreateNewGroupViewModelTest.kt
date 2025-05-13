package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.ImmediateDispatcherElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.testDispatcher
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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
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
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class CreateNewGroupViewModelTest {
    private val userId1 = UserId("user1", "localhost")
    private val userId2 = UserId("user2", "localhost")
    private val userId3 = UserId("user3", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val usersApiClientMock = mock<UserApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    val userServiceMock = mock<UserService>()

    private val onBackMock = mock<Function0<Unit>>()
    private val onRoomCreatedMock = mock<(UserId, RoomId) -> Unit>()

    init {
        resetMocks(
            matrixClientMock,
            matrixClientServerApiClientMock,
            usersApiClientMock,
            roomsApiClientMock,
            userServiceMock,
            onBackMock,
            onRoomCreatedMock
        )
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { userServiceMock }
                })
        }.koin
        every { matrixClientMock.userId } returns userId1
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.user } returns usersApiClientMock
        every { matrixClientServerApiClientMock.room } returns roomsApiClientMock
    }

    @Test
    fun `create a room for public + encrypted`() = runTest {
        everySuspend {
            roomsApiClientMock.createRoom(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } returns Result.success(RoomId("room1", "localhost"))
        val cut = createNewGroupViewModel()
        cut.isPrivate.value = false
        cut.isEncrypted.value = true
        eventually(2.seconds) {
            cut.canCreateNewGroup.value shouldBe true
        }
        cut.createNewGroup()
        yield()

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

    @Test
    fun `not allow creation of room when private and unencrypted`() = runTest {

        val cut = createNewGroupViewModel()
        cut.isPrivate.value = true
        cut.isEncrypted.value = false

        eventually(2.seconds) {
            cut.canCreateNewGroup.value shouldBe false
        }
    }

    @Test
    fun `add user to group list when selected and remove from list when deselected`() = runTest {
        everySuspend {
            usersApiClientMock.searchUsers(
                eq("u"), any(), any(), eqNull()
            )
        } returns Result.success(
            SearchUsers.Response(
                false, listOf(
                    SearchUsers.Response.SearchUser(userId = userId1),
                    SearchUsers.Response.SearchUser(userId = userId2),
                    SearchUsers.Response.SearchUser(userId = userId3)
                )
            )
        )
        val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
        val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

        val cut = createNewGroupViewModel()
        val searchHandler = cut.createNewRoomViewModel.searchHandler
        backgroundScope.launch { cut.canCreateNewGroup.collect {} }
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first {
            it == listOf(user2, user3)
        }
        cut.onUserClick(user2)

        searchHandler.foundUsers.firstOrNull { !it.contains(user2) }

        eventually(3.seconds) {
            cut.groupUsers.value shouldContainExactly listOf(user2)
            searchHandler.foundUsers.value shouldNotContain user2
        }

        cut.removeUserFromGroup(user2)

        searchHandler.foundUsers.firstOrNull { it.contains(user2) }

        eventually(3.seconds) {
            cut.groupUsers.value shouldBe emptyList()
            searchHandler.foundUsers.value shouldContain user2
        }
    }

    @Test
    fun `create group with all selected users`() = runTest {
        val roomId = RoomId("room1", "localhost")
        every {
            onRoomCreatedMock.invoke(any(), any())
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
        val searchHandler = cut.createNewRoomViewModel.searchHandler
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first {
            it == listOf(user2, user3)
        }
        cut.onUserClick(user2)
        cut.onUserClick(user3)
        cut.createNewGroup()

        eventually(3.seconds) {
            verify {
                onRoomCreatedMock.invoke(UserId("test", "server"), roomId)
            }
        }
    }

    @Test
    fun `create group with a custom topic`() = runTest {
        val roomId = RoomId("room1", "localhost")
        val topicText = "This is a room for testing!"
        every {
            onRoomCreatedMock.invoke(any(), any())
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
        cut.canCreateNewGroup.first { it }

        cut.optionalGroupTopic.update(topicText)
        cut.createNewGroup()

        eventually(3.seconds) {
            verify {
                onRoomCreatedMock.invoke(UserId("test", "server"), roomId)
            }
        }
    }

    @Test
    fun `show error message when group cannot be created`() = runTest {
        var groupCreatedWasCalled = false
        every { onRoomCreatedMock.invoke(any(), any()) } calls {
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
                HttpStatusCode.Forbidden, ErrorResponse.Forbidden("403")
            )
        )
        everySuspend {
            usersApiClientMock.searchUsers(
                eq("u"), any(), any(), eqNull()
            )
        } returns Result.success(
            SearchUsers.Response(
                false, listOf(
                    SearchUsers.Response.SearchUser(userId = userId1),
                    SearchUsers.Response.SearchUser(userId = userId2),
                    SearchUsers.Response.SearchUser(userId = userId3)
                )
            )
        )
        val user2 = Search.SearchUserElementImpl(userId = userId2, displayName = userId2.full, initials = "U")
        val user3 = Search.SearchUserElementImpl(userId = userId3, displayName = userId3.full, initials = "U")

        val cut = createNewGroupViewModel()
        val searchHandler = cut.createNewRoomViewModel.searchHandler
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first {
            it == listOf(user2, user3)
        }
        cut.onUserClick(user2)
        cut.onUserClick(user3)

        cut.createNewGroup()
        yield()

        cut.error.value shouldNotBe null
        groupCreatedWasCalled shouldBe false
    }

    private fun TestScope.createNewGroupViewModel(): CreateNewGroupViewModelImpl {
        val createNewGroupViewModelImpl = CreateNewGroupViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        )
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = backgroundScope.coroutineContext + ImmediateDispatcherElement(testDispatcher)
            ),
            createNewRoomViewModel = createNewRoomViewModel(),
            onBack = onBackMock,
        )
        return createNewGroupViewModelImpl
    }


    private fun TestScope.createNewRoomViewModel(): CreateNewRoomViewModel {
        return CreateNewRoomViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        )
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = backgroundScope.coroutineContext + ImmediateDispatcherElement(testDispatcher)
            ),
            onRoomCreated = onRoomCreatedMock,
        )
    }
}

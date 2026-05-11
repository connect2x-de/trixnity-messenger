package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.clientserverapi.client.UserApiClient
import de.connect2x.trixnity.clientserverapi.model.room.CreateRoom
import de.connect2x.trixnity.clientserverapi.model.room.DirectoryVisibility
import de.connect2x.trixnity.clientserverapi.model.user.SearchUsers
import de.connect2x.trixnity.core.ErrorResponse
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.InitialStateEvent
import de.connect2x.trixnity.core.model.events.m.room.EncryptionEventContent
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
        every { userServiceMock.getPresence(any()) } returns flowOf(null)
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `create a room for public + encrypted`() = runTest {
        everySuspend {
            roomsApiClientMock.createRoom(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns Result.success(RoomId("!room1"))
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
                null,
                any(),
                any(),
                any(),
                any(),
                any(),
                listOf(
                    InitialStateEvent(EncryptionEventContent(), "")
                ),
                CreateRoom.Request.Preset.PUBLIC,
                any(),
                any(),
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
                "u", any(), any(),
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
        backgroundScope.launch { searchHandler.foundUsers.collect { } }
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first {
            it == listOf(user2, user3)
        }
        cut.onUserClick(user2)

        eventually(3.seconds) {
            cut.groupUsers.value shouldContainExactly listOf(user2)
            searchHandler.foundUsers.value shouldNotContain user2
        }

        cut.removeUserFromGroup(user2)

        eventually(3.seconds) {
            cut.groupUsers.value shouldBe emptyList()
            searchHandler.foundUsers.value shouldContain user2
        }
    }

    @Test
    fun `create group with all selected users`() = runTest {
        val roomId = RoomId("!room1")
        every {
            onRoomCreatedMock.invoke(any(), any())
        } returns Unit
        everySuspend {
            roomsApiClientMock.createRoom(
                DirectoryVisibility.PRIVATE,
                any(),
                any(),
                any(),
                setOf(userId2, userId3),
                any(),
                any(),
                any(),
                listOf(
                    InitialStateEvent(EncryptionEventContent(), "")
                ),
                any(),
                false,
                any(),
            )
        } returns Result.success(roomId)
        everySuspend {
            usersApiClientMock.searchUsers(
                "u",
                any(),
                any(),
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
        val roomId = RoomId("!room1")
        val topicText = "This is a room for testing!"
        every {
            onRoomCreatedMock.invoke(any(), any())
        } returns Unit
        everySuspend {
            roomsApiClientMock.createRoom(
                DirectoryVisibility.PRIVATE,
                any(),
                any(),
                topicText,
                any(),
                any(),
                any(),
                any(),
                listOf(
                    InitialStateEvent(EncryptionEventContent(), "")
                ),
                any(),
                false,
                any(),
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
                DirectoryVisibility.PRIVATE,
                any(),
                any(),
                any(),
                setOf(userId2, userId3),
                any(),
                any(),
                any(),
                listOf(
                    InitialStateEvent(EncryptionEventContent(), "")
                ),
                any(),
                false,
                any(),
            )
        } returns Result.failure(
            MatrixServerException(
                HttpStatusCode.Forbidden, ErrorResponse.Forbidden("403")
            )
        )
        everySuspend {
            usersApiClientMock.searchUsers(
                "u", any(), any(),
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

    @Test
    fun `test relationship between isPrivate and directoryVisibilityIsPublic`() = runTest {
        val model = createNewGroupViewModel() as CreateNewGroupViewModel

        backgroundScope.launch { model.isPrivate.collect {} }
        backgroundScope.launch { model.directoryVisibilityIsPublic.collect {} }

        eventually(2.seconds) {
            model.isPrivate.value shouldBe true
            model.directoryVisibilityIsPublic.value shouldBe false
        }

        model.setDirectoryVisibilityIsPublic(true)

        eventually(2.seconds) {
            model.isPrivate.value shouldBe true
            model.directoryVisibilityIsPublic.value shouldBe false
        }

        model.setIsPrivate(false)

        eventually(2.seconds) {
            model.isPrivate.value shouldBe false
            model.directoryVisibilityIsPublic.value shouldBe false
        }

        model.setDirectoryVisibilityIsPublic(false)

        eventually(2.seconds) {
            model.isPrivate.value shouldBe false
            model.directoryVisibilityIsPublic.value shouldBe false
        }

        model.setIsPrivate(true)

        eventually(2.seconds) {
            model.isPrivate.value shouldBe true
            model.directoryVisibilityIsPublic.value shouldBe false
        }
    }

    @Test
    fun `directoryVisibilityIsPublic is true results in visibility is PUBLIC`() = runTest {
        everySuspend {
            roomsApiClientMock.createRoom(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns Result.success(RoomId("!room1"))

        val model = createNewGroupViewModel()

        model.directoryVisibilityIsPublic.value = true

        eventually(2.seconds) {
            model.canCreateNewGroup.value shouldBe true
        }

        model.createNewGroup()
        yield()

        verifySuspend {
            roomsApiClientMock.createRoom(
                name = any(),
                topic = any(),
                preset = any(),
                isDirect = any(),
                invite = any(),
                initialState = any(),
                visibility = DirectoryVisibility.PUBLIC,
            )
        }
    }


    @Test
    fun `directoryVisibilityIsPublic is false results in visibility is PRIVATE`() = runTest {
        everySuspend {
            roomsApiClientMock.createRoom(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns Result.success(RoomId("!room1"))

        val model = createNewGroupViewModel()

        model.directoryVisibilityIsPublic.value = false

        eventually(2.seconds) {
            model.canCreateNewGroup.value shouldBe true
        }

        model.createNewGroup()
        yield()

        verifySuspend {
            roomsApiClientMock.createRoom(
                name = any(),
                topic = any(),
                preset = any(),
                isDirect = any(),
                invite = any(),
                initialState = any(),
                visibility = DirectoryVisibility.PRIVATE,
            )
        }
    }

    @Test
    fun `encryption is set to true when the room is public`() = runTest {
        val model = createNewGroupViewModel()
        delay(2.seconds)

        assertEquals(model.isPrivate.value, true)
        assertEquals(model.isEncrypted.value, true)

        model.changeEncryptionStatus(true)
        delay(2.seconds)

        assertEquals(model.isPrivate.value, true)
        assertEquals(model.isEncrypted.value, true)

        // This should change isEncrypted to false since it is the only valid configuration
        model.setIsPrivate(false)
        delay(2.seconds)

        assertEquals(model.isPrivate.value, false)
        assertEquals(model.isEncrypted.value, false)

        // You shouldn't be able to change isEncrypted while the room is public
        model.changeEncryptionStatus(true)
        delay(2.seconds)

        assertEquals(model.isPrivate.value, false)
        assertEquals(model.isEncrypted.value, false)

        // Should enable encryption again
        model.setIsPrivate(true)
        delay(2.seconds)

        assertEquals(model.isPrivate.value, true)
        assertEquals(model.isEncrypted.value, true)

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
                coroutineContext = backgroundScope.coroutineContext,
                name = "NewGroup"
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
                coroutineContext = backgroundScope.coroutineContext,
                name = "NewRoom"
            ),
            onRoomCreated = onRoomCreatedMock,
        )
    }
}

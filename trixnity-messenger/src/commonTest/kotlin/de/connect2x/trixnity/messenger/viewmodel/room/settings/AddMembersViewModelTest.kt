package de.connect2x.trixnity.messenger.viewmodel.room.settings

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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
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
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class AddMembersViewModelTest {
    private val roomId = RoomId("room", "localhost")

    private val userId1 = UserId("user1", "localhost")
    private val userId2 = UserId("user2", "localhost")
    private val userId3 = UserId("user3", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val usersApiClientMock = mock<UserApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    val userServiceMock = mock<UserService>()

    private val onBackMock = mock<Function0<Unit>>()

    init {
        resetMocks(
            matrixClientMock,
            matrixClientServerApiClientMock,
            usersApiClientMock,
            roomsApiClientMock,
            userServiceMock,
            onBackMock
        )
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { userServiceMock }
                })
        }.koin
        every { matrixClientMock.syncState } returns MutableStateFlow(SyncState.STARTED)
        every { matrixClientMock.userId } returns userId1
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.user } returns usersApiClientMock
        every { matrixClientServerApiClientMock.room } returns roomsApiClientMock
        every { userServiceMock.getAll(roomId) } returns MutableStateFlow(emptyMap())
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

        val cut = createNewAddMembersViewmodel()
        val subscriberJob = launch { cut.canAddMembers.collect {} }
        val searchHandler = cut.potentialMembersViewModel.searchHandler
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first {
            it == listOf(user2, user3)
        }
        cut.canAddMembers.value shouldBe false
        cut.onUserClick(user2)

        eventually(3.seconds) {
            cut.canAddMembers.value shouldBe true
            cut.groupUsers.value shouldContainExactly listOf(user2)
            searchHandler.foundUsers.value shouldNotContain user2
        }

        cut.removeUserFromGroup(user2)

        eventually(3.seconds) {
            cut.canAddMembers.value shouldBe false
            cut.groupUsers.value shouldBe emptyList()
            searchHandler.foundUsers.value shouldContain user2
        }
        subscriberJob.cancel()
    }

    @Test
    fun `add Members with all selected users and go back to room settings`() = runTest {
        every { onBackMock.invoke() } returns Unit

        everySuspend {
            roomsApiClientMock.inviteUser(
                eq(roomId), eq(userId2), eqNull(), eqNull()
            )
        } returns Result.success(Unit)
        everySuspend {
            roomsApiClientMock.inviteUser(
                eq(roomId), eq(userId3), eqNull(), eqNull()
            )
        } returns Result.success(Unit)
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

        val cut = createNewAddMembersViewmodel()
        val searchHandler = cut.potentialMembersViewModel.searchHandler
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first {
            it == listOf(user2, user3)
        }
        cut.onUserClick(user2)
        cut.onUserClick(user3)

        cut.addMembers()

        eventually(3.seconds) {
            verify { onBackMock.invoke() }
            cut.error.value shouldBe null
        }
    }

    @Test
    fun `show error message when a user cannot be added`() = runTest {
        var onBackWasCalled = false
        every { onBackMock.invoke() } calls {
            onBackWasCalled = true
        }

        everySuspend {
            roomsApiClientMock.inviteUser(
                eq(roomId), eq(userId2), eqNull(), eqNull()
            )
        } returns Result.failure(
            MatrixServerException(
                HttpStatusCode.Forbidden, ErrorResponse.Forbidden("403")
            )
        )
        everySuspend {
            roomsApiClientMock.inviteUser(
                eq(roomId), eq(userId3), eqNull(), eqNull()
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

        val cut = createNewAddMembersViewmodel()

        val searchHandler = cut.potentialMembersViewModel.searchHandler
        searchHandler.searchTerm.update("u")
        searchHandler.foundUsers.first {
            it == listOf(user2, user3)
        }

        cut.onUserClick(user2)
        cut.onUserClick(user3)

        cut.addMembers()
        yield() // uses launch -> need to yield on js

        cut.error.value shouldNotBe null
        onBackWasCalled shouldBe false
    }

    private fun TestScope.createNewAddMembersViewmodel(): AddMembersViewModel {
        return AddMembersViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        ),
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = backgroundScope.coroutineContext + ImmediateDispatcherElement(testDispatcher),
            ), potentialMembersViewModel = potentialMembersViewModel(), onBack = onBackMock, roomId = roomId
        )
    }


    private fun TestScope.potentialMembersViewModel(): PotentialMembersViewModel {
        return PotentialMembersViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        ),
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = backgroundScope.coroutineContext + ImmediateDispatcherElement(testDispatcher),
            ), roomId
        )
    }
}

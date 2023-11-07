package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.core.spec.style.ShouldSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomsApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.clientserverapi.client.UsersApiClient
import net.folivo.trixnity.clientserverapi.model.users.SearchUsers
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class PotentialMembersViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

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
    lateinit var usersApiClientMock: UsersApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomsApiClient

    @Mock
    lateinit var userServiceMock: UserService

    private lateinit var syncStateMocker: Mocker.Every<StateFlow<SyncState>>

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
                syncStateMocker = every { matrixClientMock.syncState }
                syncStateMocker returns MutableStateFlow(SyncState.STARTED)
                every { matrixClientMock.userId } returns userId1
                every { matrixClientMock.api } returns matrixClientServerApiClientMock
                every { matrixClientServerApiClientMock.users } returns usersApiClientMock
                every { matrixClientServerApiClientMock.rooms } returns roomsApiClientMock
            }
        }

        context("room has no members") {
            beforeTest {
                mocker.every { userServiceMock.getAll(roomId) } returns MutableStateFlow(emptyMap())
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

                val cut = createPotentialMembersViewModel()

                cut.userSearchTerm.value = "us"
                cut.foundUsers.first {
                    it == listOf(
                        Search.SearchUserElementImpl(
                            userId = userId2,
                            displayName = userId2.full,
                            initials = "U"
                        ),
                        Search.SearchUserElementImpl(
                            userId = userId3,
                            displayName = userId3.full,
                            initials = "U"
                        )
                    )
                }
                cut.userSearchTerm.value = "user3"
                cut.foundUsers.first {
                    it == listOf(
                        Search.SearchUserElementImpl(
                            userId = userId3,
                            displayName = userId3.full,
                            initials = "U"
                        )
                    )
                }
                cut.userSearchTerm.value = "user1"
                cut.foundUsers.first {
                    it == emptyList<SearchUserElement>()
                }
            }
        }

        context("room has members") {

            val userId1 = UserId("user1", "localhost")
            val userId2 = UserId("user2", "localhost")
            val userId3 = UserId("user3", "localhost")
            val userId4 = UserId("user1a", "localhost")
            val userId5 = UserId("user5", "localhost")
            val userId6 = UserId("user6", "localhost")

            val memberEvent1 = StateEvent(
                MemberEventContent(membership = Membership.JOIN),
                EventId("event1"),
                userId1,
                roomId,
                123,
                null,
                ""
            )
            val memberEvent2 = StateEvent(
                MemberEventContent(membership = Membership.INVITE),
                EventId("event2"),
                userId1,
                roomId,
                123,
                null,
                ""
            )
            val memberEvent3 = StateEvent(
                MemberEventContent(membership = Membership.LEAVE),
                EventId("event3"),
                userId1,
                roomId,
                123,
                null,
                ""
            )

            val roomUser4 = RoomUser(roomId, userId4, "user1a", memberEvent1)
            val roomUser5 = RoomUser(roomId, userId5, "user5", memberEvent2)
            val roomUser6 = RoomUser(roomId, userId6, "user6", memberEvent3)

            beforeTest {
                mocker.every { userServiceMock.getAll(roomId) } returns MutableStateFlow(
                    mapOf(
                        roomUser4.userId to flowOf(roomUser4),
                        roomUser5.userId to flowOf(roomUser5),
                        roomUser6.userId to flowOf(roomUser6)
                    )
                )
            }

            should("filter users by search term + do not show users who are already in the room") {
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
                                    SearchUsers.Response.SearchUser(userId = userId3),
                                    SearchUsers.Response.SearchUser(userId = userId4),
                                    SearchUsers.Response.SearchUser(userId = userId5),
                                    SearchUsers.Response.SearchUser(userId = userId6),
                                )
                            )
                        )
                val cut = createPotentialMembersViewModel()

                cut.userSearchTerm.value = "us"
                cut.foundUsers.first {
                    it == listOf(
                        Search.SearchUserElementImpl(
                            userId = userId2,
                            displayName = userId2.full,
                            initials = "U"
                        ),
                        Search.SearchUserElementImpl(
                            userId = userId3,
                            displayName = userId3.full,
                            initials = "U"
                        ),
                        Search.SearchUserElementImpl(
                            userId = userId6,
                            displayName = userId6.full,
                            initials = "U"
                        )
                    )
                }
            }
        }
    }


    private fun createPotentialMembersViewModel(): PotentialMembersViewModel {
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
            roomId = roomId
        )
    }
}
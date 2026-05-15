package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.continually
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class JoinRoomActionViewModelTest {

    val lifecycle = LifecycleRegistry()
    val backPressedHandler = BackDispatcher()
    val user = UserId("me", "server")
    val matrixClientMock = mock<MatrixClient>()
    val room = RoomId("room")
    val roomServiceMock = mock<RoomService>()
    val matrixClientApiMock = mock<MatrixClientServerApiClient>()
    val roomApiClientMock = mock<RoomApiClient>()

    val allJoinRules = listOf(
        JoinRule.Public,
        JoinRule.Invite,
        JoinRule.Knock,
        JoinRule.KnockRestricted,
        JoinRule.Restricted,
        JoinRule.Private,
    )

    init {
        resetMocks(
            matrixClientMock,
            roomServiceMock,
            matrixClientApiMock,
            roomApiClientMock
        )
        every { matrixClientMock.di } returns koinApplication {
            modules(module {
                single { roomServiceMock }
                single { matrixClientApiMock }
            })
        }.koin
        every { matrixClientMock.api } returns matrixClientApiMock
        every { matrixClientApiMock.room } returns roomApiClientMock
        every { matrixClientMock.syncState } returns MutableStateFlow(SyncState.STARTED)
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `show join action necessary when room is joinable and no invite exists and join`() = runTest {
        verifyJoinAction(
            JoinRule.Public,
            additionalMocks = {
                everySuspend { roomApiClientMock.joinRoom(room, any(), any(), any()) } returns Result.success(
                    room
                )
            }
        ) {
            eventually(2.seconds) {
                it.actionNecessary.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.Join>()
            }
            (it.actionNecessary.value as? JoinRoomActionViewModel.JoinRoomAction.Join)?.onJoinRoom()
            continually(2.seconds) {
                it.error.value.shouldBeNull()
            }
        }
    }

    @Test
    fun `show knock action necessary when room is knock and no invite exists and knock`() = runTest {
        verifyJoinAction(
            JoinRule.Knock,
            additionalMocks = {
                everySuspend { roomApiClientMock.knockRoom(room, any(), any()) } returns Result.success(room)
            }
        ) {
            eventually(2.seconds) {
                it.actionNecessary.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.Knock>()
                (it.actionNecessary.value as? JoinRoomActionViewModel.JoinRoomAction.Knock)?.onKnock()
                continually(2.seconds) {
                    it.error.value.shouldBeNull()
                }
            }
        }
    }

    @Test
    fun `show knock action necessary when room is knock restricted and no invite exists and knock`() = runTest {
        verifyJoinAction(
            JoinRule.KnockRestricted,
            additionalMocks = {
                everySuspend { roomApiClientMock.knockRoom(room, any(), any()) } returns Result.success(room)
            }
        ) {
            eventually(2.seconds) {
                it.actionNecessary.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.Knock>()
                (it.actionNecessary.value as? JoinRoomActionViewModel.JoinRoomAction.Knock)?.onKnock()
                continually(2.seconds) {
                    it.error.value.shouldBeNull()
                }
            }
        }
    }

    @Test
    fun `show restricted action and correct rooms when room is restricted and no invite exists`() = runTest {
        val restrictedRoom1 = RoomId("needToJoin1")
        val restrictedRoom2 = RoomId("needToJoin2")
        verifyJoinAction(
            JoinRule.Restricted,
            allowCondition = setOf(
                JoinRulesEventContent.AllowCondition(
                    restrictedRoom1,
                    JoinRulesEventContent.AllowCondition.AllowConditionType.RoomMembership
                ),
                JoinRulesEventContent.AllowCondition(
                    restrictedRoom2,
                    JoinRulesEventContent.AllowCondition.AllowConditionType.RoomMembership
                )
            )
        ) {
            eventually(2.seconds) {
                val value = it.actionNecessary.value
                value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.Restricted>()
                value.requiredRooms shouldBe setOf(
                    restrictedRoom1,
                    restrictedRoom2
                )
            }
        }
    }

    @Test
    fun `show joining impossible action when room is invite only and no invite exists`() = runTest {
        verifyJoinAction(
            JoinRule.Invite,
        ) {
            eventually(2.seconds) { it.actionNecessary.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.Impossible>() }
        }
    }

    @Test
    fun `show joining impossible action when room is private and no invite exists`() = runTest {
        verifyJoinAction(
            JoinRule.Private,
        ) {
            eventually(2.seconds) { it.actionNecessary.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.Impossible>() }
        }
    }


    @Test
    fun `show null action when room is already joined and open room`() = allJoinRules.forEach { joinRule ->
        runTest {
            var openRoomCalled = 0
            verifyJoinAction(
                joinRule,
                membership = Membership.JOIN,
                onOpenRoom = { openRoomCalled++ }
            ) {
                continually(2.seconds) { it.actionNecessary.value.shouldBeNull() }
                eventually(2.seconds) { openRoomCalled shouldBe 1 }
            }
        }
    }

    @Test
    fun `show accept invite action necessary when invite exists no matter the join rule and join`() =
        allJoinRules.forEach { joinRule ->
            runTest {
                verifyJoinAction(
                    joinRule,
                    membership = Membership.INVITE,
                    additionalMocks = {
                        everySuspend { roomApiClientMock.joinRoom(room, any(), any()) } returns Result.success(
                            room
                        )
                    }
                ) {
                    eventually(2.seconds) { it.actionNecessary.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.AcceptInvitation>() }
                    (it.actionNecessary.value as? JoinRoomActionViewModel.JoinRoomAction.AcceptInvitation)?.onAcceptInvite()
                    continually(2.seconds) {
                        it.error.value.shouldBeNull()
                    }
                }
            }
        }

    @Test
    fun `update error when join fails`() = runTest {
        verifyJoinAction(
            JoinRule.Public,
            additionalMocks = {
                everySuspend { roomApiClientMock.joinRoom(room, any(), any()) } returns Result.failure(
                    IllegalStateException()
                )
            }) {
            eventually(2.seconds) {
                it.actionNecessary.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.Join>()
            }
            (it.actionNecessary.value as? JoinRoomActionViewModel.JoinRoomAction.Join)?.onJoinRoom()
            eventually(2.seconds) {
                it.error.value.shouldNotBeNull()
            }
        }
    }

    @Test
    fun `update error when knock fails`() = runTest {
        verifyJoinAction(
            JoinRule.Knock,
            additionalMocks = {
                everySuspend { roomApiClientMock.knockRoom(room, any(), any()) } returns Result.failure(
                    IllegalStateException()
                )
            }) {
            eventually(2.seconds) {
                it.actionNecessary.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.Knock>()
            }
            (it.actionNecessary.value as? JoinRoomActionViewModel.JoinRoomAction.Knock)?.onKnock()
            eventually(2.seconds) {
                it.error.value.shouldNotBeNull()
            }
        }
    }


    private suspend fun TestScope.verifyJoinAction(
        joinRule: JoinRule,
        membership: Membership? = null,
        allowCondition: Set<JoinRulesEventContent.AllowCondition>? = null,
        additionalMocks: () -> Unit = {},
        onOpenRoom: (RoomId) -> Unit = {},
        expectedResult: suspend (JoinRoomActionViewModel) -> Unit
    ) {
        every { roomServiceMock.getById(any()) } returns flowOf(
            if (membership == null) null else Room(
                room,
                membership = membership
            )
        )
        every { roomServiceMock.getState<JoinRulesEventContent>(room, any(), any()) } returns flowOf(
            ClientEvent.StrippedStateEvent(
                content = JoinRulesEventContent(
                    joinRule = joinRule,
                    allow = allowCondition
                ),
                sender = user,
                stateKey = "state",
            )
        )
        additionalMocks()
        val cut = JoinRoomActionViewModel(onOpenRoom = onOpenRoom)
        val jobAction = cut.actionNecessary.launchIn(this)
        val jobError = cut.error.launchIn(this)
        expectedResult(cut)
        jobAction.cancel()
        jobError.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.JoinRoomActionViewModel(onOpenRoom: (RoomId) -> Unit = {}): JoinRoomActionViewModel {
        Dispatchers.setMain(testDispatcher)
        return JoinRoomActionViewModelImpl(
            MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(
                    lifecycle = lifecycle,
                    backHandler = backPressedHandler,
                ),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(
                                user to matrixClientMock
                            )
                        )
                    )
                }.koin,
                userId = user,
                coroutineContext = backgroundScope.coroutineContext,
                name = "RoomJoinAction"
            ),
            roomId = room,
            onOpenRoom = onOpenRoom,
            onDismiss = {}
        )
    }
}

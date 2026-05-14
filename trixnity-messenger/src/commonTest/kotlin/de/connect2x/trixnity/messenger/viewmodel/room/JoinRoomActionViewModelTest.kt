package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
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
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
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
            roomServiceMock
        )
        every { matrixClientMock.di } returns koinApplication {
            modules(module {
                single { roomServiceMock }
            })
        }.koin
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `show join action necessary when room is joinable and no invite exists`() = runTest {
        verifyJoinAction(
            JoinRule.Public,
        ) {
            eventually(2.seconds) { it.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.Join>() }
        }
    }

    @Test
    fun `show knock action necessary when room is knock and no invite exists`() = runTest {
        verifyJoinAction(
            JoinRule.Knock,
        ) {
            eventually(2.seconds) { it.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.Knock>() }
        }
    }

    @Test
    fun `show knock action necessary when room is knock restricted and no invite exists`() = runTest {
        verifyJoinAction(
            JoinRule.KnockRestricted,
        ) {
            eventually(2.seconds) { it.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.Knock>() }
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
                val value = it.value
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
            eventually(2.seconds) { it.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.Impossible>() }
        }
    }

    @Test
    fun `show joining impossible action when room is private and no invite exists`() = runTest {
        verifyJoinAction(
            JoinRule.Private,
        ) {
            eventually(2.seconds) { it.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.Impossible>() }
        }
    }


    @Test
    fun `show null action when room is already joined`() = allJoinRules.forEach { joinRule ->
        runTest {
            verifyJoinAction(
                joinRule,
                membership = Membership.JOIN
            ) {
                continually(2.seconds) { it.value.shouldBeNull() }
            }
        }
    }

    @Test
    fun `show accept invite action necessary when invite exists no matter the join rule`() =
        allJoinRules.forEach { joinRule ->
            runTest {
                verifyJoinAction(
                    joinRule,
                    membership = Membership.INVITE
                ) {
                    eventually(2.seconds) { it.value.shouldBeInstanceOf<JoinRoomActionViewModel.JoinRoomAction.AcceptInvitation>() }
                }
            }
        }


    private suspend fun TestScope.verifyJoinAction(
        joinRule: JoinRule,
        membership: Membership? = null,
        allowCondition: Set<JoinRulesEventContent.AllowCondition>? = null,
        expectedResult: suspend (StateFlow<JoinRoomActionViewModel.JoinRoomAction?>) -> Unit
    ) {
        every { roomServiceMock.getById(room) } returns flowOf(
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
        val cut = JoinRoomActionViewModel()
        val job = cut.actionNecessary.launchIn(this)
        expectedResult(cut.actionNecessary)
        job.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.JoinRoomActionViewModel(): JoinRoomActionViewModel {
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
            onOpenRoom = {},
            onDismiss = {}
        )
    }
}

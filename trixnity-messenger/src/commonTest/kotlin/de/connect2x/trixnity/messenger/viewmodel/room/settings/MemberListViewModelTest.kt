package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.key.UserTrustLevel
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.PresenceEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class MemberListViewModelTest : ShouldSpec() {
    private val roomId = RoomId("room", "localhost")

    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    private val roomUserMeFlow = MutableStateFlow(RoomUser(
        roomId,
        me,
        "User1",
        StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            me,
            roomId,
            0L,
            stateKey = ""
        )
    ))

    private val roomUserAliceFlow = MutableStateFlow(RoomUser(
        roomId, alice, "Alice", StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            alice,
            roomId,
            0L,
            stateKey = ""
        )
    ))

    private val roomUserBobFlow = MutableStateFlow(RoomUser(
        roomId, bob, "Bob", StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            bob,
            roomId,
            0L,
            stateKey = ""
        )
    ))

    private val roomUserMapFlow = MutableStateFlow(mutableMapOf<UserId, MutableStateFlow<RoomUser>>())

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val keyServiceMock = mock<KeyService>()

    val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    init {
        coroutineTestScope = true

        beforeTest {
            roomUserMapFlow.value.clear()
            roomUserMapFlow.value = mutableMapOf(
                me to roomUserMeFlow,
                alice to roomUserAliceFlow,
                bob to roomUserBobFlow,
            )

            resetMocks(
                matrixClientMock,
                roomsApiClientMock,
                userServiceMock,
                keyServiceMock,
                matrixClientServerApiMock,
                roomsApiClientMock
            )
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                        single { userServiceMock }
                        single { keyServiceMock }
                    }
                )
            }.koin

            every { matrixClientMock.syncState } returns MutableStateFlow(SyncState.RUNNING)

            every { matrixClientMock.api } returns matrixClientServerApiMock

            every { matrixClientServerApiMock.room } returns roomsApiClientMock

            every { matrixClientMock.userId } returns me

            every { roomServiceMock.getById(eq(roomId)) } returns MutableStateFlow(
                Room(isDirect = true, roomId = roomId)
            )

            every { userServiceMock.getAll(eq(roomId)) } returns roomUserMapFlow
            every { userServiceMock.canKickUser(eq(roomId), any()) } returns MutableStateFlow(true)
            every { userServiceMock.canBanUser(eq(roomId), any()) } returns MutableStateFlow(true)
            every { userServiceMock.canUnbanUser(eq(roomId), any()) } returns MutableStateFlow(true)
            every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns flowOf(50)

            every { userServiceMock.getById(eq(roomId), eq(me)) } returns roomUserMeFlow
            every { userServiceMock.getById(eq(roomId), eq(alice)) } returns roomUserAliceFlow
            every { userServiceMock.getById(eq(roomId), eq(bob)) } returns roomUserBobFlow
            every { userServiceMock.canSetPowerLevelToMax(eq(roomId), any()) } returns MutableStateFlow(1)
            every { userServiceMock.getAccountData(IgnoredUserListEventContent::class) } returns flowOf(
                IgnoredUserListEventContent(emptyMap())
            )

            everySuspend { roomsApiClientMock.banUser(eq(roomId), any(), any(), any()) } calls {
                val userId = (it.args[1] as UserId)
                val roomUserFlow = userServiceMock.getById(roomId, userId) as MutableStateFlow<RoomUser?>
                setMemberEventContentOf(roomUserFlow, MemberEventContent(membership = Membership.BAN,
                    reason = it.args[2] as String))
                Result.success(Unit)
            }
            everySuspend { roomsApiClientMock.unbanUser(eq(roomId), any(), any(), any()) } calls {
                val userId = (it.args[1] as UserId)
                roomUserMapFlow.value.remove(userId) // TODO: Don't work
                Result.success(Unit)
            }

            every { keyServiceMock.getTrustLevel(any()) } returns flowOf(UserTrustLevel.Blocked)
            every { userServiceMock.userPresence } returns MutableStateFlow(
                mapOf(me to PresenceEventContent(Presence.OFFLINE))
            )
        }

        should("ban user from room") {
            val powerLevelsEventContent = PowerLevelsEventContent(users = mapOf(alice to 1, bob to 1, me to 100))
            val powerLevelEvent = StateEvent(
                powerLevelsEventContent,
                EventId("I'm an EventId"),
                sender = me,
                originTimestamp = 123,
                roomId = roomId,
                stateKey = ""
            )
            every { roomServiceMock.getState(roomId, PowerLevelsEventContent::class, "") } returns
                    MutableStateFlow(powerLevelEvent)

            val createEvent = StateEvent(
                CreateEventContent(creator = me),
                EventId("I'm an EventId too"),
                sender = bob,
                originTimestamp = 122,
                roomId = roomId,
                stateKey = ""
            )
            every { roomServiceMock.getState(roomId, CreateEventContent::class, "") } returns
                    MutableStateFlow(createEvent)
            every { userServiceMock.getPowerLevel(eq(roomId), eq(me)) } returns flowOf(100)

            every {
                userServiceMock.getPowerLevel(
                    alice,
                    bob,
                    powerLevelsEventContent = powerLevelsEventContent,
                )
            } returns 1

            every {
                userServiceMock.getPowerLevel(
                    bob,
                    bob,
                    powerLevelsEventContent = powerLevelsEventContent,
                )
            } returns 1

            every {
                userServiceMock.getPowerLevel(
                    me,
                    bob,
                    powerLevelsEventContent = powerLevelsEventContent,
                )
            } returns 100

            val cut = memberListViewModel(coroutineContext)
            launch { cut.memberListElementViewModels.collect() }
            testCoroutineScheduler.advanceUntilIdle()
            val memberListElementViewModel = cut.memberListElementViewModels.value[1].second
            val roomUser = userServiceMock.getById(roomId, memberListElementViewModel.userId) as MutableStateFlow<RoomUser?>
            memberListElementViewModel.banUser("Test reason")
            eventually(2.seconds) {
                requireNotNull(roomUser.value).membership shouldBe Membership.BAN
            }
            setMemberEventContentOf(roomUser, MemberEventContent(membership = Membership.JOIN))
            cancelNeverEndingCoroutines()
        }

        should("unban user from room") {
            val powerLevelsEventContent = PowerLevelsEventContent(users = mapOf(alice to 1, bob to 1, me to 100))
            val powerLevelEvent = StateEvent(
                powerLevelsEventContent,
                EventId("I'm an EventId"),
                sender = me,
                originTimestamp = 123,
                roomId = roomId,
                stateKey = ""
            )
            every { roomServiceMock.getState(roomId, PowerLevelsEventContent::class, "") } returns
                    MutableStateFlow(powerLevelEvent)

            val createEvent = StateEvent(
                CreateEventContent(creator = me),
                EventId("I'm an EventId too"),
                sender = bob,
                originTimestamp = 122,
                roomId = roomId,
                stateKey = ""
            )
            every { roomServiceMock.getState(roomId, CreateEventContent::class, "") } returns
                    MutableStateFlow(createEvent)
            every { userServiceMock.getPowerLevel(eq(roomId), eq(me)) } returns flowOf(100)

            every {
                userServiceMock.getPowerLevel(
                    alice,
                    bob,
                    powerLevelsEventContent = powerLevelsEventContent,
                )
            } returns 1

            every {
                userServiceMock.getPowerLevel(
                    bob,
                    bob,
                    powerLevelsEventContent = powerLevelsEventContent,
                )
            } returns 1

            every {
                userServiceMock.getPowerLevel(
                    me,
                    bob,
                    powerLevelsEventContent = powerLevelsEventContent,
                )
            } returns 100

            val cut = memberListViewModel(coroutineContext)
            launch { cut.memberListElementViewModels.collect() }
            testCoroutineScheduler.advanceUntilIdle()
            val memberListElementViewModel = cut.memberListElementViewModels.value[1].second
            val roomUser = userServiceMock.getById(roomId, memberListElementViewModel.userId) as MutableStateFlow<RoomUser?>
            setMemberEventContentOf(roomUser, MemberEventContent(membership = Membership.BAN))
            memberListElementViewModel.unbanUser(null)

            val allUsers = userServiceMock.getAll(roomId) as MutableStateFlow<Map<UserId, Flow<RoomUser?>>>
            eventually(2.seconds) {
                allUsers.value.size shouldBe 2
            }
            cancelNeverEndingCoroutines()
        }

        should("create List of sorted MemberListElementViewModels after initiation and subscription") {
            val powerLevelsEventContent =
                PowerLevelsEventContent(users = mapOf(alice to 100, bob to 50, me to 1))
            val createEventContent = CreateEventContent(creator = bob)

            val powerLevelEvent = StateEvent(
                powerLevelsEventContent,
                EventId("I'm an EventId"),
                sender = bob,
                originTimestamp = 123,
                roomId = roomId,
                stateKey = ""
            )
            val createEvent = StateEvent(
                createEventContent,
                EventId("I'm an EventId too"),
                sender = bob,
                originTimestamp = 122,
                roomId = roomId,
                stateKey = ""
            )

            every {
                roomServiceMock.getState(
                    roomId,
                    PowerLevelsEventContent::class,
                    ""
                )
            } returns MutableStateFlow(powerLevelEvent)

            every {
                roomServiceMock.getState(
                    roomId,
                    CreateEventContent::class,
                    ""
                )
            } returns MutableStateFlow(createEvent)

            every {
                userServiceMock.getPowerLevel(
                    alice,
                    bob,
                    powerLevelsEventContent = powerLevelsEventContent,
                )
            } returns 100

            every {
                userServiceMock.getPowerLevel(
                    bob,
                    bob,
                    powerLevelsEventContent = powerLevelsEventContent,
                )
            } returns 50

            every {
                userServiceMock.getPowerLevel(
                    me,
                    bob,
                    powerLevelsEventContent = powerLevelsEventContent,
                )
            } returns 1

            val cut = memberListViewModel(coroutineContext)

            launch { cut.memberListElementViewModels.collect() }

            testCoroutineScheduler.advanceUntilIdle()

            cut.memberListElementViewModels.value should containSortedMemberListElementViewModelsFor(
                listOf(alice, bob, me)
            )
            cancelNeverEndingCoroutines()
        }
    }

    private fun setMemberEventContentOf(roomUser: MutableStateFlow<RoomUser?>, eventContent: MemberEventContent) {
        roomUser.value = requireNotNull(roomUser.value).copy(event = StateEvent(
            eventContent,
            EventId(""),
            requireNotNull(roomUser.value).userId,
            roomId,
            0L,
            stateKey = ""
        ))
    }

    private fun containSortedMemberListElementViewModelsFor(userIds: List<UserId>) =
        Matcher<List<Pair<UserId, MemberListElementViewModel>>> { resultList ->
            MatcherResult(
                userIds.foldIndexed(true) { index, acc, userId ->
                    val (_, vm) = resultList.getOrElse(index) { Pair(null, null) }
                    acc && (vm?.userId == userId)
                },
                {
                    "Expecting: " + userIds + "\n" +
                            "but was:   " + resultList.fold(listOf<UserId>()) { acc, (_, vm) ->
                        acc.plus(
                            vm.userId
                        )
                    }
                },
                {
                    "Expecting: " + userIds + "\n" +
                            "but was:   " + resultList.fold(mutableListOf<UserId>()) { acc, pair ->
                        acc.add(
                            pair.first
                        ); acc
                    }
                })
        }


    private suspend fun memberListViewModel(
        coroutineContext: CoroutineContext
    ): MemberListViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return MemberListViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock)),
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext,
            ),
            selectedRoomId = roomId,

            error = MutableStateFlow("")
        )
    }
}

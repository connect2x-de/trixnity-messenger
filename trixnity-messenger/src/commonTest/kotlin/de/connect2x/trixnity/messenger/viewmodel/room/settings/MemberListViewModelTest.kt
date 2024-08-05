package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
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
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
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

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class MemberListViewModelTest : ShouldSpec() {
    private val roomId = RoomId("room", "localhost")

    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val bob = UserId("bob", "localhost")

    private val roomUserMe = RoomUser(
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
    )
    private val roomUserAlice = RoomUser(
        roomId, alice, "Alice", StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            alice,
            roomId,
            0L,
            stateKey = ""
        )
    )

    private val roomUserBob = RoomUser(
        roomId, bob, "Bob", StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            bob,
            roomId,
            0L,
            stateKey = ""
        )
    )

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val keyServiceMock = mock<KeyService>()

    val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    init {
        coroutineTestScope = true

        beforeTest {
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

            every { matrixClientMock.api } returns matrixClientServerApiMock

            every { matrixClientServerApiMock.room } returns roomsApiClientMock

            every { matrixClientMock.userId } returns me

            every { roomServiceMock.getById(eq(roomId)) } returns MutableStateFlow(
                Room(isDirect = true, roomId = roomId)
            )

            every {
                userServiceMock.getAll(eq(roomId))
            } returns MutableStateFlow(
                mapOf(
                    roomUserMe.userId to flowOf(roomUserMe),
                    roomUserAlice.userId to flowOf(roomUserAlice),
                    roomUserBob.userId to flowOf(roomUserBob),
                )
            )
            every { userServiceMock.canKickUser(eq(roomId), any()) } returns
                    MutableStateFlow(true)
            every { userServiceMock.canBanUser(eq(roomId), any()) } returns
                    MutableStateFlow(true)
            every { userServiceMock.getPowerLevel(eq(roomId), any()) } returns
                    MutableStateFlow(50)
            every { userServiceMock.getById(eq(roomId), eq(roomUserMe.userId)) } returns flowOf(roomUserMe)
            every { userServiceMock.getById(eq(roomId), eq(roomUserAlice.userId)) } returns flowOf(roomUserAlice)
            every { userServiceMock.getById(eq(roomId), eq(roomUserBob.userId)) } returns flowOf(roomUserBob)
            every { userServiceMock.canSetPowerLevelToMax(eq(roomId), any()) } returns MutableStateFlow(1)
            every { userServiceMock.getAccountData(IgnoredUserListEventContent::class) } returns flowOf(
                IgnoredUserListEventContent(emptyMap())
            )

            every { keyServiceMock.getTrustLevel(any()) } returns flowOf(UserTrustLevel.Blocked)

            every { userServiceMock.userPresence } returns MutableStateFlow(
                mapOf(me to PresenceEventContent(Presence.OFFLINE))
            )
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

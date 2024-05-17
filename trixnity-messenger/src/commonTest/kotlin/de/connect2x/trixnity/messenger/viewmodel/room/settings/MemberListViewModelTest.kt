package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.key.UserTrustLevel
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.client.user.getAccountData
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
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class MemberListViewModelTest : ShouldSpec() {
    val mocker = Mocker()

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

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var keyServiceMock: KeyService

    @Mock
    lateinit var matrixClientServerApiMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomApiClient

    init {
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
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

                every { roomServiceMock.getById(isEqual(roomId)) } returns MutableStateFlow(
                    Room(isDirect = true, roomId = roomId)
                )

                every {
                    userServiceMock.getAll(isEqual(roomId))
                } returns MutableStateFlow(
                    mapOf(
                        roomUserMe.userId to flowOf(roomUserMe),
                        roomUserAlice.userId to flowOf(roomUserAlice),
                        roomUserBob.userId to flowOf(roomUserBob),
                    )
                )
                every { userServiceMock.canKickUser(isEqual(roomId), isAny()) } returns
                        MutableStateFlow(true)
                every { userServiceMock.getPowerLevel(isEqual(roomId), isAny()) } returns
                        MutableStateFlow(50)
                every { userServiceMock.canSetPowerLevelToMax(isEqual(roomId), isAny()) } returns MutableStateFlow(1)
                every { userServiceMock.getAccountData<IgnoredUserListEventContent>() } returns flowOf(
                    IgnoredUserListEventContent(emptyMap())
                )

                every { keyServiceMock.getTrustLevel(isAny()) } returns flowOf(UserTrustLevel.Blocked)

                every { userServiceMock.userPresence } returns MutableStateFlow(
                    mapOf(me to PresenceEventContent(Presence.OFFLINE))
                )
            }
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

            with(mocker) {
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
            }

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

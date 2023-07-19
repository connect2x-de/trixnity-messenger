package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomDisplayName
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomsApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.EventType
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.*
import net.folivo.trixnity.core.model.push.PushAction.DontNotify
import net.folivo.trixnity.core.model.push.PushAction.Notify
import net.folivo.trixnity.core.model.push.PushCondition
import net.folivo.trixnity.core.model.push.PushRule
import net.folivo.trixnity.core.model.push.PushRuleKind
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomSettingsViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 4_000

    val mocker = Mocker()

    private val roomId = RoomId("room", "localhost")

    private val me = UserId("user1", "localhost")

    private val roomUserMe = RoomUser(
        roomId,
        me,
        "User1",
        Event.StateEvent(
            MemberEventContent(membership = Membership.JOIN),
            EventId(""),
            me,
            roomId,
            0L,
            stateKey = ""
        )
    )

    private val powerLevelsEventContent =
        PowerLevelsEventContent(users = mapOf(me to 100))
    private val createEventContent = CreateEventContent(creator = me)

    private val powerLevelEvent = Event.StateEvent(
        powerLevelsEventContent,
        EventId("I'm an EventId"),
        sender = me,
        originTimestamp = 123,
        roomId = roomId,
        stateKey = ""
    )
    private val createEvent = Event.StateEvent(
        createEventContent,
        EventId("I'm an EventId too"),
        sender = me,
        originTimestamp = 122,
        roomId = roomId,
        stateKey = ""
    )

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var matrixClientServerApiMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomsApiClient

    private lateinit var syncStateMocker: Mocker.Every<StateFlow<SyncState>>
    private lateinit var roomPowerLevel: Mocker.Every<Flow<Event<PowerLevelsEventContent>?>>
    private lateinit var roomGetById: Mocker.Every<Flow<Room?>>
    private val userPowerLevel = MutableStateFlow(50)

    init {
        Dispatchers.setMain(testMainDispatcher)
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
                        }
                    )
                }.koin
                syncStateMocker = every { matrixClientMock.syncState }
                syncStateMocker returns MutableStateFlow(SyncState.STARTED)
                every { matrixClientMock.api } returns matrixClientServerApiMock
                every { matrixClientMock.userId } returns me

                every { matrixClientServerApiMock.rooms } returns roomsApiClientMock

                roomGetById = every { roomServiceMock.getById(roomId) }
                roomGetById returns MutableStateFlow(
                    Room(
                        isDirect = true,
                        roomId = roomId,
                        name = RoomDisplayName(explicitName = "Old name", summary = null),
                    )
                )

                roomPowerLevel = mocker.every {
                    roomServiceMock.getState(
                        roomId,
                        PowerLevelsEventContent::class,
                        ""
                    )
                }
                roomPowerLevel returns MutableStateFlow(powerLevelEvent)
                every {
                    roomServiceMock.getState(
                        roomId,
                        CreateEventContent::class,
                        ""
                    )
                } returns MutableStateFlow(createEvent)

                every {
                    userServiceMock.getPowerLevel(
                        me,
                        powerLevelsEventContent = powerLevelsEventContent,
                        createEventContent = createEventContent
                    )
                } returns 100

                every {
                    userServiceMock.getAll(isEqual(roomId))
                } returns MutableStateFlow(
                    mapOf(
                        roomUserMe.userId to flowOf(roomUserMe),
                    )
                )

                every { userServiceMock.canKickUser(isEqual(roomId), isAny()) } returns
                        MutableStateFlow(true)

                every { userServiceMock.canInvite(isAny()) } returns
                        MutableStateFlow(true)

                every { userServiceMock.getPowerLevel(isEqual(roomId), isAny()) } returns userPowerLevel

                every { userServiceMock.canSetPowerLevelToMax(isEqual(roomId), isAny()) } returns MutableStateFlow(100)

            }
        }

        should("set room's push rule to SILENT when we override all room notifications with 'do not notify'") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(
                PushRulesEventContent(
                    global = mapOf(
                        PushRuleKind.OVERRIDE to listOf(
                            PushRule(
                                conditions = setOf(
                                    PushCondition.EventMatch(
                                        key = "room_id",
                                        "!room:localhost"
                                    )
                                ),
                                actions = setOf(DontNotify),
                                enabled = true,
                                default = false,
                                ruleId = "!room:localhost",
                            ),
                        ),
                    )
                )
            )
            val cut = roomSettingsViewModel(coroutineContext)
            val subscriberJob = launch { cut.selectedRoomNotificationsLevel.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.SILENT

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set room's push rule to SILENT when we set room notifications to 'do not notify' and do not have a rule for our name mention") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(
                PushRulesEventContent(
                    global = mapOf(
                        PushRuleKind.ROOM to listOf(
                            PushRule(
                                actions = setOf(DontNotify),
                                enabled = true,
                                default = false,
                                ruleId = "!room:localhost",
                            ),
                        ),
                    )
                )
            )
            val cut = roomSettingsViewModel(coroutineContext)
            val subscriberJob = launch { cut.selectedRoomNotificationsLevel.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.SILENT

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set room's push rule to MENTIONS when we set room notifications to 'do not notify'") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(
                PushRulesEventContent(
                    global = mapOf(
                        PushRuleKind.CONTENT to listOf(
                            PushRule(
                                actions = setOf(Notify),
                                enabled = true,
                                default = true,
                                ruleId = ".m.rule.contains_user_name",
                            ),
                        ),
                        PushRuleKind.ROOM to listOf(
                            PushRule(
                                actions = setOf(DontNotify),
                                enabled = true,
                                default = false,
                                ruleId = "!room:localhost",
                            ),
                        ),
                    )
                )
            )
            val cut = roomSettingsViewModel(coroutineContext)
            val subscriberJob = launch { cut.selectedRoomNotificationsLevel.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.MENTIONS

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set room's push rule to ALL there are no 'do not notify' rules for the room") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)
            val cut = roomSettingsViewModel(coroutineContext)
            val subscriberJob = launch { cut.selectedRoomNotificationsLevel.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.ALL

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("go back to the room list view when leaving the room successfully") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)
            mocker.everySuspending {
                roomsApiClientMock.leaveRoom(
                    isEqual(roomId),
                    isAny(),
                    isNull()
                )
            } returns
                    Result.success(Unit)
            val onBackMock = mockFunction0(mocker) {}
            val cut = roomSettingsViewModel(coroutineContext, onBackMock)

            cut.leaveRoom()
            testCoroutineScheduler.advanceUntilIdle()

            mocker.verifyWithSuspend(exhaustive = false, inOrder = false) {
                roomsApiClientMock.leaveRoom(isEqual(roomId), isAny(), isNull())
                onBackMock()
            }

            cancelNeverEndingCoroutines()
        }

        should("show an error message when trying to leave a room and we are not connected") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)
            syncStateMocker returns MutableStateFlow(SyncState.ERROR)

            val cut = roomSettingsViewModel(coroutineContext)
            cut.leaveRoom()
            testCoroutineScheduler.advanceUntilIdle()

            cut.error.value shouldNotBe null
            // we have not mocked roomsApiClientMock.leaveRoom() and onBackMock.invoke(), so if they would be called, an exception would be thrown

            cancelNeverEndingCoroutines()
        }

        should("show an error message when leaving the room fails") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)
            mocker.everySuspending {
                roomsApiClientMock.leaveRoom(
                    isEqual(roomId),
                    isAny(),
                    isNull()
                )
            } returns
                    Result.failure(RuntimeException("Oh no!"))

            val cut = roomSettingsViewModel(coroutineContext)
            cut.leaveRoom()
            testCoroutineScheduler.advanceUntilIdle()

            // onBackMock is not mocked correctly, so if called, an exception would be thrown
            cut.error.value shouldNotBe null

            cancelNeverEndingCoroutines()
        }

        should("not allow to invite users") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)

            mocker.every { userServiceMock.canInvite(roomId) } returns
                    MutableStateFlow(false)
            val cut = roomSettingsViewModel(coroutineContext)

            delay(50)
            cut.hasPowerToInvite.first() shouldBe false

            cancelNeverEndingCoroutines()
        }


        should("allow to invite users") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)

            mocker.every { userServiceMock.canInvite(roomId) } returns
                    MutableStateFlow(true)
            val cut = roomSettingsViewModel(coroutineContext)
            cut.hasPowerToInvite.first { it } shouldBe true

            cancelNeverEndingCoroutines()
        }

        should("allow to change to room's name when the user's power level is above the standard required level 50") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)

            roomPowerLevel returns MutableStateFlow(
                Event.StateEvent(
                    content = PowerLevelsEventContent(),
                    id = EventId("1"),
                    sender = me,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )

            val cut = roomSettingsViewModel(coroutineContext)
            val canChangeRoomNameStateFlow = cut.canChangeRoomName // hold reference for WhileSubscribed
            println("power level OK")
            canChangeRoomNameStateFlow.first { it }

            userPowerLevel.value = 45
            println("power level not OK")
            canChangeRoomNameStateFlow.first { it.not() }

            userPowerLevel.value = 55
            println("power level OK")
            canChangeRoomNameStateFlow.first { it }

            cancelNeverEndingCoroutines()
        }

        should("allow to change the room's name when the user's power level is above the supplied power level requirements") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)

            val roomPowerLevelRequirements = MutableStateFlow(
                stateEvent(
                    PowerLevelsEventContent(
                        events = mapOf(
                            EventType(CreateEventContent::class, "") to 80,
                            EventType(NameEventContent::class, "") to 20,
                        ),
                        stateDefault = 40,
                    )
                )
            )
            roomPowerLevel returns roomPowerLevelRequirements
            userPowerLevel.value = 25

            val cut = roomSettingsViewModel(coroutineContext)
            val canChangeRoomNameStateFlow = cut.canChangeRoomName // hold reference for WhileSubscribed
            println("power level OK")
            canChangeRoomNameStateFlow.first { it }

            roomPowerLevelRequirements.value = stateEvent(
                PowerLevelsEventContent(
                    stateDefault = 30,
                )
            )
            println("power level not OK")
            canChangeRoomNameStateFlow.first { it.not() }

            roomPowerLevelRequirements.value = stateEvent(
                PowerLevelsEventContent(
                    stateDefault = 20,
                )
            )
            println("power level OK")
            canChangeRoomNameStateFlow.first { it }

            roomPowerLevelRequirements.value = stateEvent(
                PowerLevelsEventContent(
                    events = mapOf(EventType(NameEventContent::class, "") to 30)
                )
            )
            println("power level not OK")
            canChangeRoomNameStateFlow.first { it.not() }

            roomPowerLevelRequirements.value = stateEvent(
                PowerLevelsEventContent(
                    events = mapOf(EventType(NameEventContent::class, "") to 20)
                )
            )
            println("power level OK")
            canChangeRoomNameStateFlow.first { it }

            roomPowerLevelRequirements.value = stateEvent(
                PowerLevelsEventContent(
                    events = mapOf(EventType(NameEventContent::class, "") to 30),
                    stateDefault = 20,
                )
            )
            println("power level not OK")
            canChangeRoomNameStateFlow.first { it.not() }

            cancelNeverEndingCoroutines()
        }

        should("load the room name, set it when loaded, and can be manipulated afterwards") {
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)

            val roomStateFlow = MutableStateFlow<Room?>(null)
            roomGetById returns roomStateFlow

            val cut = roomSettingsViewModel(coroutineContext)
            // subscribe to all values in order to check for correct values later
            CoroutineScope(Dispatchers.Default).launch {
                cut.roomNameLoading.collect()
            }
            CoroutineScope(Dispatchers.Default).launch {
                cut.roomName.collect()
            }

            cut.roomNameLoading.value shouldBe true
            cut.roomName.value shouldBe ""

            roomStateFlow.value = Room(
                roomId,
                name = RoomDisplayName(explicitName = "Old name", summary = null)
            )
            testCoroutineScheduler.advanceUntilIdle()
            cut.roomNameLoading.value shouldBe false
            cut.roomName.value shouldBe "Old name"

            cancelNeverEndingCoroutines()
        }

        should("set the room's name to `Undetermined` when the name is currently set") {
            val coroutineScope = CoroutineScope(Dispatchers.Default)
            userPowerLevel.value = 1000
            roomPowerLevel returns MutableStateFlow(
                Event.StateEvent(
                    content = PowerLevelsEventContent(),
                    id = EventId("1"),
                    sender = me,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
            mocker.every {
                userServiceMock.getAccountData(isEqual(PushRulesEventContent::class), isAny())
            } returns MutableStateFlow(null)
            mocker.everySuspending {
                roomsApiClientMock.sendStateEvent(isEqual(roomId), isAny(), isAny(), isAny())
            } runs {
                coroutineScope.async {
                    delay(1.seconds)
                    Result.success(EventId("1"))
                }.await()
            }

            val cut = roomSettingsViewModel(coroutineContext)
            // subscribe to all values in order to check for correct values later
            coroutineScope.launch { cut.roomNameLoading.collect() }
            coroutineScope.launch { cut.roomNameIsBeingEdited.collect() }
            coroutineScope.launch { cut.canChangeRoomName.collect() }

            testCoroutineScheduler.advanceUntilIdle()
            cut.roomName.value shouldBe "Old name"

            cut.roomName.value = "New name"
            testCoroutineScheduler.advanceUntilIdle()
            cut.roomNameIsBeingEdited.value shouldBe true
            cut.roomNameLoading.value shouldBe false

            cut.changeRoomName()
            testCoroutineScheduler.advanceUntilIdle()
            cut.roomNameLoading.value shouldBe true

            coroutineScope.launch {
                eventually(2.seconds) {
                    cut.roomName.value shouldBe "New name"
                    cut.roomNameLoading.value shouldBe false
                }
            }.join()

            cancelNeverEndingCoroutines()
        }
    }

    private fun stateEvent(eventContent: PowerLevelsEventContent) = Event.StateEvent(
        content = eventContent,
        id = EventId("1"),
        sender = me,
        roomId = roomId,
        originTimestamp = 0L,
        stateKey = "",
    )

    private fun roomSettingsViewModel(
        coroutineContext: CoroutineContext,
        onBackMock: () -> Unit = mockFunction0(mocker),
    ) = RoomSettingsViewModelImpl(
        viewModelContext = MatrixClientViewModelContextImpl(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            di = koinApplication {
                modules(
                    trixnityMessengerModule(),
                    testMatrixClientModule(matrixClientMock),
                )
            }.koin,
            accountName = "test",
            coroutineContext = coroutineContext,
        ),
        selectedRoomId = roomId,
        onBack = onBackMock,
        onCloseRoomSettings = mockFunction0(mocker),

        onShowAddMembers = mockFunction0(mocker)
    )
}

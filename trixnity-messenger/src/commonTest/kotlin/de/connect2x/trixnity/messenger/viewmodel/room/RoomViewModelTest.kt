package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.files.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import de.connect2x.trixnity.messenger.viewmodel.mock.MockSyncApiClient
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.NoOpTimeline
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter
import de.connect2x.trixnity.messenger.viewmodel.util.IsNetworkAvailable
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.should
import io.kotest.matchers.types.beOfType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.DeviceTrustLevel
import net.folivo.trixnity.client.key.KeySecretService
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.key.KeyTrustService
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.room.getAccountData
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.client.verification.VerificationService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.SyncApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.FullyReadEventContent
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.events.m.TypingEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.kodein.mock.mockFunction5
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RoomViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    private val mocker = Mocker()

    private lateinit var lifecycle: LifecycleRegistry
    private val backPressedHandler = BackDispatcher()

    private val roomId = RoomId("room", "localhost")

    private val myUserId = UserId("user1", "localhost")
    private val myDeviceId = "deviceId"
    private val roomsFlow = MutableStateFlow(emptyMap<RoomId, StateFlow<Room?>>())

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var keyServiceMock: KeyService

    @Mock
    lateinit var keySecretServiceMock: KeySecretService

    @Mock
    lateinit var keyTrustServiceMock: KeyTrustService

    @Mock
    lateinit var verificationServiceMock: VerificationService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var matrixClientServerApiClientMock: MatrixClientServerApiClient

    lateinit var syncApiClientMock: SyncApiClient

    @Mock
    lateinit var downloadManagerMock: DownloadManager

    @Mock
    lateinit var isNetworkAvailable: IsNetworkAvailable

    @Mock
    lateinit var runInitialSyncMock: RunInitialSync

    @Mock
    lateinit var minimizeMessengerMock: () -> Unit

    lateinit var selfVerificationMethods: Mocker.Every<Flow<VerificationService.SelfVerificationMethods>>
    lateinit var syncState: Mocker.Every<StateFlow<SyncState>>

    init {
        Dispatchers.setMain(testMainDispatcher)
        isolationMode = IsolationMode.InstancePerTest // since we want to reset Lifecycle, etc.

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            lifecycle = LifecycleRegistry()
            lifecycle.resume()

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(module {
                        single { roomServiceMock }
                        single { keyServiceMock }
                        single { userServiceMock }
                        single { verificationServiceMock }
                    })
                }.koin

                syncApiClientMock = MockSyncApiClient(mocker)

                every { matrixClientMock.userId } returns myUserId
                every { matrixClientMock.deviceId } returns myDeviceId
                every { matrixClientMock.api } returns matrixClientServerApiClientMock
                syncState = every { matrixClientMock.syncState }
                syncState returns MutableStateFlow(SyncState.RUNNING)
                everySuspending { matrixClientMock.startSync() } returns Unit
                everySuspending { matrixClientMock.cancelSync(isAny()) } returns Unit

                every { matrixClientServerApiClientMock.sync } returns syncApiClientMock

                every { roomServiceMock.getAll() } returns roomsFlow
                every { roomServiceMock.getById(roomId) } returns MutableStateFlow(Room(roomId))
                every {
                    roomServiceMock.getAccountData<FullyReadEventContent>(
                        roomId,
                        ""
                    )
                } returns MutableStateFlow(null)
                every {
                    roomServiceMock.getTimeline(
                        isAny(),
                        isAny<suspend (Flow<TimelineEvent>) -> Unit>()
                    )
                } returns
                        NoOpTimeline
                every { roomServiceMock.getById(isAny()) } returns flowOf(null)
                every {
                    roomServiceMock.getAccountData(isAny(), isEqual(FullyReadEventContent::class), isAny())
                } returns flowOf(null)
                every { roomServiceMock.getOutbox() } returns MutableStateFlow(listOf())

                every {
                    roomServiceMock.getState(isAny(), isEqual(CreateEventContent::class), isAny())
                } returns MutableStateFlow(null)

                every {
                    roomServiceMock.getState(isAny(), isEqual(PowerLevelsEventContent::class), isAny())
                } returns MutableStateFlow(null)
                every {
                    roomServiceMock.usersTyping
                } returns MutableStateFlow(mapOf())

                every { verificationServiceMock.activeDeviceVerification } returns MutableStateFlow(
                    null
                )
                selfVerificationMethods =
                    every { verificationServiceMock.getSelfVerificationMethods() }
                selfVerificationMethods returns MutableStateFlow(
                    VerificationService.SelfVerificationMethods.PreconditionsNotMet(
                        emptySet()
                    )
                )

                every { keyServiceMock.getTrustLevel(isAny<UserId>(), isAny()) } returns
                        flowOf(DeviceTrustLevel.Valid(false))

                everySuspending {
                    userServiceMock.loadMembers(
                        RoomId(isAny()),
                        isAny()
                    )
                } returns Unit
                every { userServiceMock.getById(isAny(), isAny()) } returns MutableStateFlow(null)

                every { userServiceMock.userPresence } returns MutableStateFlow(emptyMap())
                every { userServiceMock.getAll(roomId) } returns MutableStateFlow(null)
                every { userServiceMock.canInvite(roomId) } returns MutableStateFlow(false)
                every { userServiceMock.getAccountData<DirectEventContent>("") } returns
                        MutableStateFlow(null)
                every { userServiceMock.getAccountData<PushRulesEventContent>("") } returns
                        MutableStateFlow(null)

                every { minimizeMessengerMock.invoke() } returns Unit

                every { syncApiClientMock.subscribe(isAny<KClass<TypingEventContent>>(), isAny()) } returns Unit
                every { syncApiClientMock.unsubscribe(isAny<KClass<TypingEventContent>>(), isAny()) } returns Unit
            }
        }

        should("show selected room without settings initially") {
            val cut = roomViewModel()

            cut.shouldShowInitialView()
        }

        context(RoomViewModel::setTwoPane.toString()) {
            context("settings aren't activated") {
                should("show room list in single-pane view") {
                    val cut = roomViewModel()
                    cut.shouldShowInitialView()

                    cut.setTwoPane(true)

                    eventually(1.seconds) {
                        assertSoftly {
                            cut.timelineStack.value.active.instance should beOfType<TimelineRouter.TimelineWrapper.View>()
                            cut.settingsStack.value.active.instance should beOfType<SettingsRouter.SettingsWrapper.None>()
                        }
                    }
                }
                should("show room list in multi-pane view") {
                    val cut = roomViewModel()
                    cut.shouldShowInitialView()

                    cut.setTwoPane(false)

                    eventually(1.seconds) {
                        assertSoftly {
                            cut.timelineStack.value.active.instance should beOfType<TimelineRouter.TimelineWrapper.View>()
                            cut.settingsStack.value.active.instance should beOfType<SettingsRouter.SettingsWrapper.None>()
                        }
                    }
                }
            }
            context("settings are activated") {
                should("show room's settings when settings are activated in single-pane view") {
                    val cut = roomViewModel()
                    cut.shouldShowInitialView()

                    val timelineWrapper =
                        cut.timelineStack.value.active.instance as TimelineRouter.TimelineWrapper.View
                    timelineWrapper.timelineViewModel.roomHeaderViewModel.showRoomSettings()

                    cut.setTwoPane(true)

                    eventually(1.seconds) {
                        assertSoftly {
                            cut.timelineStack.value.active.instance should beOfType<TimelineRouter.TimelineWrapper.None>()
                            cut.settingsStack.value.active.instance should beOfType<SettingsRouter.SettingsWrapper.View>()
                        }
                    }
                }

                should("show room view and room settings when settings are activated in multi-pane view") {
                    val cut = roomViewModel()
                    cut.shouldShowInitialView()

                    val timelineWrapper =
                        cut.timelineStack.value.active.instance as TimelineRouter.TimelineWrapper.View
                    timelineWrapper.timelineViewModel.roomHeaderViewModel.showRoomSettings()

                    cut.setTwoPane(false)

                    eventually(1.seconds) {
                        assertSoftly {
                            cut.timelineStack.value.active.instance should beOfType<TimelineRouter.TimelineWrapper.View>()
                            cut.settingsStack.value.active.instance should beOfType<SettingsRouter.SettingsWrapper.View>()
                        }
                    }
                }
            }
        }
        should("show the room when room settings are getting disabled in two-pane view") {
            val cut = roomViewModel()
            cut.shouldShowInitialView()
            cut.setTwoPane(true)

            cut.onSettingsBack()

            eventually(1.seconds) {
                assertSoftly {
                    cut.timelineStack.value.active.instance should beOfType<TimelineRouter.TimelineWrapper.View>()
                    cut.settingsStack.value.active.instance should beOfType<SettingsRouter.SettingsWrapper.None>()
                }
            }
        }

        should("show the room when room settings are getting disabled in multi-pane view") {
            val cut = roomViewModel()
            cut.shouldShowInitialView()
            cut.setTwoPane(false)

            cut.onSettingsBack()

            eventually(1.seconds) {
                assertSoftly {
                    cut.timelineStack.value.active.instance should beOfType<TimelineRouter.TimelineWrapper.View>()
                    cut.settingsStack.value.active.instance should beOfType<SettingsRouter.SettingsWrapper.None>()
                }
            }
        }

        should("show the room settings view when room settings are getting disabled in two-pane view") {
            val cut = roomViewModel()
            cut.shouldShowInitialView()
            cut.setTwoPane(true)

            cut.onShowSettings()

            eventually(1.seconds) {
                assertSoftly {
                    cut.timelineStack.value.active.instance should beOfType<TimelineRouter.TimelineWrapper.None>()
                    cut.settingsStack.value.active.instance should beOfType<SettingsRouter.SettingsWrapper.View>()
                }
            }
        }

        should("show the room view and the room settings view when room settings are getting disabled in multi-pane view") {
            val cut = roomViewModel()
            cut.shouldShowInitialView()
            cut.setTwoPane(false)

            cut.onShowSettings()

            eventually(1.seconds) {
                assertSoftly {
                    cut.timelineStack.value.active.instance should beOfType<TimelineRouter.TimelineWrapper.View>()
                    cut.settingsStack.value.active.instance should beOfType<SettingsRouter.SettingsWrapper.View>()
                }
            }
        }
    }

    private suspend fun RoomViewModelImpl.shouldShowInitialView() =
        eventually(1.seconds) {
            assertSoftly {
                timelineStack.value.active.instance should beOfType<TimelineRouter.TimelineWrapper.View>()
                settingsStack.value.active.instance should beOfType<SettingsRouter.SettingsWrapper.None>()
            }
        }

    private fun roomViewModel(): RoomViewModelImpl {
        val roomViewModel = RoomViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(
                    lifecycle,
                    backHandler = backPressedHandler
                ),
                di = koinApplication {
                    modules(trixnityMessengerModule(), testMatrixClientModule(matrixClientMock), module {
                        single { downloadManagerMock }
                        single { isNetworkAvailable }
                        single { runInitialSyncMock }
                    })
                }.koin,
                accountName = "test",
                coroutineContext = Dispatchers.Unconfined,
            ),
            roomId = roomId,
            onRoomBack = mockFunction0(mocker),
            onOpenModal = mockFunction5(mocker),
            isBackButtonVisible = MutableStateFlow(false),
        )
        return roomViewModel
    }
}
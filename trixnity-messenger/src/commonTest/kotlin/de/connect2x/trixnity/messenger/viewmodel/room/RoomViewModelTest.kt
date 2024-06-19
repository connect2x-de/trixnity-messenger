package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.IsNetworkAvailable
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.*
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.should
import io.kotest.matchers.types.beOfType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.*
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
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.kodein.mock.mockFunction2
import org.kodein.mock.mockFunction5
import org.koin.dsl.koinApplication
import org.koin.dsl.module


@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class RoomViewModelTest : ShouldSpec() {
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
    lateinit var verificationServiceMock: VerificationService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var matrixClientServerApiClientMock: MatrixClientServerApiClient

    @Mock
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
        coroutineTestScope = true

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
                every { roomServiceMock.getOutbox() } returns MutableStateFlow(mapOf())

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
                every { userServiceMock.getAll(roomId) } returns MutableStateFlow(mapOf())
                every { userServiceMock.getAllReceipts(isEqual(roomId)) } returns MutableStateFlow(emptyMap())
                every { userServiceMock.canInvite(roomId) } returns MutableStateFlow(false)
                every { userServiceMock.getAccountData<DirectEventContent>("") } returns
                        MutableStateFlow(null)
                every { userServiceMock.getAccountData<PushRulesEventContent>("") } returns
                        MutableStateFlow(null)
                every { userServiceMock.getPowerLevel(isAny(), isAny()) } returns MutableStateFlow(50)
                every { userServiceMock.canSendEvent(isAny(), isAny()) } returns flowOf(true)

                every { minimizeMessengerMock.invoke() } returns Unit
            }
        }

        afterTest {
            lifecycle.destroy()
        }

        should("show selected room without settings initially") {
            val cut = roomViewModel()

            shouldShowInitialView(cut)
            testCoroutineScheduler.advanceUntilIdle()
        }

        context(RoomViewModel::setSinglePane.toString()) {
            context("settings aren't activated") {
                should("show room list in single-pane view") {
                    val cut = roomViewModel()
                    shouldShowInitialView(cut)

                    cut.setSinglePane(true)
                    testCoroutineScheduler.advanceUntilIdle()

                    assertSoftly {
                        cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.View>()
                        cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.None>()
                    }
                }
                should("show room list in multi-pane view") {
                    val cut = roomViewModel()
                    shouldShowInitialView(cut)

                    cut.setSinglePane(false)
                    testCoroutineScheduler.advanceUntilIdle()

                    assertSoftly {
                        cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.View>()
                        cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.None>()
                    }
                }
            }
            context("settings are activated") {
                should("show room's settings when settings are activated in single-pane view") {
                    val cut = roomViewModel()
                    shouldShowInitialView(cut)

                    val timelineWrapper =
                        cut.timelineStack.value.active.instance as TimelineRouter.Wrapper.View
                    timelineWrapper.viewModel.roomHeaderViewModel.showRoomSettings()

                    cut.setSinglePane(true)
                    testCoroutineScheduler.advanceUntilIdle()

                    assertSoftly {
                        cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.None>()
                        cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.View>()
                    }
                }

                should("show room view and room settings when settings are activated in multi-pane view") {
                    val cut = roomViewModel()
                    shouldShowInitialView(cut)

                    val timelineWrapper =
                        cut.timelineStack.value.active.instance as TimelineRouter.Wrapper.View
                    timelineWrapper.viewModel.roomHeaderViewModel.showRoomSettings()

                    cut.setSinglePane(false)
                    testCoroutineScheduler.advanceUntilIdle()

                    assertSoftly {
                        cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.View>()
                        cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.View>()
                    }
                }
            }
        }
        should("show the room when room settings are getting disabled in two-pane view") {
            val cut = roomViewModel()
            shouldShowInitialView(cut)
            cut.setSinglePane(true)
            testCoroutineScheduler.advanceUntilIdle()

            cut.onSettingsBack()
            testCoroutineScheduler.advanceUntilIdle()

            assertSoftly {
                cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.View>()
                cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.None>()
            }
        }

        should("show the room when room settings are getting disabled in multi-pane view") {
            val cut = roomViewModel()
            shouldShowInitialView(cut)
            cut.setSinglePane(false)
            testCoroutineScheduler.advanceUntilIdle()

            cut.onSettingsBack()
            testCoroutineScheduler.advanceUntilIdle()

            assertSoftly {
                cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.View>()
                cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.None>()
            }
        }

        should("show the room settings view when room settings are getting disabled in two-pane view") {
            val cut = roomViewModel()
            shouldShowInitialView(cut)
            cut.setSinglePane(true)
            testCoroutineScheduler.advanceUntilIdle()

            cut.onShowSettings()
            testCoroutineScheduler.advanceUntilIdle()

            assertSoftly {
                cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.None>()
                cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.View>()
            }
        }

        should("show the room view and the room settings view when room settings are getting disabled in multi-pane view") {
            val cut = roomViewModel()
            shouldShowInitialView(cut)
            cut.setSinglePane(false)
            testCoroutineScheduler.advanceUntilIdle()

            cut.onShowSettings()
            testCoroutineScheduler.advanceUntilIdle()

            assertSoftly {
                cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.View>()
                cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.View>()
            }
        }
    }

    private fun TestScope.shouldShowInitialView(cut: RoomViewModel) {
        testCoroutineScheduler.advanceUntilIdle()
        assertSoftly {
            cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.View>()
            cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.None>()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun roomViewModel(): RoomViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val roomViewModel = RoomViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(
                    lifecycle,
                    backHandler = backPressedHandler
                ),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(
                                UserId(
                                    "test",
                                    "server"
                                ) to matrixClientMock
                            )
                        ) + module {
                            single { downloadManagerMock }
                            single { isNetworkAvailable }
                            single { runInitialSyncMock }
                            single<RoomHeaderViewModelFactory> {
                                object : RoomHeaderViewModelFactory {
                                    override fun create(
                                        viewModelContext: MatrixClientViewModelContext,
                                        selectedRoomId: RoomId,
                                        isBackButtonVisible: MutableStateFlow<Boolean>,
                                        onBack: () -> Unit,
                                        onVerifyUser: () -> Unit,
                                        onShowRoomSettings: () -> Unit,
                                    ): RoomHeaderViewModel {
                                        return object : RoomHeaderViewModel {
                                            override val error: StateFlow<String?> = MutableStateFlow(null)
                                            override val isBackButtonVisible: StateFlow<Boolean> =
                                                MutableStateFlow(false)
                                            override val roomHeaderInfo: StateFlow<RoomHeaderInfo> =
                                                MutableStateFlow(
                                                    RoomHeaderInfo("", "", "", null, null, false, false)
                                                )
                                            override val usersTyping: StateFlow<String?> = MutableStateFlow(null)
                                            override val userTrustLevel: StateFlow<UserTrustLevel?> =
                                                MutableStateFlow(null)
                                            override val canVerifyUser: StateFlow<Boolean> = MutableStateFlow(false)
                                            override val canBlockUser: StateFlow<Boolean> = MutableStateFlow(false)
                                            override val canUnblockUser: StateFlow<Boolean> = MutableStateFlow(false)
                                            override val isUserBlocked: StateFlow<Boolean> = MutableStateFlow(false)

                                            override fun blockUser() {}
                                            override fun unblockUser() {}
                                            override fun verifyUser() {}
                                            override fun showRoomSettings() {
                                                onShowRoomSettings()
                                            }

                                            override fun goBack() {
                                                onBack()
                                            }
                                        }
                                    }
                                }
                            }
                        })
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = Dispatchers.Unconfined,
            ),
            roomId = roomId,
            onRoomBack = mockFunction0(mocker),
            onOpenModal = mockFunction5(mocker),
            isBackButtonVisible = MutableStateFlow(false),
            onOpenMention = mockFunction2(mocker),
        )
        return roomViewModel
    }
}

package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.IsNetworkAvailable
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.NoOpTimeline
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderInfo
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.should
import io.kotest.matchers.types.beOfType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.DeviceTrustLevel
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.key.UserTrustLevel
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
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
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class RoomViewModelTest : ShouldSpec() {
    private lateinit var lifecycle: LifecycleRegistry
    private val backPressedHandler = BackDispatcher()

    private val roomId = RoomId("room", "localhost")

    private val myUserId = UserId("user1", "localhost")
    private val myDeviceId = "deviceId"
    private val roomsFlow = MutableStateFlow(emptyMap<RoomId, StateFlow<Room?>>())

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val keyServiceMock = mock<KeyService>()

    val verificationServiceMock = mock<VerificationService>()

    val userServiceMock = mock<UserService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val syncApiClientMock = mock<SyncApiClient>()

    val downloadManagerMock = mock<DownloadManager>()

    val isNetworkAvailable = mock<IsNetworkAvailable>()

    val runInitialSyncMock = mock<RunInitialSync>()

    val minimizeMessengerMock = mock<() -> Unit>()

    lateinit var selfVerificationMethods: BlockingAnsweringScope<Flow<VerificationService.SelfVerificationMethods>>
    lateinit var syncState: BlockingAnsweringScope<StateFlow<SyncState>>

    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(
                matrixClientMock,
                roomServiceMock,
                keyServiceMock,
                verificationServiceMock,
                userServiceMock,
                matrixClientServerApiClientMock,
                syncApiClientMock,
                downloadManagerMock,
                isNetworkAvailable,
                runInitialSyncMock,
                minimizeMessengerMock,
            )
            lifecycle = LifecycleRegistry()
            lifecycle.resume()

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
            everySuspend { matrixClientMock.startSync() } returns Unit
            everySuspend { matrixClientMock.cancelSync(any()) } returns Unit

            every { matrixClientServerApiClientMock.sync } returns syncApiClientMock

            every { roomServiceMock.getAll() } returns roomsFlow
            every { roomServiceMock.getById(roomId) } returns MutableStateFlow(Room(roomId))
            every {
                roomServiceMock.getAccountData(roomId, FullyReadEventContent::class, "")
            } returns MutableStateFlow(null)
            every {
                roomServiceMock.getTimeline(
                    any(),
                    any(),
                    any<suspend (Flow<TimelineEvent>) -> Unit>()
                )
            } returns
                    NoOpTimeline
            every { roomServiceMock.getById(any()) } returns flowOf(null)
            every {
                roomServiceMock.getAccountData(any(), FullyReadEventContent::class, "")
            } returns flowOf(null)
            every { roomServiceMock.getOutbox() } returns MutableStateFlow(listOf())
            every { roomServiceMock.getOutbox(roomId = any()) } returns MutableStateFlow(listOf())

            every {
                roomServiceMock.getState(any(), eq(CreateEventContent::class), any())
            } returns MutableStateFlow(null)

            every {
                roomServiceMock.getState(any(), eq(PowerLevelsEventContent::class), any())
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

            every { keyServiceMock.getTrustLevel(any<UserId>(), any()) } returns
                    flowOf(DeviceTrustLevel.Valid(false))

            everySuspend {
                userServiceMock.loadMembers(
                    RoomId(any()),
                    any()
                )
            } returns Unit
            every { userServiceMock.getById(any(), any()) } returns MutableStateFlow(null)

            every { userServiceMock.userPresence } returns MutableStateFlow(emptyMap())
            every { userServiceMock.getAll(roomId) } returns MutableStateFlow(mapOf())
            every { userServiceMock.getAllReceipts(eq(roomId)) } returns MutableStateFlow(emptyMap())
            every { userServiceMock.canInvite(roomId) } returns MutableStateFlow(false)
            every { userServiceMock.getAccountData(DirectEventContent::class, "") } returns
                    MutableStateFlow(null)
            every { userServiceMock.getAccountData(PushRulesEventContent::class, "") } returns
                    MutableStateFlow(null)
            every { userServiceMock.getPowerLevel(any(), any()) } returns MutableStateFlow(50)
            every { userServiceMock.canSendEvent(any(), any()) } returns flowOf(true)

            every { minimizeMessengerMock.invoke() } returns Unit
        }

        afterTest {
            lifecycle.destroy()
        }

        should("show selected room without settings initially") {
            val cut = roomViewModel()

            shouldShowInitialView(cut)
            delay(100.milliseconds)
        }

        context(RoomViewModel::setSinglePane.toString()) {
            context("settings aren't activated") {
                should("show room list in single-pane view") {
                    val cut = roomViewModel()
                    shouldShowInitialView(cut)

                    cut.setSinglePane(true)
                    delay(100.milliseconds)

                    assertSoftly {
                        cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.View>()
                        cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.None>()
                    }
                }
                should("show room list in multi-pane view") {
                    val cut = roomViewModel()
                    shouldShowInitialView(cut)

                    cut.setSinglePane(false)
                    delay(100.milliseconds)

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
                    delay(100.milliseconds)

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
                    delay(100.milliseconds)

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
            delay(100.milliseconds)

            cut.onSettingsBack()
            delay(100.milliseconds)

            assertSoftly {
                cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.View>()
                cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.None>()
            }
        }

        should("show the room when room settings are getting disabled in multi-pane view") {
            val cut = roomViewModel()
            shouldShowInitialView(cut)
            cut.setSinglePane(false)
            delay(100.milliseconds)

            cut.onSettingsBack()
            delay(100.milliseconds)

            assertSoftly {
                cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.View>()
                cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.None>()
            }
        }

        should("show the room settings view when room settings are getting disabled in two-pane view") {
            val cut = roomViewModel()
            shouldShowInitialView(cut)
            cut.setSinglePane(true)
            delay(100.milliseconds)

            cut.onShowSettings()
            delay(100.milliseconds)

            assertSoftly {
                cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.None>()
                cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.View>()
            }
        }

        should("show the room view and the room settings view when room settings are getting disabled in multi-pane view") {
            val cut = roomViewModel()
            shouldShowInitialView(cut)
            cut.setSinglePane(false)
            delay(100.milliseconds)

            cut.onShowSettings()
            delay(100.milliseconds)

            assertSoftly {
                cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.View>()
                cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.View>()
            }
        }
    }

    private suspend fun TestScope.shouldShowInitialView(cut: RoomViewModel) {
        delay(100.milliseconds)
        assertSoftly {
            cut.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.View>()
            cut.settingsStack.value.active.instance should beOfType<SettingsRouter.Wrapper.None>()
        }
    }

    private suspend fun roomViewModel(): RoomViewModelImpl {
        Dispatchers.setMain(Dispatchers.Unconfined)
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
                                        onShowUserProfile: (UserId) -> Unit,
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
                                            override val canShowUserProfile: StateFlow<Boolean> = MutableStateFlow(false)

                                            override fun blockUser() {}
                                            override fun unblockUser() {}
                                            override fun verifyUser() {}
                                            override fun showRoomSettings() {
                                                onShowRoomSettings()
                                            }

                                            override fun showUserProfile() {}

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
                coroutineContext = currentCoroutineContext(),
            ),
            roomId = roomId,
            onRoomBack = mock(),
            isBackButtonVisible = MutableStateFlow(false),
            onOpenAvatarCutter = { _, _, _ -> },
            onOpenMention = mock(),
            goToRoom = mock()
        )
        return roomViewModel
    }
}

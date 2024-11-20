package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.IsNetworkAvailable
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncRouter
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.InputAreaViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.InputAreaViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.NoOpTimeline
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElement
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.SpaceViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.ErrorType
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationRouter
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beOfType
import io.kotest.matchers.types.instanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.DeviceTrustLevel
import net.folivo.trixnity.client.key.KeySecretService
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.key.KeyTrustService
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.client.verification.SelfVerificationMethod
import net.folivo.trixnity.client.verification.VerificationService
import net.folivo.trixnity.client.verification.VerificationService.SelfVerificationMethods.PreconditionsNotMet
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.clientserverapi.model.sync.Sync
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.FullyReadEventContent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.secretstorage.SecretKeyEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    private lateinit var lifecycle: LifecycleRegistry
    private val backPressedHandler = BackDispatcher()

    private val myUserId = UserId("user1", "localhost")
    private val myDeviceId = "deviceId"
    private val roomsFlow = MutableStateFlow(emptyMap<RoomId, StateFlow<Room?>>())

    private lateinit var messengerSettings: MatrixMessengerSettingsHolder

    val matrixClientMock = mock<MatrixClient>()

    val matrixClientMock2 = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val keyServiceMock = mock<KeyService>()

    val keySecretServiceMock = mock<KeySecretService>()

    val keyTrustServiceMock = mock<KeyTrustService>()

    val verificationServiceMock = mock<VerificationService>()

    val verificationServiceMock2 = mock<VerificationService>()

    val userServiceMock = mock<UserService>()

    val downloadManagerMock = mock<DownloadManager>()

    val isNetworkAvailable = mock<IsNetworkAvailable>()

    val roomHeaderViewModelMock = mock<RoomHeaderViewModel>()

    val inputAreaViewModelMock = mock<InputAreaViewModel>()

    val runInitialSyncMock = mock<RunInitialSync>()

    lateinit var selfVerificationMethods: BlockingAnsweringScope<Flow<VerificationService.SelfVerificationMethods>>
    lateinit var networkAvailable: BlockingAnsweringScope<Boolean>
    lateinit var syncState: BlockingAnsweringScope<StateFlow<SyncState>>
    lateinit var initialSyncDone: BlockingAnsweringScope<StateFlow<Boolean>>
    private val startSyncPresenceCapture = mutableListOf<Presence>()

    init {
        beforeTest {
            resetMocks(
                matrixClientMock,
                matrixClientMock2,
                roomServiceMock,
                keyServiceMock,
                keySecretServiceMock,
                keyTrustServiceMock,
                verificationServiceMock,
                verificationServiceMock2,
                userServiceMock,
                downloadManagerMock,
                isNetworkAvailable,
                roomHeaderViewModelMock,
                inputAreaViewModelMock,
                runInitialSyncMock,
            )
            lifecycle = LifecycleRegistry()
            lifecycle.resume()
            startSyncPresenceCapture.clear()
            messengerSettings = createTestMatrixMessengerSettingsHolder()
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
            every { matrixClientMock.displayName } returns MutableStateFlow(null)
            every { matrixClientMock.avatarUrl } returns MutableStateFlow(null)
            syncState = every { matrixClientMock.syncState }
            syncState returns MutableStateFlow(SyncState.RUNNING)
            everySuspend { matrixClientMock.startSync(any()) } calls { startSyncPresenceCapture.add(it.arg(0)) }
            everySuspend { matrixClientMock.stopSync(any()) } returns Unit
            everySuspend { matrixClientMock.cancelSync(any()) } returns Unit

            every { roomServiceMock.getAll() } returns roomsFlow
            every {
                roomServiceMock.getState(any(), eq(CreateEventContent::class), any())
            } returns MutableStateFlow(null)
            every {
                roomServiceMock.getTimeline(
                    any(),
                    any<suspend (Flow<TimelineEvent>) -> Unit>()
                )
            } returns
                    NoOpTimeline
            every { roomServiceMock.getById(any()) } returns MutableStateFlow(null)
            every {
                roomServiceMock.getAccountData(any(), eq(FullyReadEventContent::class), any())
            } returns flowOf(null)
            every { roomServiceMock.getOutbox() } returns flowOf(listOf())
            every { userServiceMock.getAll(any()) } returns flowOf(mapOf())
            every { userServiceMock.getById(any(), any()) } returns flowOf(null)
            every { userServiceMock.getAllReceipts(any()) } returns flowOf(mapOf())
            every { userServiceMock.getReceiptsById(any(), any()) } returns flowOf(null)

            every { verificationServiceMock.activeDeviceVerification } returns MutableStateFlow(null)
            selfVerificationMethods =
                every { verificationServiceMock.getSelfVerificationMethods() }
            selfVerificationMethods returns MutableStateFlow(PreconditionsNotMet(emptySet()))

            every { keyServiceMock.getTrustLevel(any<UserId>(), any()) } returns
                    flowOf(DeviceTrustLevel.Valid(true))

            everySuspend { userServiceMock.loadMembers(RoomId(any()), any()) } returns Unit
            every { userServiceMock.getAccountData(DirectEventContent::class) } returns
                    MutableStateFlow(DirectEventContent(emptyMap()))

            networkAvailable = every { isNetworkAvailable.invoke() }
            networkAvailable returns false

            initialSyncDone = every { matrixClientMock.initialSyncDone }
            initialSyncDone returns MutableStateFlow(true)

            // matrixClientMock2
            every { matrixClientMock2.di } returns koinApplication {
                modules(module {
                    single { roomServiceMock }
                    single { keyServiceMock }
                    single { userServiceMock }
                    single { verificationServiceMock2 }
                })
            }.koin
            every { matrixClientMock2.userId } returns myUserId
            every { matrixClientMock2.deviceId } returns myDeviceId
            every { matrixClientMock2.displayName } returns MutableStateFlow(null)
            every { matrixClientMock2.avatarUrl } returns MutableStateFlow(null)
            every { matrixClientMock2.syncState } returns MutableStateFlow(SyncState.RUNNING)
            everySuspend { matrixClientMock2.startSync() } returns Unit
            everySuspend { matrixClientMock2.cancelSync(any()) } returns Unit
            every { matrixClientMock2.initialSyncDone } returns MutableStateFlow(true)
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(UserId("test", "server")) {
                it.copy(
                    accountSetupFinished = true
                )
            }

        }

        afterTest {
            lifecycle.destroy()
        }

        should("select no room initially") {
            everySuspend {
                matrixClientMock.syncOnce(any(), any(), any<suspend (Sync.Response) -> Unit>())
            } returns Result.success(Unit)

            val cut = mainViewModel()

            eventually(2.seconds) {
                assertSoftly {
                    cut.selectedRoomId.value shouldBe null
                    cut.roomListRouterStack.value.active.instance should beOfType<RoomListRouter.Wrapper.List>()
                    cut.roomRouterStack.value.active.instance should beOfType<RoomRouter.Wrapper.None>()
                }
            }

        }

        should("show its room view when room is selected") {
            val cut = mainViewModel()

            val roomId = RoomId("!Room:localhost")
            cut.setSinglePane(true)

            cut.onRoomSelected(UserId("test", "server"), roomId)

            eventually(2.seconds) {
                assertSoftly {
                    cut.isSinglePane.value shouldBe true
                    cut.selectedRoomId.value shouldBe roomId

                    cut.roomRouterStack.value.active.instance should beOfType<RoomRouter.Wrapper.View>()
                    cut.roomListRouterStack.value.active.instance should beOfType<RoomListRouter.Wrapper.None>() // since single pane
                }
            }

        }

        should("show room list view if room is selected in multi-pane view") {
            val cut = mainViewModel()

            val roomId = RoomId("!Room:localhost")
            cut.setSinglePane(false)

            cut.onRoomSelected(UserId("test", "server"), roomId)

            eventually(2.seconds) {
                assertSoftly {
                    cut.isSinglePane.value shouldBe false
                    cut.selectedRoomId.value shouldBe roomId
                    cut.roomRouterStack.value.active.instance should beOfType<RoomRouter.Wrapper.View>()
                    cut.roomListRouterStack.value.active.instance should beOfType<RoomListRouter.Wrapper.List>() // since multi pane
                }
            }

        }

        should("show list of rooms when the room view is closed") {
            val cut = mainViewModel()

            cut.onRoomSelected(UserId("test", "server"), RoomId("!Room:localhost"))

            cut.closeDetailsAndShowList()

            eventually(2.seconds) {
                cut.selectedRoomId.value shouldBe null
                cut.roomListRouterStack.value.active.instance should beOfType<RoomListRouter.Wrapper.List>()
                cut.roomRouterStack.value.active.instance should beOfType<RoomRouter.Wrapper.None>()
            }

        }

        should("show room view after switching to multipane when room was selected before") {
            val cut = mainViewModel()

            cut.onRoomSelected(UserId("test", "server"), RoomId("!Room:localhost"))
            cut.selectedRoomId.first { it == RoomId("!Room:localhost") }
            cut.setSinglePane(false)

            eventually(2.seconds) {
                cut.roomListRouterStack.value.active.instance should beOfType<RoomListRouter.Wrapper.List>()
                cut.roomRouterStack.value.active.instance should beOfType<RoomRouter.Wrapper.View>()
            }

        }

        should("not show room view after switching to multi-pane when no room was selected") {
            val cut = mainViewModel()

            cut.setSinglePane(false)

            eventually(2.seconds) {
                cut.roomListRouterStack.value.active.instance should beOfType<RoomListRouter.Wrapper.List>()
                cut.roomRouterStack.value.active.instance should beOfType<RoomRouter.Wrapper.None>()
            }

        }

        should("close the room list when a room is selected and switch to single-pane") {
            val cut = mainViewModel()

            cut.onRoomSelected(UserId("test", "server"), RoomId("!Room:localhost"))
            cut.selectedRoomId.first { it == RoomId("!Room:localhost") }
            cut.setSinglePane(true)

            eventually(2.seconds) {
                cut.roomListRouterStack.value.active.instance should beOfType<RoomListRouter.Wrapper.None>()
                cut.roomRouterStack.value.active.instance should beOfType<RoomRouter.Wrapper.View>()
            }

        }

        should("show the room list when no room is selected and switch to single-pane") {
            val cut = mainViewModel()

            cut.setSinglePane(true)

            eventually(2.seconds) {
                cut.roomListRouterStack.value.active.instance should beOfType<RoomListRouter.Wrapper.List>()
                cut.roomRouterStack.value.active.instance should beOfType<RoomRouter.Wrapper.None>()
            }

        }

        should("show room list when the room view is left with the back button in single-pane") {
            val cut = mainViewModel()

            cut.onRoomSelected(UserId("test", "server"), RoomId("!Room:localhost"))
            cut.setSinglePane(true)
            eventually(2.seconds) { // wait for single pane to be set async
                cut.roomListRouterStack.value.active.instance should beOfType<RoomListRouter.Wrapper.None>()
            }

            backPressedHandler.back()

            eventually(2.seconds) {
                cut.roomListRouterStack.value.active.instance should beOfType<RoomListRouter.Wrapper.List>()
                cut.roomRouterStack.value.active.instance should beOfType<RoomRouter.Wrapper.None>()
            }
        }

        should("still show the room list when the back button is pressed in a single-pane with the room list visible") {
            val cut = mainViewModel()

            cut.setSinglePane(true)

            backPressedHandler.back()

            eventually(2.seconds) {
                cut.roomListRouterStack.value.active.instance should beOfType<RoomListRouter.Wrapper.List>()
                cut.roomRouterStack.value.active.instance should beOfType<RoomRouter.Wrapper.None>()
            }

        }

        should("still show the room list when the back button is pressed in a multi-pane with the room list visible") {
            val cut = mainViewModel()


            backPressedHandler.back()

            eventually(2.seconds) {
                cut.roomListRouterStack.value.active.instance should beOfType<RoomListRouter.Wrapper.List>()
                cut.roomRouterStack.value.active.instance should beOfType<RoomRouter.Wrapper.None>()
            }

        }

        should("show the back button in single-pane layout") {
            val cut = mainViewModel()

            cut.setSinglePane(true)

            eventually(2.seconds) {
                cut.isBackButtonVisible.value shouldBe true
            }

        }

        should("not show the back button in multi-pane layout") {
            val cut = mainViewModel()

            cut.setSinglePane(false)

            eventually(2.seconds) {
                cut.isBackButtonVisible.value shouldBe false
            }

        }

        should("show self verification modal when self verification is needed") {
            selfVerificationMethods returns MutableStateFlow(
                VerificationService.SelfVerificationMethods.CrossSigningEnabled(
                    setOf(
                        SelfVerificationMethod.CrossSignedDeviceVerification(
                            UserId(""),
                            setOf(),
                        ) { _, _ -> Result.failure(RuntimeException()) },
                        SelfVerificationMethod.AesHmacSha2RecoveryKey(
                            keySecretServiceMock,
                            keyTrustServiceMock,
                            "keyId",
                            SecretKeyEventContent.AesHmacSha2Key()
                        )
                    )
                )
            )
            everySuspend {
                matrixClientMock.syncOnce(any(), any(), any<suspend (Sync.Response) -> Unit>())
            } returns Result.success(Unit)

            val cut = mainViewModel()


            eventually(2.seconds) {
                cut.selfVerificationStack.value.active.configuration should beOfType<SelfVerificationRouter.Config.SelfVerification>()
            }
            cut.selfVerificationRouter.closeSelfVerification(UserId("test", "server"))
            eventually(2.seconds) {
                cut.selfVerificationStack.value.active.configuration should beOfType<SelfVerificationRouter.Config.None>()
            }
        }

        should("show multiple self verifications sequentially if needed") {
            // test
            selfVerificationMethods returns MutableStateFlow(
                VerificationService.SelfVerificationMethods.CrossSigningEnabled(
                    setOf(
                        SelfVerificationMethod.CrossSignedDeviceVerification(
                            UserId(""),
                            setOf(),
                        ) { _, _ -> Result.failure(RuntimeException()) },
                        SelfVerificationMethod.AesHmacSha2RecoveryKey(
                            keySecretServiceMock,
                            keyTrustServiceMock,
                            "keyId",
                            SecretKeyEventContent.AesHmacSha2Key()
                        )
                    )
                )
            )
            // test2
            every { verificationServiceMock2.activeDeviceVerification } returns MutableStateFlow(null)
            every { verificationServiceMock2.getSelfVerificationMethods() } returns MutableStateFlow(
                VerificationService.SelfVerificationMethods.CrossSigningEnabled(
                    setOf(
                        SelfVerificationMethod.CrossSignedDeviceVerification(
                            UserId(""),
                            setOf(),
                        ) { _, _ -> Result.failure(RuntimeException()) },
                        SelfVerificationMethod.AesHmacSha2RecoveryKey(
                            keySecretServiceMock,
                            keyTrustServiceMock,
                            "keyId",
                            SecretKeyEventContent.AesHmacSha2Key()
                        )
                    )
                )
            )

            everySuspend {
                matrixClientMock.syncOnce(any(), any(), any<suspend (Sync.Response) -> Unit>())
            } returns Result.success(Unit)
            everySuspend {
                matrixClientMock2.syncOnce(any(), any(), any<suspend (Sync.Response) -> Unit>())
            } returns Result.success(Unit)


            val cut = mainViewModel(
                mapOf(
                    UserId("test", "server") to matrixClientMock,
                    UserId("test2", "server") to matrixClientMock2
                )
            )

            eventually(2.seconds) {
                val configuration = cut.selfVerificationStack.value.active.configuration
                configuration.shouldBeInstanceOf<SelfVerificationRouter.Config.SelfVerification>()
                configuration.userId shouldBe UserId("test", "server")
            }
            cut.selfVerificationRouter.closeSelfVerification(UserId("test", "server"))
            eventually(2.seconds) {
                val configuration = cut.selfVerificationStack.value.active.configuration
                configuration.shouldBeInstanceOf<SelfVerificationRouter.Config.SelfVerification>()
                configuration.userId shouldBe UserId("test2", "server")
            }
            cut.selfVerificationRouter.closeSelfVerification(UserId("test2", "server"))
            eventually(2.seconds) {
                cut.selfVerificationStack.value.active.configuration.shouldBeInstanceOf<SelfVerificationRouter.Config.None>()
            }
        }

        should("not show self verification when at least one account isn't bootstrapped") {
            selfVerificationMethods returns MutableStateFlow(
                VerificationService.SelfVerificationMethods.CrossSigningEnabled(
                    setOf(
                        SelfVerificationMethod.CrossSignedDeviceVerification(
                            UserId(""),
                            setOf(),
                        ) { _, _ -> Result.failure(RuntimeException()) },
                        SelfVerificationMethod.AesHmacSha2RecoveryKey(
                            keySecretServiceMock,
                            keyTrustServiceMock,
                            "keyId",
                            SecretKeyEventContent.AesHmacSha2Key()
                        )
                    )
                )
            )
            everySuspend {
                matrixClientMock.syncOnce(any(), any(), any<suspend (Sync.Response) -> Unit>())
            } returns Result.success(Unit)

            messengerSettings.update<MatrixMessengerAccountSettingsBase>(UserId("test", "server")) {
                it.copy(
                    accountSetupFinished = false
                )
            }
            val cut = mainViewModel()

            continually(2.seconds) { cut.selfVerificationStack.value.active.configuration.shouldBeInstanceOf<SelfVerificationRouter.Config.None>() }
        }

        should("skip initial sync when initial sync is already done") {
            syncState returns MutableStateFlow(SyncState.STOPPED)
            networkAvailable returns true
            initialSyncDone returns MutableStateFlow(true)
            everySuspend { runInitialSyncMock.invoke(matrixClientMock) } calls {
                delay(500.milliseconds)
                true
            }

            val cut = mainViewModel()

            eventually(300.milliseconds) {
                cut.initialSyncStack.value.active.configuration shouldBe instanceOf<InitialSyncRouter.Config.None>()
            }
        }

        should("perform initial sync whe not yet done") {
            syncState returns MutableStateFlow(SyncState.STOPPED)
            networkAvailable returns true
            val initialSyncDoneFlow = MutableStateFlow(false)
            initialSyncDone returns initialSyncDoneFlow
            everySuspend { runInitialSyncMock.invoke(matrixClientMock) } calls {
                delay(500.milliseconds)
                true
            }

            val cut = mainViewModel()

            // initial state is: InitialSyncConfig.Undefined, but is switched so quickly, we cannot assert it here

            eventually(300.milliseconds) {
                val configuration = cut.initialSyncStack.value.active.configuration
                configuration.shouldBeTypeOf<InitialSyncRouter.Config.Sync>()
            }

            initialSyncDoneFlow.value = true
            eventually(2.seconds) {
                cut.initialSyncStack.value.active.configuration shouldBe instanceOf<InitialSyncRouter.Config.None>()
                verifySuspend { matrixClientMock.startSync() }
            }
        }

        should("directly switch to regular sync when no network is available") {
            syncState returns MutableStateFlow(SyncState.STOPPED)
            networkAvailable returns false
            initialSyncDone returns MutableStateFlow(false)

            val cut = mainViewModel()

            eventually(800.milliseconds) {
                cut.initialSyncStack.value.active.configuration shouldBe instanceOf<InitialSyncRouter.Config.None>()
                verifySuspend { matrixClientMock.startSync() }
            }
        }

        should("cancel the sync when the app is stopped and restart the sync when the app is resumed again") {
            syncState returns MutableStateFlow(SyncState.STOPPED)
            networkAvailable returns true
            initialSyncDone returns MutableStateFlow(true)
            everySuspend { runInitialSyncMock.invoke(matrixClientMock) } returns true
            mainViewModel()

            eventually(300.milliseconds) {
                verifySuspend {
                    matrixClientMock.startSync()
                }
            }

            lifecycle.stop()
            eventually(300.milliseconds) {
                verifySuspend {
                    matrixClientMock.cancelSync(any())
                }
            }

            lifecycle.resume()
            eventually(300.milliseconds) {
                verifySuspend {
                    matrixClientMock.startSync()
                }
            }
        }

        should("set the presence to OFFLINE when settings change to private and set presence to ONLINE when settings change to public") {
            val cut = mainViewModel()
            delay(300.milliseconds) // give viewmodel time to start first sync
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(UserId("test", "server")) {
                it.copy(presenceIsPublic = false)
            }
            delay(10.milliseconds)
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(UserId("test", "server")) {
                it.copy(presenceIsPublic = true)
            }
            delay(10.milliseconds)
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(UserId("test", "server")) {
                it.copy(presenceIsPublic = false)
            }
            delay(10.milliseconds)
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(UserId("test", "server")) {
                it.copy(presenceIsPublic = true)
            }
            delay(10.milliseconds)

            startSyncPresenceCapture shouldBe
                    listOf(
                        Presence.ONLINE, // initial sync
                        Presence.ONLINE, // first normal sync
                        Presence.OFFLINE, // 4 changes
                        Presence.ONLINE,
                        Presence.OFFLINE,
                        Presence.ONLINE
                    )
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun mainViewModel(
        matrixClients: Map<UserId, MatrixClient> = mapOf(UserId("test", "server") to matrixClientMock),
    ): MainViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val mainViewModel = MainViewModelImpl(
            viewModelContext = ViewModelContextImpl(
                componentContext = DefaultComponentContext(lifecycle, backHandler = backPressedHandler),
                di = koinApplication {
                    modules(createTestDefaultTrixnityMessengerModules(matrixClients, messengerSettings) + module {
                        single { CoroutineScope(Dispatchers.Default) }
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
                                    return roomHeaderViewModelMock
                                }
                            }
                        }
                        single<InputAreaViewModelFactory> {
                            object : InputAreaViewModelFactory {
                                override fun create(
                                    viewModelContext: MatrixClientViewModelContext,
                                    selectedRoomId: RoomId,
                                    onMessageEditFinished: (EventId) -> Unit,
                                    onMessageReplyToFinished: (EventId) -> Unit,
                                    onShowAttachmentSendView: (file: FileDescriptor) -> Unit
                                ): InputAreaViewModel {
                                    return inputAreaViewModelMock
                                }
                            }
                        }
                        single<RoomListViewModelFactory> {
                            object : RoomListViewModelFactory {
                                override fun create(
                                    viewModelContext: ViewModelContext,
                                    selectedRoomId: StateFlow<RoomId?>,
                                    onRoomSelected: (UserId, RoomId) -> Unit,
                                    onStartCreateNewRoom: (UserId) -> Unit,
                                    onUserSettingsSelected: () -> Unit,
                                    onOpenAppInfo: () -> Unit,
                                    onSendLogs: () -> Unit,
                                    onOpenAccountsOverview: () -> Unit,
                                    onAccountSelected: () -> Unit,
                                ): RoomListViewModel {
                                    return object : RoomListViewModel {
                                        override val selectedRoomId: StateFlow<RoomId?> = MutableStateFlow(null)
                                        override val error: MutableStateFlow<String?> = MutableStateFlow(null)
                                        override val errorType: MutableStateFlow<ErrorType> =
                                            MutableStateFlow(ErrorType.JUST_DISMISS)
                                        override val sortedRoomListElementViewModels: StateFlow<List<RoomListElement>> =
                                            MutableStateFlow(emptyList())
                                        override val syncStateError: StateFlow<Map<UserId, Boolean>> = MutableStateFlow(
                                            emptyMap()
                                        )
                                        override val allSyncError: StateFlow<Boolean> = MutableStateFlow(false)
                                        override val initialSyncFinished: StateFlow<Boolean> = MutableStateFlow(true)
                                        override val showSearch: MutableStateFlow<Boolean> = MutableStateFlow(false)
                                        override val searchTerm: MutableStateFlow<String> = MutableStateFlow("")
                                        override val spaces: StateFlow<List<SpaceViewModel>> = MutableStateFlow(
                                            emptyList()
                                        )
                                        override val activeSpace: MutableStateFlow<RoomId?> = MutableStateFlow(null)
                                        override val showSpaces: MutableStateFlow<Boolean> = MutableStateFlow(false)
                                        override val canCreateNewRoomWithAccount: StateFlow<Boolean> =
                                            MutableStateFlow(true)
                                        override val unverifiedAccounts: StateFlow<List<UserId>> =
                                            MutableStateFlow(listOf())
                                        override val closeProfileNeeded: Boolean = false
                                        override val accountViewModel: AccountViewModel = object : AccountViewModel {
                                            override val activeAccount: StateFlow<UserId?> = MutableStateFlow(null)
                                            override val isSingleAccount: StateFlow<Boolean> = MutableStateFlow(false)
                                            override val accounts: StateFlow<List<AccountInfo>> =
                                                MutableStateFlow(listOf())

                                            override fun selectActiveAccount(userId: UserId?) {
                                            }

                                            override fun userSettings() {
                                            }

                                            override fun appInfo() {
                                            }
                                        }

                                        override fun createNewRoom() {
                                        }

                                        override fun createNewRoomFor(userId: UserId) {
                                        }

                                        override fun selectRoom(roomId: RoomId) {
                                        }

                                        override fun errorDismiss() {
                                        }

                                        override fun sendLogs() {
                                        }

                                        override fun openAccountsOverview() {
                                        }

                                        override fun closeProfile() {
                                        }

                                        override fun verifyAccount(userId: UserId) {
                                        }
                                    }
                                }
                            }
                        }
                    })
                }.koin,
                coroutineContext = Dispatchers.Unconfined,
            ),
            onCreateNewAccount = {},
            onRemoveAccount = {},
        )
        mainViewModel.start()
        return mainViewModel
    }
}

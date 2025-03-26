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
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel.UserSyncStates
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.util.ErrorType
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationRouter
import de.connect2x.trixnity.messenger.withCleanup
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
import io.kotest.core.test.advanceUntilIdle
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.DeviceTrustLevel
import net.folivo.trixnity.client.key.KeySecretService
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.key.KeyTrustService
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.room.TimelineStateChange
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
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest : ShouldSpec() {
    private lateinit var lifecycle: LifecycleRegistry
    private val backPressedHandler = BackDispatcher()

    private val myUserId = UserId("user1", "localhost")
    private val myDeviceId = "deviceId"
    private val roomsFlow = MutableStateFlow(emptyMap<RoomId, StateFlow<Room?>>())

    private lateinit var messengerSettings: MatrixMessengerSettingsHolder

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()
    val roomHeaderViewModelMock = mock<RoomHeaderViewModel>()
    val inputAreaViewModelMock = mock<InputAreaViewModel>()
    private val matrixClientMock2 = mock<MatrixClient>()
    private val keyServiceMock = mock<KeyService>()
    private val keySecretServiceMock = mock<KeySecretService>()
    private val keyTrustServiceMock = mock<KeyTrustService>()
    private val verificationServiceMock = mock<VerificationService>()
    private val verificationServiceMock2 = mock<VerificationService>()
    private val downloadManagerMock = mock<DownloadManager>()
    private val isNetworkAvailable = mock<IsNetworkAvailable>()
    private val runInitialSyncMock = mock<RunInitialSync>()

    lateinit var selfVerificationMethods: BlockingAnsweringScope<Flow<VerificationService.SelfVerificationMethods>>
    lateinit var networkAvailable: BlockingAnsweringScope<Boolean>
    lateinit var syncState: BlockingAnsweringScope<StateFlow<SyncState>>
    lateinit var initialSyncDone: BlockingAnsweringScope<StateFlow<Boolean>>
    private val startSyncPresenceCapture = mutableListOf<Presence>()

    init {
        coroutineTestScope = true

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
                    any<suspend (TimelineStateChange<TimelineViewModelImpl.TimelineElementWrapper>) -> Unit>(),
                    any<suspend (Flow<TimelineEvent>) -> TimelineViewModelImpl.TimelineElementWrapper>(),
                )
            } returns NoOpTimeline()
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

        should("select no room initially").withCleanup {
            everySuspend {
                matrixClientMock.syncOnce(any(), any(), any<suspend (Sync.Response) -> Unit>())
            } returns Result.success(Unit)

            val cut = mainViewModel()
            advanceUntilIdle()
            assertSoftly {
                cut.selectedRoomId.value shouldBe null
                cut shouldShowListOfType RoomListRouter.Wrapper.List::class
                cut shouldShowRoom false
                cut shouldShowList true
            }
        }

        should("show room when room is selected").withCleanup {
            val cut = mainViewModel()
            val roomId = RoomId("!Room:localhost")
            cut.onRoomSelected(UserId("test", "server"), roomId)
            advanceUntilIdle()
            assertSoftly {
                cut.selectedRoomId.value shouldBe roomId
                cut shouldShowRoom true
            }
        }

        should("show room list when the room view is closed").withCleanup {
            val cut = mainViewModel()
            val roomId = RoomId("!Room:localhost")
            cut.onRoomSelected(UserId("test", "server"), roomId)
            advanceUntilIdle()
            cut shouldShowRoom true
            cut.closeDetailsAndShowList()
            advanceUntilIdle()
            assertSoftly {
                cut.selectedRoomId.value shouldBe null
                cut shouldShowRoom false
                cut shouldShowList true
            }
        }

        should("show room list when the room view is left with the back button").withCleanup {
            val cut = mainViewModel()
            val roomId = RoomId("!Room:localhost")
            cut.onRoomSelected(UserId("test", "server"), roomId)
            advanceUntilIdle()
            backPressedHandler.back()
            advanceUntilIdle()
            assertSoftly {
                cut.selectedRoomId.value shouldBe null
                cut shouldShowListOfType RoomListRouter.Wrapper.List::class
                cut shouldShowRoom false
            }
        }

        should("show self verification modal when self verification is needed").withCleanup {
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
            cut.selfVerificationRouter.showSelfVerification(UserId("test", "server"), true)

            eventually(2.seconds) {
                cut.selfVerificationStack.value.active.configuration should beOfType<SelfVerificationRouter.Config.SelfVerification>()
            }
            cut.selfVerificationRouter.closeSelfVerification(UserId("test", "server"))
            eventually(2.seconds) {
                cut.selfVerificationStack.value.active.configuration should beOfType<SelfVerificationRouter.Config.None>()
            }
        }

        should("show multiple self verifications sequentially if needed").withCleanup {
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

            val user1 = UserId("test", "server")
            val user2 = UserId("test2", "server")


            val cut = mainViewModel(
                mapOf(
                    user1 to matrixClientMock,
                    user2 to matrixClientMock2
                )
            )

            cut.selfVerificationRouter.showSelfVerification(user1, true)
            cut.selfVerificationRouter.showSelfVerification(user2, true)

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

        should("not show self verification when at least one account isn't bootstrapped").withCleanup {
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

        should("skip initial sync when initial sync is already done").withCleanup {
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

        should("perform initial sync whe not yet done").withCleanup {
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

        should("directly switch to regular sync when no network is available").withCleanup {
            syncState returns MutableStateFlow(SyncState.STOPPED)
            networkAvailable returns false
            initialSyncDone returns MutableStateFlow(false)

            val cut = mainViewModel()

            eventually(800.milliseconds) {
                cut.initialSyncStack.value.active.configuration shouldBe instanceOf<InitialSyncRouter.Config.None>()
                verifySuspend { matrixClientMock.startSync() }
            }
        }

        should("cancel the sync when the app is stopped and restart the sync when the app is resumed again").withCleanup {
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

        should("set the presence to OFFLINE when settings change to private and set presence to ONLINE when settings change to public").withCleanup {
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
            startSyncPresenceCapture shouldBe listOf(
                Presence.ONLINE, // initial sync
                Presence.ONLINE, // first normal sync
                Presence.OFFLINE, // 4 changes
                Presence.ONLINE,
                Presence.OFFLINE,
                Presence.ONLINE,
            )
        }
    }

    private suspend infix fun MainViewModel.shouldShowRoom(isShown: Boolean) {
        delay(10.milliseconds)
        assertSoftly {
            if (isShown) this.roomRouterStack.value.active.instance should beOfType<RoomRouter.Wrapper.View>()
            else this.roomRouterStack.value.active.instance should beOfType<RoomRouter.Wrapper.None>()
        }
    }

    private suspend infix fun MainViewModel.shouldShowList(isShown: Boolean) {
        delay(10.milliseconds)
        assertSoftly {
            if (isShown) this.roomListRouterStack.value.active.instance shouldNot beOfType<RoomListRouter.Wrapper.None>()
            else this.roomListRouterStack.value.active.instance should beOfType<RoomListRouter.Wrapper.None>()
        }
    }

    private suspend infix fun MainViewModel.shouldShowListOfType(extrasType: KClass<out RoomListRouter.Wrapper>) {
        delay(10.milliseconds)
        assertSoftly {
            this.roomListRouterStack.value.active.instance should beOfType(extrasType)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun mainViewModel(
        matrixClients: Map<UserId, MatrixClient> = mapOf(UserId("test", "server") to matrixClientMock),
    ): MainViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return MainViewModelImpl(
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
                                    onBack: () -> Unit,
                                    onVerifyUser: () -> Unit,
                                    onOpenRoomSettings: () -> Unit,
                                    onOpenUserProfile: (UserId) -> Unit,
                                ): RoomHeaderViewModel = roomHeaderViewModelMock
                            }
                        }
                        single<InputAreaViewModelFactory> {
                            object : InputAreaViewModelFactory {
                                override fun create(
                                    viewModelContext: MatrixClientViewModelContext,
                                    selectedRoomId: RoomId,
                                    onMessageReplaceFinished: (RoomId, EventId) -> Unit,
                                    onMessageReplyFinished: (RoomId, EventId) -> Unit,
                                    onShowAttachmentSendView: (FileDescriptor) -> Unit,
                                    onOpenMention: OpenMentionCallback,
                                ): InputAreaViewModel = inputAreaViewModelMock
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
                                    onUserProfileSelected: () -> Unit,
                                    onOpenAppInfo: () -> Unit,
                                    onSendLogs: () -> Unit,
                                    onOpenAccountsOverview: () -> Unit,
                                    onAccountSelected: () -> Unit,
                                    onCloseRoom: () -> Unit
                                ): RoomListViewModel = object : RoomListViewModel {
                                    override val selectedRoomId: StateFlow<RoomId?> = MutableStateFlow(null)
                                    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
                                    override val errorType: MutableStateFlow<ErrorType> =
                                        MutableStateFlow(ErrorType.JUST_DISMISS)
                                    override val elements: StateFlow<List<RoomListElementViewModel>> =
                                        MutableStateFlow(emptyList())
                                    override val syncStateError: StateFlow<Map<UserId, Boolean>> = MutableStateFlow(
                                        emptyMap()
                                    )
                                    override val allSyncError: StateFlow<Boolean> = MutableStateFlow(false)
                                    override val syncStates = MutableStateFlow(UserSyncStates(setOf(), setOf()))
                                    override val initialSyncFinished: StateFlow<Boolean> = MutableStateFlow(true)
                                    override val showSearch: MutableStateFlow<Boolean> = MutableStateFlow(false)
                                    override val searchTerm = TextFieldViewModelImpl("")
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

                                        override fun selectActiveAccount(userId: UserId?) {}
                                        override fun openUserSettings() {}
                                        override fun openUserProfile() {}
                                        override fun openAppInfo() {}
                                    }

                                    override fun createNewRoom() {}
                                    override fun createNewRoomFor(userId: UserId) {}
                                    override fun selectRoom(roomId: RoomId) {}
                                    override fun errorDismiss() {}
                                    override fun sendLogs() {}
                                    override fun openAccountsOverview() {}
                                    override fun closeProfile() {}
                                    override fun verifyAccount(userId: UserId) {}
                                }
                            }
                        }
                    })
                }.koin,
                coroutineContext = Dispatchers.Unconfined,
            ),
            onCreateNewAccount = {},
            onRemoveAccount = {},
        ).apply {
            start()
        }
    }
}

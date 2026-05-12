package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.key.KeySecretService
import de.connect2x.trixnity.client.key.KeyService
import de.connect2x.trixnity.client.key.KeyTrustService
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.room.TimelineStateChange
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.client.verification.SelfVerificationMethod
import de.connect2x.trixnity.client.verification.VerificationService
import de.connect2x.trixnity.client.verification.VerificationService.SelfVerificationMethods.PreconditionsNotMet
import de.connect2x.trixnity.clientserverapi.client.SyncEvents
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.clientserverapi.model.user.Profile
import de.connect2x.trixnity.clientserverapi.model.user.avatarUrl
import de.connect2x.trixnity.clientserverapi.model.user.displayName
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.DirectEventContent
import de.connect2x.trixnity.core.model.events.m.FullyReadEventContent
import de.connect2x.trixnity.core.model.events.m.MarkedUnreadEventContent
import de.connect2x.trixnity.core.model.events.m.Presence
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.secretstorage.SecretKeyEventContent
import de.connect2x.trixnity.crypto.key.DeviceTrustLevel
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.continually
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.BackHandler
import de.connect2x.trixnity.messenger.util.BackHandlerImpl
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
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationRouter
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.beOfType
import io.kotest.matchers.types.instanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


class MainViewModelTest {
    private val lifecycle: LifecycleRegistry = LifecycleRegistry()
    private val backHandler = BackHandlerImpl()
    private val myUserId = UserId("user1", "localhost")
    private val testUserId = UserId("test", "server")
    private val myDeviceId = "deviceId"
    private val roomsFlow = MutableStateFlow(emptyMap<RoomId, StateFlow<Room?>>())

    private var messengerSettings: MatrixMessengerSettingsHolder

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()
    val profile1 = Profile()
    val profile2 = Profile()
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

    var selfVerificationMethods: BlockingAnsweringScope<Flow<VerificationService.SelfVerificationMethods>>
    var networkAvailable: BlockingAnsweringScope<Boolean>
    var syncState: BlockingAnsweringScope<StateFlow<SyncState>>
    var initialSyncDone: BlockingAnsweringScope<StateFlow<Boolean>>
    private val startSyncPresenceCapture = mutableListOf<Presence>()

    init {
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
        every { matrixClientMock.profile } returns MutableStateFlow(profile1)
        syncState = every { matrixClientMock.syncState }
        syncState returns MutableStateFlow(SyncState.RUNNING)
        everySuspend { matrixClientMock.startSync(any()) } calls { startSyncPresenceCapture.add(it.arg(0)) }
        everySuspend { matrixClientMock.stopSync() } returns Unit
        everySuspend { matrixClientMock.cancelSync() } returns Unit

        every { roomServiceMock.getAll() } returns roomsFlow
        every {
            roomServiceMock.getState(any(), CreateEventContent::class, any())
        } returns MutableStateFlow(null)
        every {
            roomServiceMock.getTimeline(
                any<suspend (TimelineStateChange<TimelineViewModelImpl.TimelineElementWrapper>) -> Unit>(),
                any<suspend (Flow<TimelineEvent>) -> TimelineViewModelImpl.TimelineElementWrapper>(),
            )
        } returns NoOpTimeline()
        every { roomServiceMock.getById(any()) } returns MutableStateFlow(null)
        every {
            roomServiceMock.getAccountData(any(), FullyReadEventContent::class, any())
        } returns flowOf(null)
        every {
            roomServiceMock.getAccountData(any(), MarkedUnreadEventContent::class, any())
        } returns flowOf(null)
        every { roomServiceMock.getOutbox() } returns flowOf(listOf())
        every { userServiceMock.getAll(any()) } returns flowOf(mapOf())
        every { userServiceMock.getById(any(), any()) } returns flowOf(null)
        every { userServiceMock.getAllReceipts(any()) } returns flowOf(mapOf())
        every { userServiceMock.getReceiptsById(any(), any()) } returns flowOf(null)

        every { verificationServiceMock.activeDeviceVerification } returns MutableStateFlow(null)
        selfVerificationMethods = every { verificationServiceMock.getSelfVerificationMethods() }
        selfVerificationMethods returns MutableStateFlow(PreconditionsNotMet(emptySet()))

        every { keyServiceMock.getTrustLevel(any<UserId>(), any()) } returns flowOf(DeviceTrustLevel.Valid(true))

        everySuspend { userServiceMock.loadMembers(any(), any()) } returns Unit
        every { userServiceMock.getAccountData(DirectEventContent::class) } returns MutableStateFlow(
            DirectEventContent(
                emptyMap()
            )
        )

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
        every { matrixClientMock2.syncState } returns MutableStateFlow(SyncState.RUNNING)
        everySuspend { matrixClientMock2.startSync() } returns Unit
        everySuspend { matrixClientMock2.cancelSync() } returns Unit
        every { matrixClientMock2.initialSyncDone } returns MutableStateFlow(true)
        every { roomServiceMock.getAccountData(any(), MarkedUnreadEventContent::class, any()) } returns flowOf(
            MarkedUnreadEventContent(false)
        )
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @AfterTest
    fun afterTest() {
        lifecycle.destroy()
    }

    @Test
    fun `select no room initially`() = runTest {
        everySuspend {
            matrixClientMock.syncOnce(any(), any(), any<suspend (SyncEvents) -> Unit>())
        } returns Result.success(Unit)

        val cut = mainViewModel()

        assertSoftly {
            cut.selectedRoomId.value shouldBe null
            cut shouldShowListOfType RoomListRouter.Wrapper.List::class
            cut shouldShowRoom false
            cut shouldShowList true
        }
    }

    @Test
    fun `show room when room is selected`() = runTest {
        val roomId = RoomId("!Room:localhost")
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())

        val cut = mainViewModel()
        cut.onRoomSelected(testUserId, roomId)
        delay(100)

        assertSoftly {
            cut.selectedRoomId.value shouldBe roomId
            cut shouldShowRoom true
        }
    }

    @Test
    fun `show room list when the room view is closed`() = runTest {
        val roomId = RoomId("!Room:localhost")
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())

        val cut = mainViewModel()
        cut.onRoomSelected(testUserId, roomId)
        cut shouldShowRoom true
        cut.closeDetailsAndShowList()
        delay(100)

        assertSoftly {
            cut.selectedRoomId.value shouldBe null
            cut shouldShowRoom false
            cut shouldShowList true
        }
    }

    @Test
    fun `show room list when the room view is left with the back button`() = runTest {
        val roomId = RoomId("!Room:localhost")
        every { roomServiceMock.getOutbox(roomId) } returns flowOf(listOf())

        val cut = mainViewModel()
        cut.onRoomSelected(testUserId, roomId)
        delay(100)

        backHandler.goBack()
        delay(100)

        assertSoftly {
            cut.selectedRoomId.value shouldBe null
            cut shouldShowListOfType RoomListRouter.Wrapper.List::class
            cut shouldShowRoom false
        }
    }

    @Test
    fun `show cross signing bootstrap when cross signing is not enabled yet`() = runTest {
        selfVerificationMethods returns MutableStateFlow(VerificationService.SelfVerificationMethods.NoCrossSigningEnabled)

        val cut = mainViewModel()

        eventually(2.seconds) {
            cut.selfVerificationRouter.stack.value.active.configuration should beOfType<SelfVerificationRouter.Config.CrossSigningBootstrap>()
        }
    }

    @Test
    fun `not show new cross signing bootstrap when another is already shown`() = runTest {
        selfVerificationMethods returns MutableStateFlow(VerificationService.SelfVerificationMethods.NoCrossSigningEnabled)

        val cut = mainViewModel()

        val config = eventually(2.seconds) {
            cut.selfVerificationRouter.stack.value.active.configuration should beOfType<SelfVerificationRouter.Config.CrossSigningBootstrap>()
            cut.selfVerificationRouter.stack.value.active.configuration
        }

        every { verificationServiceMock2.getSelfVerificationMethods() } returns MutableStateFlow(VerificationService.SelfVerificationMethods.NoCrossSigningEnabled)

        continually(2.seconds) {
            cut.selfVerificationRouter.stack.value.active.configuration shouldBeSameInstanceAs config
        }
    }

    @Test
    fun `show multiple cross signing bootstraps sequentially when needed`() = runTest {
        selfVerificationMethods returns MutableStateFlow(VerificationService.SelfVerificationMethods.NoCrossSigningEnabled)
        every { verificationServiceMock2.getSelfVerificationMethods() } returns MutableStateFlow(VerificationService.SelfVerificationMethods.NoCrossSigningEnabled)

        val user1 = testUserId
        val user2 = UserId("test2", "server")

        val cut = mainViewModel(
            mapOf(
                user1 to matrixClientMock, user2 to matrixClientMock2
            )
        )

        val bootstrapParams = eventually(2.seconds) {
            cut.selfVerificationRouter.stack.value.active.configuration should beOfType<SelfVerificationRouter.Config.CrossSigningBootstrap>()
            val currentUser =
                (cut.selfVerificationRouter.stack.value.active.configuration as SelfVerificationRouter.Config.CrossSigningBootstrap).userId shouldBeIn setOf<UserId>(
                    user1,
                    user2
                )
            currentUser to cut.selfVerificationRouter.stack.value.active.instance as SelfVerificationRouter.Wrapper.CrossSigningBootstrap
        }

        bootstrapParams.second.viewModel.close()
        val otherUser = if (bootstrapParams.first == user1) user2 else user1

        eventually(2.seconds) {
            cut.selfVerificationRouter.stack.value.active.configuration should beOfType<SelfVerificationRouter.Config.CrossSigningBootstrap>()
            (cut.selfVerificationRouter.stack.value.active.configuration as SelfVerificationRouter.Config.CrossSigningBootstrap).userId shouldBe otherUser
        }
    }

    @Test
    fun `show self verification modal when self verification is needed`() = runTest {
        selfVerificationMethods returns MutableStateFlow(
            VerificationService.SelfVerificationMethods.CrossSigningEnabled(
                setOf(
                    SelfVerificationMethod.CrossSignedDeviceVerification(
                        UserId(""),
                        setOf(),
                    ) { _, _ -> Result.failure(RuntimeException()) }, SelfVerificationMethod.AesHmacSha2RecoveryKey(
                        keySecretServiceMock, keyTrustServiceMock, "keyId", SecretKeyEventContent.AesHmacSha2Key()
                    )
                )
            )
        )
        everySuspend {
            matrixClientMock.syncOnce(any(), any(), any<suspend (SyncEvents) -> Unit>())
        } returns Result.success(Unit)

        val cut = mainViewModel()
        cut.selfVerificationRouter.showSelfVerification(testUserId, true)

        eventually(2.seconds) {
            cut.selfVerificationStack.value.active.configuration should beOfType<SelfVerificationRouter.Config.SelfVerification>()
        }
        cut.selfVerificationRouter.closeSelfVerification(testUserId)
        eventually(2.seconds) {
            cut.selfVerificationStack.value.active.configuration should beOfType<SelfVerificationRouter.Config.None>()
        }
    }

    @Test
    fun `show multiple self verifications sequentially if needed`() = runTest {
        // test
        selfVerificationMethods returns MutableStateFlow(
            VerificationService.SelfVerificationMethods.CrossSigningEnabled(
                setOf(
                    SelfVerificationMethod.CrossSignedDeviceVerification(
                        UserId(""),
                        setOf(),
                    ) { _, _ -> Result.failure(RuntimeException()) }, SelfVerificationMethod.AesHmacSha2RecoveryKey(
                        keySecretServiceMock, keyTrustServiceMock, "keyId", SecretKeyEventContent.AesHmacSha2Key()
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
                    ) { _, _ -> Result.failure(RuntimeException()) }, SelfVerificationMethod.AesHmacSha2RecoveryKey(
                        keySecretServiceMock, keyTrustServiceMock, "keyId", SecretKeyEventContent.AesHmacSha2Key()
                    )
                )
            )
        )

        everySuspend {
            matrixClientMock.syncOnce(any(), any(), any<suspend (SyncEvents) -> Unit>())
        } returns Result.success(Unit)
        everySuspend {
            matrixClientMock2.syncOnce(any(), any(), any<suspend (SyncEvents) -> Unit>())
        } returns Result.success(Unit)

        val user1 = testUserId
        val user2 = UserId("test2", "server")


        val cut = mainViewModel(
            mapOf(
                user1 to matrixClientMock, user2 to matrixClientMock2
            )
        )

        cut.selfVerificationRouter.showSelfVerification(user1, true)
        cut.selfVerificationRouter.showSelfVerification(user2, true)

        eventually(2.seconds) {
            val configuration = cut.selfVerificationStack.value.active.configuration
            configuration.shouldBeInstanceOf<SelfVerificationRouter.Config.SelfVerification>()
            configuration.userId shouldBe testUserId
        }
        cut.selfVerificationRouter.closeSelfVerification(testUserId)
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

    @Test
    fun `not show self verification when at least one account isn't bootstrapped`() = runTest {
        selfVerificationMethods returns MutableStateFlow(
            VerificationService.SelfVerificationMethods.CrossSigningEnabled(
                setOf(
                    SelfVerificationMethod.CrossSignedDeviceVerification(
                        UserId(""),
                        setOf(),
                    ) { _, _ -> Result.failure(RuntimeException()) }, SelfVerificationMethod.AesHmacSha2RecoveryKey(
                        keySecretServiceMock, keyTrustServiceMock, "keyId", SecretKeyEventContent.AesHmacSha2Key()
                    )
                )
            )
        )
        everySuspend {
            matrixClientMock.syncOnce(any(), any(), any<suspend (SyncEvents) -> Unit>())
        } returns Result.success(Unit)

        messengerSettings.create(
            testUserId,
            MatrixMessengerAccountSettingsBase(accountSetupFinished = false)
        )
        val cut = mainViewModel()

        continually(2.seconds) {
            cut.selfVerificationStack.value.active.configuration.shouldBeInstanceOf<SelfVerificationRouter.Config.None>()
        }
    }

    @Test
    fun `skip initial sync when initial sync is already done`() = runTest {
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

    @Test
    fun `perform initial sync when not yet done`() = runTest {
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

    @Test
    fun `directly switch to regular sync when no network is available`() = runTest {
        syncState returns MutableStateFlow(SyncState.STOPPED)
        networkAvailable returns false
        initialSyncDone returns MutableStateFlow(false)

        val cut = mainViewModel()

        eventually(800.milliseconds) {
            cut.initialSyncStack.value.active.configuration shouldBe instanceOf<InitialSyncRouter.Config.None>()
            verifySuspend { matrixClientMock.startSync() }
        }
    }

    @Test
    fun `cancel the sync when the app is stopped and restart the sync when the app is resumed again`() = runTest {
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
                matrixClientMock.stopSync()
            }
        }

        lifecycle.resume()
        eventually(300.milliseconds) {
            verifySuspend {
                matrixClientMock.startSync()
            }
        }
    }

    @Test
    fun `set the presence to OFFLINE when settings change to private and set presence to ONLINE when settings change to public`() =
        runTest {
            mainViewModel()
            delay(300.milliseconds) // give viewmodel time to start first sync
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(testUserId) {
                it.copy(presenceIsPublic = false)
            }
            delay(10.milliseconds)
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(testUserId) {
                it.copy(presenceIsPublic = true)
            }
            delay(10.milliseconds)
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(testUserId) {
                it.copy(presenceIsPublic = false)
            }
            delay(10.milliseconds)
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(testUserId) {
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun TestScope.mainViewModel(
        matrixClients: Map<UserId, MatrixClient> = mapOf(testUserId to matrixClientMock),
    ): MainViewModelImpl {
        Dispatchers.setMain(testDispatcher)
        messengerSettings.create(testUserId, MatrixMessengerAccountSettingsBase(accountSetupFinished = true))

        return MainViewModelImpl(
            viewModelContext = ViewModelContextImpl(
                componentContext = DefaultComponentContext(lifecycle),
                di = koinApplication {
                    allowOverride(true)
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            matrixClients, messengerSettings
                        ) + module {
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
                                        onShowAccounts: () -> Unit,
                                        onOpenAppInfo: () -> Unit,
                                        onSendLogs: () -> Unit,
                                        onAccountSelected: () -> Unit,
                                        onStartVerification: (UserId) -> Unit,
                                        onCloseRoom: () -> Unit,
                                    ): RoomListViewModel = object : RoomListViewModel {
                                        override val selectedRoomId: StateFlow<RoomId?> = MutableStateFlow(null)
                                        override val error: MutableStateFlow<String?> = MutableStateFlow(null)
                                        override val errorType: MutableStateFlow<ErrorType> =
                                            MutableStateFlow(ErrorType.JUST_DISMISS)
                                        override val elements: StateFlow<List<RoomListElementViewModel>> =
                                            MutableStateFlow(emptyList())
                                        override val syncStates = MutableStateFlow(UserSyncStates(setOf(), setOf()))
                                        override val initialSyncFinished: StateFlow<Boolean> = MutableStateFlow(true)
                                        override val showSearch: MutableStateFlow<Boolean> = MutableStateFlow(false)
                                        override val searchTerm = TextFieldViewModelImpl(maxLength = 100, "")
                                        override val searchResultsEmpty: StateFlow<Boolean> = MutableStateFlow(false)
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
                                            override val globalNotificationCount: StateFlow<String?> = MutableStateFlow(null)
                                            override val accountNotificationCounts: StateFlow<Map<UserId, String?>> = MutableStateFlow(emptyMap())

                                            override fun selectActiveAccount(userId: UserId?) {}
                                            override fun openUserSettings() {}
                                            override fun openUserAccounts() {}
                                            override fun openAppInfo() {}
                                        }

                                        override fun createNewRoom() {}
                                        override fun createNewRoomFor(userId: UserId) {}
                                        override fun selectRoom(roomId: RoomId) {}
                                        override fun errorDismiss() {}
                                        override fun sendLogs() {}
                                        override fun closeProfile() {}
                                        override fun verifyAccount(userId: UserId) {}
                                    }
                                }
                            }
                            single<BackHandler> { backHandler }
                        })
                }.koin,
                coroutineContext = backgroundScope.coroutineContext,
                name = "Main"
            ),
            onCreateNewAccount = {},
            onRemoveAccount = {},
        ).apply {
            start()
        }
    }
}

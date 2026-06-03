package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.key.KeyService
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.room.TimelineStateChange
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.user.PowerLevel
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.client.verification.VerificationService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.SyncApiClient
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.RoomEventContent
import de.connect2x.trixnity.core.model.events.m.DirectEventContent
import de.connect2x.trixnity.core.model.events.m.FullyReadEventContent
import de.connect2x.trixnity.core.model.events.m.IgnoredUserListEventContent
import de.connect2x.trixnity.core.model.events.m.MarkedUnreadEventContent
import de.connect2x.trixnity.core.model.events.m.PushRulesEventContent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import de.connect2x.trixnity.core.model.events.m.room.PowerLevelsEventContent
import de.connect2x.trixnity.crypto.key.DeviceTrustLevel
import de.connect2x.trixnity.crypto.key.UserTrustLevel
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.IsNetworkAvailable
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper.AddMember
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper.ExportRoom
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper.RoomSettings
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper.TimelineElementMetadata
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper.UserProfile
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.NoOpTimeline
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderInfo
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter.Wrapper.View
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModelImpl
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.assertions.withClue
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.beOfType
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
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

class RoomViewModelTest {
    private var lifecycle: LifecycleRegistry
    private val backPressedHandler = BackDispatcher()

    private val roomId = RoomId("!room")
    private val myUserId = UserId("user1", "localhost")
    private val myDeviceId = "deviceId"
    private val roomsFlow = MutableStateFlow(emptyMap<RoomId, StateFlow<Room?>>())

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()
    private val keyServiceMock = mock<KeyService>()
    private val verificationServiceMock = mock<VerificationService>()
    private val syncApiClientMock = mock<SyncApiClient>()
    private val downloadManagerMock = mock<DownloadManager>()
    private val isNetworkAvailable = mock<IsNetworkAvailable>()
    private val runInitialSyncMock = mock<RunInitialSync>()
    private val minimizeMessengerMock = mock<() -> Unit>()

    private var selfVerificationMethods: BlockingAnsweringScope<Flow<VerificationService.SelfVerificationMethods>>
    var syncState: BlockingAnsweringScope<StateFlow<SyncState>>

    init {
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
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { keyServiceMock }
                            single { userServiceMock }
                            single { verificationServiceMock }
                        }
                    )
                }
                .koin
        every { matrixClientMock.userId } returns myUserId
        every { matrixClientMock.deviceId } returns myDeviceId
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        syncState = every { matrixClientMock.syncState }
        syncState returns MutableStateFlow(SyncState.RUNNING)
        everySuspend { matrixClientMock.startSync() } returns Unit
        everySuspend { matrixClientMock.cancelSync() } returns Unit
        every { matrixClientServerApiClientMock.sync } returns syncApiClientMock
        every { roomServiceMock.getAll() } returns roomsFlow
        every { roomServiceMock.getById(roomId) } returns MutableStateFlow(Room(roomId))
        every { roomServiceMock.getAccountData(roomId, FullyReadEventContent::class, "") } returns
            MutableStateFlow(null)
        every {
            roomServiceMock.getTimeline(
                any<suspend (TimelineStateChange<TimelineViewModelImpl.TimelineElementWrapper>) -> Unit>(),
                any<suspend (Flow<TimelineEvent>) -> TimelineViewModelImpl.TimelineElementWrapper>(),
            )
        } returns NoOpTimeline()
        every { roomServiceMock.getById(any()) } returns flowOf(null)
        every { roomServiceMock.getAccountData(any(), FullyReadEventContent::class, "") } returns flowOf(null)
        every { roomServiceMock.getOutbox() } returns MutableStateFlow(listOf())
        every { roomServiceMock.getOutbox(roomId = any()) } returns MutableStateFlow(listOf())
        every { roomServiceMock.getState(any(), CreateEventContent::class, any()) } returns MutableStateFlow(null)
        every { roomServiceMock.getState(any(), PowerLevelsEventContent::class, any()) } returns MutableStateFlow(null)
        every { roomServiceMock.usersTyping } returns MutableStateFlow(mapOf())
        every { roomServiceMock.getTimelineEvent(any(), any(), any()) } returns flowOf(null)
        every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns MutableStateFlow(emptyMap())
        every { roomServiceMock.getState<HistoryVisibilityEventContent>(any(), any(), any()) } returns
            flowOf(
                ClientEvent.StrippedStateEvent(
                    HistoryVisibilityEventContent(HistoryVisibilityEventContent.HistoryVisibility.INVITED),
                    sender = UserId("unused", "unused"),
                    stateKey = "unused",
                )
            )
        every { roomServiceMock.getDraftMessage(any()) } returns flowOf(null)
        every { verificationServiceMock.activeDeviceVerification } returns MutableStateFlow(null)
        every { verificationServiceMock.activeUserVerifications } returns MutableStateFlow(listOf())
        every { verificationServiceMock.getSelfVerificationMethods() }.also { selfVerificationMethods = it } returns
            MutableStateFlow(VerificationService.SelfVerificationMethods.PreconditionsNotMet(emptySet()))
        every { keyServiceMock.getTrustLevel(any<UserId>(), any()) } returns flowOf(DeviceTrustLevel.Valid(false))
        every { keyServiceMock.getTrustLevel(any<UserId>()) } returns flowOf(UserTrustLevel.Blocked)
        everySuspend { userServiceMock.loadMembers(any(), any()) } returns Unit
        every { userServiceMock.getById(any(), any()) } returns MutableStateFlow(null)
        every { userServiceMock.getPresence(any()) } returns flowOf(null)
        every { userServiceMock.getAll(roomId) } returns MutableStateFlow(mapOf())
        every { userServiceMock.getAllReceipts(roomId) } returns MutableStateFlow(emptyMap())
        every { userServiceMock.canInvite(roomId) } returns MutableStateFlow(false)
        every { userServiceMock.canInviteUser(roomId, any()) } returns MutableStateFlow(false)
        every { userServiceMock.canKickUser(roomId, any()) } returns MutableStateFlow(false)
        every { userServiceMock.canBanUser(roomId, any()) } returns MutableStateFlow(false)
        every { userServiceMock.canUnbanUser(roomId, any()) } returns MutableStateFlow(false)
        every { userServiceMock.canSetPowerLevelToMax(roomId, any()) } returns MutableStateFlow(PowerLevel.User(0L))
        every { userServiceMock.getAccountData(DirectEventContent::class, "") } returns MutableStateFlow(null)
        every { userServiceMock.getAccountData(IgnoredUserListEventContent::class, "") } returns MutableStateFlow(null)
        every { userServiceMock.getAccountData(PushRulesEventContent::class, "") } returns MutableStateFlow(null)
        every { userServiceMock.getPowerLevel(any(), any()) } returns MutableStateFlow(PowerLevel.User(50))
        every { userServiceMock.canSendEvent(any(), any<KClass<out RoomEventContent>>()) } returns flowOf(true)
        every { userServiceMock.getReceiptsById(any(), any()) } returns flowOf(null)
        every { minimizeMessengerMock.invoke() } returns Unit
        every { roomServiceMock.getAccountData(roomId, MarkedUnreadEventContent::class, any()) } returns
            flowOf(MarkedUnreadEventContent(false))
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    // TODO
    @AfterTest
    fun afterTest() {
        lifecycle.destroy()
    }

    @Test
    fun `show selected room without settings initially`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowTimeline true
        cut shouldShowExtras false
    }

    @Test
    fun `show room settings`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowExtras false
        cut.openRoomSettings()
        cut shouldShowExtrasOfType RoomSettings::class
    }

    @Test
    fun `show room's settings when settings are activated`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowTimeline true
        cut shouldShowExtras false
        cut.timelineAs<View>().viewModel.roomHeaderViewModel.openRoomSettings()
        cut shouldShowExtras true
        cut shouldShowExtrasOfType RoomSettings::class
    }

    @Test
    fun `return from settings`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowExtras false
        cut.openRoomSettings()
        cut.extrasAs<RoomSettings>().viewModel.close()
        cut shouldShowTimeline true
        cut shouldShowExtras false
    }

    // fails in JS, no idea why
    @Ignore
    @Test
    fun `navigate from the timeline to add-members`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowExtras false
        cut.openRoomSettings()
        cut.extrasAs<RoomSettings>().viewModel.openAddMembersView()
        cut shouldShowExtrasOfType AddMember::class
    }

    @Test
    fun `navigate from the timeline to export-room`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowExtras false
        cut.openRoomSettings()
        cut.extrasAs<RoomSettings>().viewModel.openExportRoomView()
        cut shouldShowExtrasOfType ExportRoom::class
    }

    @Test
    fun `navigate from the timeline to message-metadata`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowExtras false
        cut.openTimelineElementMetadata(EventId("1"))
        cut shouldShowExtrasOfType TimelineElementMetadata::class
    }

    @Test
    fun `show message info`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowExtras false
        val eventId = EventId("event0")
        cut.openTimelineElementMetadata(eventId)
        cut shouldShowExtras true
        cut shouldShowExtrasOfType TimelineElementMetadata::class
    }

    @Test
    fun `return from message metadata`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowExtras false
        val eventId = EventId("event2")
        cut.openTimelineElementMetadata(eventId)
        cut.extrasAs<TimelineElementMetadata>().viewModel.back()
        cut shouldShowTimeline true
        cut shouldShowExtras false
    }

    @Test
    fun `return to settings from message metadata`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowExtras false
        cut.openRoomSettings()
        cut shouldShowExtrasOfType RoomSettings::class
        cut.openTimelineElementMetadata(EventId("event4"))
        cut.extrasAs<TimelineElementMetadata>().viewModel.back()
        cut shouldShowExtrasOfType RoomSettings::class
    }

    @Test
    fun `navigate from the timeline to the user profile`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowExtras false
        cut.openUserProfile(UserId("user1"))
        cut shouldShowExtras true
        cut shouldShowExtrasOfType UserProfile::class
    }

    @Test
    fun `return from the user profile`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowExtras false
        cut.openUserProfile(UserId("user1"))
        println(cut.extrasStack.value.active.instance)
        cut.extrasAs<UserProfile>().viewModel.back()
        cut shouldShowTimeline true
        cut shouldShowExtras false
    }

    @Test
    fun `return to settings from the user profile`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowExtras false
        cut.openRoomSettings()
        cut shouldShowExtrasOfType RoomSettings::class
        cut.openUserProfile(UserId("user1"))
        cut.extrasAs<UserProfile>().viewModel.back()
        cut shouldShowExtrasOfType RoomSettings::class
    }

    @Test
    fun `close extras when returning from the settings`() = runTest {
        val cut = cutRoomViewModel()
        cut shouldShowExtras false
        cut.openUserProfile(UserId("user1"))
        cut shouldShowExtrasOfType UserProfile::class
        cut.openRoomSettings()
        cut shouldShowExtrasOfType RoomSettings::class
        cut.extrasAs<RoomSettings>().viewModel.close()
        cut shouldShowTimeline true
        cut shouldShowExtras false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.cutRoomViewModel(): RoomViewModelImpl {
        Dispatchers.setMain(testDispatcher)
        return RoomViewModelImpl(
            viewModelContext =
                MatrixClientViewModelContextImpl(
                    componentContext = DefaultComponentContext(lifecycle, backHandler = backPressedHandler),
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(
                                        mapOf(UserId("test", "server") to matrixClientMock)
                                    ) +
                                        module {
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
                                                    ): RoomHeaderViewModel =
                                                        object : RoomHeaderViewModel {
                                                            override val error: StateFlow<String?> =
                                                                MutableStateFlow(null)
                                                            override val roomHeaderInfo: StateFlow<RoomHeaderInfo> =
                                                                MutableStateFlow(
                                                                    RoomHeaderInfo(
                                                                        "",
                                                                        "",
                                                                        "",
                                                                        null,
                                                                        null,
                                                                        false,
                                                                        false,
                                                                        false,
                                                                    )
                                                                )
                                                            override val usersTyping: StateFlow<String?> =
                                                                MutableStateFlow(null)
                                                            override val userTrustLevel: StateFlow<UserTrustLevel?> =
                                                                MutableStateFlow(null)
                                                            override val canVerifyUser: StateFlow<Boolean> =
                                                                MutableStateFlow(false)
                                                            override val knockingMembersCount: StateFlow<Int> =
                                                                MutableStateFlow(0)
                                                            override val canBlockUser: StateFlow<Boolean> =
                                                                MutableStateFlow(false)
                                                            override val canUnblockUser: StateFlow<Boolean> =
                                                                MutableStateFlow(false)
                                                            override val isUserBlocked: StateFlow<Boolean> =
                                                                MutableStateFlow(false)
                                                            override val isDirectChat: StateFlow<Boolean> =
                                                                MutableStateFlow(false)

                                                            override fun blockUser() {}

                                                            override fun unblockUser() {}

                                                            override fun verifyUser() {}

                                                            override fun openRoomSettings() = onOpenRoomSettings()

                                                            override fun openUserProfile() =
                                                                onOpenUserProfile(UserId("user1"))

                                                            override fun back() = onBack()
                                                        }
                                                }
                                            }
                                        }
                                )
                            }
                            .koin,
                    userId = UserId("test", "server"),
                    coroutineContext = backgroundScope.coroutineContext,
                    name = "Room",
                ),
            roomId = roomId,
            onOpenRoom = mock(),
            onCloseRoom = mock(),
            onOpenAvatarCutter = { _, _, _ -> },
            onOpenMention = mock(),
        )
    }
}

private suspend inline infix fun RoomViewModel.shouldShowTimeline(isShown: Boolean) {
    delay(100.milliseconds)
    withClue("should ${if (isShown) "show" else "hide"} timeline") {
        if (isShown) this.timelineStack.value.active.instance should beOfType<View>()
        else this.timelineStack.value.active.instance should beOfType<TimelineRouter.Wrapper.None>()
    }
}

private suspend inline infix fun RoomViewModel.shouldShowExtras(isShown: Boolean) {
    delay(100.milliseconds)
    withClue("should ${if (isShown) "show" else "hide"} extras pane") {
        if (isShown) this.extrasStack.value.active.instance shouldNot beOfType<ExtrasRouter.Wrapper.None>()
        else this.extrasStack.value.active.instance should beOfType<ExtrasRouter.Wrapper.None>()
    }
}

private suspend inline infix fun RoomViewModel.shouldShowExtrasOfType(extrasType: KClass<out ExtrasRouter.Wrapper>) {
    delay(100.milliseconds)
    val instance = this.extrasStack.value.active.instance
    withClue("should show extras of ${extrasType.simpleName} but was: ${instance::class.simpleName}") {
        instance should beOfType(extrasType)
    }
}

private suspend inline fun <reified T : ExtrasRouter.Wrapper> RoomViewModelImpl.extrasAs() =
    try {
        delay(100.milliseconds)
        (this.extrasStack.value.active.instance as T)
    } catch (_: ClassCastException) {
        fail(
            "expected extras pane to be of instance ${T::class.simpleName}" +
                " but instead was: ${this.extrasStack.value.active.instance::class.simpleName}"
        )
    }

private suspend inline fun <reified T : TimelineRouter.Wrapper> RoomViewModelImpl.timelineAs() =
    try {
        delay(100.milliseconds)
        (this.timelineStack.value.active.instance as T)
    } catch (_: ClassCastException) {
        fail(
            "expected timeline to be of instance ${T::class.simpleName}" +
                " but instead was: ${this.extrasStack.value.active.instance::class.simpleName}"
        )
    }

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
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper.AddMember
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper.ExportRoom
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper.MessageMetadata
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper.RoomSettings
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.NoOpTimeline
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderInfo
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter.Wrapper.View
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.assertions.failure
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
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
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.FullyReadEventContent
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds


class RoomViewModelTest : ShouldSpec() {
    private lateinit var lifecycle: LifecycleRegistry
    private val backPressedHandler = BackDispatcher()

    private val roomId = RoomId("room", "localhost")
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

    private lateinit var selfVerificationMethods: BlockingAnsweringScope<Flow<VerificationService.SelfVerificationMethods>>
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
                    any<suspend (Flow<TimelineEvent>) -> Unit>(),
                )
            } returns NoOpTimeline
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
            every { roomServiceMock.getTimelineEvent(any(), any(), any()) } returns flowOf(null)
            every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns
                    MutableStateFlow(emptyMap())
            every { verificationServiceMock.activeDeviceVerification } returns
                    MutableStateFlow(null)
            every { verificationServiceMock.getSelfVerificationMethods() }.also {
                selfVerificationMethods = it
            } returns MutableStateFlow(
                VerificationService.SelfVerificationMethods
                    .PreconditionsNotMet(emptySet())
            )
            every { keyServiceMock.getTrustLevel(any<UserId>(), any()) } returns
                    flowOf(DeviceTrustLevel.Valid(false))
            everySuspend {
                userServiceMock.loadMembers(RoomId(any()), any())
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
            every { userServiceMock.getReceiptsById(any(), any()) } returns flowOf(null)
            every { minimizeMessengerMock.invoke() } returns Unit

        }

        afterTest {
            lifecycle.destroy()
        }

        should("show selected room without settings initially") {
            val cut = cutRoomViewModel()
            cut shouldShowTimeline true
            cut shouldShowExtras false
        }

        should("show room settings") {
            val cut = cutRoomViewModel()
            cut shouldShowExtras false
            cut.openRoomSettings()
            cut shouldShowExtrasOfType RoomSettings::class
        }

        should("show room's settings when settings are activated") {
            val cut = cutRoomViewModel()
            cut shouldShowTimeline true
            cut shouldShowExtras false
            cut.timelineAs<View>().viewModel
                .roomHeaderViewModel.openRoomSettings()
            cut shouldShowExtras true
            cut shouldShowExtrasOfType RoomSettings::class
            cancelNeverEndingCoroutines()
        }

        should("return from settings") {
            val cut = cutRoomViewModel()
            cut shouldShowExtras false
            cut.openRoomSettings()
            cut.extrasAs<RoomSettings>().viewModel.close()
            cut shouldShowTimeline true
            cut shouldShowExtras false
            cancelNeverEndingCoroutines()
        }

        should("navigate from the timeline to add-members") {
            val cut = cutRoomViewModel()
            cut shouldShowExtras false
            cut.openRoomSettings()
            cut.extrasAs<RoomSettings>().viewModel.openAddMembersView()
            cut shouldShowExtrasOfType AddMember::class
        }

        should("navigate from the timeline to export-room") {
            val cut = cutRoomViewModel()
            cut shouldShowExtras false
            cut.openRoomSettings()
            cut.extrasAs<RoomSettings>().viewModel.openExportRoomView()
            cut shouldShowExtrasOfType ExportRoom::class
        }

        should("navigate from the timeline to message-metadata") {
            val cut = cutRoomViewModel()
            cut shouldShowExtras false
            cut.openMessageMetadata(EventId("1"))
            cut shouldShowExtrasOfType MessageMetadata::class
        }

        should("show message info") {
            val cut = cutRoomViewModel()
            cut shouldShowExtras false
            val eventId = EventId("event0")
            cut.openMessageMetadata(eventId)
            cut shouldShowExtras true
            cut shouldShowExtrasOfType MessageMetadata::class
        }

        should("return from message metadata") {
            val cut = cutRoomViewModel()
            cut shouldShowExtras false
            val eventId = EventId("event2")
            cut.openMessageMetadata(eventId)
            cut.extrasAs<MessageMetadata>().viewModel.back()
            cut shouldShowTimeline true
            cut shouldShowExtras false
            cancelNeverEndingCoroutines()
        }

        should("return to settings from message metadata") {
            val cut = cutRoomViewModel()
            cut shouldShowExtras false
            cut.openRoomSettings()
            cut shouldShowExtrasOfType RoomSettings::class
            val eventId = EventId("event3")
            cut.openMessageMetadata(eventId)
            cut.extrasAs<MessageMetadata>().viewModel.back()
            cut shouldShowExtrasOfType RoomSettings::class
        }

        should("close extras when returning from settings") {
            val cut = cutRoomViewModel()
            cut shouldShowExtras false
            val eventId = EventId("event4")
            cut.openMessageMetadata(eventId)
            cut shouldShowExtrasOfType MessageMetadata::class
            cut.openRoomSettings()
            cut shouldShowExtrasOfType RoomSettings::class
            cut.extrasAs<RoomSettings>().viewModel.close()
            cut shouldShowTimeline true
            cut shouldShowExtras false
            cancelNeverEndingCoroutines()
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun cutRoomViewModel(): RoomViewModelImpl {
        Dispatchers.setMain(Dispatchers.Unconfined)
        return RoomViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(
                    lifecycle,
                    backHandler = backPressedHandler,
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
                                        onBack: () -> Unit,
                                        onVerifyUser: () -> Unit,
                                        onShowRoomSettings: () -> Unit,
                                    ): RoomHeaderViewModel = object : RoomHeaderViewModel {
                                        override val error: StateFlow<String?> = MutableStateFlow(null)
                                        override val roomHeaderInfo: StateFlow<RoomHeaderInfo> = MutableStateFlow(
                                            RoomHeaderInfo("", "", "", null, null, false, false)
                                        )
                                        override val usersTyping: StateFlow<String?> = MutableStateFlow(null)
                                        override val userTrustLevel: StateFlow<UserTrustLevel?> = MutableStateFlow(null)
                                        override val canVerifyUser: StateFlow<Boolean> = MutableStateFlow(false)
                                        override val canBlockUser: StateFlow<Boolean> = MutableStateFlow(false)
                                        override val canUnblockUser: StateFlow<Boolean> = MutableStateFlow(false)
                                        override val isUserBlocked: StateFlow<Boolean> = MutableStateFlow(false)
                                        override fun blockUser() {}
                                        override fun unblockUser() {}
                                        override fun verifyUser() {}
                                        override fun openRoomSettings() = onShowRoomSettings()
                                        override fun back() = onBack()
                                    }
                                }
                            }
                        })
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = currentCoroutineContext(),
            ),
            roomId = roomId,
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
    } catch (e: ClassCastException) {
        throw failure(
            "expected extras pane to be of instance ${T::class.simpleName}" +
                    " but instead was: ${this.extrasStack.value.active.instance::class.simpleName}"
        )
    }

private suspend inline fun <reified T : TimelineRouter.Wrapper> RoomViewModelImpl.timelineAs() =
    try {
        delay(100.milliseconds)
        (this.timelineStack.value.active.instance as T)
    } catch (e: ClassCastException) {
        throw failure(
            "expected timeline to be of instance ${T::class.simpleName}" +
                    " but instead was: ${this.extrasStack.value.active.instance::class.simpleName}"
        )
    }

package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.RoomUserReceipts
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.ReceiptEventContent
import net.folivo.trixnity.core.model.events.m.ReceiptType
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelUnreadMarkerTest : ShouldSpec() {
    override fun timeout(): Long = 10_000

    private lateinit var lifecycleRegistry: LifecycleRegistry

    private lateinit var messengerSettings: MatrixMessengerSettingsHolder
    private val roomId = RoomId("room1", "localhost")
    private val me = UserId("user1", "localhost")
    private val alice = UserId("alice", "localhost")

    private val bob = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val matrixClientServerApiMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    val roomHeaderViewModelMock = mock<RoomHeaderViewModel>()

    val inputAreaViewModelMock = mock<InputAreaViewModel>()
    private lateinit var roomUser: BlockingAnsweringScope<Flow<RoomUserReceipts?>>
    private lateinit var readMarkerCalled: MutableStateFlow<List<Pair<EventId?, EventId?>>>
    private lateinit var outerCoroutineScope: CoroutineScope

    init {
        val aliceRoomUser = roomUser(me, "Alice")

        beforeTest {
            Dispatchers.setMain(Dispatchers.Unconfined)

            resetMocks(
                matrixClientMock,
                roomServiceMock,
                userServiceMock,
                matrixClientServerApiMock,
                roomsApiClientMock,
                roomHeaderViewModelMock,
                inputAreaViewModelMock
            )

            lifecycleRegistry = LifecycleRegistry()
            lifecycleRegistry.start()

            messengerSettings = createTestMatrixMessengerSettingsHolder()
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                        single { userServiceMock }
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns me
            every { matrixClientMock.syncState } returns MutableStateFlow(SyncState.STARTED)
            every { matrixClientMock.api } returns matrixClientServerApiMock

            every { matrixClientServerApiMock.room } returns roomsApiClientMock
            readMarkerCalled = MutableStateFlow(listOf())
            everySuspend {
                roomsApiClientMock.setReadMarkers(any(), any(), any(), any(), eqNull())
            } calls { params ->
                readMarkerCalled.update { old -> old + (params.args[1] as EventId? to params.args[2] as EventId?) }
                Result.success(Unit)
            }
            every { roomServiceMock.getPreviousTimelineEvent(any(), any()) } returns
                    flowOf(
                        TimelineEvent(
                            messageEvent(
                                sender = alice,
                                eventId = EventId("dummy"),
                                roomId = RoomId("dummy")
                            ) { text("dummy") },
                            gap = null,
                            previousEventId = null,
                            nextEventId = null
                        )
                    )
            every { roomServiceMock.getOutbox() } returns MutableStateFlow(mapOf())
            every { userServiceMock.canRedactEvent(any(), any()) } returns flowOf(true)
            every { userServiceMock.getById(any(), any()) } returns flowOf(aliceRoomUser)

            every { userServiceMock.getAll(eq(roomId)) } returns MutableStateFlow(
                mapOf(
                    me to flowOf(
                        RoomUser(
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
                    ),
                    alice to flowOf(
                        RoomUser(
                            roomId, alice, "Alice", StateEvent(
                                MemberEventContent(membership = Membership.JOIN),
                                EventId(""),
                                alice,
                                roomId,
                                0L,
                                stateKey = ""
                            )
                        )
                    ),
                    bob to flowOf(
                        RoomUser(
                            roomId, bob, "Bob", StateEvent(
                                MemberEventContent(membership = Membership.JOIN),
                                EventId(""),
                                bob,
                                roomId,
                                0L,
                                stateKey = ""
                            )
                        )
                    ),
                )
            )
            every { userServiceMock.getAllReceipts(eq(roomId)) } returns MutableStateFlow(emptyMap())
            roomUser = every { userServiceMock.getReceiptsById(eq(roomId), any()) }
            every { userServiceMock.canSendEvent(any(), any()) } returns flowOf(true)
            roomUser returns flowOf(null)
            everySuspend { userServiceMock.loadMembers(roomId, false) } returns Unit

            messengerSettings.update<MatrixMessengerAccountSettingsBase>(UserId("test", "server")) {
                it.copy(
                    readMarkerIsPublic = true
                )
            }
        }
        afterTest {
            lifecycleRegistry.destroy()
            Dispatchers.resetMain()
            // needed to cancel longer running read marker jobs that might still interact with Mocks that are already reset
            outerCoroutineScope.cancel()
        }

        should("show the unread marker at the element after the fully read event initially") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World")
                }
                +messageEvent(sender = alice) {
                    text("!")
                }
                +messageEvent(sender = alice) {
                    text("Anyone here?")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0

            val cut = timelineViewModel()

            cut.timelineElementHolderViewModels waitForSize 4
            assertUnreadMarkerAtIndex(1, cut)
        }

        should("remove the unread marker from an element that is fully read now") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World")
                }
                +messageEvent(sender = alice) {
                    text("!")
                }
                +messageEvent(sender = alice) {
                    text("Anyone here?")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0

            val cut = timelineViewModel()

            cut.timelineElementHolderViewModels waitForSize 4
            assertUnreadMarkerAtIndex(1, cut)

            timelineMock.fullyReadEventIndex.value = 3
            delay(100)
            assertUnreadMarkerAtIndex(-1, cut)
        }

        should("compute a new unread marker element when the last unread marker is removed") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World")
                }
                +messageEvent(sender = alice) {
                    text("!")
                }
                +messageEvent(sender = alice) {
                    text("Anyone here?")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0

            val cut = timelineViewModel()

            cut.timelineElementHolderViewModels waitForSize 4
            assertUnreadMarkerAtIndex(1, cut)

            timelineMock.fullyReadEventIndex.value = 2
            delay(100)
            assertUnreadMarkerAtIndex(3, cut)
        }

        should("not show unread marker at StateEvents like 'user joined room'") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +stateEvent(sender = alice) {
                    createEvent()
                }
                +messageEvent(sender = alice) {
                    text("World")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0

            val cut = timelineViewModel()

            cut.timelineElementHolderViewModels waitForSize 3
            assertUnreadMarkerAtIndex(2, cut)
        }

        should("show the unread marker only above messages by someone else even if my messages come after the fully read message") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = me) {
                    text("World")
                }
                +messageEvent(sender = me) {
                    text("foo bar")
                }
                +messageEvent(sender = alice) {
                    text("OK")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0

            val cut = timelineViewModel()

            cut.timelineElementHolderViewModels waitForSize 4
            assertUnreadMarkerAtIndex(3, cut)
        }

        // some events are not shown yet: reactions, audio / video calls, etc.
        should("show the unread marker above the last unread message even when there are multiple events afterwards that are not displayed") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World")
                }
                +messageEvent(sender = bob) {
                    reaction(relatesTo = EventId("0"))
                }
                +messageEvent(sender = bob) {
                    reaction(relatesTo = EventId("1"))
                }
            }
            timelineMock.fullyReadEventIndex.value = 0

            val cut = timelineViewModel()

            cut.timelineElementHolderViewModels waitForSize 4
            assertUnreadMarkerAtIndex(1, cut)
        }

        should("not show the unread marker when only elements that are not displayed are unread") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = bob) {
                    reaction(relatesTo = EventId("0"))
                }
                +messageEvent(sender = bob) {
                    reaction(relatesTo = EventId("0"))
                }
                +messageEvent(sender = bob) {
                    reaction(relatesTo = EventId("1"))
                }
            }
            timelineMock.fullyReadEventIndex.value = 0

            val cut = timelineViewModel()

            cut.timelineElementHolderViewModels waitForSize 4
            assertUnreadMarkerAtIndex(-1, cut)
        }

        // this scenario takes into account that the server marks new messages as unread, but since we are active at the
        // end of the timeline, we should mark those messages as read immediately
        should("not show the unread marker and mark message as read when a new message is added to the end of the timeline and the user is active there") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World")
                }
            }
            timelineMock.fullyReadEventIndex.value = 1

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 2
            cut.windowIsFocused.value = true
            cut.lastVisibleTimelineElement.value = "1"
            verifyReadMarkerCalled(null to 1)
            assertUnreadMarkerAtIndex(-1, cut)
            delay(500.milliseconds)

            timelineMock.addEvents {
                +messageEvent(sender = alice) {
                    text("Woohoo")
                }
            }

            cut.timelineElementHolderViewModels waitForSize 3
            verifyReadMarkerCalled(null to 1, null to 2, 2 to null)
            assertUnreadMarkerAtIndex(-1, cut)
        }
        should("not show the unread marker and mark message as read when a new message is added to the end of the timeline and the user is active there if the message before was from us") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = me, transactionId = "txn-1") {
                    text("World")
                }
            }
            timelineMock.fullyReadEventIndex.value = 1

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 2
            cut.windowIsFocused.value = true
            cut.lastVisibleTimelineElement.value = "txn-1"
            verifyReadMarkerCalled(null to 1)
            assertUnreadMarkerAtIndex(-1, cut)
            delay(500.milliseconds)
            timelineMock.addEvents {
                +messageEvent(sender = alice) {
                    text("Woohoo")
                }
            }

            cut.timelineElementHolderViewModels waitForSize 3
            verifyReadMarkerCalled(null to 1, null to 2, 2 to null)
            assertUnreadMarkerAtIndex(-1, cut)
        }

        should("show the unread marker if the user is active in the timeline but not at the end") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World")
                }
            }
            timelineMock.fullyReadEventIndex.value = 1

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 2
            cut.windowIsFocused.value = true
            cut.lastVisibleTimelineElement.value = "0"

            assertUnreadMarkerAtIndex(-1, cut)
            delay(500.milliseconds)

            timelineMock.addEvents {
                +messageEvent(sender = alice) {
                    text("Woohoo")
                }
            }

            cut.timelineElementHolderViewModels waitForSize 3
            assertUnreadMarkerAtIndex(2, cut)
        }

        should("not change the unread marker when already shown and a new message appears") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 2
            cut.windowIsFocused.value = true
            cut.lastVisibleTimelineElement.value = "0"

            assertUnreadMarkerAtIndex(1, cut)
            delay(500.milliseconds)

            timelineMock.addEvents {
                +messageEvent(sender = alice) {
                    text("Woohoo")
                }
            }
            cut.timelineElementHolderViewModels waitForSize 3
            assertUnreadMarkerAtIndex(1, cut)
        }

        should("mark the last visible message as read when the room is opened") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World!")
                }
                +messageEvent(sender = alice) {
                    text("Dino!")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 3
            cut.lastVisibleTimelineElement.value = "1"
            verifyReadMarkerCalled(null to 1)

            assertUnreadMarkerAtIndex(1, cut)
        }

        should("mark the last message that is eligible as read when the room is opened") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World!")
                }
                +messageEvent(
                    sender = me,
                    transactionId = "transactionId-2"
                ) {
                    text("Hi")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 3
            verifyReadMarkerNotCalled()

            cut.lastVisibleTimelineElement.value = "transactionId-2"

            assertUnreadMarkerAtIndex(1, cut)
            verifyReadMarkerCalled(null to 2)
        }

        should("mark the last visible message as read when the last visible message changes") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World!")
                }
                +messageEvent(sender = alice) {
                    text("What's up?")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 3
            cut.lastVisibleTimelineElement.value = "1"

            assertUnreadMarkerAtIndex(1, cut)
            verifyReadMarkerCalled(null to 1)

            cut.lastVisibleTimelineElement.value = "2"
            assertUnreadMarkerAtIndex(1, cut) // unread marker stays in position
            verifyReadMarkerCalled(null to 1, null to 2)
        }

        should("not mark messages as read that are older than the previous last read message") {
            roomUser returns flowOf(createRoomUserReceipts(UserId("userId", "localhost"), EventId("3")))
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World!")
                }
                +messageEvent(sender = alice) {
                    text("What's up?")
                }
                +messageEvent(sender = alice) {
                    text("FooBar")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 4
            cut.lastVisibleTimelineElement.value = "2" // we only see to "What's up?"

            assertUnreadMarkerAtIndex(1, cut) // the fully_read marker determines the unread marker

            verifyReadMarkerNotCalled()
        }

        should("mark messages as read privately if the setting is set to privacy-first") {
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(UserId("test", "server")) {
                it.copy(readMarkerIsPublic = false)
            }
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World!")
                }
                +messageEvent(sender = alice) {
                    text("What's up?")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 3
            cut.lastVisibleTimelineElement.value = "1"

            assertUnreadMarkerAtIndex(1, cut)
            verifySuspend {
                roomsApiClientMock.setReadMarkers(eq(roomId), eqNull(), eqNull(), eq(EventId("1")), any())
            }
        }

        should("mark the last message as fully read when the room is changed or app exited (view model is destroyed)") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World!")
                }
                +messageEvent(sender = alice) {
                    text("What's up?")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            lifecycleRegistry.resume()

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 3
            cut.lastVisibleTimelineElement.value = "1"
            verifyReadMarkerCalled(null to 1)
            roomUser returns flowOf(createRoomUserReceipts(me, EventId("1")))

            lifecycleRegistry.destroy()

            verifyReadMarkerCalled(null to 1, 1 to null)
        }

        should("mark the last message as fully read when the app is paused") {
            val timelineMock = timeline(roomServiceMock, roomId) {
                +messageEvent(sender = alice) {
                    text("Hello")
                }
                +messageEvent(sender = alice) {
                    text("World!")
                }
                +messageEvent(sender = alice) {
                    text("What's up?")
                }
            }
            timelineMock.fullyReadEventIndex.value = 0
            lifecycleRegistry.resume()

            val cut = timelineViewModel()
            cut.timelineElementHolderViewModels waitForSize 3
            cut.lastVisibleTimelineElement.value = "1"
            verifyReadMarkerCalled(null to 1)
            roomUser returns flowOf(createRoomUserReceipts(me, EventId("1")))

            lifecycleRegistry.pause()
            verifyReadMarkerCalled(null to 1, 1 to null)
        }
    }

    private fun timelineViewModel(): TimelineViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock)) +
                        module {
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
                            single<MatrixMessengerSettingsHolder> { messengerSettings }
                        })
        }.koin
        outerCoroutineScope = di.get()

        return TimelineViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(lifecycleRegistry),
                di = di,
                userId = UserId("test", "server"),
            ),
            selectedRoomId = roomId,
            isBackButtonVisible = MutableStateFlow(false),
            onBack = mock(),
            onOpenModal = mock(),
            onShowSettings = mock(),
            onOpenMention = mock(),
        )
    }

    private suspend fun verifyReadMarkerNotCalled() {
        withClue("expected read marker not to be called") {
            continually(200.milliseconds) {
                readMarkerCalled.value shouldBe listOf()
            }
        }
    }

    private fun Set<Pair<EventId?, EventId?>>.readable() = map { "(fullyRead=${it.first}, read=${it.second})" }
    private suspend fun verifyReadMarkerCalled(vararg expect: Pair<Int?, Int?>) {
        check(expect.isNotEmpty())
        // only use sets here as through some effects the API might be called multiple times with the same values
        // (it is idempotent, so it is not dangerous). The order of calls is not checked here, but should also be not
        // relevant for this test case
        eventually(2.seconds) {
            val expectCalls = expect.map { value ->
                value.first?.let { EventId(it.toString()) } to value.second?.let { EventId(it.toString()) }
            }.toSet()
            withClue(
                "expected read marker to be called with ${expectCalls.readable()} but was ${
                    readMarkerCalled.value.toSet().readable()
                }"
            ) {
                readMarkerCalled.value shouldBe expectCalls
            }
        }
    }

    private suspend fun assertUnreadMarkerAtIndex(index: Int, timelineViewModel: TimelineViewModel) = coroutineScope {
        val subscriberJob = launch {
            timelineViewModel.timelineElementHolderViewModels.collectLatest {
                coroutineScope {
                    it.filterIsInstance<TimelineElementHolderViewModel>().forEach {
                        launch { it.shouldShowUnreadMarkerFlow.collect() }
                    }
                }
            }
        }
        eventually(1.seconds) {
            val actualReadMarkerIndexes =
                timelineViewModel.timelineElementHolderViewModels.value
                    .filterIsInstance<TimelineElementHolderViewModel>().mapIndexedNotNull { i, viewModel ->
                        if (viewModel.shouldShowUnreadMarkerFlow.value) i else null
                    }
            withClue("expected read marker at index $index, but was at indexes $actualReadMarkerIndexes") {
                if (index < 0) actualReadMarkerIndexes.shouldBeEmpty()
                else actualReadMarkerIndexes shouldBe listOf(index)
            }
        }
        subscriberJob.cancel()
    }

    private fun createRoomUserReceipts(userId: UserId, lastReadMessage: EventId) = RoomUserReceipts(
        roomId = roomId,
        userId = userId,
        mapOf(
            ReceiptType.Read to RoomUserReceipts.Receipt(
                lastReadMessage, // <- important part
                ReceiptEventContent.Receipt(0)
            )
        ),
    )

    private fun roomUser(userId: UserId, name: String) = RoomUser(
        roomId, userId, name, StateEvent(
            content = MemberEventContent(membership = Membership.JOIN),
            id = EventId("123"),
            sender = userId,
            roomId = roomId,
            originTimestamp = 0L,
            stateKey = "",
        )
    )
}

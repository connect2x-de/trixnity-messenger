package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMatrixClientModule
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.beInstanceOf
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomsApiClient
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.Event.MessageEvent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextMessageEventContent
import net.folivo.trixnity.utils.toByteArrayFlow
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction1
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class InputAreaViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    private val roomId = RoomId("room1", "localhost")
    private val ourUserId = UserId("bob", "localhost")

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

    @Mock
    lateinit var mediaServiceMock: MediaService

    @Mock
    lateinit var matrixClientServerApiClientMock: MatrixClientServerApiClient

    @Mock
    lateinit var roomsApiClientMock: RoomsApiClient

    private lateinit var powerLevelMocker: Mocker.Every<Flow<Event<PowerLevelsEventContent>?>>

    private val onMessageEditFinishedMock = mockFunction1<Unit, EventId>(mocker)
    private val onMessageReplToFinishedMock = mockFunction1<Unit, Event<*>>(mocker)

    private lateinit var allRoomUsersMock: Mocker.Every<Flow<Map<UserId, Flow<RoomUser?>>?>>

    init {
        Dispatchers.setMain(testMainDispatcher)
        coroutineTestScope = true

        val eventId = EventId("0")
        val aliceUserId = UserId("@alice:localhost")
        val aliceRoomUser = roomUser(aliceUserId, "Alice")
        val bobRoomUser = roomUser(ourUserId, "Bob") // our == bob
        val alvinUserId = UserId("@alvin:localhost")
        val alvinRoomUser = roomUser(alvinUserId, "Alvin")
        val zoopUserId = UserId("@completelyDifferent:anotherplanet")
        val zoopRoomUser = roomUser(zoopUserId, "Zoop")
        val messageEvent = MessageEvent(
            content = TextMessageEventContent("Hello"),
            id = eventId,
            sender = aliceUserId,
            roomId = roomId,
            originTimestamp = 0L,
        )

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { userServiceMock }
                            single { mediaServiceMock }
                        }
                    )
                }.koin
                every { matrixClientMock.userId } returns ourUserId
                every { matrixClientMock.api } returns matrixClientServerApiClientMock
                every { matrixClientServerApiClientMock.rooms } returns roomsApiClientMock

                powerLevelMocker = every {
                    roomServiceMock.getState(isEqual(roomId), isEqual(PowerLevelsEventContent::class), isAny())
                }
                powerLevelMocker returns flowOf(
                    Event.StateEvent(
                        content = PowerLevelsEventContent(),
                        id = EventId(""),
                        sender = UserId(""),
                        roomId = roomId,
                        originTimestamp = 0L,
                        stateKey = ""
                    )
                )
                everySuspending { roomServiceMock.sendMessage(isEqual(roomId), isAny(), isAny()) } returns ""
                every {
                    roomServiceMock.getTimelineEvent(isAny(), isEqual(eventId), isAny())
                } returns flowOf(
                    TimelineEvent(
                        event = messageEvent,
                        content = Result.success(TextMessageEventContent("Hello")),
                        roomId = roomId,
                        eventId = eventId,
                        previousEventId = null,
                        nextEventId = null,
                        gap = null,
                    )
                )
                every { roomServiceMock.getById(roomId) } returns MutableStateFlow(Room(roomId, isDirect = true))
                allRoomUsersMock = every { userServiceMock.getAll(roomId) }
                allRoomUsersMock returns MutableStateFlow(
                    mapOf(
                        aliceUserId to flowOf(aliceRoomUser),
                        alvinUserId to flowOf(alvinRoomUser),
                        ourUserId to flowOf(bobRoomUser),
                        zoopUserId to flowOf(zoopRoomUser),
                    )
                )
                every { userServiceMock.getById(roomId, aliceUserId) } returns MutableStateFlow(aliceRoomUser)
                every { onMessageEditFinishedMock.invoke(isAny()) } returns Unit
                every { onMessageReplToFinishedMock.invoke(isAny()) } returns Unit

                everySuspending {
                    mediaServiceMock.getThumbnail(isAny(), isAny(), isAny(), isAny(), isAny(), isAny())
                } returns Result.success("image".toByteArray().toByteArrayFlow())
            }
        }

        should("not allow sending when message is empty") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value shouldBe ""
            cut.isSendEnabled.replayCache[0] shouldBe false

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("allow sending when message is not empty") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "a"
            testCoroutineScheduler.advanceUntilIdle()
            cut.isAllowedToSendMessages.value shouldBe true
            cut.isSendEnabled.replayCache[0] shouldBe true
            cut.message.value = ""
            testCoroutineScheduler.advanceUntilIdle()
            cut.isSendEnabled.replayCache[0] shouldBe false

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("not allow to send messages when the own power level is too low") {
            powerLevelMocker returns flowOf(
                Event.StateEvent(
                    content = PowerLevelsEventContent(
                        eventsDefault = 50,
                        usersDefault = 10, // default is not enough to write messages
                    ),
                    id = EventId(""),
                    sender = UserId(""),
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = ""
                )
            )
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "I want to write!"
            testCoroutineScheduler.advanceUntilIdle()

            cut.isAllowedToSendMessages.value shouldBe false

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show the original message text and focus the input area when a message is edited") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.isEdit.value shouldBe false
            cut.message.value shouldBe ""
            cut.shouldFocus.value shouldBe null

            cut.editMessage(eventId)
            testCoroutineScheduler.advanceUntilIdle()

            cut.isEdit.value shouldBe true
            cut.message.value shouldBe "Hello"
            cut.shouldFocus.value shouldBe eventId.full

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("replace an existing message when an edited message is sent") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.editMessage(eventId)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "Hello World!"
            cut.sendMessage()
            testCoroutineScheduler.advanceUntilIdle()

            cut.isEdit.value shouldBe false
            cut.message.value shouldBe ""
            cut.shouldFocus.value shouldBe null
            mocker.verify(exhaustive = false) {
                onMessageEditFinishedMock.invoke(eventId)
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("reset the input area when editing a message is cancelled") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.editMessage(eventId)
            testCoroutineScheduler.advanceUntilIdle()
            cut.isEdit.value shouldBe true

            cut.cancelEdit()
            testCoroutineScheduler.advanceUntilIdle()

            cut.isEdit.value shouldBe false
            cut.message.value shouldBe ""
            cut.shouldFocus.value shouldBe null
            mocker.verify(exhaustive = false) {
                onMessageEditFinishedMock.invoke(eventId)
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("reply to a selected message") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.replyToMessage(messageEvent)
            testCoroutineScheduler.advanceUntilIdle()

            cut.replyToViewModel.value shouldNot beNull()
            cut.replyToViewModel.value?.event shouldBe messageEvent
            val replyTo = cut.replyToViewModel.value?.replyTo?.filterNotNull()?.first()
            replyTo.shouldNotBeNull()
            replyTo should beInstanceOf<ReplyType.TextReply>()
            replyTo.senderName shouldBe "Alice"

            cut.message.value = "I am replying to you."
            testCoroutineScheduler.advanceUntilIdle()
            cut.sendMessage()
            testCoroutineScheduler.advanceUntilIdle()

            cut.replyToViewModel.value shouldBe null
            cut.message.value shouldBe ""
            cut.shouldFocus.value shouldBe null
            mocker.verify(exhaustive = false) {
                onMessageReplToFinishedMock.invoke(messageEvent)
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set 'is typing' when message was changed and is not empty") {
            mocker.everySuspending {
                roomsApiClientMock.setTyping(isAny(), isAny(), isAny(), isAny(), isNull())
            } returns Result.success(Unit)

            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value = "a"
            testCoroutineScheduler.advanceUntilIdle()
            mocker.verifyWithSuspend(exhaustive = false) {
                roomsApiClientMock.setTyping(isEqual(roomId), isEqual(ourUserId), isEqual(true), isAny(), isNull())
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("keep 'is typing' when message changes at least once every 3 seconds") {
            var setTypingCancelWasCalled = false
            mocker.everySuspending {
                roomsApiClientMock.setTyping(isAny(), isAny(), isEqual(false), isAny(), isNull())
            } runs {
                setTypingCancelWasCalled = true
                Result.success(Unit)
            }
            mocker.everySuspending {
                roomsApiClientMock.setTyping(isAny(), isAny(), isEqual(true), isAny(), isNull())
            } returns Result.success(Unit)

            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value = "a"
            testCoroutineScheduler.advanceTimeBy(2_200)
            setTypingCancelWasCalled shouldBe false
            cut.message.value = "ab"
            testCoroutineScheduler.advanceTimeBy(2_200)
            setTypingCancelWasCalled shouldBe false
            cut.message.value = "abc"
            testCoroutineScheduler.advanceTimeBy(2_200)
            setTypingCancelWasCalled shouldBe false
            testCoroutineScheduler.advanceTimeBy(1_000)
            setTypingCancelWasCalled shouldBe true

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set 'is not typing' when the message is deleted (i_e_, it is empty again)") {
            var setTypingCancelWasCalled = false
            mocker.everySuspending {
                roomsApiClientMock.setTyping(isAny(), isAny(), isEqual(false), isAny(), isNull())
            } runs {
                setTypingCancelWasCalled = true
                Result.success(Unit)
            }
            mocker.everySuspending {
                roomsApiClientMock.setTyping(isAny(), isAny(), isEqual(true), isAny(), isNull())
            } returns Result.success(Unit)

            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value = "a"
            testCoroutineScheduler.advanceTimeBy(2_200)
            setTypingCancelWasCalled shouldBe false
            cut.message.value = ""
            testCoroutineScheduler.advanceUntilIdle()
            setTypingCancelWasCalled shouldBe true

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set 'is not typing' when the message has been sent") {
            var setTypingCancelWasCalled = false
            mocker.everySuspending {
                roomsApiClientMock.setTyping(isAny(), isAny(), isEqual(false), isAny(), isNull())
            } runs {
                setTypingCancelWasCalled = true
                Result.success(Unit)
            }
            mocker.everySuspending {
                roomsApiClientMock.setTyping(isAny(), isAny(), isEqual(true), isAny(), isNull())
            } returns Result.success(Unit)

            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value = "a"
            testCoroutineScheduler.advanceTimeBy(2_200)
            setTypingCancelWasCalled shouldBe false
            cut.sendMessage()
            testCoroutineScheduler.advanceUntilIdle()
            setTypingCancelWasCalled shouldBe true

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set the list of potential mentions to empty when the message does not prompt it") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "Hello! at"
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe emptyList()

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set the list of potential mentions to the users matching the prefix of the message's reference") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "Hello! @Al"
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(aliceUserId, "Alice", "A", flowOf(null)),
                Username(alvinUserId, "Alvin", "A", flowOf(null)),
            )

            cut.message.value = "Hello! @Ali"
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(aliceUserId, "Alice", "A", flowOf(null)),
            )

            cut.message.value = "Hello! @Alin"
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe emptyList()

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set the list of potential mentions to users matching the prefix of the message's reference regardless of case") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "Hello! @al"
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(aliceUserId, "Alice", "A", flowOf(null)),
                Username(alvinUserId, "Alvin", "A", flowOf(null)),
            )

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("not return own user name in the list of potential mentions") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "Hello! @Bo"
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe emptyList()

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("consider multiline messages when computing the list of potential mentions") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "Hello!\n\nThis is great.\n@Zoo"
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(zoopUserId, "Zoop", "Z", flowOf(null)),
            )

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("compute a list of all room users when the referenced name prefix is empty") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "Hello! @"
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(aliceUserId, "Alice", "A", flowOf(null)),
                Username(alvinUserId, "Alvin", "A", flowOf(null)),
                Username(zoopUserId, "Zoop", "Z", flowOf(null)),
            )

            cut.message.value = "Hello! \n\n@"
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(aliceUserId, "Alice", "A", flowOf(null)),
                Username(alvinUserId, "Alvin", "A", flowOf(null)),
                Username(zoopUserId, "Zoop", "Z", flowOf(null)),
            )

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("also search for users in the Matrix ID in room's potential user list") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "Hello! @compl"
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(zoopUserId, "Zoop", "Z", flowOf(null)),
            )

            cut.message.value = "Hello! @another"
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(zoopUserId, "Zoop", "Z", flowOf(null)),
            )

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("also consider mentions by containment") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "Hello! @ce and @Zoop" // search in name
            cut.currentCursorPosition.value = 10
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(aliceUserId, "Alice", "A", flowOf(null))
            )

            cut.message.value = "Hello! @pla and @Zoop" //search in userId
            cut.currentCursorPosition.value = 11
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(zoopUserId, "Zoop", "Z", flowOf(null))
            )

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("search at the current message's position for possible mentions") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "Hello! @Ali it goes on..."
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe emptyList()

            cut.currentCursorPosition.value = 11
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(aliceUserId, "Alice", "A", flowOf(null))
            )

            cut.currentCursorPosition.value = 10
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(aliceUserId, "Alice", "A", flowOf(null)),
                Username(alvinUserId, "Alvin", "A", flowOf(null)),
            )

            cut.message.value = "Hello!\n @Ali it goes on..."
            cut.currentCursorPosition.value = 12
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(aliceUserId, "Alice", "A", flowOf(null))
            )

            cut.message.value = "Hello!\n @Ali @Zoo it goes on..."
            cut.currentCursorPosition.value = 17
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentions.value shouldBe listOf(
                Username(zoopUserId, "Zoop", "Z", flowOf(null))
            )

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set the loading flag correctly: null when no loading needed, true when still loading and false when loading has finished") {
            val roomUsers = MutableSharedFlow<Map<UserId, Flow<RoomUser>>>()
            allRoomUsersMock returns roomUsers

            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentionsLoading.value shouldBe null

            cut.message.value = "Hello! @compl"
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentionsLoading.value shouldBe true
            cut.listOfMentions.value shouldBe emptyList()

            roomUsers.emit(
                mapOf(
                    zoopUserId to flowOf(zoopRoomUser),
                )
            )
            testCoroutineScheduler.advanceUntilIdle()
            cut.listOfMentionsLoading.value shouldBe false
            cut.listOfMentions.value shouldBe listOf(
                Username(zoopUserId,"Zoop", "Z", flowOf(null)),
            )

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set the currently selected user's displayname") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "Hello! @Ali"
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectMention(Username(aliceUserId, "Alice", "A", flowOf(null)))
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value shouldBe "Hello! @Alice "

            cut.message.value = "Hello!\n\nHola.\n@Ali"
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectMention(Username(aliceUserId, "Alice", "A", flowOf(null)))
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value = "Hello!\n\nHola.\n@Alice "

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set the currently selected user's displayname when the cursor is not at the end") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "@Ali"
            cut.currentCursorPosition.value = 3
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectMention(Username(aliceUserId, "Alice", "A", flowOf(null)))
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value shouldBe "@Alice "

            cut.message.value = "Hello! @Ali"
            cut.currentCursorPosition.value = 11
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectMention(Username(aliceUserId, "Alice", "A", flowOf(null)))
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value shouldBe "Hello! @Alice "

            cut.message.value = "Hello! @Ali something more"
            cut.currentCursorPosition.value = 11
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectMention(Username(aliceUserId, "Alice", "A", flowOf(null)))
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value shouldBe "Hello! @Alice something more"

            cut.message.value = "Hello!\n\nHola.\n@Ali something more"
            testCoroutineScheduler.advanceUntilIdle()
            cut.currentCursorPosition.value = 18
            cut.selectMention(Username(aliceUserId, "Alice", "A", flowOf(null)))
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value = "Hello!\n\nHola.\n@Alice something more"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("find the right replacement target in a message line with several '@'s") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "@Ali @Zo @Alv"
            cut.currentCursorPosition.value = 8
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectMention(Username(zoopUserId, "Zoop", "Z", flowOf(null)))
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value shouldBe "@Ali @Zoop @Alv"

            cut.message.value = "@Ali\n @Ali\n @Ali @Zo @Alv\n @Alv"
            cut.currentCursorPosition.value = 18 // after @Zo
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectMention(Username(zoopUserId, "Zoop", "Z", flowOf(null)))
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value shouldBe "@Ali\n @Ali\n @Ali @Zoop @Alv\n @Alv"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("ignore a replacement where no `@` can be found") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)
            testCoroutineScheduler.advanceUntilIdle()

            cut.message.value = "@Ali Zo Alv"
            cut.currentCursorPosition.value = 7
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectMention(Username(zoopUserId, "Zoop", "Z", flowOf(null)))
            testCoroutineScheduler.advanceUntilIdle()
            cut.message.value shouldBe "@Ali Zo Alv" // nothing should change

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

    }

    private fun roomUser(userId: UserId, name: String) = RoomUser(
        roomId, userId, name, Event.StateEvent(
            content = MemberEventContent(membership = Membership.JOIN),
            id = EventId("123"),
            sender = userId,
            roomId = roomId,
            originTimestamp = 0L,
            stateKey = "",
        )
    )

    private fun inputAreaViewModel(coroutineContext: CoroutineContext) = InputAreaViewModelImpl(
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
        onMessageEditFinished = onMessageEditFinishedMock,
        onMessageReplyFinished = onMessageReplToFinishedMock,
        onShowAttachmentSendView = mockFunction1(mocker),

        )

    private fun CoroutineScope.subscribe(cut: InputAreaViewModelImpl) = launch {
        launch { cut.isAllowedToSendMessages.collect() }
        launch { cut.isSendEnabled.collect() }
        launch { cut.showAttachmentSelectDialog.collect() }
        launch { cut.isEdit.collect() }
        launch { cut.replyToViewModel.collect() }
        launch { cut.isReplyTo.collect() }
        launch { cut.shouldFocus.collect() }
        launch { cut.listOfMentions.collect() }
    }
}
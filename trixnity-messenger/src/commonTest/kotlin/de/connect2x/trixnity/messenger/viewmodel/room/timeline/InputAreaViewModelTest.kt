package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.beInstanceOf
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.room.message.MessageBuilder
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.toByteArrayFlow
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class InputAreaViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 4_000

    private val roomId = RoomId("room1", "localhost")
    private val ourUserId = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val mediaServiceMock = mock<MediaService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    private lateinit var canSendEventMocker: BlockingAnsweringScope<Flow<Boolean>>

    private val onMessageEditFinishedMock = mock<Function1<EventId, Unit>>()
    private val onMessageReplToFinishedMock = mock<Function1<EventId, Unit>>()

    private lateinit var allRoomUsersMock: BlockingAnsweringScope<Flow<Map<UserId, Flow<RoomUser?>>?>>

    init {
        val eventId = EventId("0")
        val aliceUserId = UserId("@alice:localhost")
        val aliceRoomUser = roomUser(aliceUserId, "Alice")
        val bobRoomUser = roomUser(ourUserId, "Bob") // our == bob
        val alvinUserId = UserId("@alvin:localhost")
        val alvinRoomUser = roomUser(alvinUserId, "Alvin")
        val zoopUserId = UserId("@completelyDifferent:anotherplanet")
        val zoopRoomUser = roomUser(zoopUserId, "Zoop")
        val messageEvent = MessageEvent(
            content = RoomMessageEventContent.TextBased.Text("Hello"),
            id = eventId,
            sender = aliceUserId,
            roomId = roomId,
            originTimestamp = 0L,
        )

        var formattedBody: String? = null
        var body = ""

        beforeTest {
            resetMocks(
                matrixClientMock,
                roomServiceMock,
                userServiceMock,
                mediaServiceMock,
                matrixClientServerApiClientMock,
                roomsApiClientMock,
                onMessageEditFinishedMock,
                onMessageReplToFinishedMock
            )
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
            every { matrixClientServerApiClientMock.room } returns roomsApiClientMock

            canSendEventMocker = every {
                userServiceMock.canSendEvent(any(), any())
            }

            canSendEventMocker returns flowOf(true)
            everySuspend { roomServiceMock.sendMessage(eq(roomId), any(), any()) } returns ""
            every {
                roomServiceMock.getTimelineEvent(any(), eq(eventId), any())
            } returns flowOf(
                TimelineEvent(
                    event = messageEvent,
                    content = Result.success(RoomMessageEventContent.TextBased.Text("Hello")),
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
            every { onMessageEditFinishedMock.invoke(any()) } returns Unit
            every { onMessageReplToFinishedMock.invoke(any()) } returns Unit

            everySuspend { roomServiceMock.sendMessage(any(), any(), any()) } calls {
                val roomId = it.arg<RoomId>(0)
                val builderFunction = it.arg<suspend MessageBuilder.() -> Unit>(2)
                val builder = MessageBuilder(roomId, roomServiceMock, mediaServiceMock, ourUserId)
                val message = builder.build(builderFunction)

                if (message is RoomMessageEventContent.TextBased) {
                    formattedBody = message.formattedBody
                    body = message.body
                }

                ""
            }

            everySuspend {
                mediaServiceMock.getThumbnail(any(), any(), any(), any(), any(), any())
            } returns Result.success(InMemoryPlatformMedia("image".toByteArray().toByteArrayFlow()))
        }

        should("not allow sending when message is empty") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            eventually(2.seconds) {
                cut.message.value shouldBe ""
                cut.isSendEnabled.replayCache[0] shouldBe false
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("allow sending when message is not empty") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "a"
            eventually(2.seconds) {
                cut.isAllowedToSendMessages.value shouldBe true
                cut.isSendEnabled.replayCache[0] shouldBe true
            }
            cut.message.value = ""

            eventually(2.seconds) {
                cut.isSendEnabled.replayCache[0] shouldBe false
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("not allow to send messages when the own power level is too low") {
            canSendEventMocker returns flowOf(false)
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "I want to write!"

            eventually(2.seconds) {
                cut.isAllowedToSendMessages.value shouldBe false
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show the original message text and focus the input area when a message is edited") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            eventually(2.seconds) {
                cut.isEdit.value shouldBe false
                cut.message.value shouldBe ""
                cut.shouldFocus.value shouldBe null
            }

            cut.editMessage(eventId)

            eventually(2.seconds) {
                cut.isEdit.value shouldBe true
                cut.message.value shouldBe "Hello"
                cut.shouldFocus.value shouldBe eventId.full
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("replace an existing message when an edited message is sent") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.editMessage(eventId)

            eventually(2.seconds) {
                cut.isEdit.value shouldBe true
            }

            cut.message.value = "Hello World!"
            cut.sendMessage()

            eventually(2.seconds) {
                cut.isEdit.value shouldBe false
                cut.message.value shouldBe ""
                cut.shouldFocus.value shouldBe null
            }

            verify {
                onMessageEditFinishedMock.invoke(eventId)
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("reset the input area when editing a message is cancelled") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.editMessage(eventId)

            eventually(2.seconds) {
                cut.isEdit.value shouldBe true
            }

            cut.cancelEdit()

            eventually(2.seconds) {
                cut.isEdit.value shouldBe false
                cut.message.value shouldBe ""
                cut.shouldFocus.value shouldBe null
            }

            verify {
                onMessageEditFinishedMock.invoke(eventId)
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("reply to a selected message") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.replyToMessage(messageEvent.id)

            eventually(2.seconds) {
                cut.replyToViewModel.value shouldNot beNull()
                cut.replyToViewModel.value?.eventId shouldBe messageEvent.id
            }

            val replyTo = cut.replyToViewModel.value?.replyTo?.filterNotNull()?.first()

            eventually(2.seconds) {
                cut.isEdit.value shouldBe false
                replyTo.shouldNotBeNull()
                replyTo should beInstanceOf<ReplyType.TextReply>()
                replyTo.senderName shouldBe "Alice"
            }

            cut.message.value = "I am replying to you."

            eventually(2.seconds) {
                cut.isSendEnabled.value shouldBe true
            }

            cut.sendMessage()

            eventually(4.seconds) {
                cut.replyToViewModel.value shouldBe null
                cut.message.value shouldBe ""
                cut.shouldFocus.value shouldBe null
            }

            verify {
                onMessageReplToFinishedMock.invoke(messageEvent.id)
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set 'is typing' when message was changed and is not empty") {
            var setTypingWasCalled = false
            everySuspend {
                roomsApiClientMock.setTyping(eq(roomId), eq(ourUserId), eq(true), any(), eqNull())
            } calls {
                setTypingWasCalled = true
                Result.success(Unit)
            }

            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "a"

            eventually(2.seconds) {
                setTypingWasCalled shouldBe true
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("keep 'is typing' when message changes at least once every 3 seconds") {
            var setTypingCancelWasCalled = false
            everySuspend {
                roomsApiClientMock.setTyping(any(), any(), eq(false), any(), eqNull())
            } calls {
                setTypingCancelWasCalled = true
                Result.success(Unit)
            }
            everySuspend {
                roomsApiClientMock.setTyping(any(), any(), eq(true), any(), eqNull())
            } returns Result.success(Unit)

            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "a"

            eventually(2.seconds) {
                setTypingCancelWasCalled shouldBe false
            }

            cut.message.value = "ab"

            eventually(2.seconds) {
                setTypingCancelWasCalled shouldBe false
            }

            cut.message.value = "abc"

            eventually(2.seconds) {
                setTypingCancelWasCalled shouldBe false
            }

            eventually(4.seconds) {
                setTypingCancelWasCalled shouldBe true
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set 'is not typing' when the message is deleted (i_e_, it is empty again)") {
            var setTypingCancelWasCalled = false
            everySuspend {
                roomsApiClientMock.setTyping(any(), any(), eq(false), any(), eqNull())
            } calls {
                setTypingCancelWasCalled = true
                Result.success(Unit)
            }
            everySuspend {
                roomsApiClientMock.setTyping(any(), any(), eq(true), any(), eqNull())
            } returns Result.success(Unit)

            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "a"

            eventually(2.seconds) {
                setTypingCancelWasCalled shouldBe false
            }

            cut.message.value = ""

            eventually(2.seconds) {
                setTypingCancelWasCalled shouldBe true
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set 'is not typing' when the message has been sent") {
            var setTypingCancelWasCalled = false
            everySuspend {
                roomsApiClientMock.setTyping(any(), any(), eq(false), any(), eqNull())
            } calls {
                setTypingCancelWasCalled = true
                Result.success(Unit)
            }
            everySuspend {
                roomsApiClientMock.setTyping(any(), any(), eq(true), any(), eqNull())
            } returns Result.success(Unit)

            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "a"

            eventually(2.seconds) {
                setTypingCancelWasCalled shouldBe false
            }

            cut.sendMessage()

            eventually(2.seconds) {
                setTypingCancelWasCalled shouldBe true
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set the list of potential mentions to empty when the message does not prompt it") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "Hello! at"

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe emptyList()
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set the list of potential mentions to the users matching the prefix of the message's reference") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "Hello! @Al"

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(aliceUserId, "Alice", "A", flowOf(null)),
                    Username(alvinUserId, "Alvin", "A", flowOf(null)),
                )
            }

            cut.message.value = "Hello! @Ali"

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(aliceUserId, "Alice", "A", flowOf(null)),
                )
            }

            cut.message.value = "Hello! @Alin"

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe emptyList()
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set the list of potential mentions to users matching the prefix of the message's reference regardless of case") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "Hello! @al"

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(aliceUserId, "Alice", "A", flowOf(null)),
                    Username(alvinUserId, "Alvin", "A", flowOf(null)),
                )
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("not return own user name in the list of potential mentions") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "Hello! @Bo"

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe emptyList()
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("consider multiline messages when computing the list of potential mentions") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "Hello!\n\nThis is great.\n@Zoo"

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(zoopUserId, "Zoop", "Z", flowOf(null)),
                )
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("compute a list of all room users when the referenced name prefix is empty") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "Hello! @"

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(aliceUserId, "Alice", "A", flowOf(null)),
                    Username(alvinUserId, "Alvin", "A", flowOf(null)),
                    Username(zoopUserId, "Zoop", "Z", flowOf(null)),
                )
            }
            cut.message.value = "Hello! \n\n@"

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(aliceUserId, "Alice", "A", flowOf(null)),
                    Username(alvinUserId, "Alvin", "A", flowOf(null)),
                    Username(zoopUserId, "Zoop", "Z", flowOf(null)),
                )
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("also search for users in the Matrix ID in room's potential user list") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "Hello! @compl"

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(zoopUserId, "Zoop", "Z", flowOf(null)),
                )
            }

            cut.message.value = "Hello! @another"

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(zoopUserId, "Zoop", "Z", flowOf(null)),
                )
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("also consider mentions by containment") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "Hello! @ce and @Zoop" // search in name
            cut.currentCursorPosition.value = 10

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(aliceUserId, "Alice", "A", flowOf(null))
                )
            }

            cut.message.value = "Hello! @pla and @Zoop" //search in userId
            cut.currentCursorPosition.value = 11

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(zoopUserId, "Zoop", "Z", flowOf(null))
                )
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("search at the current message's position for possible mentions") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "Hello! @Ali it goes on..."

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe emptyList()
            }

            cut.currentCursorPosition.value = 11

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(aliceUserId, "Alice", "A", flowOf(null))
                )
            }

            cut.currentCursorPosition.value = 10

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(aliceUserId, "Alice", "A", flowOf(null)),
                    Username(alvinUserId, "Alvin", "A", flowOf(null)),
                )
            }

            cut.message.value = "Hello!\n @Ali it goes on..."
            cut.currentCursorPosition.value = 12

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(aliceUserId, "Alice", "A", flowOf(null))
                )
            }

            cut.message.value = "Hello!\n @Ali @Zoo it goes on..."
            cut.currentCursorPosition.value = 17

            eventually(2.seconds) {
                cut.listOfMentions.value shouldBe listOf(
                    Username(zoopUserId, "Zoop", "Z", flowOf(null))
                )
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set the loading flag correctly: null when no loading needed, true when still loading and false when loading has finished") {
            val roomUsers = MutableSharedFlow<Map<UserId, Flow<RoomUser>>>()
            allRoomUsersMock returns roomUsers

            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            eventually(2.seconds) {
                cut.listOfMentionsLoading.value shouldBe null
            }

            cut.message.value = "Hello! @compl"

            eventually(2.seconds) {
                cut.listOfMentionsLoading.value shouldBe true
                cut.listOfMentions.value shouldBe emptyList()
            }

            roomUsers.emit(
                mapOf(
                    zoopUserId to flowOf(zoopRoomUser),
                )
            )

            eventually(2.seconds) {
                cut.listOfMentionsLoading.value shouldBe false
                cut.listOfMentions.value shouldBe listOf(
                    Username(zoopUserId, "Zoop", "Z", flowOf(null)),
                )
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set the currently selected user's userId when cursor is null") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "Hello!\n\nHola.\n@Ali"

            cut.selectMention(Username(aliceUserId, "Alice", "A", flowOf(null)))

            eventually(2.seconds) {
                cut.message.value shouldBe "Hello!\n\nHola.\n@${aliceUserId.full} "
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set the currently selected user's id when the cursor is not at the end") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "@Ali"
            cut.currentCursorPosition.value = 3

            cut.selectMention(Username(aliceUserId, "Alice", "A", flowOf(null)))

            eventually(2.seconds) {
                cut.message.value shouldBe "${aliceUserId.full} "
            }

            cut.message.value = "Hello! @Ali"
            cut.currentCursorPosition.value = 11

            cut.selectMention(Username(aliceUserId, "Alice", "A", flowOf(null)))

            eventually(2.seconds) {
                cut.message.value shouldBe "Hello! ${aliceUserId.full} "
            }

            cut.message.value = "Hello! @Ali something more"
            cut.currentCursorPosition.value = 11

            cut.selectMention(Username(aliceUserId, "Alice", "A", flowOf(null)))

            eventually(2.seconds) {
                cut.message.value shouldBe "Hello! ${aliceUserId.full} something more"
            }

            cut.message.value = "Hello!\n\nHola.\n@Ali something more"

            cut.currentCursorPosition.value = 18
            cut.selectMention(Username(aliceUserId, "Alice", "A", flowOf(null)))

            eventually(2.seconds) {
                cut.message.value = "Hello!\n\nHola.\n${aliceUserId.full} something more"
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("find the right replacement target in a message line with several '@'s") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "@Ali @Zo @Alv"
            cut.currentCursorPosition.value = 8

            cut.selectMention(Username(zoopUserId, "Zoop", "Z", flowOf(null)))

            eventually(2.seconds) {
                cut.message.value shouldBe "@Ali ${zoopUserId.full} @Alv"
            }

            cut.message.value = "@Ali\n @Ali\n @Ali @Zo @Alv\n @Alv"
            cut.currentCursorPosition.value = 18 // after @Zo

            cut.selectMention(Username(zoopUserId, "Zoop", "Z", flowOf(null)))

            eventually(2.seconds) {
                cut.message.value shouldBe "@Ali\n @Ali\n @Ali ${zoopUserId.full} @Alv\n @Alv"
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("ignore a replacement where no `@` can be found") {
            val cut = inputAreaViewModel(coroutineContext)
            val subscriberJob = subscribe(cut)

            cut.message.value = "@Ali Zo Alv"
            cut.currentCursorPosition.value = 7

            cut.selectMention(Username(zoopUserId, "Zoop", "Z", flowOf(null)))

            eventually(2.seconds) {
                cut.message.value shouldBe "@Ali Zo Alv"
            }

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("convert markdown to HTML") {
            val markdown = """
                # The train station and Sony
               
                ## Origins
                
                There once was an amazing train station. It was so amazing that people in Germany began to say
                
                > I only understand train station
                
                But then the Playstation arrived and people adopted it *fast* so the Deutsche Bahn gave up and neglected
                the development of their railway network.
                
                ## Story time
                
                One day the people of the Playstation started adopting other forms of media such as YouTube. Due to 
                its relation to Tubes through whom trains drive, YouTube encourage people to embrace trains again.
                
                The Playstation overlords didn't like **that** 😠 so they started filing copyright cases on YouTube.
                This annoyed the following people:
                
                - the pirates as they couldn't sail now
                - the airports as they were overfilled with pirates now
                
                So ✨ `the coders` ✨ started greeting the world for which they used magic glyphs Computers could understand
                for example:
                
                ```
                fun main() {
                    println("Hello World 👋👋👋")
                }
                ```
                
                The empire of Playstation however is based on a group of coders developing the devilish Unix flavour.
                The republic of Germany however does not rely on them due to ancient technology for which the people of
                the Tube mock them. There are three Locations which get endorsed by them for their advanced technology:
                
                1. North America
                2. China
                3. Baltics
                
                The Deutsch Bahn didn't like that. So they rolled out the Deutschlandticket and began modernising their
                infrastructure. This way the people of the Tube are able to produce more Europe Transport > America Transport
                video and ignore the technological issues. 
                                
                At this point I forgot what the story was about but I markdown complete now. 
                Hope you had a good read? It's mostly non-sense
                Checkout [Tammy](https://gitlab.com/connect2x/tammy) btw :^)
            """.trimIndent()

            val html =
                """<h1>The train station and Sony</h1><h2>Origins</h2><p>There once was an amazing train station. It was so amazing that people in Germany began to say</p><blockquote><p>I only understand train station</p></blockquote><p>But then the Playstation arrived and people adopted it <em>fast</em> so the Deutsche Bahn gave up and neglected
the development of their railway network.</p><h2>Story time</h2><p>One day the people of the Playstation started adopting other forms of media such as YouTube. Due to 
its relation to Tubes through whom trains drive, YouTube encourage people to embrace trains again.</p><p>The Playstation overlords didn't like <strong>that</strong> 😠 so they started filing copyright cases on YouTube.
This annoyed the following people:</p><ul><li>the pirates as they couldn't sail now</li><li>the airports as they were overfilled with pirates now</li></ul><p>So ✨ <code>the coders</code> ✨ started greeting the world for which they used magic glyphs Computers could understand
for example:</p><pre><code>fun main() {
    println(&quot;Hello World 👋👋👋&quot;)
}
</code></pre><p>The empire of Playstation however is based on a group of coders developing the devilish Unix flavour.
The republic of Germany however does not rely on them due to ancient technology for which the people of
the Tube mock them. There are three Locations which get endorsed by them for their advanced technology:</p><ol><li>North America</li><li>China</li><li>Baltics</li></ol><p>The Deutsch Bahn didn't like that. So they rolled out the Deutschlandticket and began modernising their
infrastructure. This way the people of the Tube are able to produce more Europe Transport &gt; America Transport
video and ignore the technological issues.</p><p>At this point I forgot what the story was about but I markdown complete now. 
Hope you had a good read? It's mostly non-sense
Checkout <a href="https://gitlab.com/connect2x/tammy">Tammy</a> btw :^)</p>"""
            val cut = inputAreaViewModel(coroutineContext)
            val job = subscribe(cut)

            cut.message.value = markdown

            eventually(2.seconds) {
                cut.isSendEnabled.value shouldBe true
            }

            cut.sendMessage()

            eventually(2.seconds) {
                body shouldBe markdown
                formattedBody shouldBe html
            }

            job.cancel()
            cancelNeverEndingCoroutines()
        }
    }

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

    private suspend fun inputAreaViewModel(coroutineContext: CoroutineContext): InputAreaViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return InputAreaViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock)),
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext,
            ),
            selectedRoomId = roomId,
            onMessageEditFinished = onMessageEditFinishedMock,
            onMessageReplyFinished = onMessageReplToFinishedMock,
            onShowAttachmentSendView = mock(),
        )
    }

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

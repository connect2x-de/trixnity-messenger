package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.settle
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.matchers.shouldBe
import io.ktor.utils.io.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
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
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.toByteArrayFlow
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class InputAreaViewModelTest {

    private val roomId = RoomId("!room1")
    private val ourUserId = UserId("bob", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val mediaServiceMock = mock<MediaService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    private var canSendEventMocker: BlockingAnsweringScope<Flow<Boolean>>

    private val onMessageEditFinishedMock = mock<Function2<RoomId, EventId, Unit>>()
    private val onMessageReplToFinishedMock = mock<Function2<RoomId, EventId, Unit>>()

    private var allRoomUsersMock: BlockingAnsweringScope<Flow<Map<UserId, Flow<RoomUser?>>?>>

    val eventId = EventId("0")
    val aliceUserId = UserId("@alice:hallo.com")
    val aliceRoomUser = roomUser(aliceUserId, "Alice")
    val bobRoomUser = roomUser(ourUserId, "Bob") // our == bob
    val alvinUserId = UserId("@alvin:example.org")
    val alvinRoomUser = roomUser(alvinUserId, "Alvin")
    val alvin2UserId = UserId("@alvin:example.orgg")
    val alvin2RoomUser = roomUser(alvin2UserId, "Alvina")
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


    init {
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
                })
        }.koin
        every { matrixClientMock.userId } returns ourUserId
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.room } returns roomsApiClientMock

        canSendEventMocker = every {
            userServiceMock.canSendEvent(any(), any<KClass<out RoomEventContent>>())
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
        every { userServiceMock.getById(roomId, alvinUserId) } returns MutableStateFlow(alvinRoomUser)
        every { userServiceMock.getById(roomId, alvin2UserId) } returns MutableStateFlow(alvin2RoomUser)
        every { onMessageEditFinishedMock.invoke(any(), any()) } returns Unit
        every { onMessageReplToFinishedMock.invoke(any(), any()) } returns Unit

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

        everySuspend {
            roomsApiClientMock.setTyping(
                any(), any(), any(), any(), any()
            )
        } returns Result.success(Unit)
    }


    @Test
    fun `not allow sending when message is empty`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        eventually(300.milliseconds) {
            cut.textField.textValue shouldBe ""
            cut.isSendEnabled.replayCache[0] shouldBe false
        }
    }

    @Test
    fun `allow sending when message is not empty`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("a")
        eventually(300.milliseconds) {
            cut.isAllowedToSendMessages.value shouldBe true
            cut.isSendEnabled.replayCache[0] shouldBe true
        }
        cut.textField.update("")

        eventually(300.milliseconds) {
            cut.isSendEnabled.replayCache[0] shouldBe false
        }
    }

    @Test
    fun `not allow to send messages when the own power level is too low`() = runTest {
        canSendEventMocker returns flowOf(false)
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("I want to write!")

        eventually(300.milliseconds) {
            cut.isAllowedToSendMessages.value shouldBe false
        }
    }

    @Test
    fun `show the original message text and focus the input area when a message is edited`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        eventually(300.milliseconds) {
            cut.isReplace.value shouldBe false
            cut.textField.textValue shouldBe ""
        }

        cut.replaceMessage(roomId, eventId)

        eventually(300.milliseconds) {
            cut.isReplace.value shouldBe true
            cut.textField.textValue shouldBe "Hello"
        }
    }

    @Test
    fun `replace an existing message when an edited message is sent`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.replaceMessage(roomId, eventId)

        eventually(300.milliseconds) {
            cut.isReplace.value shouldBe true
        }

        cut.textField.update("Hello World!")
        cut.sendMessage()

        eventually(300.milliseconds) {
            cut.isReplace.value shouldBe false
            cut.textField.textValue shouldBe ""
        }

        verify {
            onMessageEditFinishedMock.invoke(roomId, eventId)
        }
    }

    @Test
    fun `reset the input area when editing a message is cancelled`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.replaceMessage(roomId, eventId)

        eventually(300.milliseconds) {
            cut.isReplace.value shouldBe true
        }

        cut.cancelReplace()

        eventually(300.milliseconds) {
            cut.isReplace.value shouldBe false
            cut.textField.textValue shouldBe ""
        }

        verify {
            onMessageEditFinishedMock.invoke(roomId, eventId)
        }
    }

    @Test
    fun `set 'is typing' when message was changed and is not empty`() = runTest {
        var setTypingWasCalled = false
        everySuspend {
            roomsApiClientMock.setTyping(eq(roomId), eq(ourUserId), eq(true), any(), eqNull())
        } calls {
            setTypingWasCalled = true
            Result.success(Unit)
        }

        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("a")

        eventually(300.milliseconds) {
            setTypingWasCalled shouldBe true
        }
    }

    @Test
    fun `keep 'is typing' when message changes at least once every 3 seconds`() = runTest {
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

        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("a")

        delay(2.seconds)
        setTypingCancelWasCalled shouldBe false

        cut.textField.update("ab")

        delay(2.seconds)
        setTypingCancelWasCalled shouldBe false

        cut.textField.update("abc")
        delay(2.seconds)

        setTypingCancelWasCalled shouldBe false

        delay(1.seconds)
        yield()
        setTypingCancelWasCalled shouldBe true
    }

    @Test
    fun `set isNotTyping when the message is cleared`() = runTest {
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

        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("a")

        eventually(300.milliseconds) {
            setTypingCancelWasCalled shouldBe false
        }
        delay(100.milliseconds)

        cut.textField.update("")

        eventually(300.milliseconds) {
            setTypingCancelWasCalled shouldBe true
        }
    }

    @Test
    fun `set 'is not typing' when the message has been sent`() = runTest {
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

        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("a")

        eventually(300.milliseconds) {
            setTypingCancelWasCalled shouldBe false
        }
        delay(100.milliseconds)

        cut.sendMessage()

        eventually(300.milliseconds) {
            setTypingCancelWasCalled shouldBe true
        }
    }

    @Test
    fun `set the list of potential mentions to null when the message does not prompt it`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("Hello! at")

        eventually(300.milliseconds) {
            cut.listOfMentions.value shouldBe null
        }
    }

    @Test
    fun `set the list of potential mentions to the users matching the prefix of the message's reference`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("Hello! @Al", IntRange(10, 10))

        eventually(300.milliseconds) {
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(aliceUserId, alvinUserId)
        }

        cut.textField.update("Hello! @Ali", IntRange(11, 11))

        eventually(300.milliseconds) {
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(aliceUserId)
        }

        cut.textField.update("Hello! @Alin", IntRange(12, 12))

        eventually(300.milliseconds) {
            cut.listOfMentions.value shouldBe emptyList()
        }
    }

    @Test
    fun `set the list of potential mentions to users matching the prefix of the message's reference regardless of case`() =
        runTest {
            val cut = inputAreaViewModel()
            subscribe(cut)

            cut.textField.update("Hello! @al", IntRange(10, 10))

            eventually(300.milliseconds) {
                cut.listOfMentions.value?.map { it.userId } shouldBe listOf(aliceUserId, alvinUserId)
            }
        }

    @Test
    fun `not return own user name in the list of potential mentions`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("Hello! @Bo", IntRange(10, 10))

        eventually(300.milliseconds) {
            cut.listOfMentions.value shouldBe emptyList()
        }
    }

    @Test
    fun `consider multiline messages when computing the list of potential mentions`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("Hello!\n\nThis is great.\n@Zoo", IntRange(30, 30))

        eventually(300.milliseconds) {
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(zoopUserId)
        }
    }

    @Test
    fun `compute a list of all room users when the referenced name prefix is empty`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("Hello! @", IntRange(8, 8))

        eventually(300.milliseconds) {
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(aliceUserId, alvinUserId, zoopUserId)
        }
        cut.textField.update("Hello! \n\n@", IntRange(12, 12))

        eventually(300.milliseconds) {
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(aliceUserId, alvinUserId, zoopUserId)
        }
    }

    @Test
    fun `also search for users in the Matrix ID in room's potential user list`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("Hello! @compl", IntRange(13, 13))

        eventually(300.milliseconds) {
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(zoopUserId)
        }

        cut.textField.update("Hello! @another", IntRange(15, 15))

        eventually(300.milliseconds) {
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(zoopUserId)
        }
    }

    @Test
    fun `also consider mentions by containment`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("Hello! @ce and @Zoop", IntRange(10, 10)) // search in name

        eventually(300.milliseconds) {
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(aliceUserId)
        }

        cut.textField.update("Hello! @pla and @Zoop", IntRange(11, 11)) //search in userId

        eventually(300.milliseconds) {
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(zoopUserId)
        }
    }

    @Test
    fun `search at the current message's position for possible mentions`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("Hello! @Ali it goes on...")

        eventually(300.milliseconds) {
            cut.listOfMentions.value shouldBe null
        }

        cut.textField.update("Hello! @Ali it goes on...", IntRange(11, 11))

        eventually(300.milliseconds) {
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(aliceUserId)
        }

        cut.textField.update("Hello! @Ali it goes on...", IntRange(10, 10))

        eventually(300.milliseconds) {
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(aliceUserId, alvinUserId)
        }

        cut.textField.update("Hello!\n @Ali it goes on...", IntRange(12, 12))

        eventually(300.milliseconds) {
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(aliceUserId)
        }

        cut.textField.update("Hello!\n @Ali @Zoo it goes on...", IntRange(17, 17))

        eventually(300.milliseconds) {
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(zoopUserId)
        }
    }

    @Test
    fun `set the loading flag correctly true when still loading and false when loading has finished`() = runTest {
        val roomUsers = MutableSharedFlow<Map<UserId, Flow<RoomUser>>>()
        allRoomUsersMock returns roomUsers.transform {
            delay(50.milliseconds)
            emit(it)
        }

        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("Hello! @compl", IntRange(13, 13))

        eventually(300.milliseconds) {
            cut.listOfMentionsLoading.value shouldBe true
            cut.listOfMentions.value shouldBe null
        }

        roomUsers.emit(
            mapOf(
                zoopUserId to flowOf(zoopRoomUser),
            )
        )

        eventually(300.milliseconds) {
            cut.listOfMentionsLoading.value shouldBe false
            cut.listOfMentions.value?.map { it.userId } shouldBe listOf(zoopUserId)
        }
    }

    @Test
    fun `set the currently selected user's id when the cursor is not at the end`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)
        settle()

        cut.textField.update("@Ali", IntRange(4, 4))
        settle()

        cut.selectMention(aliceUserId)
        cut.textField.textValue shouldBe aliceUserId.full

        cut.textField.update("Hello! @Ali", IntRange(11, 11))
        settle()

        cut.selectMention(aliceUserId)
        cut.textField.textValue shouldBe "Hello! ${aliceUserId.full}"

        cut.textField.update("Hello! @Ali something more", IntRange(11, 11))
        settle()

        cut.selectMention(aliceUserId)
        cut.textField.textValue shouldBe "Hello! ${aliceUserId.full} something more"

        cut.textField.update("Hello!\n\nHola.\n@Ali something more", IntRange(18, 18))
        settle()

        cut.selectMention(aliceUserId)
        cut.textField.textValue shouldBe "Hello!\n\nHola.\n${aliceUserId.full} something more"
    }

    @Test
    fun `find the right replacement target in a message line with several at's`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("@Ali @Zo @Alv", IntRange(8, 8))

        delay(50.milliseconds)
        cut.selectMention(zoopUserId)

        eventually(300.milliseconds) {
            cut.textField.textValue shouldBe "@Ali ${zoopUserId.full} @Alv"
        }

        cut.textField.update("@Ali\n @Ali\n @Ali @Zo @Alv\n @Alv", IntRange(20, 20))

        delay(50.milliseconds)
        cut.selectMention(zoopUserId)

        eventually(300.milliseconds) {
            cut.textField.textValue shouldBe "@Ali\n @Ali\n @Ali ${zoopUserId.full} @Alv\n @Alv"
        }
    }

    @Test
    fun `ignore a replacement where no 'at' can be found`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("@Ali Zo Alv", IntRange(7, 7))

        cut.selectMention(zoopUserId)

        eventually(300.milliseconds) {
            cut.textField.textValue shouldBe "@Ali Zo Alv"
        }
    }

    @Test
    fun `convert markdown to HTML`() = runTest {
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

        val html = """
            <h1>The train station and Sony</h1><h2>Origins</h2><p>There once was an amazing train station. It was so amazing that people in Germany began to say</p><blockquote><p>I only understand train station</p></blockquote><p>But then the Playstation arrived and people adopted it <em>fast</em> so the Deutsche Bahn gave up and neglected
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
            Checkout <a href="https://gitlab.com/connect2x/tammy">Tammy</a> btw :^)</p>
        """.trimIndent()
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update(markdown)

        eventually(300.milliseconds) {
            cut.textField.textValue shouldBe markdown
            cut.isSendEnabled.value shouldBe true
        }

        cut.sendMessage()

        eventually(3000.milliseconds) {
            body shouldBe markdown
            formattedBody shouldBe html
        }
    }

    @Test
    fun `convert message with only one Link to HTML properly`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("https://en.m.wikipedia.org/wiki/The_Rise_and_Fall_of_D.O.D.O.")

        eventually(300.milliseconds) {
            cut.isSendEnabled.value shouldBe true
        }

        cut.sendMessage()

        eventually(300.milliseconds) {
            body shouldBe "https://en.m.wikipedia.org/wiki/The_Rise_and_Fall_of_D.O.D.O."
            formattedBody shouldBe "<p><a href=\"https://en.m.wikipedia.org/wiki/The_Rise_and_Fall_of_D.O.D.O\">https://en.m.wikipedia.org/wiki/The_Rise_and_Fall_of_D.O.D.O</a>.</p>"
        }
    }

    @Test
    fun `convert mentions with text inbetween into anchor tags`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("Hiii ${aliceUserId.full} und ${alvinUserId.full}\nund ${alvin2UserId.full} und ${alvinUserId.full} zusammen!")

        eventually(300.milliseconds) {
            cut.isSendEnabled.value shouldBe true
        }

        cut.sendMessage()

        eventually(300.milliseconds) {
            formattedBody shouldBe """
                <p>Hiii <a href="matrix:u/alice:hallo.com">Alice</a> und <a href="matrix:u/alvin:example.org">Alvin</a>
                und <a href="matrix:u/alvin:example.orgg">Alvina</a> und <a href="matrix:u/alvin:example.org">Alvin</a> zusammen!</p>
            """.trimIndent()
        }
    }

    @Test
    fun `convert single mention into anchor tag`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update(aliceUserId.full)

        eventually(300.milliseconds) {
            cut.isSendEnabled.value shouldBe true
        }

        cut.sendMessage()

        eventually(300.milliseconds) {
            formattedBody shouldBe """<p><a href="matrix:u/alice:hallo.com">Alice</a></p>"""
        }
    }

    @Test
    fun `convert mentions without text inbetween into anchor tags`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("${aliceUserId.full} ${alvinUserId.full} hii!")

        eventually(300.milliseconds) {
            cut.isSendEnabled.value shouldBe true
        }

        cut.sendMessage()

        eventually(300.milliseconds) {
            formattedBody shouldBe """<p><a href="matrix:u/alice:hallo.com">Alice</a> <a href="matrix:u/alvin:example.org">Alvin</a> hii!</p>"""
        }
    }

    @Test
    fun `convert mention at the end`() = runTest {
        val cut = inputAreaViewModel()
        subscribe(cut)

        cut.textField.update("Hi ${aliceUserId.full}")

        eventually(300.milliseconds) {
            cut.isSendEnabled.value shouldBe true
        }

        cut.sendMessage()

        eventually(300.milliseconds) {
            formattedBody shouldBe """<p>Hi <a href="matrix:u/alice:hallo.com">Alice</a></p>"""
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

    private suspend fun TestScope.inputAreaViewModel(): InputAreaViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(UserId("test", "server") to matrixClientMock)
                ),
            )
        }.koin
        di.get<MatrixMessengerSettingsHolder>().create(UserId("test", "server"), MatrixMessengerAccountSettingsBase())
        return InputAreaViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(di = di, userId = UserId("test", "server")),
            roomId = roomId,
            onMessageReplaceFinished = onMessageEditFinishedMock,
            onMessageReplyFinished = onMessageReplToFinishedMock,
            onShowAttachmentSendView = mock(),
            onOpenMention = mock(),
        )
    }

    private fun TestScope.subscribe(cut: InputAreaViewModelImpl) = backgroundScope.launch {
        launch { cut.isAllowedToSendMessages.collect() }
        launch { cut.isSendEnabled.collect() }
        launch { cut.showAttachmentSelectDialog.collect() }
        launch { cut.isReplace.collect() }
        launch { cut.isReply.collect() }
        launch { cut.listOfMentions.collect() }
    }
}

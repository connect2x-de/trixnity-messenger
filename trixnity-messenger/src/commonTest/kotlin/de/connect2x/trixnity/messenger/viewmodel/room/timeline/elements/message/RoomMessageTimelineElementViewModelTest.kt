package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomAliasId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.m.Mentions
import de.connect2x.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.firstNotNullWithClue
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@Suppress("NonAsciiCharacters")
class RoomMessageTimelineElementViewModelTest {
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()
    val mediaServiceMock = mock<MediaService>()
    private val uri = "mxc://server/image"
    private val maxSize = MatrixMessengerConfiguration().downloadLimits.thumbnail

    val roomId = RoomId("!bathroom:example")
    val roomAliasId = RoomAliasId("bathroom", "example")
    val roomInfoElement = RoomInfoElement(roomAliasId.full, roomId, "#", null)
    val room = Room(roomId)

    val meUserId = UserId("tester", "example")
    val meName = "Tester"
    val meRoomUser =
        RoomUser(
            roomId,
            meUserId,
            meName,
            event =
                ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(membership = Membership.JOIN),
                    id = EventId("\$spawned"),
                    roomId = roomId,
                    sender = meUserId,
                    stateKey = meUserId.full,
                    originTimestamp = 0,
                ),
        )
    val meUserInfoElement = UserInfoElement(meUserId, meName, "T")

    init {
        resetMocks(matrixClientMock, userServiceMock, roomServiceMock)
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { userServiceMock }
                            single { roomServiceMock }
                            single { mediaServiceMock }
                        }
                    )
                }
                .koin

        every { roomServiceMock.getAll() } returns flowOf(mapOf(roomId to flowOf()))
        every { roomServiceMock.getState(roomId, CanonicalAliasEventContent::class, any()) } returns
            flowOf(
                ClientEvent.RoomEvent.StateEvent(
                    content = CanonicalAliasEventContent(roomAliasId, setOf()),
                    id = EventId("\$alias"),
                    sender = meUserId,
                    roomId = roomId,
                    originTimestamp = 0,
                    stateKey = "key",
                )
            )
        every { roomServiceMock.getById(roomId) } returns flowOf(room)

        every { userServiceMock.getById(roomId, meUserId) } returns flowOf(meRoomUser)
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    // TODO Test for RoomIDs and EventIDs once they are being found again

    @Test
    fun `mentions » find and process userid mention in the body`() = runTest {
        val cut = cut(body = "Hii $meUserId!! :3")
        cut.mentionsInBody.keys shouldBe setOf(4..18)

        cut.mentionsInBody[4..18]?.firstNotNullWithClue()

        delay(100.milliseconds)
        cut.mentionsInBody[4..18]?.value shouldBe TimelineElementMention.User(meUserInfoElement)
    }

    @Test
    fun `mentions » find and process roomalias mention in the body`() = runTest {
        val cut = cut(body = "I'm in $roomAliasId, wbu?")
        cut.mentionsInBody.keys shouldBe setOf(7..23)

        cut.mentionsInBody[7..23]?.firstNotNullWithClue()

        delay(100.milliseconds)
        cut.mentionsInBody[7..23]?.value shouldBe TimelineElementMention.Room(roomInfoElement)
    }

    @Test
    fun `mentions » isMentioned should be true if the user was directly mentioned`() = runTest {
        val mentions = Mentions(setOf(meUserId))
        val cut = cut(body = "Yo, how is it going $meUserId", mentions = mentions)
        delay(300.milliseconds)
        cut.isMentioned shouldBe true
    }

    @Test
    fun `mentions » isMentioned should be true if the user was mentioned through a room mention`() = runTest {
        val mentions = Mentions(room = true)
        val cut = cut(body = "@room Listen up, I have an important announcement to make!", mentions = mentions)
        delay(300.milliseconds)
        cut.isMentioned shouldBe true
    }

    @Test
    fun `mention » isMentioned should be false if the user was not mentioned`() = runTest {
        val cut = cut(body = "Is anybody here?")
        delay(300.milliseconds)
        cut.isMentioned shouldBe false
    }

    @Test
    fun `load smaller thumbnails for emoticons`() = runTest {
        val bytes = "emoticon".encodeToByteArray()
        everySuspend {
            mediaServiceMock.getThumbnail(uri, 128L, 128L, maxSize, maxSize, any(), progress = any())
        } returns Result.success(InMemoryPlatformMedia(bytes))

        val cut = cut()
        cut.loadInlineImage(uri, 128, 128) shouldBe bytes
    }

    @Test
    fun `load server thumbnail without downloading original`() = runTest {
        val bytes = "thumbnail".encodeToByteArray()
        everySuspend {
            mediaServiceMock.getThumbnail(uri, 800L, 800L, maxSize, maxSize, any(), progress = any())
        } returns Result.success(InMemoryPlatformMedia(bytes))

        val cut = cut()
        cut.loadInlineImage(uri, 800, 800) shouldBe bytes
    }

    @Test
    fun `fall back to original with download limit when thumbnail fails`() = runTest {
        val bytes = "original".encodeToByteArray()
        everySuspend {
            mediaServiceMock.getThumbnail(uri, 800L, 800L, maxSize, maxSize, any(), progress = any())
        } returns Result.failure(IllegalStateException("thumbnail unavailable"))
        everySuspend { mediaServiceMock.getMedia(uri, maxSize, maxSize, any()) } returns
            Result.success(InMemoryPlatformMedia(bytes))

        val cut = cut()
        cut.loadInlineImage(uri, 800, 800) shouldBe bytes
    }

    @Test
    fun `reject non Matrix media sources`() = runTest {
        val cut = cut()
        cut.loadInlineImage("https://server/image", 800, 800) shouldBe null
    }

    private fun TestScope.cut(
        body: String = "Amazing Message!",
        formattedBody: String? = null,
        mentions: Mentions? = null,
    ): RoomMessageTimelineElementViewModel<*> {
        return TextRoomMessageTimelineElementViewModelImpl(
            viewModelContext =
                testMatrixClientViewModelContext(
                    di =
                        koinApplication {
                                modules(createTestDefaultTrixnityMessengerModules(mapOf(meUserId to matrixClientMock)))
                            }
                            .koin,
                    userId = meUserId,
                ),
            content = RoomMessageEventContent.TextBased.Text(body, formattedBody = formattedBody, mentions = mentions),
            roomId = roomId,
            onOpenMention = { _, _ -> },
        )
    }
}

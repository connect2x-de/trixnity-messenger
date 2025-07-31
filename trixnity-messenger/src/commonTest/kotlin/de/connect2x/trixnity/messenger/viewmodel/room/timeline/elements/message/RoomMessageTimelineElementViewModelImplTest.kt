package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

@Suppress("NonAsciiCharacters")
class RoomMessageTimelineElementViewModelImplTest {
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    val roomId = RoomId("!bathroom:server")
    val roomAliasId = RoomAliasId("bathroom", "server")
    val roomInfoElement = RoomInfoElement(
        roomAliasId.full, roomId, "#", null
    )
    val room = Room(roomId)

    val meUserId = UserId("tester", "server")
    val meName = "Tester"
    val meRoomUser = RoomUser(
        roomId, meUserId, meName, event = ClientEvent.RoomEvent.StateEvent(
            content = MemberEventContent(membership = Membership.JOIN),
            id = EventId("\$spawned"),
            roomId = roomId,
            sender = meUserId,
            stateKey = meUserId.full,
            originTimestamp = 0,
        )
    )
    val meUserInfoElement = UserInfoElement(meUserId, meName, "T")

    init {
        resetMocks(matrixClientMock, userServiceMock, roomServiceMock)
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { userServiceMock }
                    single { roomServiceMock }
                })
        }.koin

        every { roomServiceMock.getAll() } returns flowOf(mapOf(roomId to flowOf()))
        every { roomServiceMock.getState(roomId, CanonicalAliasEventContent::class) } returns flowOf(
            ClientEvent.RoomEvent.StateEvent(
                content = CanonicalAliasEventContent(roomAliasId, setOf()),
                id = EventId("\$alias"),
                sender = meUserId,
                roomId = roomId,
                originTimestamp = 0,
                stateKey = "key"
            )
        )
        every { roomServiceMock.getById(roomId) } returns flowOf(room)

        every { userServiceMock.getById(roomId, meUserId) } returns flowOf(meRoomUser)
    }

    // TODO Test for RoomIDs and EventIDs once they are being found again

    @Test
    fun `mentions » find and process userid mention in the body`() = runTest {
        val cut = roomMessageTimelineElementViewModel(body = "Hii $meUserId!! :3")
        cut.mentionsInBody[4..17].shouldNotBeNull()

        val job = backgroundScope.launch {
            cut.mentionsInBody[4..17]?.collect {}
        }

        delay(100.milliseconds)
        cut.mentionsInBody[4..17]?.value shouldBe TimelineElementMention.User(meUserInfoElement)

        job.cancel()
    }

    @Test
    fun `mentions » find and process roomalias mention in the body`() = runTest {
        val cut = roomMessageTimelineElementViewModel(body = "I'm in $roomAliasId, wbu?")
        cut.mentionsInBody[7..22].shouldNotBeNull()

        val job = backgroundScope.launch {
            cut.mentionsInBody[7..22]?.collect {}
        }

        delay(100.milliseconds)
        cut.mentionsInBody[7..22]?.value shouldBe TimelineElementMention.Room(roomInfoElement)

        job.cancel()
    }


    private fun TestScope.roomMessageTimelineElementViewModel(
        body: String = "Amazing Message!",
        formattedBody: String? = null,
    ): RoomMessageTimelineElementViewModel<*> {
        return TextRoomMessageTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(meUserId to matrixClientMock)
                        )
                    )
                }.koin,
                userId = meUserId,
            ),
            content = RoomMessageEventContent.TextBased.Text(
                body,
                formattedBody = formattedBody
            ),
            roomId = roomId,
            onOpenMention = { _, _ -> }
        )
    }
}

package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomDisplayName
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.model.sync.Sync.Response.Rooms.JoinedRoom.RoomSummary
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class RoomNameTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    private val roomId = RoomId("Room", "localhost")

    private val user1Id = UserId("user1", "localhost")
    private val user2Id = UserId("user2", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val roomInviter = mock<RoomInviter>()

    lateinit var i18n: I18n

    init {
        beforeTest {
            resetMocks(matrixClientMock, roomServiceMock, userServiceMock, roomInviter)
            i18n = object : I18n(DefaultLanguages, createTestMatrixMessengerSettingsHolder(), GetSystemLang { "en" }) {}

            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                        single { userServiceMock }
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns user1Id
        }

        should("change the name of the room according to the explicitName field") {
            every { userServiceMock.getAll(eq(roomId)) } returns MutableStateFlow(emptyMap())
            val room = MutableStateFlow(
                createBasicRoom(
                    RoomDisplayName(
                        explicitName = null,
                        isEmpty = false,
                        otherUsersCount = 1,
                        summary = RoomSummary(heroes = listOf(user1Id))
                    )
                )
            )
            every { roomServiceMock.getById(roomId) } returns room
            every {
                userServiceMock.getById(eq(roomId), eq(user1Id))
            } returns MutableStateFlow(
                createRoomUser(
                    i = 1,
                    roomId = roomId,
                    userId = user1Id,
                    name = "user1",
                    membership = Membership.JOIN
                )
            )

            val scope = CoroutineScope(Dispatchers.Unconfined)
            val cut = RoomNameImpl(i18n, roomInviter)
            val result = cut.getRoomName(roomId, matrixClientMock, true).stateIn(scope)

            result.value shouldBe "user1 and one other"
            room.value =
                createBasicRoom(
                    RoomDisplayName(
                        explicitName = "Room name",
                        isEmpty = false,
                        otherUsersCount = 1,
                        summary = RoomSummary(heroes = listOf(user1Id, user2Id))
                    )
                )
            result.value shouldBe "Room name"
        }
    }

    private fun createRoomUser(
        i: Long,
        roomId: RoomId,
        userId: UserId,
        name: String,
        displayName: String = name,
        membership: Membership
    ): RoomUser {
        return RoomUser(
            roomId = roomId,
            userId = userId,
            name = name,
            event = StateEvent(
                MemberEventContent(
                    displayName = displayName,
                    membership = membership
                ),
                EventId("\$event$i"),
                userId,
                roomId,
                i,
                stateKey = userId.full
            )
        )
    }

    private fun createBasicRoom(name: RoomDisplayName?): Room {
        return Room(
            roomId = roomId,
            name = name,
            lastEventId = null,
            unreadMessageCount = 0,
            membership = Membership.JOIN,
            membersLoaded = false,
        )
    }
}

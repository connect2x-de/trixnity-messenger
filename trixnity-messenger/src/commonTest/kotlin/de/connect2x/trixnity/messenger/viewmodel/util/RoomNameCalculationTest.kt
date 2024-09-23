package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.client.MatrixClient
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

class RoomNameCalculationTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    private val user1 = UserId("user1", "server")
    private val user2 = UserId("user2", "server")
    private val user3 = UserId("user3", "server")
    private val user4 = UserId("user4", "server")
    private val roomId = RoomId("room1", "server")

    val matrixClientMock = mock<MatrixClient>()

    val userServiceMock = mock<UserService>()

    val roomInviterMock = mock<RoomInviter>()

    lateinit var i18n: I18n

    lateinit var user1Mocker: BlockingAnsweringScope<Flow<RoomUser?>>

    init {
        beforeTest {
            resetMocks(matrixClientMock, userServiceMock, roomInviterMock)
            i18n = object : I18n(DefaultLanguages, createTestMatrixMessengerSettingsHolder(), GetSystemLang { "en" }) {}

            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { userServiceMock }
                    }
                )
            }.koin
            user1Mocker = every { userServiceMock.getById(roomId, user1) }
            user1Mocker returns flowOf(
                RoomUser(
                    roomId,
                    userId = user1,
                    name = "User 1",
                    event = memberEvent(user1),
                )
            )
            every { userServiceMock.getById(roomId, user2) } returns flowOf(
                RoomUser(
                    roomId,
                    userId = user2,
                    name = "User 2",
                    event = memberEvent(user2),
                )
            )
            every { userServiceMock.getById(roomId, user3) } returns flowOf(
                RoomUser(
                    roomId,
                    userId = user3,
                    name = "User 3",
                    event = memberEvent(user3),
                )
            )
            every { userServiceMock.getById(roomId, user4) } returns flowOf(null)
        }

        should("return the room id when name field is empty") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(roomId, null, matrixClientMock).first() shouldBe "!room1:server"
        }

        should("return the value of the explicit name when the corresponding field is given") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = "Room name",
                    isEmpty = false,
                    otherUsersCount = 4,
                    heroes = listOf(user1),
                    summary = RoomSummary(heroes = listOf(user1))
                ),
                matrixClientMock,
            ).first() shouldBe "Room name"
        }

        should("return 'Emmpty chat' when isEmpty=false, |heroes|=0, otherUserCount=0") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = false,
                    otherUsersCount = 0,
                    heroes = listOf(),
                    summary = RoomSummary(heroes = listOf())
                ),
                matrixClientMock,
            ).first() shouldBe "Empty chat"
        }

        should("return the display name of the hero, when isEmpty=false, |heroes|=1, otherUserCount=0") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = false,
                    otherUsersCount = 0,
                    heroes = listOf(user1),
                    summary = RoomSummary(heroes = listOf(user1))
                ),
                matrixClientMock,
            ).first() shouldBe "User 1"
        }

        should("return the display names of all heroes, when isEmpty=false, |heroes|=2, otherUserCount=0") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = false,
                    otherUsersCount = 0,
                    heroes = listOf(user1, user2),
                    summary = RoomSummary(heroes = listOf(user1, user2))
                ),
                matrixClientMock,
            ).first() shouldBe "User 1 and User 2"
        }

        should("return the display names of all heroes, when isEmpty=false, |heroes|=3, otherUserCount=0") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = false,
                    otherUsersCount = 0,
                    heroes = listOf(user1, user2, user3),
                    summary = RoomSummary(heroes = listOf(user1, user2, user3))
                ),
                matrixClientMock,
            ).first() shouldBe "User 1, User 2 and User 3"
        }

        should("return the display name of the hero along with a count of the remaining users when isEmpty=false, |heroes|=1, otherUserCount=1") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = false,
                    otherUsersCount = 1,
                    heroes = listOf(user1),
                    summary = RoomSummary(heroes = listOf(user1))
                ),
                matrixClientMock,
            ).first() shouldBe "User 1 and one other"
        }

        should("return the display names of the heroes along with a count of the remaining users when isEmpty=false, |heroes|=2, otherUserCount=1") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = false,
                    otherUsersCount = 1,
                    heroes = listOf(user1, user2),
                    summary = RoomSummary(heroes = listOf(user1, user2))
                ),
                matrixClientMock,
            ).first() shouldBe "User 1, User 2 and one other"
        }

        should("return the display name of the hero along with a count of the remaining users when isEmpty=false, |heroes|=1, otherUserCount=2") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = false,
                    otherUsersCount = 2,
                    heroes = listOf(user1),
                    summary = RoomSummary(heroes = listOf(user1))
                ),
                matrixClientMock,
            ).first() shouldBe "User 1 and 2 others"
        }

        should("return the display names of the heroes along with a count of the remaining users when isEmpty=false, |heroes|=2, otherUserCount=2") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = false,
                    otherUsersCount = 2,
                    heroes = listOf(user1, user2),
                    summary = RoomSummary(heroes = listOf(user1, user2))
                ),
                matrixClientMock,
            ).first() shouldBe "User 1, User 2 and 2 others"
        }

        should("return 'Empty Chat' when isEmpty=true, |heroes|=0, otherUserCount=0") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = true,
                    otherUsersCount = 0,
                    heroes = listOf(),
                    summary = RoomSummary(heroes = listOf())
                ),
                matrixClientMock,
            ).first() shouldBe "Empty chat"
        }

        should("return the display name of the hero surrounded by an Empty-Room-String when isEmpty=true |heroes|=1, otherUserCount=0") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = true,
                    otherUsersCount = 0,
                    heroes = listOf(user1),
                    summary = RoomSummary(heroes = listOf(user1))
                ),
                matrixClientMock,
            ).first() shouldBe "Empty chat (was User 1)"
        }

        should("return the display names of the heroes surrounded by an Empty-Room-String, when isEmpty=true, |heroes|=2, otherUserCount=0") {
            val roomDisplayName =
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = true,
                    otherUsersCount = 0,
                    heroes = listOf(user1, user2),
                    summary = RoomSummary(heroes = listOf(user1, user2))
                )

            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                roomDisplayName,
                matrixClientMock,
            ).first() shouldBe "Empty chat (was User 1 and User 2)"
        }

        should("return the display names of the heroes surrounded by an Empty-Room-String, when isEmpty=true, |heroes|=3, otherUserCount=0") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = true,
                    otherUsersCount = 0,
                    heroes = listOf(user1, user2, user3),
                    summary = RoomSummary(heroes = listOf(user1, user2, user3))
                ),
                matrixClientMock,
            ).first() shouldBe "Empty chat (was User 1, User 2 and User 3)"
        }

        should("return the display name of the hero along with a count of the remaining users surrounded by an Empty-Room-String when isEmpty=true, |heroes|=1, otherUserCount=1") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = true,
                    otherUsersCount = 1,
                    heroes = listOf(user1),
                    summary = RoomSummary(heroes = listOf(user1))
                ),
                matrixClientMock,
            ).first() shouldBe "Empty chat (was User 1 and one other)"
        }

        should("return the display names of the heroes along with a count of the remaining users surrounded by an Empty-Room-String when isEmpty=true, |heroes|=2, otherUserCount=1") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = true,
                    otherUsersCount = 1,
                    heroes = listOf(user1, user2),
                    summary = RoomSummary(heroes = listOf(user1, user2))
                ),
                matrixClientMock,
            ).first() shouldBe "Empty chat (was User 1, User 2 and one other)"
        }

        should("return the display name of the hero along with the count of the remaining users surrounded by an Empty-Room-String when isEmpty=true, |heroes|=1, otherUserCount=2") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = true,
                    otherUsersCount = 2,
                    heroes = listOf(user1),
                    summary = RoomSummary(heroes = listOf(user1))
                ),
                matrixClientMock,
            ).first() shouldBe "Empty chat (was User 1 and 2 others)"
        }

        should("return the display names of the heroes along with a count of the remaining users surrounded by an Empty-Room-String when isEmpty=true, |heroes|=2, otherUserCount=2") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = true,
                    otherUsersCount = 2,
                    heroes = listOf(user1, user2),
                    summary = RoomSummary(heroes = listOf(user1, user2))
                ),
                matrixClientMock,
            ).first() shouldBe "Empty chat (was User 1, User 2 and 2 others)"
        }

        should("update the name of the room when the display name of a member changes") {
            val user1displayName = MutableStateFlow(RoomUser(roomId, user1, "User 1", memberEvent(user1)))
            user1Mocker returns user1displayName

            val roomDisplayName =
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = false,
                    otherUsersCount = 1,
                    heroes = listOf(user1, user2),
                    summary = RoomSummary(heroes = listOf(user1, user2))
                )

            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(roomId, roomDisplayName, matrixClientMock)
                .first() shouldBe "User 1, User 2 and one other"
            user1displayName.value = RoomUser(roomId, user1, "User 1 changed", memberEvent(user1))
            cut.calculateRoomName(
                roomId,
                roomDisplayName,
                matrixClientMock,
            ).first() shouldBe "User 1 changed, User 2 and one other"
        }

        should("return the UserId from the hero without RoomDisplayName object when isEmpty=true, |heroes|=2, otherUserCount=2") {
            val cut = RoomNameImpl(i18n, roomInviterMock)
            cut.calculateRoomName(
                roomId,
                RoomDisplayName(
                    explicitName = null,
                    isEmpty = true,
                    otherUsersCount = 2,
                    heroes = listOf(user1, user2, user4),
                    summary = RoomSummary(heroes = listOf(user1, user2, user4))
                ),
                matrixClientMock,
            ).first() shouldBe "Empty chat (was User 1, User 2, @user4:server and 2 others)"
        }
    }

    private fun memberEvent(userId: UserId) = StateEvent(
        MemberEventContent(membership = Membership.JOIN),
        EventId(""),
        userId,
        roomId,
        0L,
        stateKey = ""
    )
}

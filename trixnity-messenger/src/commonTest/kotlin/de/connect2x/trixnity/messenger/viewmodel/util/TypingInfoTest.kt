package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.m.TypingEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test

class TypingInfoTest {
    val matrixClientMock = mock<MatrixClient>()
    val userServiceMock = mock<UserService>()
    val roomServiceMock = mock<RoomService>()

    val roomId = RoomId("!room1")
    val user1 = UserId("1", "localhost")
    val user2 = UserId("2", "localhost")
    val user3 = UserId("3", "localhost")
    val user4 = UserId("4", "localhost")
    val user5 = UserId("5", "localhost")
    val user6 = UserId("6", "localhost")

    val i18n: I18n

    init {
        resetMocks(matrixClientMock)
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { userServiceMock }
                    single { roomServiceMock }
                })
        }.koin
        every { matrixClientMock.userId } returns user1
        mockUser(user1, "1")
        mockUser(user2, "2")
        mockUser(user3, "3")
        mockUser(user4, "4")
        mockUser(user5, "5")
        mockUser(user6, "6")
        i18n = object : I18n(
            DefaultLanguages,
            createTestMatrixMessengerSettingsHolder(),
            GetSystemLang { "en" },
            TimeZone.of("CET"),
        ) {}
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `return null when no user is typing anything`() = runTest {
        typingInfo(
            matrixClientMock, roomId, i18n, TypingEventContent(
                users = setOf()
            )
        ) shouldBe null
    }

    @Test
    fun `return writing message when is direct room`() = runTest {
        every { roomServiceMock.getById(roomId) } returns MutableStateFlow(Room(roomId, isDirect = true))
        typingInfo(
            matrixClientMock, roomId, i18n, TypingEventContent(
                users = setOf(user2)
            )
        ) shouldBe "is typing..."
    }

    @Test
    fun `return username is typing when single user is writing in a group`() = runTest {
        every { roomServiceMock.getById(roomId) } returns MutableStateFlow(Room(roomId, isDirect = false))
        typingInfo(
            matrixClientMock, roomId, i18n, TypingEventContent(
                users = setOf(user2)
            )
        ) shouldBe "2 is typing..."
    }

    @Test
    fun `return all names when multiple users are writing in a group`() = runTest {
        every { roomServiceMock.getById(roomId) } returns MutableStateFlow(Room(roomId, isDirect = false))
        typingInfo(
            matrixClientMock, roomId, i18n, TypingEventContent(
                users = setOf(user2, user3, user4)
            )
        ) shouldBe "2, 3 and 4 are typing..."
    }

    @Test
    fun `filter current user from the list`() = runTest {
        every { roomServiceMock.getById(roomId) } returns MutableStateFlow(Room(roomId, isDirect = false))
        typingInfo(
            matrixClientMock, roomId, i18n, TypingEventContent(
                users = setOf(user1, user2)
            )
        ) shouldBe "2 is typing..."
    }

    @Test
    fun `cut more than 4 users writing in parallel and return a shorter message`() = runTest {
        every { roomServiceMock.getById(roomId) } returns MutableStateFlow(Room(roomId, isDirect = false))
        typingInfo(
            matrixClientMock, roomId, i18n, TypingEventContent(
                users = setOf(user2, user3, user4, user5, user6)
            )
        ) shouldBe "2, 3 and others are typing..."
    }

    private fun mockUser(userId: UserId, name: String) {
        every { userServiceMock.getById(roomId, userId) } returns MutableStateFlow(
            RoomUser(roomId, userId, name, memberEvent())
        )
    }

    private fun memberEvent(): ClientEvent.RoomEvent.StateEvent<MemberEventContent> = ClientEvent.RoomEvent.StateEvent(
        content = MemberEventContent(membership = Membership.JOIN),
        id = EventId("1"),
        sender = user1,
        roomId = roomId,
        originTimestamp = 0L,
        stateKey = "",
    )
}

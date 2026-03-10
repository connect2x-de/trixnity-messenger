package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.UnsignedRoomEventData
import de.connect2x.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import kotlinx.coroutines.delay
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

@Suppress("NonAsciiCharacters")
class MemberStateTimelineElementViewModelTest {

    val roomId = RoomId("!room")
    val sender = UserId("user", "server")
    val bob = UserId("bob", "localhost")
    val eventId = EventId("event")
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    val isDirect = MutableStateFlow(false)
    val senderName = MutableStateFlow("Sender")

    init {
        resetCalls(matrixClientMock, roomServiceMock, userServiceMock)
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                })
        }.koin
        senderName.value = "Sender"
        every { userServiceMock.getById(roomId, sender) } returns senderName.map {
            RoomUser(
                roomId, sender, it, ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(membership = Membership.JOIN),
                    id = eventId,
                    sender = sender,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
        }
        isDirect.value = false
        every { roomServiceMock.getById(roomId) } returns isDirect.map { Room(roomId, isDirect = it) }
        every { roomServiceMock.getState<HistoryVisibilityEventContent>(roomId, any(),any()) } returns
                flowOf(
                    ClientEvent.StrippedStateEvent(
                        HistoryVisibilityEventContent(HistoryVisibilityEventContent.HistoryVisibility.INVITED),
                        sender = UserId("user", "server"),
                        stateKey = "stateKey",
                    )
                )

        every {
            userServiceMock.getById(any(), UserId("bob", "localhost"))
        } returns MutableStateFlow(
            RoomUser(
                roomId = RoomId("!room1"),
                userId = UserId("bob", "localhost"),
                name = "Bob",
                event = ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(membership = Membership.JOIN),
                    id = EventId(""),
                    sender = UserId(""),
                    roomId = RoomId(""),
                    originTimestamp = 0L,
                    stateKey = "",
                ),
            )
        )
        every {
            userServiceMock.getById(any(), UserId("mallory", "localhost"))
        } returns MutableStateFlow(
            RoomUser(
                roomId = RoomId("!room1"),
                userId = UserId("mallory", "localhost"),
                name = "Mallory",
                event = ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(membership = Membership.JOIN),
                    id = EventId(""),
                    sender = UserId(""),
                    roomId = RoomId(""),
                    originTimestamp = 0L,
                    stateKey = "",
                ),
            )
        )
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `name change » should show an indicator for name changes`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                displayName = "I have changed!",
                previousMemberEventContent = memberEventContent(displayName = "I am the original"),
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "'I am the original' has changed their name to 'I have changed!'"
    }

    @Test
    fun `avatar change » should show an indicator for avatar image changes`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                avatarUrl = "mxc://localhost/new_url",
                previousMemberEventContent = memberEventContent(avatarUrl = "mxc://localhost/boring_old_url"),
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Sender has changed the avatar image"
        
    }

    @Test
    fun `joining user » should show an indicator without history warning when someone else joins an unencrypted room`() = runTest {
        val currentUser = UserId("@alice:localhost")
        every { matrixClientMock.userId } returns currentUser
        every { roomServiceMock.getById(roomId) } returns isDirect.map { Room(roomId, isDirect = it, encrypted = false) }
        
        val joiningUser = UserId("@bob:localhost")
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.JOIN,
                previousMemberEventContent = memberEventContent(),
                stateKey = joiningUser.full,
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        backgroundScope.launch { cut.preJoinHistoryWarning.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob has joined the group"
        cut.preJoinHistoryWarning.value shouldBe null
    }
    
    @Test
    fun `joining user » should show an indicator without history warning when I join an unencrypted room`() = runTest {
        val joiningUser = UserId("@bob:localhost")
        every { matrixClientMock.userId } returns joiningUser
        every { roomServiceMock.getById(roomId) } returns isDirect.map { Room(roomId, isDirect = it, encrypted = false) }
        
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.JOIN,
                previousMemberEventContent = memberEventContent(),
                stateKey = joiningUser.full,
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        backgroundScope.launch { cut.preJoinHistoryWarning.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob has joined the group"
        cut.preJoinHistoryWarning.value shouldBe null
    }

    @Test
    fun `joining user » should show an indicator without history warning when someone else joins an encrypted room`() = runTest {
        val currentUser = UserId("@alice:localhost")
        every { matrixClientMock.userId } returns currentUser
        every { roomServiceMock.getById(roomId) } returns isDirect.map { Room(roomId, isDirect = it, encrypted = true) }
        
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.JOIN,
                previousMemberEventContent = memberEventContent(),
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        backgroundScope.launch { cut.preJoinHistoryWarning.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob has joined the group"
        cut.preJoinHistoryWarning.value shouldBe null
    }

    @Test
    fun `joining user » should show an indicator without history warning when I join an encrypted room but there is no previous content eg on room creation`() = runTest {
        val currentUser = UserId("@bob:localhost")
        every { matrixClientMock.userId } returns currentUser
        every { roomServiceMock.getById(roomId) } returns isDirect.map { Room(roomId, isDirect = it, encrypted = true) }

        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.JOIN,
                previousMemberEventContent = null,
                stateKey = currentUser.full,
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        backgroundScope.launch { cut.preJoinHistoryWarning.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob has joined the group"
        cut.preJoinHistoryWarning.value shouldBe null
        
    }

    @Test
    fun `joining user » should show an indicator with history warning when I join an encrypted room`() = runTest {
        val currentUser = UserId("@bob:localhost")
        every { matrixClientMock.userId } returns currentUser
        every { roomServiceMock.getById(roomId) } returns isDirect.map { Room(roomId, isDirect = it, encrypted = true) }
        
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.JOIN,
                previousMemberEventContent = memberEventContent(),
                stateKey = currentUser.full,
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        backgroundScope.launch { cut.preJoinHistoryWarning.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob has joined the group"
        cut.preJoinHistoryWarning.value shouldBe "Messages from before you joined are unavailable"
    }

    @Test
    fun `history warning » should not show history warning on any other event than join`() = runTest {
        val currentUser = UserId("@bob:localhost")
        every { matrixClientMock.userId } returns currentUser
        every { roomServiceMock.getById(roomId) } returns isDirect.map { Room(roomId, isDirect = it, encrypted = true) }

        suspend fun testNoHistoryWarning(membership: Membership) {
            val cut = memberStatusViewModel(
                mockTimelineEvent(
                    membership = membership,
                    previousMemberEventContent = memberEventContent(),
                    stateKey = currentUser.full,
                ),
            )
            backgroundScope.launch { cut.preJoinHistoryWarning.collect {} }
            delay(1.seconds)
            
            cut.preJoinHistoryWarning.value shouldBe null
        }
        val noJoin = listOf(Membership.INVITE, Membership.LEAVE, Membership.BAN, Membership.KNOCK)
        noJoin.forEach { testNoHistoryWarning(it) }
    }

    @Test
    fun `leaving user » should show an indicator for user leaving a room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                senderId = UserId("@bob:localhost"),
                membership = Membership.LEAVE,
                previousMemberEventContent = memberEventContent(membership = Membership.JOIN),
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob has left the group"
        
    }

    @Test
    fun `banned user » should show an indicator for user being banned from a room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.BAN,
                previousMemberEventContent = memberEventContent(membership = Membership.JOIN),
                stateKey = "@mallory:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Mallory was banned from the group by Sender"
    }

    @Test
    fun `banned user » should show an indicator with reason for user being banned from a room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.BAN,
                previousMemberEventContent = memberEventContent(membership = Membership.JOIN),
                stateKey = "@mallory:localhost",
                reason = "he spammed our chat :("
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Mallory was banned from the group by Sender because \"he spammed our chat :(\""
    }

    @Test
    fun `banned user » should show an indicator for user being unbanned from a room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.LEAVE,
                previousMemberEventContent = memberEventContent(membership = Membership.BAN),
                stateKey = "@mallory:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Mallory was unbanned by Sender"
    
    }

    @Test
    fun `banned user » should show an indicator with reason for user being unbanned from a room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.LEAVE,
                previousMemberEventContent = memberEventContent(membership = Membership.BAN),
                stateKey = "@mallory:localhost",
                reason = "he spammed our chat :("
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Mallory was unbanned by Sender because \"he spammed our chat :(\""
        
    }

    @Test
    fun `kicked user » should show an indicator for user being kicked from a room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.LEAVE,
                previousMemberEventContent = memberEventContent(membership = Membership.JOIN),
                stateKey = "@mallory:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Mallory was removed from the group by Sender"
        
    }

    @Test
    fun `kicked user » should show an indicator with reason for user being kicked from a room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.LEAVE,
                previousMemberEventContent = memberEventContent(membership = Membership.JOIN),
                stateKey = "@mallory:localhost",
                reason = "he spammed our chat :("
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Mallory was removed from the group by Sender because \"he spammed our chat :(\""
    }

    @Test
    fun `invited user » should show an indicator for an invitation of a user to the room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.INVITE,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob was invited by Sender"
    }

    @Test
    fun `invited user » should show an indicator with reason for an invitation of a user to the room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.INVITE,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
                reason = "I want him to play Stardew Valley with us"
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob was invited by Sender because \"I want him to play Stardew Valley with us\""
    }

    @Test
    fun `invited user » should show an appropriate indicator when the invitation was rejected`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.LEAVE,
                previousMemberEventContent = memberEventContent(membership = Membership.INVITE),
                stateKey = "@bob:localhost",
                reason = "I don't want to play Stardew Valley with you",
                senderId = bob
            )
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob has rejected the invitation because \"I don't want to play Stardew Valley with you\""
    }

    @Test
    fun `invited user » should show an appropriate indicator when the invitation was revoked`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.LEAVE,
                previousMemberEventContent = memberEventContent(membership = Membership.INVITE),
                stateKey = bob.full,
                reason = "I don't want him to play Stardew Valley with us",
            )
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        cut.changeMessage.value shouldBe "Sender has revoked the invitation to Bob because \"I don't want him to play Stardew Valley with us\""
    }

    @Test
    fun `knocking user » should show an indicator for a user knocking at the room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.KNOCK,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob requested to join the group. Check the room settings to manage the Request"
    }

    @Test
    fun `knocking user » should show an indicator with the reason for a user knocking at the room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.KNOCK,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
                reason = "he also likes treecake"
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob requested to join the group because \"he also likes treecake\". Check the room settings to manage the Request"
    }

    @Test
    fun `changed name » should update indicator on username changes`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.INVITE,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob was invited by Sender"
        senderName.value = "Sender2"
        delay(1.seconds)
        cut.changeMessage.value shouldBe "Bob was invited by Sender2"
    }

    @Test
    fun `changed room » update indicator on room changing 'direct' state`() = runTest {
        val currentUser = UserId("@alice:localhost")
        every { matrixClientMock.userId } returns currentUser

        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.JOIN,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob has joined the group"
        isDirect.value = true
        delay(1.seconds)
        cut.changeMessage.value shouldBe "Bob has joined the chat"
    }
    
    @Test
    fun `changed room » update indicator and keep history warning on room changing 'direct' state`() = runTest {
        val currentUser = UserId("@bob:localhost")
        every { matrixClientMock.userId } returns currentUser
        every { roomServiceMock.getById(roomId) } returns isDirect.map { Room(roomId, isDirect = it, encrypted = true) }

        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.JOIN,
                previousMemberEventContent = memberEventContent(),
                stateKey = currentUser.full,
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        backgroundScope.launch { cut.preJoinHistoryWarning.collect {} }
        delay(1.seconds)
        
        cut.changeMessage.value shouldBe "Bob has joined the group"
        cut.preJoinHistoryWarning.value shouldBe "Messages from before you joined are unavailable"
        isDirect.value = true
        delay(1.seconds)
        cut.changeMessage.value shouldBe "Bob has joined the chat"
        cut.preJoinHistoryWarning.value shouldBe "Messages from before you joined are unavailable"
    }
    
    private fun TestScope.memberStatusViewModel(
        timelineEvent: TimelineEvent,
    ): MemberStateTimelineElementViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(UserId("test", "server") to matrixClientMock)
                )
            )
        }.koin
        return MemberStateTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = UserId("test", "server"),
            ),
            content = timelineEvent.event.content as MemberEventContent,
            roomId = roomId,
            eventId = eventId,
        )
    }

    private fun memberEventContent(
        avatarUrl: String = "",
        displayName: String = "Bob",
        membership: Membership = Membership.JOIN,
        isDirect: Boolean = false,
    ) = MemberEventContent(
        avatarUrl = avatarUrl,
        displayName = displayName,
        membership = membership,
        isDirect = isDirect,
    )

    private fun mockTimelineEvent(
        avatarUrl: String = "",
        displayName: String = "Bob",
        membership: Membership = Membership.JOIN,
        isDirect: Boolean = false,
        stateKey: String = "",
        previousMemberEventContent: MemberEventContent? = null,
        reason: String? = null,
        senderId: UserId? = null
    ): TimelineEvent {
        val timelineEvent = TimelineEvent(
            event = ClientEvent.RoomEvent.StateEvent(
                content = MemberEventContent(
                    avatarUrl = avatarUrl,
                    displayName = displayName,
                    membership = membership,
                    isDirect = isDirect,
                    reason = reason,
                ),
                id = eventId,
                sender = senderId ?: sender,
                roomId = roomId,
                originTimestamp = 0L,
                unsigned = UnsignedRoomEventData.UnsignedStateEventData(
                    previousContent = previousMemberEventContent,
                ),
                stateKey = stateKey,
            ),
            content = null,
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )
        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns flowOf(timelineEvent)
        return timelineEvent
    }
}


package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.key.KeyService
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.m.IgnoredUserListEventContent
import de.connect2x.trixnity.core.model.events.m.Presence
import de.connect2x.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.crypto.key.UserTrustLevel
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.RoomPresence
import de.connect2x.trixnity.messenger.viewmodel.util.RoomTopic
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import de.connect2x.trixnity.utils.toByteArrayFlow
import dev.mokkery.answering.BlockingAnsweringScope
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@Suppress("NonAsciiCharacters")
class RoomHeaderViewModelTest {
    private val roomId = RoomId("!room1")

    private val me = UserId("bob", "localhost")
    private val meRoomUser =
        RoomUser(
            roomId,
            me,
            me.full,
            StateEvent(
                MemberEventContent(membership = Membership.JOIN),
                EventId(""),
                me,
                roomId,
                originTimestamp = 0L,
                stateKey = "",
            ),
        )

    private val otherUser = UserId("lala", "localhost")
    private val otherRoomUser =
        RoomUser(
            roomId,
            otherUser,
            otherUser.full,
            StateEvent(
                MemberEventContent(membership = Membership.JOIN),
                EventId(""),
                otherUser,
                roomId,
                originTimestamp = 0L,
                stateKey = "",
            ),
        )

    private val knockingUser = UserId("maria", "localhost")
    private val knockingRoomUser =
        RoomUser(
            roomId,
            otherUser,
            otherUser.full,
            StateEvent(
                MemberEventContent(membership = Membership.KNOCK),
                EventId(""),
                otherUser,
                roomId,
                originTimestamp = 0L,
                stateKey = "",
            ),
        )

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()
    private val mediaServiceMock = mock<MediaService>()
    private val keyServiceMock = mock<KeyService>()
    private val roomNameMock = mock<RoomName>()
    private val roomTopicMock = mock<RoomTopic>()
    private val initialsMock = mock<Initials>()
    private val roomPresenceMock = mock<RoomPresence>()
    private val userBlockingMock = mock<UserBlocking>()

    private var roomNameElement: BlockingAnsweringScope<Flow<String>>
    private var roomTopicElement: BlockingAnsweringScope<Flow<String>>
    private var ignoredUsers: BlockingAnsweringScope<Flow<IgnoredUserListEventContent?>>
    private val room = MutableStateFlow<Room?>(null)

    init {
        resetMocks(
            matrixClientMock,
            roomServiceMock,
            userServiceMock,
            mediaServiceMock,
            keyServiceMock,
            roomNameMock,
            roomTopicMock,
            initialsMock,
            roomPresenceMock,
            userBlockingMock,
        )
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { userServiceMock }
                            single { mediaServiceMock }
                            single { keyServiceMock }
                        }
                    )
                }
                .koin
        every { matrixClientMock.userId } returns me

        roomNameElement = every { roomNameMock.getRoomName(any<RoomId>(), any(), any<Boolean>()) }
        roomNameElement returns MutableStateFlow("My Room")
        roomTopicElement = every { roomTopicMock.getRoomTopic(any<RoomId>(), any(), any<Boolean>()) }
        roomTopicElement returns MutableStateFlow("My Topic")
        every { roomServiceMock.usersTyping } returns MutableStateFlow(emptyMap())

        every { userServiceMock.getAll(roomId) } returns
            flowOf(
                mapOf(
                    me to flowOf(meRoomUser),
                    otherUser to flowOf(otherRoomUser),
                    knockingUser to flowOf(knockingRoomUser),
                )
            )
        ignoredUsers = every { userServiceMock.getAccountData(IgnoredUserListEventContent::class) }
        ignoredUsers returns flowOf(IgnoredUserListEventContent(emptyMap()))

        every { initialsMock.compute(any()) } returns "MR"
        room.value = Room(roomId, avatarUrl = "mxc://localhost/123456")
        every { roomServiceMock.getById(roomId) } returns room
        everySuspend {
            mediaServiceMock.getThumbnail("mxc://localhost/123456", any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(InMemoryPlatformMedia("image".encodeToByteArray().toByteArrayFlow()))
        every { roomPresenceMock.invoke(any(), roomId) } returns flowOf(Presence.ONLINE)

        every { userBlockingMock.isUserBlocked(any(), any()) } returns MutableStateFlow(false)

        every { roomServiceMock.getState(any(), JoinRulesEventContent::class, any()) } returns
            MutableStateFlow(
                StateEvent(
                    content = JoinRulesEventContent(joinRule = JoinRulesEventContent.JoinRule.Private),
                    id = EventId("1"),
                    sender = me,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
        every { roomServiceMock.getState(any(), HistoryVisibilityEventContent::class, any()) } returns
            MutableStateFlow(
                StateEvent(
                    content =
                        HistoryVisibilityEventContent(
                            historyVisibility = HistoryVisibilityEventContent.HistoryVisibility.JOINED
                        ),
                    id = EventId("1"),
                    sender = me,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `should show correct room name with initials and avatar and react to changes`() = runTest {
        val roomName = MutableStateFlow("My Room")
        roomNameElement returns roomName
        every { userServiceMock.getAll(roomId) } returns flowOf(mapOf())

        val cut = roomHeaderViewModel()
        delay(100)

        cut.roomHeaderInfo.value shouldBe
            RoomHeaderInfo(
                "My Room",
                "My Topic",
                "MR",
                "image".encodeToByteArray(),
                Presence.ONLINE,
                isEncrypted = false,
                isPublic = false,
                isLeave = false,
            )

        roomName.value = "New Room Name"
        delay(100)

        cut.roomHeaderInfo.value shouldBe
            RoomHeaderInfo(
                "New Room Name",
                "My Topic",
                "MR",
                "image".encodeToByteArray(),
                Presence.ONLINE,
                isEncrypted = false,
                isPublic = false,
                isLeave = false,
            )
    }

    @Test
    fun `compute trust level of null for non-direct rooms`() = runTest {
        every { userServiceMock.getAll(roomId) } returns flowOf(mapOf())

        val cut = roomHeaderViewModel()
        delay(100)

        cut.userTrustLevel.value shouldBe null
    }

    @Test
    fun `react to changes in the user's trust level`() = runTest {
        val trustLevel = MutableStateFlow<UserTrustLevel>(UserTrustLevel.CrossSigned(verified = true))
        val directRoom = MutableStateFlow(mapOf(otherUser to flowOf(otherRoomUser), me to flowOf(meRoomUser)))
        room.update { it?.copy(isDirect = true) }
        every { userServiceMock.getAll(roomId) } returns directRoom
        every { keyServiceMock.getTrustLevel(otherUser) } returns trustLevel

        val cut = roomHeaderViewModel()
        delay(100)

        cut.userTrustLevel.value shouldBe UserTrustLevel.CrossSigned(verified = true)

        trustLevel.value = UserTrustLevel.Blocked
        delay(100)

        cut.userTrustLevel.value shouldBe UserTrustLevel.Blocked

        directRoom.value = mapOf()
        delay(100)

        cut.userTrustLevel.value shouldBe null
    }

    @Test
    fun `allow to verify other user if not yet verified and vice versa`() = runTest {
        val trustLevel = MutableStateFlow(UserTrustLevel.CrossSigned(verified = false))
        room.update { it?.copy(isDirect = true) }
        every { userServiceMock.getAll(roomId) } returns
            flowOf(mapOf(otherUser to flowOf(otherRoomUser), me to flowOf(meRoomUser)))
        every { keyServiceMock.getTrustLevel(otherUser) } returns trustLevel

        val cut = roomHeaderViewModel()
        delay(100)

        cut.canVerifyUser.value shouldBe true

        trustLevel.value = UserTrustLevel.CrossSigned(verified = true)
        delay(100)

        cut.canVerifyUser.value shouldBe false
    }

    @Test
    fun `not allow user verification in non-direct room`() = runTest {
        every { userServiceMock.getAll(roomId) } returns flowOf(mapOf())
        every { keyServiceMock.getTrustLevel(otherUser) } returns flowOf(UserTrustLevel.CrossSigned(verified = false))

        val cut = roomHeaderViewModel()
        delay(100)

        cut.canVerifyUser.value shouldBe false
    }

    @Test
    fun `allow to block user in a direct room with only 2 users and user is not yet blocked and unblock if already blocked`() =
        runTest {
            val ignoredUsersEventContent = MutableStateFlow(IgnoredUserListEventContent(mapOf()))
            ignoredUsers returns ignoredUsersEventContent
            room.update { it?.copy(isDirect = true) }
            every { userServiceMock.getAll(roomId) } returns
                flowOf(mapOf(otherUser to flowOf(otherRoomUser), me to flowOf(meRoomUser)))
            every { keyServiceMock.getTrustLevel(otherUser) } returns
                flowOf(UserTrustLevel.CrossSigned(verified = false))

            val cut = roomHeaderViewModel()
            delay(100)

            cut.canBlockUser.value shouldBe true
            cut.canUnblockUser.value shouldBe false

            ignoredUsersEventContent.value = IgnoredUserListEventContent(mapOf(otherUser to JsonObject(emptyMap())))
            delay(100)

            cut.canBlockUser.value shouldBe false
            cut.canUnblockUser.value shouldBe true
        }

    @Test
    fun `not allow to block user in non-direct rooms or direct rooms with more than 2 participants`() = runTest {
        val directRoom = MutableStateFlow(mapOf<UserId, Flow<RoomUser>>())
        every { userServiceMock.getAll(roomId) } returns directRoom
        every { keyServiceMock.getTrustLevel(otherUser) } returns flowOf(UserTrustLevel.CrossSigned(verified = false))

        val cut = roomHeaderViewModel()
        delay(100)

        cut.canBlockUser.value shouldBe false
        cut.canUnblockUser.value shouldBe false

        room.update { it?.copy(isDirect = true) }
        directRoom.value = mapOf(otherUser to flowOf(otherRoomUser), me to flowOf(meRoomUser))
        delay(100)

        cut.canBlockUser.value shouldBe true
        cut.canUnblockUser.value shouldBe false

        directRoom.value =
            mapOf(
                otherUser to flowOf(otherRoomUser),
                knockingUser to flowOf(knockingRoomUser),
                me to flowOf(meRoomUser),
            )
        delay(100)

        cut.canBlockUser.value shouldBe false
        cut.canUnblockUser.value shouldBe false
    }

    @Test
    fun `knocking » should calculate amount of knocking users`() = runTest {
        every { userServiceMock.getAll(roomId) } returns
            flowOf(
                mapOf(
                    otherUser to
                        flowOf(
                            otherRoomUser.copy(
                                event =
                                    StateEvent(
                                        content = MemberEventContent(membership = Membership.KNOCK),
                                        id = EventId("1"),
                                        sender = otherUser,
                                        roomId = roomId,
                                        originTimestamp = 0L,
                                        stateKey = "",
                                    )
                            )
                        )
                )
            )

        val cut = roomHeaderViewModel()
        delay(500.milliseconds)
        cut.knockingMembersCount.value shouldBe 1
    }

    @Test
    fun `encryption being turned on should lead to encryption being turned on`() = runTest {
        room.value = room.value?.copy(encrypted = true)

        val cut = roomHeaderViewModel()
        subscribe(cut)

        delay(10)

        cut.roomHeaderInfo.value.isEncrypted shouldBe true
    }

    @Test
    fun `JoinRule Public should lead to isPublic being true`() = runTest {
        every { roomServiceMock.getState(any(), JoinRulesEventContent::class, any()) } returns
            MutableStateFlow(
                StateEvent(
                    content = JoinRulesEventContent(joinRule = JoinRulesEventContent.JoinRule.Public),
                    id = EventId("1"),
                    sender = me,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )

        val cut = roomHeaderViewModel()
        subscribe(cut)

        delay(10)

        cut.roomHeaderInfo.value.isPublic shouldBe true
    }

    @Test
    fun `HistoryVisibility World_Readable should lead to isPublic being true`() = runTest {
        every { roomServiceMock.getState(any(), HistoryVisibilityEventContent::class, any()) } returns
            MutableStateFlow(
                StateEvent(
                    content =
                        HistoryVisibilityEventContent(
                            historyVisibility = HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE
                        ),
                    id = EventId("1"),
                    sender = me,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )

        val cut = roomHeaderViewModel()
        subscribe(cut)

        delay(10)

        cut.roomHeaderInfo.value.isPublic shouldBe true
    }

    private fun TestScope.roomHeaderViewModel(): RoomHeaderViewModelImpl {
        return RoomHeaderViewModelImpl(
                viewModelContext =
                    testMatrixClientViewModelContext(
                        di =
                            koinApplication {
                                    modules(
                                        createTestDefaultTrixnityMessengerModules(mapOf(me to matrixClientMock)) +
                                            module {
                                                single { roomNameMock }
                                                single { roomTopicMock }
                                                single { roomPresenceMock }
                                                single { initialsMock }
                                                single { userBlockingMock }
                                            }
                                    )
                                }
                                .koin,
                        me,
                    ),
                selectedRoomId = roomId,
                onBack = mock(),
                onVerifyUser = mock(),
                onOpenRoomSettings = mock(),
                onOpenUserProfile = mock(),
            )
            .also { subscribe(it) }
    }

    private fun TestScope.subscribe(roomHeaderViewModel: RoomHeaderViewModel) {
        backgroundScope.launch { roomHeaderViewModel.roomHeaderInfo.collect() }
        backgroundScope.launch { roomHeaderViewModel.usersTyping.collect() }
        backgroundScope.launch { roomHeaderViewModel.canVerifyUser.collect() }
        backgroundScope.launch { roomHeaderViewModel.error.collect() }
        backgroundScope.launch { roomHeaderViewModel.userTrustLevel.collect() }
        backgroundScope.launch { roomHeaderViewModel.isUserBlocked.collect() }
        backgroundScope.launch { roomHeaderViewModel.canBlockUser.collect() }
        backgroundScope.launch { roomHeaderViewModel.canUnblockUser.collect() }
        backgroundScope.launch { roomHeaderViewModel.knockingMembersCount.collect() }
    }
}

//package de.connect2x.trixnity.messenger.viewmodel.timeline
//
//import com.arkivanov.decompose.DefaultComponentContext
//import com.arkivanov.essenty.lifecycle.LifecycleRegistry
//import io.kotest.core.spec.style.ShouldSpec
//import io.kotest.matchers.shouldBe
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.test.runTest
//import de.connect2x.trixnity.messenger.viewmodel.RoomName
//import de.connect2x.trixnity.messenger.viewmodel.RoomNameElement
//import de.connect2x.trixnity.messenger.viewmodel.util.DirectRoom
//import de.connect2x.trixnity.messenger.viewmodel.util.IDirectRoom
//import de.connect2x.trixnity.messenger.viewmodel.util.Initials
//import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
//import net.folivo.trixnity.client.MatrixClient
//import net.folivo.trixnity.client.key.KeyService
//import net.folivo.trixnity.client.key.UserTrustLevel
//import net.folivo.trixnity.client.media.MediaService
//import net.folivo.trixnity.client.room.RoomService
//import net.folivo.trixnity.client.store.Room
//import net.folivo.trixnity.client.store.RoomUser
//import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
//import net.folivo.trixnity.clientserverapi.client.ISyncApiClient
//import net.folivo.trixnity.core.model.RoomId
//import net.folivo.trixnity.core.model.UserId
//import net.folivo.trixnity.core.model.events.Event
//import net.folivo.trixnity.core.model.events.m.TypingEventContent
//import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
//import net.folivo.trixnity.core.subscribe
//import org.kodein.mock.Mock
//import org.kodein.mock.Mocker
//import org.kodein.mock.mockFunction0
//
//@OptIn(ExperimentalCoroutinesApi::class)
//class RoomHeaderViewModelTest : ShouldSpec() {
//    override fun timeout(): Long = 2_000
//
//    val mocker = Mocker()
//
//    private val roomId = RoomId("room1", "localhost")
//    private val me = UserId("bob", "localhost")
//    private val otherUser1 = UserId("cob", "locahost")
//    private val otherUser2 = UserId("dob", "locahost")
//    private val otherUser3 = UserId("eob", "locahost")
//    private val otherUser4 = UserId("fob", "locahost")
//
//    @Mock
//    lateinit var matrixClientMock: MatrixClient
//
//    @Mock
//    lateinit var roomServiceMock: RoomService
//
//    @Mock
//    lateinit var mediaServiceMock: MediaService
//
//    @Mock
//    lateinit var keyServiceMock: KeyService
//
//    @Mock
//    lateinit var matrixClientServerApiClientMock: MatrixClientServerApiClient
//
//    @Mock
//    lateinit var syncApiClientMock: ISyncApiClient
//
//    @Mock
//    lateinit var directRoom: IDirectRoom
//
//    init {
//        beforeTest {
//            mocker.reset()
//            injectMocks(mocker)
//
//            with(mocker) {
//                every { matrixClientMock.userId } returns me
//                every { matrixClientMock.room } returns roomServiceMock
//                every { matrixClientMock.media } returns mediaServiceMock
//                every { matrixClientMock.key } returns keyServiceMock
//                every { matrixClientMock.api } returns matrixClientServerApiClientMock
//                every { matrixClientServerApiClientMock.sync } returns syncApiClientMock
//
//            }
//        }
//    }
//
////@BeforeTest
////fun before() {
////    mockkObject(Initials)
////    mockkObject(RoomName)
////    coEvery { RoomName.getRoomNameElement(any(), roomId) } returns MutableStateFlow(RoomNameElement("My Room"))
////    every { Initials.compute(any()) } returns "MR"
////    coEvery { DirectRoom.getUser(roomId, any(), any()) } returns MutableStateFlow(null)
////
////    coEvery { roomServiceMock.getById(roomId) } returns MutableStateFlow(Room(roomId))
////    coEvery { roomServiceMock.getState<AvatarEventContent>(roomId, any(), any()) } returns mockk {
////        every { content } returns mockk(relaxed = true)
////    }
////    coEvery {
////        mediaServiceMock.getThumbnail(any(), avatarSize().toLong(), avatarSize().toLong(), any(), any())
////    } returns Result.success("image".encodeToByteArray())
////}
////
////@AfterTest
////fun after() {
////    unmockkAll()
////    clearAllMocks()
////}
////
////@Test
////fun `should show correct room name with initials and avatar`() = runTest(dispatchTimeoutMs = 2_000) {
////    coEvery { roomServiceMock.getById(roomId) } returns MutableStateFlow(
////        Room(
////            roomId,
////            avatarUrl = "mxc://localhost/123456"
////        )
////    )
////    coEvery { roomServiceMock.getState<AvatarEventContent>(roomId, any(), any()) } returns mockk {
////        every { content } returns mockk(relaxed = true)
////    }
////    coEvery {
////        mediaServiceMock.getThumbnail(
////            "mxc://localhost/123456",
////            avatarSize().toLong(),
////            avatarSize().toLong(),
////            any(),
////            any()
////        )
////    } returns Result.success("image".encodeToByteArray())
////
////    val cut = roomHeaderViewModel()
////
////    cut.roomHeaderElement.first { roomHeaderElement ->
////        roomHeaderElement.roomName == "My Room" &&
////                roomHeaderElement.roomImageInitials == "MR" &&
////                roomHeaderElement.roomImage.contentEquals("image".encodeToByteArray())
////    }
////}
////
////@Test
////fun `should not show typing info if no one in the room is typing`() {
////    val typingInfo = slot<suspend (Event<TypingEventContent>) -> Unit>()
////    every { syncApiClientMock.subscribe(TypingEventContent::class, capture(typingInfo)) } coAnswers {
////        typingInfo.captured.invoke(Event.EphemeralEvent(TypingEventContent(listOf()), roomId = roomId))
////    }
////
////    val cut = roomHeaderViewModel()
////
////    cut.usersTyping.value shouldBe null
////}
////
////@Test
////fun `should show no typing info if the only user in the room typing is us`() {
////    val typingInfo = slot<suspend (Event<TypingEventContent>) -> Unit>()
////    every { syncApiClientMock.subscribe(TypingEventContent::class, capture(typingInfo)) } coAnswers {
////        typingInfo.captured.invoke(Event.EphemeralEvent(TypingEventContent(listOf(ourUserId)), roomId = roomId))
////    }
////
////    val cut = roomHeaderViewModel()
////
////    cut.usersTyping.value shouldBe null
////}
////
////@Test
////fun `should show typing info for one user if one other user is typing`() {
////    val typingInfo = slot<suspend (Event<TypingEventContent>) -> Unit>()
////    every { syncApiClientMock.subscribe(capture(typingInfo)) } coAnswers {
////        typingInfo.captured.invoke(Event.EphemeralEvent(TypingEventContent(listOf(otherUser1)), roomId = roomId))
////    }
////
////    val cut = roomHeaderViewModel()
////
////    cut.usersTyping.value shouldBe "cob schreibt..."
////}
////
////@Test
////fun `should show typing info for 3 users if exactly 3 other users are typing`() {
////    val typingInfo = slot<suspend (Event<TypingEventContent>) -> Unit>()
////    every { syncApiClientMock.subscribe(capture(typingInfo)) } coAnswers {
////        typingInfo.captured.invoke(
////            Event.EphemeralEvent(
////                TypingEventContent(listOf(otherUser1, otherUser2, otherUser3)),
////                roomId = roomId
////            )
////        )
////    }
////
////    val cut = roomHeaderViewModel()
////
////    cut.usersTyping.value shouldBe "cob, dob und eob schreiben..."
////}
////
////@Test
////fun `should show abbreviated typing info if 4 or more other users are typing`() {
////    val typingInfo = slot<suspend (Event<TypingEventContent>) -> Unit>()
////    every { syncApiClientMock.subscribe(capture(typingInfo)) } coAnswers {
////        typingInfo.captured.invoke(
////            Event.EphemeralEvent(
////                TypingEventContent(listOf(otherUser1, otherUser2, otherUser3, otherUser4)),
////                roomId = roomId
////            )
////        )
////    }
////
////    val cut = roomHeaderViewModel()
////
////    cut.usersTyping.value shouldBe "cob, dob und andere schreiben..."
////}
////
////@Test
////fun `trust level for non-direct rooms should be 'null'`() {
////    val cut = roomHeaderViewModel()
////
////    cut.userTrustLevel.value shouldBe null
////}
////
////@Test
////fun `should react to changes in a user's trust level`() = runTest(dispatchTimeoutMs = 2_000) {
////    val otherUser = UserId("otherUser", "localhost")
////    coEvery { DirectRoom.getUser(roomId, any(), any()) } returns MutableStateFlow(otherUser)
////    val trustLevelFlow = MutableStateFlow(UserTrustLevel.CrossSigned(verified = false))
////    coEvery { keyServiceMock.getTrustLevel(otherUser, any()) } returns trustLevelFlow
////
////    val cut = roomHeaderViewModel()
////
////    cut.userTrustLevel.first { userTrustLevel ->
////        userTrustLevel is UserTrustLevel.CrossSigned && userTrustLevel.verified.not()
////    }
////    trustLevelFlow.value = UserTrustLevel.CrossSigned(verified = true)
////    cut.userTrustLevel.first { userTrustLevel ->
////        userTrustLevel is UserTrustLevel.CrossSigned && userTrustLevel.verified
////    }
////}
////
////@Test
////fun `should allow to verify other user if not yet verified`() = runTest(dispatchTimeoutMs = 2_000) {
////    val otherUser = UserId("otherUser", "localhost")
////    coEvery { DirectRoom.getUser(roomId, any(), any()) } returns MutableStateFlow(otherUser)
////    coEvery { keyServiceMock.getTrustLevel(otherUser, any()) } returns MutableStateFlow(
////        UserTrustLevel.CrossSigned(verified = false)
////    )
////
////    val cut = roomHeaderViewModel()
////
////    cut.canVerifyUser.first { it }
////}
////
////@Test
////fun `should not allow to verify other user if already verified`() = runTest(dispatchTimeoutMs = 2_000) {
////    val otherUser = UserId("otherUser", "localhost")
////    coEvery { DirectRoom.getUser(roomId, any(), any()) } returns MutableStateFlow(otherUser)
////    coEvery { keyServiceMock.getTrustLevel(otherUser, any()) } returns MutableStateFlow(
////        UserTrustLevel.CrossSigned(verified = true)
////    )
////
////    val cut = roomHeaderViewModel()
////
////    cut.canVerifyUser.first { it.not() }
////}
////
////@Test
////fun `should not allow to verify in a non-direct room`() = runTest(dispatchTimeoutMs = 2_000) {
////    val cut = roomHeaderViewModel()
////
////    cut.canVerifyUser.first { it.not() }
////}
//
//    private fun roomHeaderViewModel() = RoomHeaderViewModel(
//        componentContext = DefaultComponentContext(LifecycleRegistry()),
//        matrixClient = matrixClientMock,
//        selectedRoomId = roomId,
//        isBackButtonVisible = MutableStateFlow(false),
//        onBack = mockFunction0(mocker),
//        onVerifyUser = mockFunction0(mocker),
//        onShowRoomSettings = mockFunction0(mocker),
//        directRoom = directRoom,
//        coroutineContext = Dispatchers.Unconfined
//    )
//}
//
////    every
////    { user } returns mockk(relaxUnitFun = true)
////    {
////        coEvery { getById(otherUser1, roomId, any()) } returns MutableStateFlow(
////            RoomUser(roomId, otherUser1, "cob", mockk())
////        )
////        coEvery { getById(otherUser2, roomId, any()) } returns MutableStateFlow(
////            RoomUser(roomId, otherUser2, "dob", mockk())
////        )
////        coEvery { getById(otherUser3, roomId, any()) } returns MutableStateFlow(
////            RoomUser(roomId, otherUser3, "eob", mockk())
////        )
////        coEvery { getById(otherUser4, roomId, any()) } returns MutableStateFlow(
////            RoomUser(roomId, otherUser4, "fob", mockk())
////        )
////        coEvery { userPresence } returns MutableStateFlow(mapOf())
////    }
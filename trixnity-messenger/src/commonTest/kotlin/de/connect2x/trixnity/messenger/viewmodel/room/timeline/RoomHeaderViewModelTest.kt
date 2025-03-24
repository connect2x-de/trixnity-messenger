package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.DirectRoom
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.RoomTopic
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import de.connect2x.trixnity.messenger.viewmodel.util.UserPresence
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.KeyService
import net.folivo.trixnity.client.key.UserTrustLevel
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.PresenceEventContent
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent
import net.folivo.trixnity.utils.toByteArrayFlow
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext


@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class RoomHeaderViewModelTest : ShouldSpec() {
    private val roomId = RoomId("room1", "localhost")
    private val me = UserId("bob", "localhost")
    private val otherUser = UserId("cob", "localhost")

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()
    private val mediaServiceMock = mock<MediaService>()
    private val keyServiceMock = mock<KeyService>()
    private val roomNameMock = mock<RoomName>()
    private val roomTopicMock = mock<RoomTopic>()
    private val initialsMock = mock<Initials>()
    private val userPresenceMock = mock<UserPresence>()
    private val directRoomMock = mock<DirectRoom>()
    private val userBlockingMock = mock<UserBlocking>()

    private lateinit var roomNameElement: BlockingAnsweringScope<Flow<String>>
    private lateinit var roomTopicElement: BlockingAnsweringScope<Flow<String>>
    private lateinit var ignoredUsers: BlockingAnsweringScope<Flow<IgnoredUserListEventContent?>>

    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(
                matrixClientMock,
                roomServiceMock,
                userServiceMock,
                mediaServiceMock,
                keyServiceMock,
                roomNameMock,
                roomTopicMock,
                initialsMock,
                userPresenceMock,
                directRoomMock,
                userBlockingMock,
            )
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                        single { userServiceMock }
                        single { mediaServiceMock }
                        single { keyServiceMock }
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns me

            roomNameElement = every {
                roomNameMock.getRoomName(any<RoomId>(), any(), any<Boolean>())
            }
            roomNameElement returns MutableStateFlow("My Room")
            roomTopicElement = every {
                roomTopicMock.getRoomTopic(any<RoomId>(), any(), any<Boolean>())
            }
            roomTopicElement returns MutableStateFlow("My Topic")
            every { roomServiceMock.usersTyping } returns MutableStateFlow(emptyMap())

            ignoredUsers = every { userServiceMock.getAccountData(IgnoredUserListEventContent::class) }
            ignoredUsers returns flowOf(
                IgnoredUserListEventContent(emptyMap())
            )

            every { initialsMock.compute(any()) } returns "MR"
            every { roomServiceMock.getById(roomId) } returns MutableStateFlow(
                Room(roomId, avatarUrl = "mxc://localhost/123456")
            )
            every {
                roomServiceMock.getState(
                    any(),
                    eq(JoinRulesEventContent::class),
                    any()
                )
            } returns MutableStateFlow(
                ClientEvent.RoomEvent.StateEvent(
                    content = JoinRulesEventContent(
                        joinRule = JoinRulesEventContent.JoinRule.Public
                    ),
                    EventId("1"),
                    me,
                    roomId,
                    0L,
                    stateKey = "",
                )
            )
            everySuspend {
                mediaServiceMock.getThumbnail(
                    eq("mxc://localhost/123456"),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns Result.success(InMemoryPlatformMedia("image".encodeToByteArray().toByteArrayFlow()))
            every { userPresenceMock.presentEventContentFlow(any(), eq(roomId)) } returns flowOf(
                PresenceEventContent(presence = Presence.ONLINE)
            )
            every { userBlockingMock.isUserBlocked(any(), any()) } returns MutableStateFlow(false)
        }

        should("should show correct room name with initials and avatar and react to changes") {
            val roomName = MutableStateFlow("My Room")
            roomNameElement returns roomName
            every { directRoomMock.getUsers(any(), eq(roomId)) } returns flowOf(emptyList())

            val cut = roomHeaderViewModel(coroutineContext)
            testCoroutineScheduler.advanceUntilIdle()

            cut.roomHeaderInfo.value shouldBe RoomHeaderInfo(
                "My Room",
                "My Topic",
                "MR",
                "image".encodeToByteArray(),
                Presence.ONLINE,
                isEncrypted = false,
                isPublic = true,
                hasLeft = false
            )

            roomName.value = "New Room Name"
            testCoroutineScheduler.advanceUntilIdle()
            cut.roomHeaderInfo.value shouldBe RoomHeaderInfo(
                "New Room Name",
                "My Topic",
                "MR",
                "image".encodeToByteArray(),
                Presence.ONLINE,
                isEncrypted = false,
                isPublic = true,
                hasLeft = false
            )
            cancelNeverEndingCoroutines()
        }

        should("compute trust level of `null` for non-direct rooms") {
            every { directRoomMock.getUsers(any(), eq(roomId)) } returns flowOf(emptyList())

            val cut = roomHeaderViewModel(coroutineContext)
            testCoroutineScheduler.advanceUntilIdle()

            cut.userTrustLevel.value shouldBe null
            cancelNeverEndingCoroutines()
        }

        should("react to changes in the user's trust level") {
            val trustLevel = MutableStateFlow<UserTrustLevel>(UserTrustLevel.CrossSigned(verified = true))
            val directRoom = MutableStateFlow(listOf(otherUser))
            every { directRoomMock.getUsers(any(), eq(roomId)) } returns directRoom
            every { keyServiceMock.getTrustLevel(eq(otherUser)) } returns trustLevel

            val cut = roomHeaderViewModel(coroutineContext)
            testCoroutineScheduler.advanceUntilIdle()
            cut.userTrustLevel.value shouldBe UserTrustLevel.CrossSigned(verified = true)

            trustLevel.value = UserTrustLevel.Blocked
            testCoroutineScheduler.advanceUntilIdle()
            cut.userTrustLevel.value shouldBe UserTrustLevel.Blocked

            directRoom.value = emptyList()
            testCoroutineScheduler.advanceUntilIdle()
            cut.userTrustLevel.value shouldBe null

            cancelNeverEndingCoroutines()
        }

        should("allow to verify other user if not yet verified and vice versa") {
            val trustLevel = MutableStateFlow(UserTrustLevel.CrossSigned(verified = false))
            every { directRoomMock.getUsers(any(), eq(roomId)) } returns flowOf(listOf(otherUser))
            every { keyServiceMock.getTrustLevel(eq(otherUser)) } returns trustLevel

            val cut = roomHeaderViewModel(coroutineContext)
            testCoroutineScheduler.advanceUntilIdle()
            cut.canVerifyUser.value shouldBe true

            trustLevel.value = UserTrustLevel.CrossSigned(verified = true)
            testCoroutineScheduler.advanceUntilIdle()
            cut.canVerifyUser.value shouldBe false

            cancelNeverEndingCoroutines()
        }

        should("not allow user verification in non-direct room") {
            every { directRoomMock.getUsers(any(), eq(roomId)) } returns flowOf(emptyList())
            every { keyServiceMock.getTrustLevel(eq(otherUser)) } returns flowOf(
                UserTrustLevel.CrossSigned(verified = false)
            )

            val cut = roomHeaderViewModel(coroutineContext)
            testCoroutineScheduler.advanceUntilIdle()

            cut.canVerifyUser.value shouldBe false

            cancelNeverEndingCoroutines()
        }

        should("allow to block user in a direct room with only 2 users and user is not yet blocked and unblock if already blocked") {
            val ignoredUsersEventContent = MutableStateFlow(IgnoredUserListEventContent(mapOf()))
            ignoredUsers returns ignoredUsersEventContent
            every { directRoomMock.getUsers(any(), eq(roomId)) } returns flowOf(listOf(otherUser))
            every { keyServiceMock.getTrustLevel(eq(otherUser)) } returns flowOf(
                UserTrustLevel.CrossSigned(verified = false)
            )

            val cut = roomHeaderViewModel(coroutineContext)
            testCoroutineScheduler.advanceUntilIdle()
            cut.canBlockUser.value shouldBe true
            cut.canUnblockUser.value shouldBe false

            ignoredUsersEventContent.value = IgnoredUserListEventContent(
                mapOf(
                    otherUser to JsonObject(emptyMap())
                )
            )
            testCoroutineScheduler.advanceUntilIdle()
            cut.canBlockUser.value shouldBe false
            cut.canUnblockUser.value shouldBe true

            cancelNeverEndingCoroutines()
        }

        should("not allow to block user in non-direct rooms or direct rooms with more than 2 participants") {
            val directRoom = MutableStateFlow(listOf(otherUser, UserId("another_dude", "localhost")))
            every { directRoomMock.getUsers(any(), eq(roomId)) } returns directRoom
            every { keyServiceMock.getTrustLevel(eq(otherUser)) } returns flowOf(
                UserTrustLevel.CrossSigned(verified = false)
            )

            val cut = roomHeaderViewModel(coroutineContext)
            testCoroutineScheduler.advanceUntilIdle()
            cut.canBlockUser.value shouldBe false
            cut.canUnblockUser.value shouldBe false

            directRoom.value = listOf(otherUser)
            testCoroutineScheduler.advanceUntilIdle()
            cut.canBlockUser.value shouldBe true
            cut.canUnblockUser.value shouldBe false

            directRoom.value = emptyList()
            testCoroutineScheduler.advanceUntilIdle()
            cut.canBlockUser.value shouldBe false
            cut.canUnblockUser.value shouldBe false

            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun roomHeaderViewModel(coroutineContext: CoroutineContext): RoomHeaderViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return RoomHeaderViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(me to matrixClientMock)) +
                                module {
                                    single { roomNameMock }
                                    single { roomTopicMock }
                                    single { userPresenceMock }
                                    single { initialsMock }
                                    single { directRoomMock }
                                    single { userBlockingMock }
                                })
                }.koin,
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                me,
                coroutineContext = coroutineContext,
            ),
            selectedRoomId = roomId,
            onBack = mock(),
            onVerifyUser = mock(),
            onOpenRoomSettings = mock(),
            onOpenUserProfile = mock(),
        ).also {
            subscribe(it)
        }
    }

    private fun subscribe(roomHeaderViewModel: RoomHeaderViewModel) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch(start = CoroutineStart.UNDISPATCHED) { roomHeaderViewModel.roomHeaderInfo.collect() }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { roomHeaderViewModel.usersTyping.collect() }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { roomHeaderViewModel.canVerifyUser.collect() }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { roomHeaderViewModel.error.collect() }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { roomHeaderViewModel.userTrustLevel.collect() }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { roomHeaderViewModel.isUserBlocked.collect() }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { roomHeaderViewModel.canBlockUser.collect() }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { roomHeaderViewModel.canUnblockUser.collect() }
    }
}

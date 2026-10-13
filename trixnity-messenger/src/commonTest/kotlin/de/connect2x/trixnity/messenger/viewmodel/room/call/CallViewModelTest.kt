package de.connect2x.trixnity.messenger.viewmodel.room.call

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.rtc.RtcService
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.store.ServerData
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.model.media.GetMediaConfig
import de.connect2x.trixnity.clientserverapi.model.server.GetVersions
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.rtc.CallRtcApplication
import de.connect2x.trixnity.core.model.events.m.rtc.RtcMemberEventContent
import de.connect2x.trixnity.core.model.events.m.rtc.RtcMemberId
import de.connect2x.trixnity.core.model.events.m.rtc.RtcSlotEventContent
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(MSC4143::class, TrixnityMessengerCallApi::class, ExperimentalCoroutinesApi::class)
class CallViewModelTest {
    private val roomId = RoomId("!room:localhost")
    private val ownUserId = UserId("me", "localhost")
    private val aliceId = UserId("alice", "localhost")
    private val bobId = UserId("bob", "localhost")

    private val matrixClientMock = mock<MatrixClient>()
    private val rtcServiceMock = mock<RtcService>()
    private val userServiceMock = mock<UserService>()

    private val members =
        MutableStateFlow<Map<Pair<UserId, RtcMemberId>, MessageEvent<RtcMemberEventContent>>>(emptyMap())
    private val slot = MutableStateFlow<StateEvent<RtcSlotEventContent>?>(null)
    private val serverData = MutableStateFlow<ServerData?>(null)
    private val canSendSlotEvent = MutableStateFlow(false)
    private val canSendMemberEvent = MutableStateFlow(false)

    init {
        resetMocks(matrixClientMock, rtcServiceMock, userServiceMock)
        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { rtcServiceMock }
                            single { userServiceMock }
                        }
                    )
                }
                .koin
        every { matrixClientMock.userId } returns ownUserId
        every { matrixClientMock.serverData } returns serverData
        every { rtcServiceMock.getMembers(roomId, CallRtcApplication.SLOT_ID) } returns members
        every { rtcServiceMock.getSlot(roomId, CallRtcApplication.SLOT_ID) } returns slot
        every { userServiceMock.canSendEvent(roomId, RtcSlotEventContent::class) } returns canSendSlotEvent
        every { userServiceMock.canSendEvent(roomId, RtcMemberEventContent::class) } returns canSendMemberEvent
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `state flows have their documented initial values`() = runTest {
        val viewModel = callViewModel()

        viewModel.state.value shouldBe CallViewModel.State.Inactive
        viewModel.canJoinCall.value shouldBe false
    }

    @Test
    fun `state follows whether participants are present`() = runTest {
        every { userServiceMock.getById(roomId, aliceId) } returns MutableStateFlow(roomUser(aliceId, "Alice"))
        val viewModel = callViewModel()
        backgroundScope.launch { viewModel.state.collect() }
        runCurrent()

        viewModel.state.value shouldBe CallViewModel.State.Inactive

        val aliceMemberId = RtcMemberId("alice-device")
        members.value = mapOf((aliceId to aliceMemberId) to rtcMemberEvent(aliceId, aliceMemberId))
        runCurrent()

        viewModel.state.value shouldBe CallViewModel.State.Active

        members.value = emptyMap()
        runCurrent()

        viewModel.state.value shouldBe CallViewModel.State.Inactive
    }

    @Test
    fun `can join call requires rtc transports`() = runTest {
        canSendSlotEvent.value = true
        canSendMemberEvent.value = true
        val viewModel = callViewModel()
        backgroundScope.launch { viewModel.canJoinCall.collect() }
        runCurrent()

        viewModel.canJoinCall.value shouldBe false

        serverData.value = serverDataWithRtcTransports()
        runCurrent()

        viewModel.canJoinCall.value shouldBe true

        serverData.value = null
        runCurrent()

        viewModel.canJoinCall.value shouldBe false
    }

    @Test
    fun `can join call without a slot requires both event permissions`() = runTest {
        serverData.value = serverDataWithRtcTransports()
        canSendMemberEvent.value = true
        val viewModel = callViewModel()
        backgroundScope.launch { viewModel.canJoinCall.collect() }
        runCurrent()

        viewModel.canJoinCall.value shouldBe false

        canSendSlotEvent.value = true
        runCurrent()

        viewModel.canJoinCall.value shouldBe true

        canSendMemberEvent.value = false
        runCurrent()

        viewModel.canJoinCall.value shouldBe false
    }

    @Test
    fun `can join call with a slot requires the member event permission`() = runTest {
        serverData.value = serverDataWithRtcTransports()
        slot.value = rtcSlotEvent()
        canSendMemberEvent.value = true
        val viewModel = callViewModel()
        backgroundScope.launch { viewModel.canJoinCall.collect() }
        runCurrent()

        viewModel.canJoinCall.value shouldBe true

        canSendMemberEvent.value = false
        runCurrent()

        viewModel.canJoinCall.value shouldBe false

        canSendSlotEvent.value = true
        runCurrent()

        viewModel.canJoinCall.value shouldBe false
    }

    @Test
    fun `preview state flows expose their fixed values`() {
        val viewModel = PreviewCallViewModel()

        viewModel.state.value shouldBe CallViewModel.State.Inactive
        viewModel.canJoinCall.value shouldBe true
    }

    private fun roomUser(userId: UserId, name: String) =
        RoomUser(
            roomId = roomId,
            userId = userId,
            name = name,
            event =
                StateEvent(
                    content = MemberEventContent(membership = Membership.JOIN),
                    id = EventId("\$member-${userId.localpart}"),
                    sender = userId,
                    roomId = roomId,
                    originTimestamp = 0,
                    stateKey = userId.full,
                ),
        )

    private fun rtcMemberEvent(userId: UserId, memberId: RtcMemberId): MessageEvent<RtcMemberEventContent> =
        MessageEvent(
            content =
                RtcMemberEventContent.Leave(
                    slotId = CallRtcApplication.SLOT_ID,
                    member = RtcMemberEventContent.Member(memberId),
                    stickyKey = memberId.value,
                ),
            id = EventId("\$rtc-member-${userId.localpart}"),
            sender = userId,
            roomId = roomId,
            originTimestamp = 0,
        )

    private fun rtcSlotEvent(): StateEvent<RtcSlotEventContent> =
        StateEvent<RtcSlotEventContent>(
            content = RtcSlotEventContent.Closed(),
            id = EventId("\$rtc-slot"),
            sender = ownUserId,
            roomId = roomId,
            originTimestamp = 0,
            stateKey = CallRtcApplication.SLOT_ID.value,
        )

    private fun serverDataWithRtcTransports() =
        ServerData(
            versions = GetVersions.Response(),
            mediaConfig = GetMediaConfig.Response(),
            capabilities = null,
            rtcTransports = emptyList(),
        )

    private fun TestScope.callViewModel() =
        CallViewModelImpl(
            viewModelContext =
                testMatrixClientViewModelContext(
                    di =
                        koinApplication {
                                modules(createTestDefaultTrixnityMessengerModules(mapOf(ownUserId to matrixClientMock)))
                            }
                            .koin,
                    userId = ownUserId,
                ),
            roomId = roomId,
        )
}

package de.connect2x.trixnity.messenger.viewmodel.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomInviterTest : ShouldSpec() {
    override fun timeout(): Long = 3_000

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    private val roomId = RoomId("room", "localhost")
    private val me = UserId("test", "localhost")
    private val inviter = UserId("inviter", "localhost")
    private val someOtherInviter = UserId("someOtherInviter", "localhost")

    init {
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                        }
                    )
                }.koin
                every { matrixClientMock.userId } returns me
                every { matrixClientMock.room } returns roomServiceMock
            }

        }

        should("get the inviter from the matching invitation state event") {
            mocker.every { roomServiceMock.getState<MemberEventContent>(roomId, me.full) } returns flowOf(
                StateEvent(
                    content = MemberEventContent(
                        membership = Membership.INVITE,
                    ),
                    id = EventId("1"),
                    sender = inviter,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = me.full,
                )
            )
            val cut = roomInviter()
            cut.getInviter(matrixClientMock, roomId) shouldBe inviter
        }

        should("not run indefinitely") {
            mocker.every { roomServiceMock.getState<MemberEventContent>(roomId, me.full) } returns MutableStateFlow(
                StateEvent(
                    content = MemberEventContent(
                        membership = Membership.LEAVE, // no invite!
                    ),
                    id = EventId("1"),
                    sender = inviter,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = me.full,
                )
            )
            val cut = roomInviter()
            cut.getInviter(matrixClientMock, roomId) shouldBe null
        }
    }

    private fun roomInviter() = RoomInviterImpl()
}
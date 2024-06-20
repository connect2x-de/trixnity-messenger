package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.resetMocks
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomInviterTest : ShouldSpec() {
    override fun timeout(): Long = 3_000

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    private val roomId = RoomId("room", "localhost")
    private val me = UserId("test", "localhost")
    private val inviter = UserId("inviter", "localhost")
    private val someOtherInviter = UserId("someOtherInviter", "localhost")

    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(matrixClientMock, roomServiceMock)
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns me
        }

        should("get the inviter from the matching invitation state event") {
            every { roomServiceMock.getState(roomId, MemberEventContent::class, me.full) } returns flowOf(
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
            every { roomServiceMock.getState(roomId, MemberEventContent::class, me.full) } returns MutableStateFlow(
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

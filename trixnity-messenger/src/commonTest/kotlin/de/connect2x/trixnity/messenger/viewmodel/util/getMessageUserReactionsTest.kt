package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomUserBuilder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineMock
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.roomUsers
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module


private val log = KotlinLogging.logger {}

class getMessageUserReactionsTest : ShouldSpec() {

    private val us = UserId("martin", "localhost")

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>() // TODO remove?

    init {
        coroutineTestScope = true

        beforeEach {
            resetMocks(
                matrixClientMock,
                roomServiceMock,
                userServiceMock,
            )
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                        single { userServiceMock }
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns us
            every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns
                    MutableStateFlow(emptyMap())
        }

        // Because Mokkery is a bitch and doesn't reinitialize mocks correctly
        // we need to differentiate the parameters by each test case.
        var runId = 0

        data class TestEnv(
            // Increment runId during the first val field assignment!
            val roomId: RoomId = RoomId("room_${runId++}", "localhost"),
            val timeline: TimelineMock = timeline(roomServiceMock, roomId) {},
            val roomUsers: RoomUserBuilder = roomUsers(userServiceMock, roomId) {}, // TODO remove?
            val eventIds: List<EventId> = (0..2).map { EventId("event_$it") },
            val us: UserId = this@getMessageUserReactionsTest.us, // Stays constant.
            val alice: UserId = UserId("alice_$runId", "localhost"),
            val bob: UserId = UserId("bob_$runId", "localhost"), // TODO remove?
            val testScope: TestScope, // Place this last so it so param deconstruct is cleaner.
        )

        should("test") {
            val env = TestEnv(testScope = this)
            val (roomId, timeline, roomUsers, event) = env
            val cut = cutMessageReactions(event[0], roomId)

            cut.first().let { log.debug { "=== REACTIONS: $it" } }

            true shouldBe false
            cancelNeverEndingCoroutines()
        }

//        should("") { TODO() }
//        should("") { TODO() }
//        should("") { TODO() }
//        should("") { TODO() }
//        should("") { TODO() }
//        should("") { TODO() }
//        should("") { TODO() }
    }

    private fun TestScope.cutMessageReactions(
        eventId: EventId,
        roomId: RoomId,
    ): Flow<MessageUserReactions> = koinApplication {
        modules(
            createTestDefaultTrixnityMessengerModules(
                mapOf(us to matrixClientMock)
            )
        )
    }.koin.let { di ->
        getMessageUserReactions(
            client = testMatrixClientViewModelContext(
                di = di,
                userId = us,
            ).matrixClient,
//            senderUserId = senderId,
            roomId = roomId,
            eventId = eventId,
        )
    }
}



package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.shouldGroup
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.roomUsers
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.withCleanup
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.advanceUntilIdle
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module


class MessageMetadataViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    private val us = UserId("us", "localhost")

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

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
            every { userServiceMock.canSendEvent(any(), any()) } returns flowOf(true)
            every { roomServiceMock.getOutbox(any()) } returns flowOf(listOf())
        }

        var runId = 0 // Mokkery fails to properly update existing mocks.

        data class TestEnv(
            val roomId: RoomId = RoomId("room_${runId++}", "localhost"),
            val us: UserId = this@MessageMetadataViewModelTest.us,
            val ourEventId: EventId = EventId("our_event_$runId"),
            val them: UserId = UserId("them_$runId", "localhost"),
            val theirEventId: EventId = EventId("their_event_$runId"),
            val reader1: UserId = UserId("reader1_$runId", "localhost"),
            val reader2: UserId = UserId("reader2_$runId", "localhost"),
            val reader3: UserId = UserId("reader3_$runId", "localhost"),
        )

        fun TestScope.cutMessageMetadataViewModel(
            roomId: RoomId,
            eventId: EventId,
        ): MessageMetadataViewModel =
            koinApplication {
                modules(
                    createTestDefaultTrixnityMessengerModules(
                        mapOf(us to matrixClientMock)
                    )
                )
            }.koin.let { di ->
                MessageMetadataViewModelImpl(
                    viewModelContext = testMatrixClientViewModelContext(
                        di = di,
                        userId = us,
                    ),
                    eventId = eventId,
                    roomId = roomId,
                    onOpenUserProfile = {},
                    onBack = {},
                )
            }

        should("return the message content and sender info").withCleanup {
            val env = TestEnv()
            timeline(roomServiceMock, env.roomId) {
                +messageEvent(sender = env.them, eventId = env.theirEventId) { text("Hi!") }
            }
            roomUsers(userServiceMock, env.roomId) {
                +roomUser("Alice", env.them, env.theirEventId)
                +roomUser("Martin", env.us, env.theirEventId)
                +roomUser("Reader1", env.reader1, env.theirEventId)
                +roomUser("Reader2", env.reader2, env.theirEventId)
                +roomUser("Reader3", env.reader3, env.theirEventId)
            }
            val cut = cutMessageMetadataViewModel(env.roomId, env.theirEventId)
            launch { cut.messagePreview.collect() }
            launch { cut.senderInfo.collect() }
            advanceUntilIdle()
            cut.messagePreview.value!!.let {
                it.eventId shouldBe env.theirEventId
                it.isByMe shouldBe false
                // TODO: test formatted time
            }
            cut.senderInfo.value!!.let {
                it.userId shouldBe env.them
                it.name shouldBe "Alice"
            }
        }

        shouldGroup("reader info") {

            should("see if and by whom a message has been read").withCleanup {
                val env = TestEnv()
                timeline(roomServiceMock, env.roomId) {
                    +messageEvent(sender = us, eventId = env.ourEventId) { text("Hi!") }
                }
                roomUsers(userServiceMock, env.roomId) {
                    +roomUser("Martin", env.us, env.ourEventId)
                    +roomUser("Alice", env.them, null)
                    +roomUser("Reader1", env.reader1, env.ourEventId)
                    +roomUser("Reader2", env.reader2, env.ourEventId)
                    +roomUser("Reader3", env.reader3, env.ourEventId)
                }
                val cut = cutMessageMetadataViewModel(env.roomId, env.ourEventId)
                launch { cut.userInteractions.collect() }
                advanceUntilIdle()
                val interactions = cut.userInteractions.value
                interactions shouldHaveSize 3
                interactions.forEach {
                    when (it.userId) {
                        env.reader1,
                        env.reader2,
                        env.reader3,
                            -> it.hasRead shouldBe true

                        else -> it.hasRead shouldBe false
                    }
                }
            }

            should("see if a message hasn't been read").withCleanup {
                val env = TestEnv()
                timeline(roomServiceMock, env.roomId) {
                    +messageEvent(sender = env.them, eventId = env.theirEventId) { text("Hi!") }
                    +messageEvent(sender = env.us, eventId = env.ourEventId) { text("Hi!") }
                }
                roomUsers(userServiceMock, env.roomId) {
                    +roomUser("Martin", env.us, env.ourEventId)
                    +roomUser("Alice", env.them, null)
                    +roomUser("Reader1", env.reader1, env.theirEventId)
                    +roomUser("Reader2", env.reader2, null)
                    +roomUser("Reader3", env.reader3, null)
                }
                val cut = cutMessageMetadataViewModel(env.roomId, env.ourEventId)
                launch { cut.userInteractions.collect() }
                advanceUntilIdle()
                val interactions = cut.userInteractions.value
                interactions shouldHaveSize 0
                interactions.forEach {
                    withClue("user ${it.userId} shouldn't have read the message") {
                        it.hasRead shouldBe false
                    }
                }
            }
        }

        shouldGroup("message reactions") {

            should("see if and by whom a message has received reactions from").withCleanup {
                val env = TestEnv()
                timeline(roomServiceMock, env.roomId) {
                    +messageEvent(sender = env.them, eventId = env.theirEventId) { text("Hi!") }
                    +messageEvent(sender = env.reader1) { reaction(env.theirEventId, "😄") }
                    +messageEvent(sender = env.reader1) { reaction(env.theirEventId, "🙂") }
                    +messageEvent(sender = env.reader1) { reaction(env.theirEventId, "🥳") }
                    +messageEvent(sender = env.reader2) { reaction(env.theirEventId, "😄") }
                    +messageEvent(sender = env.reader2) { reaction(env.theirEventId, "🙂") }
                    +messageEvent(sender = env.us) { reaction(env.theirEventId, "🙂") }
                }
                roomUsers(userServiceMock, env.roomId) {
                    +roomUser("Alice", env.them, env.theirEventId)
                    +roomUser("Martin", env.us, env.theirEventId)
                    +roomUser("Reader1", env.reader1, env.theirEventId)
                    +roomUser("Reader2", env.reader2, env.theirEventId)
                    +roomUser("Reader3", env.reader3, env.theirEventId)
                }
                val cut = cutMessageMetadataViewModel(env.roomId, env.theirEventId)
                launch { cut.userInteractions.collect() }
                launch { cut.reactionCounts.collect() }
                advanceUntilIdle()
                cut.userInteractions.value.let { interactions ->
                    interactions shouldHaveSize 4
                    interactions.forEach {
                        when (it.userId) {
                            env.reader1 -> it.reactions shouldBe setOf("😄", "🙂", "🥳")
                            env.reader2 -> it.reactions shouldBe setOf("😄", "🙂")
                            env.us -> it.reactions shouldBe setOf("🙂")
                            else -> it.reactions shouldBe setOf()
                        }
                    }
                }
                cut.reactionCounts.value.let { reactions ->
                    reactions shouldHaveSize 3
                    reactions.forEach {
                        when (it.key) {
                            "🙂" -> it.value shouldBe 3u
                            "😄" -> it.value shouldBe 2u
                            "🥳" -> it.value shouldBe 1u
                            else -> it.value shouldBe 0u
                        }
                    }
                }
            }

            should("see if a message hasn't received any reactions").withCleanup {
                val env = TestEnv()
                timeline(roomServiceMock, env.roomId) {
                    +messageEvent(sender = env.us, eventId = env.ourEventId) { text("Hi!") }
                }
                roomUsers(userServiceMock, env.roomId) {
                    +roomUser("Martin", env.us, env.ourEventId)
                    +roomUser("Alice", env.them, env.ourEventId)
                    +roomUser("Reader1", env.reader1, env.ourEventId)
                    +roomUser("Reader2", env.reader2, env.ourEventId)
                    +roomUser("Reader3", env.reader3, env.ourEventId)
                }
                val cut = cutMessageMetadataViewModel(env.roomId, env.ourEventId)
                launch { cut.userInteractions.collect() }
                launch { cut.reactionCounts.collect() }
                advanceUntilIdle()
                cut.userInteractions.value.let { interactions ->
                    interactions shouldHaveSize 4
                    interactions.forEach {
                        it.reactions shouldBe setOf()
                    }
                }
                cut.reactionCounts.value.let { reactions ->
                    reactions shouldHaveSize 0
                }
            }
        }
    }
}

package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomUserBuilder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineBuilder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineMock
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.roomUsers
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.assertions.failure
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.engine.runBlocking
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module


class getMessageUserReactionsTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    private val us = UserId("martin", "localhost")

    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    init {
        // Ideally is should run with
        // coroutineTestScope = true
        // but currently that makes
        // it fail in the test suit run.

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
        }

        // Because Mokkery is a bitch and doesn't reinitialize mocks correctly
        // we need to differentiate the parameters by each test case.
        var runId = 0

        data class TestEnv(
            // Increment runId during the first val field assignment!
            val roomId: RoomId = RoomId("room_${runId++}", "localhost"),
            val timeline: TimelineMock = timeline(roomServiceMock, roomId) {},
            val roomUsers: RoomUserBuilder = roomUsers(userServiceMock, roomId) {},
            val eventIds: List<EventId> = (0..2).map { EventId("event_${runId}_$it") },
            val us: UserId = this@getMessageUserReactionsTest.us, // Stays constant.
            val alice: UserId = UserId("alice_$runId", "localhost"),
            val bob: UserId = UserId("bob_$runId", "localhost"),
            val testScope: TestScope, // Place this last so it so param deconstruct is cleaner.
        )

        fun TestEnv.cutMessageReactions(eventId: EventId) =
            koinApplication {
                modules(
                    createTestDefaultTrixnityMessengerModules(
                        mapOf(us to matrixClientMock)
                    )
                )
            }.koin.let { di ->
                getMessageUserReactions(
                    client = testScope.testMatrixClientViewModelContext(
                        di = di,
                        userId = us,
                    ).matrixClient,
                    roomId = roomId,
                    eventId = eventId,
                )
            }

        context("reactions by user") {

            should("get nothing on other message without reactions") {
                val env = TestEnv(testScope = this)
                val (_, timeline, _, event) = env
                timeline.addEvents {
                    +textMessageBy(env.alice, event[0])
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByUsers mapOf()
                cancelNeverEndingCoroutines()
            }

            should("get nothing on our message without reactions") {
                val env = TestEnv(testScope = this)
                val (_, timeline, _, event) = env
                timeline.addEvents {
                    +textMessageBy(env.us, event[0])
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByUsers mapOf(
                    env.bob to setOf(),
                )
                cancelNeverEndingCoroutines()
            }

            should("get reaction from other user on other message") {
                val env = TestEnv(testScope = this)
                val (_, timeline, roomUsers, event) = env
                timeline.addEvents {
                    +textMessageBy(env.alice, event[0])
                    +reactionBy(env.bob, event[0], "🥳")
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", env.bob)
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByUsers mapOf(
                    env.bob to setOf("🥳"),
                )
                cancelNeverEndingCoroutines()
            }

            should("continuously get reactions from other user on own message") {
                val env = TestEnv(testScope = this)
                val (_, timeline, roomUsers, event) = env
                timeline.addEvents {
                    +textMessageBy(env.us, event[0])
                    +reactionBy(env.alice, event[0], "😄")
                    +reactionBy(env.alice, event[0], "🥳")
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Alice", env.alice)
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByUsers mapOf(
                    env.alice to setOf("🥳", "😄"),
                )
                timeline.addEvents {
                    +reactionBy(env.alice, event[0], "🙂")
                }
                cut shouldReturnReactionsByUsers mapOf(
                    env.alice to setOf("🥳", "🙂", "😄"),
                )
                cancelNeverEndingCoroutines()
            }

            should("get reaction from ourselves on other message") {
                val env = TestEnv(testScope = this)
                val (_, timeline, roomUsers, event) = env
                timeline.addEvents {
                    +textMessageBy(env.alice, event[0])
                    +reactionBy(env.us, event[0], "😄")
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Martin", env.us)
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByUsers mapOf(
                    env.us to setOf("😄"),
                    env.alice to setOf(),
                )
                cancelNeverEndingCoroutines()
            }

            should("get reaction from ourselves on own message") {
                val env = TestEnv(testScope = this)
                val (_, timeline, roomUsers, event) = env
                timeline.addEvents {
                    +textMessageBy(env.us, event[0])
                    +reactionBy(env.us, event[0], "😄")
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Martin", env.us)
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByUsers mapOf(
                    env.us to setOf("😄"),
                )
                cancelNeverEndingCoroutines()
            }

            should("get all reactions from multiple users on other message") {
                val env = TestEnv(testScope = this)
                val (_, timeline, roomUsers, event) = env
                timeline.addEvents {
                    +textMessageBy(env.alice, event[0])
                    +reactionBy(env.bob, event[0], "😄")
                    +reactionBy(env.alice, event[0], "😄")
                    +reactionBy(env.alice, event[0], "🥳")
                    +reactionBy(env.bob, event[0], "🙂")
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", env.bob)
                    +roomUser("Alice", env.alice)
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByUsers mapOf(
                    env.alice to setOf("🥳", "😄"),
                    env.bob to setOf("🙂", "😄"),
                )
                cancelNeverEndingCoroutines()
            }

            should("get all reactions from multiple users on onw message") {
                val env = TestEnv(testScope = this)
                val (_, timeline, roomUsers, event) = env
                timeline.addEvents {
                    +textMessageBy(env.us, event[0])
                    +reactionBy(env.bob, event[0], "😄")
                    +reactionBy(env.bob, event[0], "🙂")
                    +reactionBy(env.alice, event[0], "😄")
                    +reactionBy(env.alice, event[0], "🥳")
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", env.bob)
                    +roomUser("Alice", env.alice)
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByUsers mapOf(
                    env.alice to setOf("🥳", "😄"),
                    env.bob to setOf("🙂", "😄"),
                )
                cancelNeverEndingCoroutines()
            }

            should("get all reactions from multiple users and ourselves on other message") {
                val env = TestEnv(testScope = this)
                val (_, timeline, roomUsers, event) = env
                timeline.addEvents {
                    +textMessageBy(env.bob, event[0])
                    +reactionBy(env.us, event[0], "😄")
                    +reactionBy(env.bob, event[0], "😄")
                    +reactionBy(env.alice, event[0], "😄")
                    +reactionBy(env.alice, event[0], "🥳")
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", env.bob)
                    +roomUser("Martin", env.us)
                    +roomUser("Alice", env.alice)
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByUsers mapOf(
                    env.alice to setOf("🥳", "😄"),
                    env.bob to setOf("😄"),
                    env.us to setOf("😄"),
                )
                cancelNeverEndingCoroutines()
            }

            should("get all reactions from multiple users and ourselves on onw message") {
                val env = TestEnv(testScope = this)
                val (_, timeline, roomUsers, event) = env
                timeline.addEvents {
                    +textMessageBy(env.us, event[0])
                    +reactionBy(env.us, event[0], "🙂")
                    +reactionBy(env.bob, event[0], "😄")
                    +reactionBy(env.alice, event[0], "🥳")
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", env.bob)
                    +roomUser("Martin", env.us)
                    +roomUser("Alice", env.alice)
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByUsers mapOf(
                    env.alice to setOf("🥳"),
                    env.bob to setOf("😄"),
                    env.us to setOf("🙂"),
                )
                cancelNeverEndingCoroutines()
            }
        }

        context("reactions by count") {

            should("get nothing for message with no reactions") {
                val env = TestEnv(testScope = this)
                val (_, timeline, roomUsers, event) = env
                timeline.addEvents {
                    +textMessageBy(env.alice, event[0])
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByCount listOf(
                    "😄" to 0,
                )
                cancelNeverEndingCoroutines()
            }

            should("get count for single reaction") {
                val env = TestEnv(testScope = this)
                val (_, timeline, roomUsers, event) = env
                timeline.addEvents {
                    +textMessageBy(env.us, event[0])
                    +reactionBy(env.bob, event[0], "🙂")
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", env.bob)
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByCount listOf(
                    "🥳" to 0,
                    "🙂" to 1,
                )
                cancelNeverEndingCoroutines()
            }

            should("get counts for single reactions") {
                val env = TestEnv(testScope = this)
                val (_, timeline, roomUsers, event) = env
                timeline.addEvents {
                    +textMessageBy(env.us, event[0])
                    +reactionBy(env.us, event[0], "🙂")
                    +reactionBy(env.us, event[0], "😄")
                    +reactionBy(env.bob, event[0], "😊")
                    +reactionBy(env.alice, event[0], "🥳")
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", env.bob)
                    +roomUser("Martin", env.us)
                    +roomUser("Alice", env.alice)
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByCount listOf(
                    "😊" to 1,
                    "🥳" to 1,
                    "🙂" to 1,
                    "😄" to 1,
                )
                cancelNeverEndingCoroutines()
            }

            should("get counts for reactions") {
                val env = TestEnv(testScope = this)
                val (_, timeline, roomUsers, event) = env
                timeline.addEvents {
                    +textMessageBy(env.us, event[0])
                    +reactionBy(env.us, event[0], "🙂")
                    +reactionBy(env.bob, event[0], "🙂")
                    +reactionBy(env.us, event[0], "😄")
                    +reactionBy(env.bob, event[0], "😄")
                    +reactionBy(env.alice, event[0], "😄")
                    +reactionBy(env.alice, event[0], "🥳")
                }
                roomUsers.addOrUpdateUsers {
                    +roomUser("Bob", env.bob)
                    +roomUser("Martin", env.us)
                    +roomUser("Alice", env.alice)
                }
                val cut = env.cutMessageReactions(event[0])
                cut shouldReturnReactionsByCount listOf(
                    "😊" to 0,
                    "🥳" to 1,
                    "🙂" to 2,
                    "😄" to 3,
                )
                cancelNeverEndingCoroutines()
            }
        }
    }

    private fun TimelineBuilder.textMessageBy(userId: UserId, eventId: EventId) =
        messageEvent(sender = userId, eventId) { text("Hello") }

    private fun TimelineBuilder.reactionBy(userId: UserId, reactionTo: EventId, reactionKey: ReactionKey) =
        messageEvent(sender = userId) { reaction(reactionTo, reactionKey) }

    private suspend infix fun Flow<MessageUserReactions?>.shouldReturnReactionsByUsers(
        expected: Map<UserId, Set<ReactionKey>>,
    ) {
        this.firstOrNull()?.let { reactions ->
            reactions.byUser shouldHaveSize expected.filter { it.value.isNotEmpty() }.size
            reactions.byUser.let {
                expected.forEach { (expectingUser, expectedReactions) ->
                    withClue("did not match expected reactions for user $expectingUser") {
                        val receivedReactions = it[expectingUser]?.let { userReactions ->
                            withClue("checking user value if present") {
                                runBlocking {
                                    userReactions.roomUserFlow.first()!!.userId shouldBe expectingUser
                                }
                            }
                            userReactions.reactions
                        } ?: setOf()
                        receivedReactions shouldBe expectedReactions
                    }
                }
            }
        } ?: throw failure("no reaction data received")
    }

    private suspend infix fun Flow<MessageUserReactions?>.shouldReturnReactionsByCount(
        expected: List<Pair<ReactionKey, Int>>,
    ) {
        this.firstOrNull()?.let { reactions ->
            reactions.byCount shouldHaveSize expected.filter { it.second > 0 }.size
            expected.forEach { (expectedReaction, expectedCount) ->
                withClue("did not match expected count for reaction $expectedReaction") {
                    val receivedOrImpliedCount = reactions.byCount[expectedReaction]?.count ?: 0u
                    receivedOrImpliedCount.toInt() shouldBe expectedCount
                }
            }
            reactions.byCount
        } ?: throw failure("no reaction data received")
    }
}

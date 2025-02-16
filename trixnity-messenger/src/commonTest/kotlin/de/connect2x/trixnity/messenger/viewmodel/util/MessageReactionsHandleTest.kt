package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomUserBuilder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineBuilder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineMock
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.roomUsers
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.timeline
import de.connect2x.trixnity.messenger.withCleanup
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.assertions.failure
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.assertions.withClue
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds


@ExperimentalKotest
class MessageReactionsHandleTest : ShouldSpec() {

    data class Mocks(
        val matrixClientMock: MatrixClient = mock<MatrixClient>(),
        val roomServiceMock: RoomService = mock<RoomService>(),
        val userServiceMock: UserService = mock<UserService>(),
    )

    init {
        coroutineTestScope = true
        concurrency = 16
        timeout = 5_000

        // Mokkery fails to properly update existing mocks.
        data class TestEnv(
            val roomId: RoomId = RoomId("room", "localhost"),
            val mocks: Mocks = Mocks(),
            val timeline: TimelineMock = timeline(mocks.roomServiceMock, roomId) {},
            val roomUsers: RoomUserBuilder = roomUsers(mocks.userServiceMock, roomId) {},
            val eventIds: List<EventId> = (0..2).map { EventId("event_$it") },
            val us: UserId = UserId("martin", "localhost"),
            val alice: UserId = UserId("alice", "localhost"),
            val bob: UserId = UserId("bob", "localhost"),
            val testScope: TestScope,
        )

        fun TestEnv.setupMocks() {
            every { mocks.matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { mocks.roomServiceMock }
                        single { mocks.userServiceMock }
                    }
                )
            }.koin
            every { mocks.matrixClientMock.userId } returns us
            every { mocks.userServiceMock.getById(eq(roomId), eq(us)) } returns flowOf(
                RoomUser(
                    roomId, us, "Martin", StateEvent(
                        MemberEventContent(membership = Membership.JOIN),
                        EventId(""),
                        us,
                        roomId,
                        0L,
                        stateKey = "",
                    )
                )
            )
            every { mocks.userServiceMock.getById(eq(roomId), eq(alice)) } returns flowOf(
                RoomUser(
                    roomId, alice, "Alice", StateEvent(
                        MemberEventContent(membership = Membership.JOIN),
                        EventId(""),
                        alice,
                        roomId,
                        0L,
                        stateKey = "",
                    )
                )
            )
            every { mocks.userServiceMock.getById(eq(roomId), eq(bob)) } returns flowOf(
                RoomUser(
                    roomId, bob, "Bob", StateEvent(
                        MemberEventContent(membership = Membership.JOIN),
                        EventId(""),
                        bob,
                        roomId,
                        0L,
                        stateKey = "",
                    )
                )
            )
        }

        fun TestScope.getTestEnv() =
            TestEnv(testScope = this).also {
                it.setupMocks()
            }

        fun TestEnv.cutMessageReactions(
            eventId: EventId,
        ): MessageReactionsHandle {
            val di = koinApplication {
                modules(
                    createTestDefaultTrixnityMessengerModules(mapOf(us to mocks.matrixClientMock))
                )
            }.koin
            val viewModelContext = testScope.testMatrixClientViewModelContext(
                di = di,
                userId = us,
            )
            return MessageReactionsHandleImpl(
                roomId = roomId,
                eventId = eventId,
                initials = Initials,
                client = viewModelContext.matrixClient,
                config = MatrixMessengerConfiguration(),
                scope = viewModelContext.coroutineScope,
            )
        }

        should("get nothing on other message without reactions").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, _, event) = env
            timeline.addEvents {
                +textMessageBy(env.alice, event[0])
            }
            val cut = env.cutMessageReactions(event[0])
            cut shouldReturnReactionsByUsers mapOf()
        }

        should("get nothing on our message without reactions").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, _, event) = env
            timeline.addEvents {
                +textMessageBy(env.us, event[0])
            }
            val cut = env.cutMessageReactions(event[0])
            cut shouldReturnReactionsByUsers mapOf(
                env.bob to setOf(),
            )
        }

        should("get reaction from other user on other message").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, roomUsers, event) = env
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
        }

        should("continuously get reactions from other user on own message").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, roomUsers, event) = env
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
        }

        should("get reaction from ourselves on other message").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, roomUsers, event) = env
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
        }

        should("get reaction from ourselves on own message").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, roomUsers, event) = env
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
        }

        should("get all reactions from multiple users on other message").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, roomUsers, event) = env
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
        }

        should("get all reactions from multiple users on own message").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, roomUsers, event) = env
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
        }

        should("get all reactions from multiple users and ourselves on other message").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, roomUsers, event) = env
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
        }

        should("get all reactions from multiple users and ourselves on onw message").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, roomUsers, event) = env
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
        }

        should("get nothing for message with no reactions").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, roomUsers, event) = env
            timeline.addEvents {
                +textMessageBy(env.alice, event[0])
            }
            val cut = env.cutMessageReactions(event[0])
            cut shouldReturnReactionsByCount listOf(
                "😄" to 0,
            )
        }

        should("get count for single reaction").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, roomUsers, event) = env
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
        }

        should("get counts for single reactions").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, roomUsers, event) = env
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
        }

        should("get counts for reactions").withCleanup {
            val env = getTestEnv()
            val (_, _, timeline, roomUsers, event) = env
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
        }
    }

    private fun TimelineBuilder.textMessageBy(userId: UserId, eventId: EventId) =
        messageEvent(sender = userId, eventId) { text("Hello") }

    private fun TimelineBuilder.reactionBy(userId: UserId, reactionTo: EventId, reactionKey: ReactionKey) =
        messageEvent(sender = userId) { reaction(reactionTo, reactionKey) }

    private suspend inline infix fun MessageReactionsHandle.shouldReturnReactionsByUsers(
        expected: Map<UserId, Set<ReactionKey>>,
    ) {
        this.reactions.firstOrNull()?.let { reactions ->
            reactions.byUser shouldHaveSize expected.filter { it.value.isNotEmpty() }.size
            reactions.byUser.let {
                expected.forEach { (expectingUser, expectedReactions) ->
                    withClue("did not match expected reactions for user $expectingUser") {
                        val receivedReactions = it[expectingUser]?.let { userReactions ->
                            withClue("checking user value if present") {
                                eventually(eventuallyConfig {
                                    retries = 10
                                    this.listener = { _, _ -> delay(100.milliseconds) }
                                }) {
                                    userReactions.userInfo.first()?.userId shouldBe expectingUser
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

    private suspend inline infix fun MessageReactionsHandle.shouldReturnReactionsByCount(
        expected: List<Pair<ReactionKey, Int>>,
    ) {
        this.reactions.firstOrNull()?.let { reactions ->
            reactions.byReaction shouldHaveSize expected.filter { it.second > 0 }.size
            expected.forEach { (expectedReaction, expectedCount) ->
                withClue("did not match expected count for reaction $expectedReaction") {
                    val receivedOrImpliedCount = reactions.byReaction[expectedReaction]?.size ?: 0
                    receivedOrImpliedCount shouldBe expectedCount
                }
            }
        } ?: throw failure("no reaction data received")
    }
}

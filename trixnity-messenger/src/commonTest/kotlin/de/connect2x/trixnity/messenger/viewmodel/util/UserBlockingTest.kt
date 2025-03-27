package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.eventually
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beOfType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.UserApiClient
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.IgnoredUserListEventContent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds


class UserBlockingTest {
    val matrixClientMock = mock<MatrixClient>()

    val userServiceMock = mock<UserService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val usersApiClientMock = mock<UserApiClient>()

    private val userId = UserId("test", "server")
    private val contact1 = UserId("jerk", "localhost")
    private val contact2 = UserId("another_jerk", "localhost")
    private val contact3 = UserId("do_not_want", "localhost")

    private val blockedUsers = MutableStateFlow(mapOf<UserId, JsonObject>())

    init {
        resetMocks(matrixClientMock, userServiceMock, matrixClientServerApiClientMock, usersApiClientMock)
        every { matrixClientMock.di } returns koinApplication {
            modules(module { single { userServiceMock } })
        }.koin
        every { userServiceMock.getAccountData(IgnoredUserListEventContent::class) } returns blockedUsers.map {
            IgnoredUserListEventContent(
                it
            )
        }
        every { matrixClientMock.userId } returns userId
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.user } returns usersApiClientMock
        everySuspend {
            usersApiClientMock.setAccountData(
                content = any(),
                userId = eq(userId),
                asUserId = any(),
                key = any(),
            )
        } calls {
            delay(33) // Simulate a request delay to check for concurrency issues.
            val event = it.args[0] as IgnoredUserListEventContent
            blockedUsers.value = event.ignoredUsers
            Result.success(Unit)
        }
    }

    @Test
    fun `get all blocked users`() = runTest {
        blockedUsers.value = mapOf(
            contact1 to JsonObject(emptyMap()),
            contact2 to JsonObject(emptyMap()),
            contact3 to JsonObject(emptyMap()),
        )
        val cut = userBlocking()
        eventually(1.seconds) {
            cut.getBlockedUsers(matrixClientMock).first() shouldBe listOf(
                contact1,
                contact2,
                contact3,
            )
        }
    }

    @Test
    fun `get a user's blocked state by id`() = runTest {
        blockedUsers.value = mapOf(
            contact1 to JsonObject(emptyMap()),
            contact2 to JsonObject(emptyMap()),
        )
        val cut = userBlocking()
        eventually(1.seconds) {
            cut.isUserBlocked(matrixClientMock, contact1).first() shouldBe true
            cut.isUserBlocked(matrixClientMock, contact3).first() shouldBe false
        }
    }

    @Test
    fun `block users consecutively`() = runTest {
        blockedUsers.value = mapOf()
        val successes = MutableStateFlow(0)
        val failures = MutableStateFlow(0)
        val cut = userBlocking()
        backgroundScope.launch {
            cut.blockUser(
                matrixClientMock,
                contact1,
                { successes.value++ },
                { failures.value++ })
        }
        backgroundScope.launch {
            cut.blockUser(
                matrixClientMock,
                contact2,
                { successes.value++ },
                { failures.value++ })
        }
        backgroundScope.launch {
            cut.blockUser(
                matrixClientMock,
                contact3,
                { successes.value++ },
                { failures.value++ })
        }
        eventually(1.seconds) {
            cut.getBlockedUsers(matrixClientMock).first() shouldBe listOf(
                contact1,
                contact2,
                contact3,
            )
            successes.first() shouldBe 3
            failures.first() shouldBe 0
        }
    }

    @Test
    fun `unblock users consecutively`() = runTest {
        blockedUsers.value = mapOf(
            contact1 to JsonObject(emptyMap()),
            contact2 to JsonObject(emptyMap()),
            contact3 to JsonObject(emptyMap()),
        )
        val successes = MutableStateFlow(0)
        val failures = MutableStateFlow(0)
        val cut = userBlocking()
        backgroundScope.launch {
            cut.unblockUser(
                matrixClientMock,
                contact1,
                { successes.value++ },
                { failures.value++ })
        }
        backgroundScope.launch {
            cut.unblockUser(
                matrixClientMock,
                contact2,
                { successes.value++ },
                { failures.value++ })
        }
        backgroundScope.launch {
            cut.unblockUser(
                matrixClientMock,
                contact3,
                { successes.value++ },
                { failures.value++ })
        }
        eventually(1.seconds) {
            cut.getBlockedUsers(matrixClientMock).first() shouldBe listOf()
            successes.first() shouldBe 3
            failures.first() shouldBe 0
        }
    }

    @Test
    fun `error when a user is already blocked`() = runTest {
        blockedUsers.value = mapOf(
            contact1 to JsonObject(emptyMap()),
            contact2 to JsonObject(emptyMap()),
        )
        val cut = userBlocking()
        val successes = MutableStateFlow(0)
        val errors = MutableStateFlow<List<Throwable>>(listOf())
        cut.blockUser(matrixClientMock, contact1, { successes.value++ }, {
            errors.value += it
        })
        eventually(1.seconds) {
            successes.first() shouldBe 0
            errors.first().size shouldBe 1
            errors.first()[0] should beOfType<IllegalArgumentException>()
        }
    }

    @Test
    fun `error when a user is already unblocked`() = runTest {
        blockedUsers.value = mapOf(
            contact1 to JsonObject(emptyMap()),
            contact2 to JsonObject(emptyMap()),
        )
        val cut = userBlocking()
        val successes = MutableStateFlow(0)
        val errors = MutableStateFlow<List<Throwable>>(listOf())
        cut.unblockUser(matrixClientMock, contact3, { successes.value++ }, {
            errors.value += it
        })
        eventually(1.seconds) {
            successes.first() shouldBe 0
            errors.first().size shouldBe 1
            errors.first()[0] should beOfType<IllegalArgumentException>()
        }
    }

    private fun userBlocking(): UserBlocking = UserBlockingImpl()
}

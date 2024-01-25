package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.trixnity.messenger.integrationtests.messenger.acceptInvitationToRoom
import de.connect2x.trixnity.messenger.integrationtests.messenger.createChatWithUser
import de.connect2x.trixnity.messenger.integrationtests.messenger.leaveRoom
import de.connect2x.trixnity.messenger.integrationtests.messenger.login
import de.connect2x.trixnity.messenger.integrationtests.messenger.verifyAccountsArePresent
import de.connect2x.trixnity.messenger.integrationtests.util.createTestMatrixMessenger
import de.connect2x.trixnity.messenger.integrationtests.util.register
import de.connect2x.trixnity.messenger.integrationtests.util.synapseDocker
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
@Testcontainers
class DirectRoomsIT {
    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher

    private val user1 = "user1"
    private val passwordUser1 = "user$1passw0rd"
    private val user2 = "user2"
    private val passwordUser2 = "user$2passw0rd"

    @Container
    val synapseDocker = synapseDocker()

    @BeforeTest
    fun beforeEach(): Unit = runBlocking {
        singleThreadContext = newSingleThreadContext("main")
        Dispatchers.setMain(singleThreadContext) // this tricks Decompose into accepting a fake UI thread
        val baseUrl = URLBuilder(
            protocol = URLProtocol.HTTP,
            host = synapseDocker.host,
            port = synapseDocker.firstMappedPort
        ).build()

        MatrixClientServerApiClientImpl(baseUrl).register(user1, passwordUser1)
        MatrixClientServerApiClientImpl(baseUrl).register(user2, passwordUser2)
    }

    @AfterTest
    fun afterEach() {
        singleThreadContext.close()
    }

    @Test
    fun shouldUseDirectRoomEvenIfDirectRoomExistedBefore(): Unit = runBlocking {
        withTimeout(30_000) {
            val messenger1 = createTestMatrixMessenger("client-1")
            val recoveryKey =
                messenger1.login(
                    serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                    username = user1,
                    password = passwordUser1,
                )
            messenger1.verifyAccountsArePresent(user1)
            val messenger2 = createTestMatrixMessenger("client-2")
            messenger2.login(
                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                username = user2,
                password = passwordUser2,
                recoveryKey = recoveryKey,
            )
            messenger2.verifyAccountsArePresent(user2)
            messenger1.verifyAccountsArePresent(user1)
            log.info { "--- create chat" }
            val roomId = messenger1.createChatWithUser(user2).roomId
            messenger2.acceptInvitationToRoom(roomId)
            delay(2.seconds) // wait for the block information to be distributed
            log.info { "--- leave chat" }
            messenger2.leaveRoom(roomId)
            messenger1.leaveRoom(roomId)
            delay(2.seconds) // wait for the block information to be distributed
            log.info { "--- create chat, again" }
            val roomId2 = messenger1.createChatWithUser(user2).roomId
            messenger2.acceptInvitationToRoom(roomId2)
        }
    }
}
package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.trixnity.messenger.integrationtests.messenger.MatrixMessengerWithRoot
import de.connect2x.trixnity.messenger.integrationtests.messenger.createChatWithUser
import de.connect2x.trixnity.messenger.integrationtests.messenger.createGroupWithUsers
import de.connect2x.trixnity.messenger.integrationtests.messenger.findRoomWithId
import de.connect2x.trixnity.messenger.integrationtests.messenger.login
import de.connect2x.trixnity.messenger.integrationtests.messenger.rejectTheInvitationToRoomAndBlock
import de.connect2x.trixnity.messenger.integrationtests.messenger.verifyAccountsArePresent
import de.connect2x.trixnity.messenger.integrationtests.util.createTestMatrixMessenger
import de.connect2x.trixnity.messenger.integrationtests.util.register
import de.connect2x.trixnity.messenger.integrationtests.util.runBlockingWithTimeout
import de.connect2x.trixnity.messenger.integrationtests.util.synapseDocker
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@Testcontainers
class BlockUserIT {

    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher
    private lateinit var messenger1: MatrixMessengerWithRoot
    private lateinit var messenger2: MatrixMessengerWithRoot

    private val user1 = "user1"
    private val passwordUser1 = "user$1passw0rd"
    private val user2 = "user2"
    private val passwordUser2 = "user$2passw0rd"

    @Container
    val synapseDocker = synapseDocker()

    @BeforeTest
    fun beforeEach(): Unit = runBlockingWithTimeout {
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
        messenger1.close()
        messenger2.close()
    }

    @Test
    fun shouldBlockAUserOnInvitation(): Unit = runBlockingWithTimeout {
        messenger1 = createTestMatrixMessenger("client-1")
        val recoveryKey =
            messenger1.login(
                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                username = user1,
                password = passwordUser1,
            )
        messenger1.verifyAccountsArePresent(user1)
        messenger2 = createTestMatrixMessenger("client-2")
        messenger2.login(
            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
            username = user2,
            password = passwordUser2,
            recoveryKey = recoveryKey,
        )
        messenger2.verifyAccountsArePresent(user2)
        messenger1.verifyAccountsArePresent(user1)
        delay(2.seconds) // give user search time to find the user
        val roomId = messenger1.createChatWithUser(user2).roomId
        messenger2.rejectTheInvitationToRoomAndBlock(roomId)
        delay(3.seconds) // wait for the block information to be distributed
        val roomId2 = messenger1.createGroupWithUsers("Hello user2", user2).roomId
        runCatching { messenger2.findRoomWithId(roomId2) }.isFailure shouldBe true
    }
}

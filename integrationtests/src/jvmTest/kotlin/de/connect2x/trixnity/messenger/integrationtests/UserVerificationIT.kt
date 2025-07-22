package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.integrationtests.messenger.MatrixMessengerWithRoot
import de.connect2x.trixnity.messenger.integrationtests.messenger.acceptInvitationToRoom
import de.connect2x.trixnity.messenger.integrationtests.messenger.acceptUserVerification
import de.connect2x.trixnity.messenger.integrationtests.messenger.acceptVerificationWithEmoji
import de.connect2x.trixnity.messenger.integrationtests.messenger.createChatWithUser
import de.connect2x.trixnity.messenger.integrationtests.messenger.initiateUserVerification
import de.connect2x.trixnity.messenger.integrationtests.messenger.login
import de.connect2x.trixnity.messenger.integrationtests.messenger.originalClientAcceptVerificationWithEmoji
import de.connect2x.trixnity.messenger.integrationtests.messenger.startVerificationWithEmoji
import de.connect2x.trixnity.messenger.integrationtests.util.createTestMatrixMessenger
import de.connect2x.trixnity.messenger.integrationtests.util.register
import de.connect2x.trixnity.messenger.integrationtests.util.runBlockingWithTimeout
import de.connect2x.trixnity.messenger.integrationtests.util.synapseDocker
import io.ktor.http.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.core.model.UserId
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@Testcontainers
class UserVerificationIT {
    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher
    private lateinit var messenger1: MatrixMessengerWithRoot
    private lateinit var messenger2: MatrixMessengerWithRoot
    private lateinit var userId1: UserId
    private lateinit var userId2: UserId

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

        userId1 = MatrixClientServerApiClientImpl(baseUrl).register(user1, passwordUser1, "CLIENT1").getOrThrow().userId
        userId2 = MatrixClientServerApiClientImpl(baseUrl).register(user2, passwordUser2, "CLIENT2").getOrThrow().userId
    }

    @AfterTest
    fun afterEach() {
        singleThreadContext.close()
        messenger1.close()
        messenger2.close()
    }

    @Test
    fun shouldDoSelfVerificationWithRecoveryKeyAndEmojiCompare(): Unit = runBlockingWithTimeout {
        messenger1 = createTestMatrixMessenger()
        val recoveryKey =
            messenger1.login(
                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                username = "user1",
                password = passwordUser1,
            )
        messenger2 = createTestMatrixMessenger("client-2")
        messenger2.login(
            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
            username = user2,
            password = passwordUser2,
            recoveryKey = recoveryKey,
        )
        val roomId = messenger1.createChatWithUser(user2).roomId
        messenger2.acceptInvitationToRoom(roomId)
        messenger1.di.get<MatrixClients>().value.forEach { it.value.syncOnce() }
        messenger1.initiateUserVerification(roomId, userId2)
        messenger2.acceptUserVerification(roomId, userId1)
        messenger1.startVerificationWithEmoji(roomId)
        messenger2.acceptVerificationWithEmoji(roomId)
        messenger1.originalClientAcceptVerificationWithEmoji(roomId)
    }
}

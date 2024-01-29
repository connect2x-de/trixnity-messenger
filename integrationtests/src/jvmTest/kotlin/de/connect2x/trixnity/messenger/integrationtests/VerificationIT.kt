package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.trixnity.messenger.integrationtests.messenger.login
import de.connect2x.trixnity.messenger.integrationtests.util.createTestMatrixMessenger
import de.connect2x.trixnity.messenger.integrationtests.util.register
import de.connect2x.trixnity.messenger.integrationtests.util.runBlockingWithTimeout
import de.connect2x.trixnity.messenger.integrationtests.util.synapseDocker
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@Testcontainers
class VerificationIT {

    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher

    private val password = "user$1passw0rd"

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

        MatrixClientServerApiClientImpl(baseUrl).register("user1", password)
    }

    @AfterTest
    fun afterEach() {
        singleThreadContext.close()
    }

    @Test
    fun shouldDoSelfVerificationWithRecoveryKeyAndEmojiCompare(): Unit = runBlockingWithTimeout {
        val messenger1 = createTestMatrixMessenger()
        val recoveryKey =
            messenger1.login(
                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                username = "user1",
                password = password,
            )
        val messenger2 = createTestMatrixMessenger()
        messenger2.login(
            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
            username = "user1",
            password = password,
            recoveryKey = recoveryKey,
        )
        val messenger3 = createTestMatrixMessenger()
        messenger3.login(
            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
            username = "user1",
            password = password,
            otherMessenger = messenger1,
        )
    }
}
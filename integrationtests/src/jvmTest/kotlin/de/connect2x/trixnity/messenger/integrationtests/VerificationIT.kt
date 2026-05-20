package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import de.connect2x.trixnity.messenger.integrationtests.messenger.MatrixMessengerWithRoot
import de.connect2x.trixnity.messenger.integrationtests.messenger.login
import de.connect2x.trixnity.messenger.integrationtests.util.configureTestLogging
import de.connect2x.trixnity.messenger.integrationtests.util.createTestMatrixMessenger
import de.connect2x.trixnity.messenger.integrationtests.util.register
import de.connect2x.trixnity.messenger.integrationtests.util.runBlockingWithTimeout
import de.connect2x.trixnity.messenger.integrationtests.util.synapseDocker
import io.ktor.http.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@Testcontainers
class VerificationIT {
    init {
        configureTestLogging()
    }

    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher
    private lateinit var messenger1: MatrixMessengerWithRoot
    private lateinit var messenger2: MatrixMessengerWithRoot
    private lateinit var messenger3: MatrixMessengerWithRoot

    private val password = "user$1passw0rd"

    @Container val synapseDocker = synapseDocker()

    @BeforeTest
    fun beforeEach(): Unit = runBlockingWithTimeout {
        singleThreadContext = newSingleThreadContext("main")
        Dispatchers.setMain(singleThreadContext) // this tricks Decompose into accepting a fake UI thread
        val baseUrl =
            URLBuilder(protocol = URLProtocol.HTTP, host = synapseDocker.host, port = synapseDocker.firstMappedPort)
                .build()

        MatrixClientServerApiClientImpl(baseUrl).register("user1", password)
    }

    @AfterTest
    fun afterEach() {
        singleThreadContext.close()
        messenger1.close()
        messenger2.close()
        messenger3.close()
    }

    @Test
    fun shouldDoSelfVerificationWithRecoveryKeyAndEmojiCompare(): Unit = runBlockingWithTimeout {
        messenger1 = createTestMatrixMessenger()
        val recoveryKey =
            messenger1.login(
                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                username = "user1",
                password = password,
            )
        messenger2 = createTestMatrixMessenger()
        messenger2.login(
            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
            username = "user1",
            password = password,
            recoveryKey = recoveryKey,
        )
        messenger3 = createTestMatrixMessenger()
        messenger3.login(
            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
            username = "user1",
            password = password,
            otherMessenger = messenger1,
        )
    }
}

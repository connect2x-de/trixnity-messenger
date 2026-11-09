package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import de.connect2x.trixnity.messenger.integrationtests.messenger.nav3.Nav3MatrixMessengerWithRoot
import de.connect2x.trixnity.messenger.integrationtests.messenger.nav3.login
import de.connect2x.trixnity.messenger.integrationtests.util.configureTestLogging
import de.connect2x.trixnity.messenger.integrationtests.util.createTestNav3MatrixMessenger
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
class VerificationV2IT {
    init {
        configureTestLogging()
    }

    private val log: Logger = Logger("de.connect2x.trixnity.messenger.integrationtests.VerificationV2ITKT")

    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher
    private lateinit var messenger1: Nav3MatrixMessengerWithRoot
    private lateinit var messenger2: Nav3MatrixMessengerWithRoot
    private lateinit var messenger3: Nav3MatrixMessengerWithRoot

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
    fun `Should do self verification with recovery key and emoji compare via v2 view models and nav3`(): Unit =
        runBlockingWithTimeout {
            messenger1 = createTestNav3MatrixMessenger()
            log.info { "Created first messenger, logging in" }
            val recoveryKey =
                messenger1.login(
                    serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                    username = "user1",
                    password = password,
                )
            log.info { "Created first messenger with recovery key, creating second messenger" }
            messenger2 = createTestNav3MatrixMessenger()
            log.info { "Created second messenger, logging in with recovery key" }
            messenger2.login(
                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                username = "user1",
                password = password,
                recoveryKey = recoveryKey,
            )
            log.info { "Created second messenger and logged in, creating third messenger" }
            messenger3 = createTestNav3MatrixMessenger()
            log.info { "Created third messenger, logging in" }
            messenger3.login(
                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                username = "user1",
                password = password,
                otherMessenger = messenger1,
            )
            log.info { "Created third messenger and verified via first messenger" }
        }
}

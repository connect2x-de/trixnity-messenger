package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import de.connect2x.trixnity.messenger.integrationtests.messenger.MatrixMessengerWithRoot
import de.connect2x.trixnity.messenger.integrationtests.messenger.createNewAccount
import de.connect2x.trixnity.messenger.integrationtests.messenger.deleteAccount
import de.connect2x.trixnity.messenger.integrationtests.messenger.login
import de.connect2x.trixnity.messenger.integrationtests.messenger.verifyAccountsArePresent
import de.connect2x.trixnity.messenger.integrationtests.util.configureTestLogging
import de.connect2x.trixnity.messenger.integrationtests.util.createTestMatrixMultiMessenger
import de.connect2x.trixnity.messenger.integrationtests.util.register
import de.connect2x.trixnity.messenger.integrationtests.util.runBlockingWithTimeout
import de.connect2x.trixnity.messenger.integrationtests.util.synapseDocker
import de.connect2x.trixnity.messenger.multi.singleModeMatrixMessenger
import io.kotest.matchers.collections.shouldHaveSize
import io.ktor.http.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@Testcontainers
class MultiMessengerIT {
    init {
        configureTestLogging()
    }

    private val log: Logger = Logger("de.connect2x.trixnity.messenger.integrationtests.MultiMessengerIT")

    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher
    private lateinit var messenger: MatrixMessengerWithRoot

    private val password = "user$1passw0rd"
    private lateinit var baseUrl: Url

    @Container val synapseDocker = synapseDocker()

    @BeforeTest
    fun beforeEach(): Unit = runBlockingWithTimeout {
        DebugProbes.enableCreationStackTraces = true
        DebugProbes.install()

        singleThreadContext = newSingleThreadContext("main")
        Dispatchers.setMain(singleThreadContext) // this tricks Decompose into accepting a fake UI thread

        baseUrl =
            URLBuilder(protocol = URLProtocol.HTTP, host = synapseDocker.host, port = synapseDocker.firstMappedPort)
                .build()

        MatrixClientServerApiClientImpl(baseUrl).register("user1", password)
        MatrixClientServerApiClientImpl(baseUrl).register("user2", password)
    }

    @AfterTest
    fun afterEach() {
        singleThreadContext.close()
    }

    @Test
    fun shouldAddAnAccountAndRemoveAfterwardsOnMultiMatrixMessengerSingleMode(): Unit = runBlockingWithTimeout {
        val multiMessenger = createTestMatrixMultiMessenger()
        messenger = MatrixMessengerWithRoot(multiMessenger.singleModeMatrixMessenger().first())

        log.info { "login as user1" }
        messenger.login(
            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
            username = "user1",
            password = password,
        )
        messenger.verifyAccountsArePresent("user1")

        log.info { "login as user2" }
        val recoveryKey =
            messenger.createNewAccount(
                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                username = "user2",
                password = password,
            )
        messenger.verifyAccountsArePresent("user1", "user2")
        log.info { "logout as user2" }
        messenger.deleteAccount("user2")
        messenger.verifyAccountsArePresent("user1")

        log.info { "login again as user2" }
        messenger.createNewAccount(
            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
            username = "user2",
            password = password,
            recoveryKey = recoveryKey,
        )
        messenger.verifyAccountsArePresent("user1", "user2")
        multiMessenger.closeSuspending()

        delay(300.milliseconds)
        DebugProbes.dumpCoroutinesInfo() shouldHaveSize 1 // only the coroutine of this test should still be active
    }
}

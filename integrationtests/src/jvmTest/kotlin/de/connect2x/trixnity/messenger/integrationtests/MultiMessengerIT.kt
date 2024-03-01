package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.trixnity.messenger.integrationtests.messenger.MatrixMessengerWithRoot
import de.connect2x.trixnity.messenger.integrationtests.messenger.createNewAccount
import de.connect2x.trixnity.messenger.integrationtests.messenger.deleteAccount
import de.connect2x.trixnity.messenger.integrationtests.messenger.login
import de.connect2x.trixnity.messenger.integrationtests.messenger.verifyAccountsArePresent
import de.connect2x.trixnity.messenger.integrationtests.util.createTestMatrixMessengerFromMultiMessenger
import de.connect2x.trixnity.messenger.integrationtests.util.register
import de.connect2x.trixnity.messenger.integrationtests.util.runBlockingWithTimeout
import de.connect2x.trixnity.messenger.integrationtests.util.synapseDocker
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private val log = KotlinLogging.logger {}

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@Testcontainers
class MultiMessengerIT {

    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher
    private lateinit var messenger: MatrixMessengerWithRoot

    private val password = "user$1passw0rd"
    private lateinit var baseUrl: Url

    @Container
    val synapseDocker = synapseDocker()

    @BeforeTest
    fun beforeEach(): Unit = runBlockingWithTimeout {
        singleThreadContext = newSingleThreadContext("main")
        Dispatchers.setMain(singleThreadContext) // this tricks Decompose into accepting a fake UI thread

        baseUrl = URLBuilder(
            protocol = URLProtocol.HTTP,
            host = synapseDocker.host,
            port = synapseDocker.firstMappedPort
        ).build()

        MatrixClientServerApiClientImpl(baseUrl).register("user1", password)
        MatrixClientServerApiClientImpl(baseUrl).register("user2", password)
    }

    @AfterTest
    fun afterEach() {
        singleThreadContext.close()
        messenger.stop()
    }

    @Test
    fun shouldAddAnAccountAndRemoveAfterwardsOnMultiMatrixMessengerSingleMode(): Unit = runBlockingWithTimeout {
        messenger = createTestMatrixMessengerFromMultiMessenger()
        log.info { "login as user1" }
        messenger.login(
            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
            username = "user1",
            password = password,
        )
        messenger.verifyAccountsArePresent("user1")

        log.info { "login as user2" }
        val recoveryKey = messenger.createNewAccount(
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
    }
}
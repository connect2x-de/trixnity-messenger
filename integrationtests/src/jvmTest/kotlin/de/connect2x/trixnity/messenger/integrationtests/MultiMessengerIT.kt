package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.trixnity.messenger.integrationtests.messenger.createNewAccount
import de.connect2x.trixnity.messenger.integrationtests.messenger.deleteAccount
import de.connect2x.trixnity.messenger.integrationtests.messenger.login
import de.connect2x.trixnity.messenger.integrationtests.messenger.verifyAccountsArePresent
import de.connect2x.trixnity.messenger.integrationtests.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldNotBe
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@Testcontainers
class MultiMessengerIT {

    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher

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
    }

    @Test
    fun shouldHandleMultipleProfiles(): Unit = runTest {
        val multiMessenger = createTestMatrixMultiMessenger(coroutineContext = backgroundScope.coroutineContext)
        val profile1 = multiMessenger.createProfile()
        val profile2 = multiMessenger.createProfile()

        advanceTimeBy(1.seconds)
        multiMessenger.profiles.value shouldHaveSize 2
        multiMessenger.selectProfile(profile1)
        advanceTimeBy(1.seconds)
        val matrixMessenger1 = multiMessenger.activeMatrixMessenger.value shouldNotBe null

        multiMessenger.selectProfile(profile2)
        advanceTimeBy(1.seconds)
        val matrixMessenger2 = multiMessenger.activeMatrixMessenger.value shouldNotBe null

        matrixMessenger1 shouldNotBe matrixMessenger2
    }

    @Test
    fun shouldAddAnAccountAndRemoveAfterwardsOnMultiMatrixMessengerSingleMode(): Unit = runBlockingWithTimeout {
        val messenger = createTestMatrixMessengerFromMultiMessenger()
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
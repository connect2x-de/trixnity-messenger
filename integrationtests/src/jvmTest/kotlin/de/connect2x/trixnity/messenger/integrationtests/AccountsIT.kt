package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.integrationtests.messenger.createNewAccount
import de.connect2x.trixnity.messenger.integrationtests.messenger.deleteAccount
import de.connect2x.trixnity.messenger.integrationtests.messenger.login
import de.connect2x.trixnity.messenger.integrationtests.messenger.verifyAccountsArePresent
import de.connect2x.trixnity.messenger.integrationtests.util.*
import de.connect2x.trixnity.messenger.viewmodel.RootRouter
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationRequest
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private val log = KotlinLogging.logger { }

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@Testcontainers
class AccountsIT {

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
    fun shouldAddAnAccountAndRemoveAfterwards(): Unit = runBlockingWithTimeout {
        withTimeout(30_000) {
            val messenger = createTestMatrixMessenger()
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

    @Test
    fun `remove account when logged out and show login`(): Unit = runBlockingWithTimeout {
        val messenger1 = createTestMatrixMessenger()
        log.info { "login as user1" }
        messenger1.login(
            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
            username = "user1",
            password = password,
        )
        messenger1.verifyAccountsArePresent("user1")

        val matrixClient = messenger1.di.get<MatrixClients>().value.values.first()
        matrixClient.api.device.deleteDevice(matrixClient.deviceId).getOrThrow()
            .shouldBeInstanceOf<UIA.Step<*>>()
            .authenticate(
                AuthenticationRequest.Password(
                    IdentifierType.User("user1"),
                    password
                )
            ).getOrThrow()
            .shouldBeInstanceOf<UIA.Success<*>>()

        messenger1.root.stack.waitFor(RootRouter.Wrapper.AddMatrixAccount::class)
    }
}
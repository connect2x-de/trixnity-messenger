package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import de.connect2x.trixnity.clientserverapi.client.UIA
import de.connect2x.trixnity.clientserverapi.model.authentication.IdentifierType
import de.connect2x.trixnity.clientserverapi.model.uia.AuthenticationRequest
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.integrationtests.messenger.MatrixMessengerWithRoot
import de.connect2x.trixnity.messenger.integrationtests.messenger.createNewAccount
import de.connect2x.trixnity.messenger.integrationtests.messenger.deleteAccount
import de.connect2x.trixnity.messenger.integrationtests.messenger.login
import de.connect2x.trixnity.messenger.integrationtests.messenger.verifyAccountsArePresent
import de.connect2x.trixnity.messenger.integrationtests.util.configureTestLogging
import de.connect2x.trixnity.messenger.integrationtests.util.createTestMatrixMessenger
import de.connect2x.trixnity.messenger.integrationtests.util.register
import de.connect2x.trixnity.messenger.integrationtests.util.runBlockingWithTimeout
import de.connect2x.trixnity.messenger.integrationtests.util.synapseDocker
import de.connect2x.trixnity.messenger.integrationtests.util.waitFor
import de.connect2x.trixnity.messenger.util.RootPath
import de.connect2x.trixnity.messenger.viewmodel.RootRouter
import io.kotest.assertions.retry
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import okio.FileSystem
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@Testcontainers
class AccountsIT {
    init {
        configureTestLogging()
    }

    private val log: Logger = Logger("de.connect2x.trixnity.messenger.integrationtests.AccountsIT")

    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher
    private lateinit var messenger: MatrixMessengerWithRoot

    private val password = "user$1passw0rd"
    private lateinit var baseUrl: Url

    @Container val synapseDocker = synapseDocker()

    @BeforeTest
    fun beforeEach(): Unit = runBlockingWithTimeout {
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
        messenger.close()
    }

    @Test
    fun shouldAddAnAccountAndRemoveAfterwards(): Unit = runBlockingWithTimeout {
        messenger = createTestMatrixMessenger()
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
    }

    @Test
    fun `remove account when logged out and show login`(): Unit = runBlockingWithTimeout {
        messenger = createTestMatrixMessenger()
        log.info { "login as user1" }
        messenger.login(
            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
            username = "user1",
            password = password,
        )
        messenger.verifyAccountsArePresent("user1")

        val matrixClient = messenger.di.get<MatrixClients>().value.values.first()
        val userId = matrixClient.userId

        val settings = messenger.di.get<MatrixMessengerSettingsHolder>()
        val filesSystem = messenger.di.get<FileSystem>()
        val rootPath = messenger.di.get<RootPath>()
        val accountPath = rootPath.forAccount(userId)

        settings.value.base.accounts[userId] shouldNotBe null
        filesSystem.exists(accountPath) shouldBe true

        log.info { "delete device" }
        matrixClient.api.device
            .deleteDevice(matrixClient.deviceId)
            .getOrThrow()
            .shouldBeInstanceOf<UIA.Step<*>>()
            .authenticate(AuthenticationRequest.Password(IdentifierType.User("user1"), password))
            .getOrThrow()
            .shouldBeInstanceOf<UIA.Success<*>>()

        messenger.root.stack.waitFor(RootRouter.Wrapper.AddMatrixAccount::class)

        settings.value.base.accounts[userId] shouldBe null
        messenger.di.get<MatrixClients>().value[userId] shouldBe null

        retry(3, timeout = 500.milliseconds, delay = 100.milliseconds) {
            withClue("The account directory (`$accountPath`) should be deleted after logout") {
                filesSystem.exists(accountPath) shouldBe false
            }
        }
    }
}

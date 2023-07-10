package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.trixnity.messenger.integrationtests.messenger.*
import de.connect2x.trixnity.messenger.integrationtests.util.newDatabase
import de.connect2x.trixnity.messenger.integrationtests.util.register
import de.connect2x.trixnity.messenger.integrationtests.util.settingsModule
import de.connect2x.trixnity.messenger.integrationtests.util.synapseDocker
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.loginWith
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.client.store.repository.exposed.createExposedRepositoriesModule
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@Testcontainers
class AccountsIT {

    private lateinit var koinApplication: KoinApplication
    private lateinit var scope: CoroutineScope
    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher

    private val password = "user$1passw0rd"

    @Container
    val synapseDocker = synapseDocker()

    @BeforeTest
    fun beforeEach(): Unit = runBlocking {
        singleThreadContext = newSingleThreadContext("main")
        Dispatchers.setMain(singleThreadContext) // this tricks Decompose into accepting a fake UI thread

        koinApplication = koinApplication {
            modules(
                trixnityMessengerModule(),
                settingsModule()
            )
        }
        scope = CoroutineScope(Dispatchers.Default) + CoroutineName("accounts-client1")
        val baseUrl = URLBuilder(
            protocol = URLProtocol.HTTP,
            host = synapseDocker.host,
            port = synapseDocker.firstMappedPort
        ).build()
        val repositoriesModule1 = createExposedRepositoriesModule(newDatabase(null))
        val repositoriesModule2 = createExposedRepositoriesModule(newDatabase(null))

        MatrixClient.loginWith(
            baseUrl = baseUrl,
            repositoriesModule = repositoriesModule1,
            mediaStore = InMemoryMediaStore(),
            scope = scope,
            getLoginInfo = { it.register("user1", password) }
        ).getOrThrow()
        MatrixClient.loginWith(
            baseUrl = baseUrl,
            repositoriesModule = repositoriesModule2,
            mediaStore = InMemoryMediaStore(),
            scope = scope,
            getLoginInfo = { it.register("user2", password) }
        ).getOrThrow()
    }

    @AfterTest
    fun afterEach() {
        scope.cancel()
        singleThreadContext.close()
    }

    @Test
    fun shouldAddAnAccountAndRemoveAfterwards(): Unit = runBlocking {
        withTimeout(30_000) {
            val messenger1 = createMessenger(koinApplication)
            messenger1.login(
                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                username = "user1",
                password = password,
            )
            messenger1.verifyAccountsArePresent("user1")
            val recoveryKey = messenger1.createNewAccount(
                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                username = "user2",
                password = password,
            )
            messenger1.verifyAccountsArePresent("user1", "user2")
            messenger1.deleteAccount("user2")
            messenger1.verifyAccountsArePresent("user1")
            messenger1.createNewAccount(
                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                username = "user2",
                password = password,
                recoveryKey = recoveryKey,
            )
            messenger1.verifyAccountsArePresent("user1", "user2")
        }
    }
}
package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.trixnity.messenger.integrationtests.messenger.createMessenger
import de.connect2x.trixnity.messenger.integrationtests.messenger.login
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
class VerificationIT {

    private lateinit var koinApplication1: KoinApplication
    private lateinit var koinApplication2: KoinApplication
    private lateinit var koinApplication3: KoinApplication
    private lateinit var scope1: CoroutineScope
    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher

    private val password = "user$1passw0rd"

    @Container
    val synapseDocker = synapseDocker()

    @BeforeTest
    fun beforeEach(): Unit = runBlocking {
        singleThreadContext = newSingleThreadContext("main")
        Dispatchers.setMain(singleThreadContext) // this tricks Decompose into accepting a fake UI thread

        koinApplication1 = koinApplication {
            modules(
                trixnityMessengerModule(),
                settingsModule()
            )
        }
        koinApplication2 = koinApplication {
            modules(
                trixnityMessengerModule(),
                settingsModule()
            )
        }
        koinApplication3 = koinApplication {
            modules(
                trixnityMessengerModule(),
                settingsModule()
            )
        }
        scope1 = CoroutineScope(Dispatchers.Default) + CoroutineName("verification-client1")
        val baseUrl = URLBuilder(
            protocol = URLProtocol.HTTP,
            host = synapseDocker.host,
            port = synapseDocker.firstMappedPort
        ).build()
        val repositoriesModule1 = createExposedRepositoriesModule(newDatabase(null))

        MatrixClient.loginWith(
            baseUrl = baseUrl,
            repositoriesModule = repositoriesModule1,
            mediaStore = InMemoryMediaStore(),
            scope = scope1,
            getLoginInfo = { it.register("user1", password) }
        ).getOrThrow()
    }

    @AfterTest
    fun afterEach() {
        scope1.cancel()
        singleThreadContext.close()
    }

    @Test
    fun shouldDoSelfVerificationWithRecoveryKeyAndEmojiCompare(): Unit = runBlocking {
        withTimeout(30_000) {
            val messenger1 = createMessenger(koinApplication1)
            val recoveryKey =
                messenger1.login(
                    serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                    username = "user1",
                    password = password,
                )
            val messenger2 = createMessenger(koinApplication2)
            messenger2.login(
                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                username = "user1",
                password = password,
                recoveryKey = recoveryKey,
            )
            val messenger3 = createMessenger(koinApplication3)
            messenger3.login(
                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
                username = "user1",
                password = password,
                otherMessenger = messenger1,
            )
        }
    }
}
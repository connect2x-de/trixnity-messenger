//package de.connect2x.trixnity.messenger.integrationtests
//
//import de.connect2x.trixnity.messenger.integrationtests.messenger.MatrixMessengerWithRoot
//import de.connect2x.trixnity.messenger.integrationtests.messenger.acceptInvitationToRoom
//import de.connect2x.trixnity.messenger.integrationtests.messenger.createChatWithUser
//import de.connect2x.trixnity.messenger.integrationtests.messenger.findRoomWithId
//import de.connect2x.trixnity.messenger.integrationtests.messenger.getAllRooms
//import de.connect2x.trixnity.messenger.integrationtests.messenger.login
//import de.connect2x.trixnity.messenger.integrationtests.messenger.verifyAccountsArePresent
//import de.connect2x.trixnity.messenger.integrationtests.util.createTestMatrixMessenger
//import de.connect2x.trixnity.messenger.integrationtests.util.register
//import de.connect2x.trixnity.messenger.integrationtests.util.runBlockingWithTimeout
//import de.connect2x.trixnity.messenger.integrationtests.util.synapseDocker
//import io.github.oshai.kotlinlogging.KotlinLogging
//import io.kotest.matchers.collections.shouldHaveSize
//import io.ktor.http.*
//import kotlinx.coroutines.DelicateCoroutinesApi
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.ExecutorCoroutineDispatcher
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.newSingleThreadContext
//import kotlinx.coroutines.test.setMain
//import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
//import org.testcontainers.junit.jupiter.Container
//import org.testcontainers.junit.jupiter.Testcontainers
//import kotlin.test.AfterTest
//import kotlin.test.BeforeTest
//import kotlin.test.Test
//import kotlin.time.Duration.Companion.seconds
//
//private val log = KotlinLogging.logger { }
//
//@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
//@Testcontainers
//class OtherDeviceIT {
//    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher
//    private lateinit var messenger1: MatrixMessengerWithRoot
//    private lateinit var messenger2: MatrixMessengerWithRoot
//    private lateinit var messenger1_1: MatrixMessengerWithRoot
//    private lateinit var messenger1_2: MatrixMessengerWithRoot
//    private lateinit var messenger1_3: MatrixMessengerWithRoot
//    private lateinit var messenger1_4: MatrixMessengerWithRoot
//
//    private val user1 = "user1"
//    private val passwordUser1 = "user$1passw0rd"
//    private val user2 = "user2"
//    private val passwordUser2 = "user$2passw0rd"
//
//    @Container
//    val synapseDocker = synapseDocker()
//
//    @BeforeTest
//    fun beforeEach(): Unit = runBlockingWithTimeout {
//        singleThreadContext = newSingleThreadContext("main")
//        Dispatchers.setMain(singleThreadContext) // this tricks Decompose into accepting a fake UI thread
//        val baseUrl = URLBuilder(
//            protocol = URLProtocol.HTTP,
//            host = synapseDocker.host,
//            port = synapseDocker.firstMappedPort
//        ).build()
//
//        MatrixClientServerApiClientImpl(baseUrl).register(user1, passwordUser1)
//        MatrixClientServerApiClientImpl(baseUrl).register(user2, passwordUser2)
//    }
//
//    @AfterTest
//    fun afterEach() {
//        singleThreadContext.close()
//        messenger1.stop()
//        messenger2.stop()
//        messenger1_1.stop()
//        messenger1_2.stop()
//        messenger1_3.stop()
//        messenger1_4.stop()
//    }
//
//    @Test
//    fun shouldLoginWithOtherDeviceAndGetSameRooms(): Unit = runBlockingWithTimeout {
//        messenger1 = createTestMatrixMessenger("client-1")
//        val recoveryKey =
//            messenger1.login(
//                serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
//                username = user1,
//                password = passwordUser1,
//            )
//        messenger1.verifyAccountsArePresent(user1)
//        messenger2 = createTestMatrixMessenger("client-2")
//        messenger2.login(
//            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
//            username = user2,
//            password = passwordUser2,
//        )
//        messenger2.verifyAccountsArePresent(user2)
//        messenger1.verifyAccountsArePresent(user1)
//        log.info { "--- create chat" }
//        val roomId = messenger1.createChatWithUser(user2).roomId
//        messenger2.acceptInvitationToRoom(roomId)
//        delay(2.seconds) // wait for the block information to be distributed
//
//        log.info { "--- login with device 1_1" }
//        messenger1_1 = createTestMatrixMessenger("client-1-1")
//        messenger1_1.login(
//            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
//            username = user1,
//            password = passwordUser1,
//            recoveryKey = recoveryKey,
//        )
//        messenger1_1.getAllRooms(user1) shouldHaveSize 1
//        messenger1_1.findRoomWithId(roomId)
//
//        log.info { "--- login with device 1_2" }
//        messenger1_2 = createTestMatrixMessenger("client-1-2")
//        messenger1_2.login(
//            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
//            username = user1,
//            password = passwordUser1,
//            recoveryKey = recoveryKey,
//        )
//        messenger1_2.getAllRooms(user1) shouldHaveSize 1
//        messenger1_2.findRoomWithId(roomId)
//
//        log.info { "--- login with device 1_3" }
//        messenger1_3 = createTestMatrixMessenger("client-1-3")
//        messenger1_3.login(
//            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
//            username = user1,
//            password = passwordUser1,
//            recoveryKey = recoveryKey,
//        )
//        messenger1_3.getAllRooms(user1) shouldHaveSize 1
//        messenger1_3.findRoomWithId(roomId)
//
//        log.info { "--- login with device 1_4" }
//        messenger1_4 = createTestMatrixMessenger("client-1-4")
//        messenger1_4.login(
//            serverUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}",
//            username = user1,
//            password = passwordUser1,
//            recoveryKey = recoveryKey,
//        )
//        messenger1_4.getAllRooms(user1) shouldHaveSize 1
//        messenger1_4.findRoomWithId(roomId)
//    }
//}

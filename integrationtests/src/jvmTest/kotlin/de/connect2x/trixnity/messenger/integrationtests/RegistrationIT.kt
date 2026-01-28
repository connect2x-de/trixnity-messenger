package de.connect2x.trixnity.messenger.integrationtests

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.messenger.integrationtests.messenger.registerAccountWithToken
import de.connect2x.trixnity.messenger.integrationtests.util.configureTestLogging
import de.connect2x.trixnity.messenger.integrationtests.util.createTestMatrixMessenger
import de.connect2x.trixnity.messenger.integrationtests.util.runBlockingWithTimeout
import de.connect2x.trixnity.messenger.integrationtests.util.synapseDocker
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
@Testcontainers
class RegistrationIT {
    init {
        configureTestLogging()
    }

    private val log: Logger = Logger("de.connect2x.trixnity.messenger.integrationtests.RegistrationIT")
    private lateinit var singleThreadContext: ExecutorCoroutineDispatcher

    @Container
    val synapseDocker = synapseDocker(useRegistrationToken = true)

    @BeforeTest
    fun beforeEach(): Unit = runBlockingWithTimeout {
        singleThreadContext = newSingleThreadContext("main")
        Dispatchers.setMain(singleThreadContext) // this tricks Decompose into accepting a fake UI thread
    }

    @AfterTest
    fun afterEach() {
        singleThreadContext.close()
    }

    @Test
    fun shouldRegisterNewUserWithRegistrationToken(): Unit = runBlockingWithTimeout {
        val baseUrl = "http://${synapseDocker.host}:${synapseDocker.firstMappedPort}"
        val httpClient = HttpClient()
        val accessToken = createAdminAccount(httpClient, baseUrl)

        val body =
            httpClient.post("$baseUrl/_synapse/admin/v1/registration_tokens/new?access_token=$accessToken") {
                contentType(ContentType.Application.Json)
                setBody("{}")
            }.bodyAsText()
        "\"token\":\\s*\"([^\"]*)\"".toRegex().find(body)?.groupValues?.getOrNull(1)?.let { token ->
            log.info { "token: $token" }
            val messenger = createTestMatrixMessenger()
            messenger.registerAccountWithToken(
                serverUrl = baseUrl,
                registrationToken = token
            )
        } ?: throw IllegalStateException(body)
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

    private suspend fun createAdminAccount(httpClient: HttpClient, baseUrl: String): String {
        try {
            val username = "admin"
            val password = "4dm1n"

            val nonceBody = httpClient.get("$baseUrl/_synapse/admin/v1/register").bodyAsText()
            log.info { "nonceBody: $nonceBody" }
            "\"nonce\":\\s*\"([^\"]*)\"".toRegex().find(nonceBody)?.groupValues?.getOrNull(1)?.let { nonce ->
                val macAlgorithm = Mac.getInstance("HmacSHA1")
                macAlgorithm.init(
                    SecretKeySpec(
                        "8XRC-cObB+9MaK+f~n=,9TL&;5+#w6Djp&cI3:FFJay=h4FkHS".encodeToByteArray(),
                        "HmacSHA1"
                    )
                )
                val mac =
                    macAlgorithm.doFinal(nonce.encodeToByteArray() + ByteArray(1) { 0x00 } +
                            username.encodeToByteArray() + ByteArray(1) { 0x00 } +
                            password.encodeToByteArray() + ByteArray(1) { 0x00 } +
                            "admin".encodeToByteArray())
                        .toHex()
                log.info { "mac: $mac" }

                val registrationBody = httpClient.post("$baseUrl/_synapse/admin/v1/register") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                            {
                                "nonce": "$nonce",
                                "username": "$username",
                                "password": "$password",
                                "admin": true,
                                "mac": "$mac"
                            }
                        """.trimIndent()
                    )
                }.bodyAsText()
                "\"access_token\":\\s*\"([^\"]*)\"".toRegex().find(registrationBody)?.groupValues?.getOrNull(1)
                    ?.let { accessToken ->
                        return accessToken
                    } ?: throw IllegalStateException(registrationBody)
            } ?: throw IllegalStateException("something went wrong")
        } catch (exc: Exception) {
            log.error(exc) { "cannot create admin account" }
            throw exc
        }
    }
}

package de.connect2x.trixnity.messenger.compose.view.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.core.model.UserId
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentType.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.intellij.lang.annotations.Language

@OptIn(DelicateCoroutinesApi::class)
object SynapseAdmin {
    private val logger = Logger("de.connect2x.trixnity.messenger.compose.view.util.SynapseAdmin")

    private val synapseClient = synapseClient(engine = platformHttpEngine())
    private var accessToken: String? = null

    private suspend fun adminAccessToken(): String {
        val valAccessToken = accessToken
        if (valAccessToken == null) {
            val adminUsername = "admin"
            val adminPassword = "admin"
            val synapseLoginResponse =
                synapseClient.post("/_matrix/client/v3/login") {
                    contentType(Application.Json)
                    setBody(
                        SynapseLoginWithPassword(
                            type = "m.login.password",
                            username = adminUsername,
                            password = adminPassword,
                        )
                    )
                }
            if (synapseLoginResponse.status.isSuccess()) {
                logger.debug { "successfully authenticated in synapse as admin user" }
                return synapseLoginResponse.body<SynapseLoginResponse>().accessToken.also { accessToken = it }
            } else {
                val bodyMsg = synapseLoginResponse.bodyAsText()
                logger.error { "Could not login as admin; reason: ${synapseLoginResponse.status.value}, $bodyMsg" }
                throw IllegalStateException("Could not login as admin")
            }
        } else {
            return valAccessToken
        }
    }

    /** @return matrixId */
    suspend fun registerNewUser(username: String, password: String): String? {
        val matrixId = "@$username:localhost:8008"
        logger.debug { "mxId: $matrixId" }

        val accessToken = adminAccessToken()

        @Language("JSON") val body = """{"id":"$username","password":"$password"}"""
        logger.debug { "register new user: $body" }

        val response =
            synapseClient.put("/_synapse/admin/v2/users") {
                url { appendEncodedPathSegments(matrixId) }
                header("Authorization", "Bearer $accessToken")
                contentType(Application.Json)
                setBody(body)
            }
        return if (response.status.isSuccess()) {
            logger.info { "created user $matrixId" }
            matrixId
        } else
            run {
                val message = response.bodyAsText()
                logger.error { "cannot create user with $username ($message)" }
                null
            }
    }

    suspend fun deleteUser(userId: UserId) {
        logger.info { "deleting user $userId" }
        val accessToken = adminAccessToken()

        @Language("JSON") val body = """{"erase": true}"""
        val response =
            synapseClient.post("/_synapse/admin/v1/deactivate") {
                url { appendEncodedPathSegments(userId.full) }
                header("Authorization", "Bearer $accessToken") // FIXME save access token
                contentType(Application.Json)
                setBody(body)
            }
        if (response.status.isSuccess()) {
            logger.info { "successfully deleted user $userId" }
        } else {
            val msg = response.bodyAsText()
            logger.warn { "failed to delete user $userId, ${response.status} ($msg)" }
        }
    }

    // Only use for cleanup!
    suspend fun deleteAllTestUsers() {
        @Serializable data class SearchUser(@SerialName("name") val userId: String) // name == userID in this API

        @Serializable data class SearchUsers(val users: List<SearchUser>)

        logger.info { "deleting all test users" }
        val accessToken = adminAccessToken()
        val response =
            synapseClient.get("/_synapse/admin/v2/users?from=0&limit=1000&guests=false") {
                header("Authorization", "Bearer $accessToken")
            }
        val searchUsers = response.body<SearchUsers>()
        searchUsers.users.forEach { user -> deleteUser(UserId(user.userId)) }
    }
}

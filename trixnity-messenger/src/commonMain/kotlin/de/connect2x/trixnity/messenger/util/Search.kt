package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.isValid
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.clientserverapi.model.users.SearchUsers
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.utils.toByteArray

private val log = KotlinLogging.logger { }

interface Search {
    suspend fun searchUsers(
        matrixClient: MatrixClient,
        searchTerm: String,
        limit: Long?,
        filterNot: (userId: UserId) -> Boolean = { false }
    ): List<SearchUserElement>

    interface SearchUserElement {
        val displayName: String
        val initials: String
        val image: ByteArray?
        val userId: UserId
    }

    data class SearchUserElementImpl(
        override val displayName: String,
        override val initials: String,
        override val image: ByteArray? = null,
        override val userId: UserId
    ) : SearchUserElement {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as SearchUserElement

            if (userId != other.userId) return false

            return true
        }

        override fun hashCode(): Int {
            return userId.hashCode()
        }
    }
}

class SearchImpl(
    private val initials: Initials,
    private val i18n: I18n,
) : Search {

    override suspend fun searchUsers(
        matrixClient: MatrixClient,
        searchTerm: String,
        limit: Long?,
        filterNot: (userId: UserId) -> Boolean
    ): List<SearchUserElement> = coroutineScope {
        val userId = UserId(searchTerm)
        val userByUserIdAsync = async {
            if (userId.isValid()) {
                matrixClient.api.user.getProfile(userId).fold(
                    onFailure = { exc ->
                        log.error(exc) { "Cannot access user profile for $userId." }
                        null
                    },
                    onSuccess = { profileResponse ->
                        val image = profileResponse.avatarUrl?.let { url ->
                            matrixClient.media.getThumbnail(url, avatarSize().toLong(), avatarSize().toLong()).fold(
                                onSuccess = { it.toByteArray() },
                                onFailure = { null }
                            )
                        }
                        searchUserElement(
                            SearchUsers.Response.SearchUser(
                                avatarUrl = profileResponse.avatarUrl,
                                displayName = profileResponse.displayName,
                                userId,
                            ), image
                        )
                    })
            } else null
        }
        // TODO this does not search for matrix IDs, see https://github.com/matrix-org/synapse/issues/7588
        val searchUsersAsync = async {
            matrixClient.api.user.searchUsers(searchTerm, i18n.currentLang.code, limit)
                .fold( // TODO get correct language
                    onSuccess = { response ->
                        response.results
                            .filter { searchUser -> searchUser.userId != matrixClient.userId }
                            .filterNot { filterNot(it.userId) }
                            .sortedBy { searchUser -> searchUser.displayName }
                            .map { searchUser ->
                                val image = getImage(matrixClient, searchUser)
                                searchUserElement(searchUser, image)
                            }
                    },
                    onFailure = {
                        log.error(it) { "search for users resulted in error" }
                        emptyList()
                    }
                )
        }

        val user = userByUserIdAsync.await()
        val searchUsers = searchUsersAsync.await()

        return@coroutineScope listOfNotNull(user) + searchUsers
    }

    private suspend fun getImage(matrixClient: MatrixClient, searchUser: SearchUsers.Response.SearchUser): ByteArray? {
        return searchUser.avatarUrl?.let { url ->
            matrixClient.media.getThumbnail(url, avatarSize().toLong(), avatarSize().toLong()).fold(
                onSuccess = { it.toByteArray() },
                onFailure = { null }
            )
        }
    }

    private fun searchUserElement(
        searchUser: SearchUsers.Response.SearchUser,
        image: ByteArray?
    ) = Search.SearchUserElementImpl(
        searchUser.displayName ?: searchUser.userId.full,
        searchUser.displayName?.let { name -> initials.compute(name) }
            ?: initials.compute(searchUser.userId.localpart),
        image,
        searchUser.userId
    )

}

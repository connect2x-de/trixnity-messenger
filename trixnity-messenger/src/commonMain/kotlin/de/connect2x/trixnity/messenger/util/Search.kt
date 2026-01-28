package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.clientserverapi.model.user.SearchUsers
import de.connect2x.trixnity.clientserverapi.model.user.avatarUrl
import de.connect2x.trixnity.clientserverapi.model.user.displayName
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.Presence
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.isValid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

interface Search {
    suspend fun searchUsers(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        searchTerm: String,
        limit: Long?,
    ): List<SearchUserElement>

    interface SearchUserElement {
        val displayName: String
        val initials: String
        val image: ByteArray?
        val userId: UserId
        val presence: StateFlow<Presence?>
    }

    data class SearchUserElementImpl(
        override val displayName: String,
        override val initials: String,
        override val image: ByteArray? = null,
        override val userId: UserId,
        override val presence: StateFlow<Presence?> = MutableStateFlow(null)
    ) : SearchUserElement {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as SearchUserElement

            return userId == other.userId
        }

        override fun hashCode(): Int {
            return userId.hashCode()
        }
    }
}

class SearchImpl(
    private val initials: Initials,
    private val i18n: I18n,
    matrixMessengerConfiguration: MatrixMessengerConfiguration,
) : Search {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.SearchImpl")
    }

    private val maxMediaSizeInMemory = matrixMessengerConfiguration.maxMediaSizeInMemory

    override suspend fun searchUsers(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        searchTerm: String,
        limit: Long?,
    ): List<SearchUserElement> = coroutineScope {
        val userId = UserId(searchTerm)
        if (userId.isValid()) {
            val profile = matrixClient.api.user.getProfile(userId)
                .onFailure { exc ->
                    log.error(exc) { "Cannot access user profile for $userId." }
                }
                .getOrNull()
            val image = profile?.avatarUrl?.let { url ->
                matrixClient.media.getThumbnail(url, avatarSize().toLong(), avatarSize().toLong()).fold(
                    onSuccess = {
                        it.toByteArray(coroutineScope, maxSize = maxMediaSizeInMemory)
                    },
                    onFailure = { null }
                )
            }
            val presence = getPresence(matrixClient, userId)
                .map { presence ->
                    presence ?: matrixClient.api.user.getPresence(userId).getOrNull()?.presence
                }
                .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

            listOf(
                searchUserElement(
                    SearchUsers.Response.SearchUser(
                        avatarUrl = profile?.avatarUrl,
                        displayName = profile?.displayName,
                        userId,
                    ),
                    image,
                    presence
                )
            )
        } else {
            // TODO this does not search for matrix IDs, see https://github.com/matrix-org/synapse/issues/7588
            matrixClient.api.user.searchUsers(searchTerm, i18n.currentLang.code, limit)
                .fold( // TODO get correct language
                    onSuccess = { response ->
                        log.trace { "got users $searchTerm" }
                        response.results
                            .asSequence()
                            .filter { searchUser -> searchUser.userId != matrixClient.userId }
                            .take(limit?.toInt() ?: Int.MAX_VALUE)
                            .map { searchUser ->
                                async {
                                    val image = getImage(coroutineScope, matrixClient, searchUser)
                                    val presence = getPresence(matrixClient, searchUser.userId)
                                        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                                    searchUserElement(searchUser, image, presence)
                                }
                            }
                            .toList()
                            .awaitAll()
                            .also { log.trace { "found users for $searchTerm: $it" } }
                    },
                    onFailure = {
                        log.error(it) { "search for users resulted in error" }
                        emptyList()
                    }
                )
        }
    }

    private suspend fun getImage(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        searchUser: SearchUsers.Response.SearchUser,
    ): ByteArray? {
        return searchUser.avatarUrl?.let { url ->
            matrixClient.media.getThumbnail(url, avatarSize().toLong(), avatarSize().toLong()).fold(
                onSuccess = {
                    it.toByteArray(coroutineScope, maxSize = maxMediaSizeInMemory)
                },
                onFailure = { null }
            )
        }
    }

    private fun getPresence(matrixClient: MatrixClient, userId: UserId): Flow<Presence?> {
        return matrixClient.user.getPresence(userId).map { it?.presence }
    }

    private fun searchUserElement(
        searchUser: SearchUsers.Response.SearchUser,
        image: ByteArray?,
        presence: StateFlow<Presence?>
    ) = Search.SearchUserElementImpl(
        searchUser.displayName ?: searchUser.userId.full,
        searchUser.displayName?.let { name -> initials.compute(name) }
            ?: initials.compute(searchUser.userId.localpart),
        image,
        searchUser.userId,
        presence
    )

}

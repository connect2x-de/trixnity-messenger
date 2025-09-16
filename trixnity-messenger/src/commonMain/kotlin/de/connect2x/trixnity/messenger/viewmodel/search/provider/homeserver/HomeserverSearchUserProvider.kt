package de.connect2x.trixnity.messenger.viewmodel.search.provider.homeserver

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.search.provider.ProviderSearchResult
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.isValid
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.model.users.SearchUsers
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.Presence

private val log = KotlinLogging.logger {}

internal const val HOMESERVER_PROVIDER_ID = "de.connect2x.trixnity.messenger.search.homeserver"
internal const val HOMESERVER_DISPLAY_NAME = "Homeserver"

class HomeserverSearchUserProvider(
    private val initials: Initials,
    private val i18n: I18n,
    private val matrixClients: MatrixClients,
    private val matrixMessengerConfiguration: MatrixMessengerConfiguration,
) : SearchUserProvider {
    override val providerId: String = HOMESERVER_PROVIDER_ID
    override val providerDisplayName: String = HOMESERVER_DISPLAY_NAME // FIXME override in TIM?

    override suspend fun search(
        searchTerm: String,
        activeAccount: UserId,
        coroutineScope: CoroutineScope,
    ): ProviderSearchResult {
        return matrixClients.value[activeAccount]
            ?.let { matrixClient ->
                val maxMediaSizeInMemory = matrixMessengerConfiguration.maxMediaSizeInMemory
                coroutineScope {
                    val userId = UserId(searchTerm)
                    if (userId.isValid()) {
                        searchMxId(matrixClient, userId, coroutineScope, maxMediaSizeInMemory)
                    } else {
                        searchUser(matrixClient, searchTerm, coroutineScope, maxMediaSizeInMemory)
                    }
                }
            }
            ?: run {
                log.error { "No active MatrixClient found. This is something unexpected and should not happen." }
                ProviderSearchResult.Failure("No MatrixClient found.")
            }
    }

    private suspend fun searchMxId(
        matrixClient: MatrixClient,
        userId: UserId,
        coroutineScope: CoroutineScope,
        maxMediaSizeInMemory: Long
    ): ProviderSearchResult.Success {
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

        return ProviderSearchResult.Success(
            listOf(
                HomeserverUserSearchResult(
                    userId = userId,
                    displayName = profile?.displayName ?: "",
                    initials = initials.compute(profile?.displayName ?: userId.full),
                    image = image,
                    presence = presence,
                )
            )
        )
    }

    private suspend fun CoroutineScope.searchUser(
        matrixClient: MatrixClient,
        searchTerm: String,
        coroutineScope: CoroutineScope,
        maxMediaSizeInMemory: Long,
    ): ProviderSearchResult =
        // TODO this does not search for matrix IDs, see https://github.com/matrix-org/synapse/issues/7588
        matrixClient.api.user.searchUsers(searchTerm, i18n.currentLang.code, 100) // FIXME set limit?
            .fold( // TODO get correct language
                onSuccess = { response ->
                    log.trace { "got users $searchTerm" }
                    ProviderSearchResult.Success(
                        response.results
                            .asSequence()
                            .filter { searchUser -> searchUser.userId != matrixClient.userId }
                            //                            .take(limit?.toInt() ?: Int.MAX_VALUE)
                            .map { searchUser ->
                                async {
                                    val image =
                                        getImage(coroutineScope, matrixClient, searchUser, maxMediaSizeInMemory)
                                    val presence = getPresence(matrixClient, searchUser.userId)
                                        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

                                    HomeserverUserSearchResult(
                                        userId = searchUser.userId,
                                        displayName = searchUser.displayName ?: "",
                                        initials = initials.compute(searchUser.displayName ?: searchUser.userId.full),
                                        image = image,
                                        presence = presence
                                    )
                                }
                            }
                            .toList()
                            .awaitAll()
                            .also { log.trace { "found users for $searchTerm: $it" } }
                    )
                },
                onFailure = {
                    log.error(it) { "search for users resulted in error" }
                    ProviderSearchResult.Failure("Error fetching users.")
                }
            )

    private suspend fun getImage(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        searchUser: SearchUsers.Response.SearchUser,
        maxMediaSizeInMemory: Long,
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
}

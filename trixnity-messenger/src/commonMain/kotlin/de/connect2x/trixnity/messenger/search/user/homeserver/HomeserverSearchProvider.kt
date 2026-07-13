package de.connect2x.trixnity.messenger.search.user.homeserver

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.serverDiscovery
import de.connect2x.trixnity.clientserverapi.model.user.avatarUrl
import de.connect2x.trixnity.clientserverapi.model.user.displayName
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.search.provider.SearchFilter
import de.connect2x.trixnity.messenger.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.search.provider.SearchProviderResult
import de.connect2x.trixnity.messenger.search.user.UserSearchContext
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.isValid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

open class HomeserverSearchProvider(
    private val initials: Initials,
    private val i18n: I18n,
    private val matrixClients: MatrixClients,
    private val matrixMessengerConfiguration: MatrixMessengerConfiguration,
) : SearchProvider<HomeserverUserSearchResult, UserSearchContext> {
    private val log =
        Logger("de.connect2x.trixnity.messenger.viewmodel.search.provider.homeserver.HomeserverSearchProvider")

    override val key: Key = Key

    companion object Key : SearchProvider.Key<HomeserverSearchProvider>

    override val displayName: String = i18n.userSearchHomeserver()
    override val priority: Int = 100
    override val disabledByDefault: Boolean = false

    override val supportedFilters: List<SearchFilter.Key<*>> = emptyList()

    override suspend fun search(
        searchTerm: String,
        filters: List<SearchFilter>,
        searchContext: UserSearchContext,
        coroutineScope: CoroutineScope,
    ): SearchProviderResult<HomeserverUserSearchResult> {
        return matrixClients.value[searchContext.activeAccount]?.let { matrixClient ->
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
                SearchProviderResult.Failure("No MatrixClient found.")
            }
    }

    private suspend fun searchMxId(
        matrixClient: MatrixClient,
        userId: UserId,
        coroutineScope: CoroutineScope,
        maxMediaSizeInMemory: Long,
    ): SearchProviderResult<HomeserverUserSearchResult> {
        userId.domain
            .serverDiscovery(
                httpClientEngine = matrixMessengerConfiguration.httpClientEngine,
                httpClientConfig = matrixMessengerConfiguration.httpClientConfig,
            )
            .onFailure {
                return SearchProviderResult.Failure(i18n.searchUserIdHomeserverNotValid(userId.domain))
            }

        val profile =
            matrixClient.api.user
                .getProfile(userId)
                .onFailure { exc -> log.error(exc) { "Cannot access user profile for $userId." } }
                .getOrNull()

        return SearchProviderResult.Success(
            listOf(
                HomeserverUserSearchResult(
                    userId = userId,
                    displayName = profile?.displayName ?: "",
                    initials = initials.compute(profile?.displayName ?: userId.full),
                    image = getImage(profile?.avatarUrl, matrixClient, maxMediaSizeInMemory, coroutineScope),
                    presence = getPresence(userId, matrixClient, coroutineScope),
                )
            )
        )
    }

    private suspend fun searchUser(
        matrixClient: MatrixClient,
        searchTerm: String,
        coroutineScope: CoroutineScope,
        maxMediaSizeInMemory: Long,
    ): SearchProviderResult<HomeserverUserSearchResult> = coroutineScope {
        // TODO this does not search for matrix IDs, see https://github.com/matrix-org/synapse/issues/7588
        matrixClient.api.user
            .searchUsers(searchTerm, i18n.currentLang.code, 100) // FIXME set limit?
            .fold(
                // TODO get correct language
                onSuccess = { response ->
                    log.trace { "got users $searchTerm" }
                    SearchProviderResult.Success(
                        response.results
                            .asSequence()
                            .filter { searchUser -> searchUser.userId != matrixClient.userId }
                            .map { searchUser ->
                                async {
                                    HomeserverUserSearchResult(
                                        userId = searchUser.userId,
                                        displayName = searchUser.displayName ?: "",
                                        initials = initials.compute(searchUser.displayName ?: searchUser.userId.full),
                                        image =
                                            getImage(
                                                searchUser.avatarUrl,
                                                matrixClient,
                                                maxMediaSizeInMemory,
                                                coroutineScope,
                                            ),
                                        presence = getPresence(searchUser.userId, matrixClient, coroutineScope),
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
                    SearchProviderResult.Failure("Error fetching users.")
                },
            )
    }
}

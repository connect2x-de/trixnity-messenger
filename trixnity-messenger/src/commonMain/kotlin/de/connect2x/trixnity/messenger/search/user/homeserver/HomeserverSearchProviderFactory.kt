package de.connect2x.trixnity.messenger.search.user.homeserver

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.search.provider.SearchProviderFactory
import de.connect2x.trixnity.messenger.search.user.UserSearchContext
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import kotlin.reflect.KClass

class HomeserverSearchProviderFactory(
    private val initials: Initials,
    private val i18n: I18n,
    private val matrixClients: MatrixClients,
    private val matrixMessengerConfiguration: MatrixMessengerConfiguration,
) : SearchProviderFactory<HomeserverUserSearchResult, UserSearchContext> {
    override val supports: KClass<UserSearchContext> = UserSearchContext::class

    override fun create(account: UserId): SearchProvider<HomeserverUserSearchResult, UserSearchContext>? {
        return HomeserverSearchProvider(initials, i18n, matrixClients, matrixMessengerConfiguration)
    }
}

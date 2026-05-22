package de.connect2x.trixnity.messenger.compose.view.roomlist.search

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import kotlin.reflect.KClass

interface SearchUserProviderSettingsView<S : SearchUserProvider> {
    val supports: KClass<out S>

    @Composable fun create(searchUserProvider: SearchUserProvider)
}

object EmptySearchUserProviderSettingsView : SearchUserProviderSettingsView<SearchUserProvider> {
    override val supports: KClass<SearchUserProvider>
        get() = SearchUserProvider::class

    @Composable override fun create(searchUserProvider: SearchUserProvider) {}
}

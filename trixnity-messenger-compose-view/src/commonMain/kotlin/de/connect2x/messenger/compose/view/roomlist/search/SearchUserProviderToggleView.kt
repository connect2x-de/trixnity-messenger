package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import kotlin.reflect.KClass

interface SearchUserProviderToggleView<S : SearchUserProvider> {
    val supports: KClass<out S>

    @Composable
    fun create(searchUserProvider: SearchUserProvider, active: Boolean, setActive: () -> Unit)
}

object EmptySearchUserProviderToggleView : SearchUserProviderToggleView<SearchUserProvider> {
    override val supports: KClass<SearchUserProvider>
        get() = SearchUserProvider::class

    @Composable
    override fun create(searchUserProvider: SearchUserProvider, active: Boolean, setActive: () -> Unit) {
    }
}

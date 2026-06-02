package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchProvider
import kotlin.reflect.KClass

interface SearchUserProviderToggleView<S : SearchProvider<*>> {
    val supports: KClass<out S>

    @Composable
    fun create(
        searchProvider: SearchProvider<*>,
        providerSearchCanBeActivated: Boolean,
        active: Boolean,
        setActive: () -> Unit,
    )
}

object EmptySearchUserProviderToggleView : SearchUserProviderToggleView<SearchProvider<*>> {
    override val supports: KClass<SearchProvider<*>>
        get() = SearchProvider::class

    @Composable
    override fun create(
        searchProvider: SearchProvider<*>,
        providerSearchCanBeActivated: Boolean,
        active: Boolean,
        setActive: () -> Unit,
    ) {}
}

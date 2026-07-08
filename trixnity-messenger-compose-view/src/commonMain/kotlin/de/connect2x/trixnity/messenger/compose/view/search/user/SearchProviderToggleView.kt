package de.connect2x.trixnity.messenger.compose.view.search.user

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.search.provider.SearchProvider
import kotlin.reflect.KClass

interface SearchProviderToggleView<S : SearchProvider<*, *>> {
    val supports: KClass<out S>

    @Composable
    fun create(
        searchProvider: SearchProvider<*, *>,
        providerSearchCanBeEnabled: Boolean,
        enabled: Boolean,
        setEnabled: () -> Unit,
    )
}

object EmptySearchProviderToggleView : SearchProviderToggleView<SearchProvider<*, *>> {
    override val supports: KClass<SearchProvider<*, *>>
        get() = SearchProvider::class

    @Composable
    override fun create(
        searchProvider: SearchProvider<*, *>,
        providerSearchCanBeEnabled: Boolean,
        enabled: Boolean,
        setEnabled: () -> Unit,
    ) {}
}

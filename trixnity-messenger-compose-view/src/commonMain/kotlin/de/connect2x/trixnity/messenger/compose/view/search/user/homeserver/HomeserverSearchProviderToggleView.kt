package de.connect2x.trixnity.messenger.compose.view.search.user.homeserver

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.search.user.SearchProviderToggleView
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedFilterChip
import de.connect2x.trixnity.messenger.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.search.user.homeserver.HomeserverSearchProvider
import kotlin.reflect.KClass

class HomeserverSearchProviderToggleView : SearchProviderToggleView<HomeserverSearchProvider> {
    override val supports: KClass<out HomeserverSearchProvider> = HomeserverSearchProvider::class

    @Composable
    override fun create(
        searchProvider: SearchProvider<*, *>,
        providerSearchCanBeEnabled: Boolean,
        enabled: Boolean,
        setEnabled: () -> Unit,
    ) {
        ThemedFilterChip(
            selected = enabled,
            onClick = setEnabled,
            label = { Text(searchProvider.displayName) },
            trailingIcon = { Icon(Icons.Outlined.Home, contentDescription = null) },
            leadingIcon = {
                if (enabled) {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            },
            enabled = providerSearchCanBeEnabled,
        )
    }
}

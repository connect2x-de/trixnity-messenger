package de.connect2x.trixnity.messenger.compose.view.search.homeserver

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.common.icons.HomeHealth
import de.connect2x.trixnity.messenger.compose.view.search.SearchUserProviderToggleView
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedFilterChip
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchUserProvider
import de.connect2x.trixnity.messenger.viewmodel.search.provider.homeserver.HomeserverSearchUserProvider
import kotlin.reflect.KClass

class HomeserverSearchProviderToggleView : SearchUserProviderToggleView<HomeserverSearchUserProvider> {
    override val supports: KClass<out HomeserverSearchUserProvider> = HomeserverSearchUserProvider::class

    @Composable
    override fun create(
        searchUserProvider: SearchUserProvider,
        providerSearchCanBeActivated: Boolean,
        active: Boolean,
        setActive: () -> Unit,
    ) {
        ThemedFilterChip(
            selected = active,
            onClick = setActive,
            label = { Text(searchUserProvider.providerDisplayName) },
            trailingIcon = { Icon(HomeHealth, contentDescription = null) },
            leadingIcon = {
                if (active) {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            },
            enabled = providerSearchCanBeActivated,
        )
    }
}

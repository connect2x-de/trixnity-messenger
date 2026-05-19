package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.viewmodel.search.SearchUserViewModel

fun LazyListScope.searchTerm(searchUserViewModel: SearchUserViewModel, onProviderSettingsClicked: () -> Unit) {
    stickyHeader("searchTerm") {
        val providerSettings = searchUserViewModel.providerSettings.collectAsState().value

        Surface(
            Modifier.fillMaxWidth()

        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UserSearchFieldNewSearch(searchUserViewModel)

                    if (searchUserViewModel.searchUserProviders.any { searchUserProvider -> searchUserProvider.settings.isNotEmpty() }) {
                        ThemedIconButton(onClick = onProviderSettingsClicked) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                }
                SmallSpacer()
                providerSettings?.let { settings ->
                    if (settings.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            SmallSpacer()
                            Text(settings, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

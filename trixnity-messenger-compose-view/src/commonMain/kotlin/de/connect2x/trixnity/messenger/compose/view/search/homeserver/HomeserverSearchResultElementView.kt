package de.connect2x.trixnity.messenger.compose.view.search.homeserver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.common.icons.HomeHealth
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.search.SearchProviderIcon
import de.connect2x.trixnity.messenger.compose.view.theme.components.AvatarPresenceBadge
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.viewmodel.search.provider.homeserver.HomeserverUserSearchResult

interface HomeserverSearchResultElementView {
    @Composable
    fun create(
        userSearchResult: HomeserverUserSearchResult,
        showOrigin: Boolean,
        onClick: (HomeserverUserSearchResult) -> Unit,
    )
}

@Composable
fun HomeserverSearchResultElement(
    userSearchResult: HomeserverUserSearchResult,
    showOrigin: Boolean,
    onClick: (HomeserverUserSearchResult) -> Unit,
) {
    DI.get<HomeserverSearchResultElementView>().create(userSearchResult, showOrigin, onClick)
}

class HomeserverSearchResultElementViewImpl : HomeserverSearchResultElementView {
    @Composable
    override fun create(
        userSearchResult: HomeserverUserSearchResult,
        showOrigin: Boolean,
        onClick: (HomeserverUserSearchResult) -> Unit,
    ) {
        val image by userSearchResult.image.collectAsState()
        val presence by userSearchResult.presence.collectAsState()

        Box(Modifier.fillMaxWidth().clickable(onClick = { onClick(userSearchResult) }).buttonPointerModifier()) {
            Row(
                Modifier.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box {
                    Box(Modifier.align(Alignment.Center).padding(top = 10.dp, bottom = 10.dp, end = 10.dp)) {
                        ThemedUserAvatar(userSearchResult.initials, image) { AvatarPresenceBadge(presence) }
                    }
                    if (showOrigin) {
                        SearchProviderIcon(modifier = Modifier.align(Alignment.TopEnd)) {
                            Icon(HomeHealth, contentDescription = "eigene Organisation")
                        }
                    }
                }
                Column(Modifier.weight(1f, true)) {
                    Text(
                        userSearchResult.displayName,
                        maxLines = 2,
                        style = MaterialTheme.typography.labelLarge,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        userSearchResult.userId.full,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

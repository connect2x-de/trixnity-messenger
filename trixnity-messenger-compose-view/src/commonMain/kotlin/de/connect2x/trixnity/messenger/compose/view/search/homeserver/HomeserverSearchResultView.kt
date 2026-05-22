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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.common.icons.HomeHealth
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.search.SearchProviderIcon
import de.connect2x.trixnity.messenger.compose.view.search.SearchResultView
import de.connect2x.trixnity.messenger.compose.view.theme.components.AvatarPresenceBadge
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.viewmodel.search.provider.homeserver.HomeserverUserSearchResult
import kotlin.reflect.KClass

// FIXME make extensible
class HomeserverSearchResultView : SearchResultView<HomeserverUserSearchResult> {
    override val supports: KClass<out HomeserverUserSearchResult> = HomeserverUserSearchResult::class

    @Composable
    override fun create(
        userSearchResult: HomeserverUserSearchResult,
        showOrigin: Boolean,
        onClick: (HomeserverUserSearchResult) -> Unit,
    ) {
        val image by userSearchResult.image.collectAsState()

        val presence = userSearchResult.presence.collectAsState().value
        val expanded = remember { mutableStateOf(false) }

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
                        maxLines = if (expanded.value) 3 else 1,
                        style = MaterialTheme.typography.labelLarge,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        userSearchResult.userId.full,
                        maxLines = if (expanded.value) 3 else 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

package de.connect2x.messenger.compose.view.roomlist.search.homeserver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.roomlist.search.SearchResultView
import de.connect2x.messenger.compose.view.theme.components.AvatarPresenceBadge
import de.connect2x.messenger.compose.view.theme.components.ThemedInfoChip
import de.connect2x.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.viewmodel.search.provider.homeserver.HomeserverUserSearchResult
import kotlin.reflect.KClass

// FIXME make extensible
class HomeserverSearchResultView : SearchResultView<HomeserverUserSearchResult> {
    override val supports: KClass<out HomeserverUserSearchResult> = HomeserverUserSearchResult::class

    @Composable
    override fun create(userSearchResult: HomeserverUserSearchResult, onClick: (HomeserverUserSearchResult) -> Unit) {
        val presence = userSearchResult.presence.collectAsState().value

        Box(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = { onClick(userSearchResult) })
                .buttonPointerModifier()
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemedUserAvatar(userSearchResult.initials, userSearchResult.image) {
                    AvatarPresenceBadge(presence)
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f, true)) {
                    Text(
                        userSearchResult.displayName,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelLarge,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        userSearchResult.userId.full,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                ThemedInfoChip(label = { Text("Homeserver") })
            }
        }
    }
}

package de.connect2x.messenger.compose.view.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components.AvatarPresenceBadge
import de.connect2x.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement

interface UserSearchResultListView {

    // this function is no @Composable as it is used inside a LazyListScope
    fun create(
        scope: LazyListScope,
        state: SearchResultState,
        userClickReaction: (SearchUserElement) -> Unit,
    )
}

class UserSearchResultListViewImpl : UserSearchResultListView {
    override fun create(
        scope: LazyListScope,
        state: SearchResultState,
        userClickReaction: (SearchUserElement) -> Unit,
    ) {
        with(scope) {
            when (state) {
                SearchResultState.Loading ->
                    item(key = "users-loading") {
                        Box(
                            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingSpinner()
                        }
                    }

                SearchResultState.Placeholder ->
                    item(key = "users-placeholder") {
                        val i18n = DI.get<I18nView>()
                        Box(
                            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = i18n.userSearchSearchPeople(),
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                is SearchResultState.Results -> {
                    if (state.users.isEmpty()) {
                        item(key = "users-notfound") {
                            val i18n = DI.get<I18nView>()
                            Box(
                                Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = i18n.userSearchNotFound(),
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        items(state.users, key = { it.userId.toString() }) { user ->
                            UserElement(user, onClick = { userClickReaction(user) })
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun UserElement(
    user: SearchUserElement,
    onClick: () -> Unit
) {
    val presence by user.presence.collectAsState()

    Box(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .buttonPointerModifier()
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemedUserAvatar(user.initials, user.image) {
                AvatarPresenceBadge(presence)
            }
            Spacer(Modifier.size(10.dp))
            Column {
                Text(
                    user.displayName,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelLarge,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    user.userId.full,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
    HorizontalDivider(Modifier.fillMaxWidth().width(1.dp).padding(horizontal = 10.dp))
}

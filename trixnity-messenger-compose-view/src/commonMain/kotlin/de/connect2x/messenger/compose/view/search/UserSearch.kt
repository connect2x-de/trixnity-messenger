package de.connect2x.messenger.compose.view.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.UserSearchHandler

fun LazyListScope.SearchUsersLocally(
    searchHandler: UserSearchHandler,
    onUserClick: (Search.SearchUserElement) -> Unit,
) {
    stickyHeader { Box(Modifier.background(MaterialTheme.colorScheme.background)) { UserSearchField(searchHandler) } }
    item {
        UserSearchResultList(searchHandler, onUserClick)
    }
}


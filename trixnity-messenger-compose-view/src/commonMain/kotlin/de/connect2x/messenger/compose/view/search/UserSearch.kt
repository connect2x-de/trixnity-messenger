package de.connect2x.messenger.compose.view.search

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.UserSearchHandler

@Composable
fun ColumnScope.SearchUsersLocally(
    searchHandler: UserSearchHandler,
    shouldScroll: Boolean = true,
    onUserClick: suspend (Search.SearchUserElement) -> Unit,
) {
    UserSearchField(searchHandler)
    UserSearchResultList(searchHandler, shouldScroll) { searchUserElement ->
        onUserClick(searchUserElement)
    }
}


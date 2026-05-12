package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import de.connect2x.trixnity.messenger.util.UserSearchHandler
import kotlinx.coroutines.flow.map

@Composable
fun collectUserSearchResult(
    userSearchHandler: UserSearchHandler,
): SearchResultState {
    val users = userSearchHandler.foundUsers.collectAsState().value
    val waitForResults = userSearchHandler.waitForUserResults.collectAsState().value
    val searchWasApplied =
        remember { userSearchHandler.searchTerm.map { it.text.isNotBlank() } }.collectAsState(false).value
    return when {
        waitForResults -> SearchResultState.Loading
        users.isEmpty() && !searchWasApplied -> SearchResultState.Placeholder
        else -> SearchResultState.Results(users)
    }
}

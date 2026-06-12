package de.connect2x.trixnity.messenger.compose.view.search.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.theme.components.LocalSurfaceStyle
import de.connect2x.trixnity.messenger.search.user.UserSearchResult
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModel

@Composable
fun SearchUsers(
    userSearchViewModel: UserSearchViewModel,
    onUserClick: (UserSearchResult) -> Unit,
    aboveSearch: @Composable LazyItemScope.() -> Unit,
) {
    val searchResultList by userSearchViewModel.searchResultList.collectAsState()
    val noResultsFound by userSearchViewModel.noResultsFound.collectAsState()

    val listState = rememberLazyListState()
    val focusedItem: MutableState<String?> =
        remember(searchResultList) { mutableStateOf(searchResultList.firstOrNull()?.id) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.rovingFocusContainer(
                    listState = listState,
                    focusedItem = focusedItem,
                    ignoredKeys = listOf("aboveSearch", "searchTerm"),
                )
                .semantics { collectionInfo = CollectionInfo(rowCount = searchResultList.size, columnCount = 0) },
            state = listState,
        ) {
            item(key = "aboveSearch") { aboveSearch() }
            stickyHeader("searchTerm") {
                val containerColor = LocalSurfaceStyle.current?.color
                Column((if (containerColor != null) Modifier.background(containerColor) else Modifier)) {
                    SearchTerm(userSearchViewModel)
                }
            }
            searchResults(
                searchProviders = userSearchViewModel.searchProviders,
                onUserClick = onUserClick,
                searchResultList = searchResultList,
                noResultsFound = noResultsFound,
                focusedItem = focusedItem,
            )
        }
        VerticalScrollbar(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            lazyListState = listState,
            reverseLayout = false,
        )
    }
}

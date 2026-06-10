package de.connect2x.trixnity.messenger.compose.view.search.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.trixnity.messenger.search.user.UserSearchResult

interface SearchResultViewFactorySelector<T> {
    @Composable fun rememberFactory(element: UserSearchResult): T = remember(element) { selectFactory(element) }

    fun selectFactory(element: UserSearchResult): T
}

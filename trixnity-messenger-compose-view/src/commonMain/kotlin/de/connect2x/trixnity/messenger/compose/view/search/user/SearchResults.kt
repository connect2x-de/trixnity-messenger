package de.connect2x.trixnity.messenger.compose.view.search.user

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.modifier.focusHighlighting
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.search.provider.SearchProvider
import de.connect2x.trixnity.messenger.search.user.UserSearchResult

fun LazyListScope.searchResults(
    searchProviders: List<SearchProvider<*, *>>,
    onUserClick: (UserSearchResult) -> Unit,
    searchResultList: List<UserSearchResult>,
    noResultsFound: Boolean?,
    focusedItem: MutableState<String?>,
) {
    if (noResultsFound == true) {
        item(key = "noResultsFound") {
            Box(
                Modifier.padding(horizontal = 10.dp, vertical = 20.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val i18n = DI.get<I18nView>()
                Text(i18n.searchUserNoResultsFound())
            }
        }
    } else {
        itemsIndexed(searchResultList, { _, element -> element.id }) { index, userSearchResult ->
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                Modifier.padding(horizontal = 10.dp)
                    .rovingFocusItem(
                        isFocused = { focusedItem.value == userSearchResult.id },
                        onFocus = { focusedItem.value = userSearchResult.id },
                    )
                    .focusHighlighting(interactionSource)
            ) {
                SearchResultSelector(
                    userSearchResult = userSearchResult,
                    showOrigin = searchProviders.size > 1,
                    index = index,
                    interactionSource = interactionSource,
                    onClick = onUserClick,
                )
            }
        }
    }
}

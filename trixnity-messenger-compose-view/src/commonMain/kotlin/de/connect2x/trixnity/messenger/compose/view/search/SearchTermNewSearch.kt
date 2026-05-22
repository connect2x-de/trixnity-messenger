package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.viewmodel.search.SearchUserViewModel

fun LazyListScope.searchTerm(searchUserViewModel: SearchUserViewModel) {
    stickyHeader("searchTerm") {
        Surface(Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)) {
                UserSearchFieldNewSearch(searchUserViewModel)
                SearchTermFilterSettings(searchUserViewModel)
                SearchUserProviderToggles(searchUserViewModel)
            }
        }
    }
}

package de.connect2x.trixnity.messenger.compose.view.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.viewmodel.search.SearchUserViewModel

@Composable
fun SearchTerm(searchUserViewModel: SearchUserViewModel) {
    Box(Modifier.fillMaxWidth()) {
        // since the result list might be drawing behind the search and in every padding we set, we have to paint
        // the background with the current background color to ensure no visual glitches
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            UserSearchFieldNewSearch(searchUserViewModel)
            SearchUserProviderToggles(searchUserViewModel)
            SearchTermFilterSettings(searchUserViewModel)
        }
    }
}

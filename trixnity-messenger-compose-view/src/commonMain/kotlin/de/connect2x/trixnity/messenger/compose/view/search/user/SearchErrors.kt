package de.connect2x.trixnity.messenger.compose.view.search.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.roomlist.header.Banner
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModel

@Composable
fun SearchErrors(userSearchViewModel: UserSearchViewModel) {
    val errors by userSearchViewModel.errors.collectAsState()

    Banner(MaterialTheme.components.errorBanner, visible = errors.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            errors.forEach { error -> Text("${error.key}: ${error.value}") }
        }
    }
}

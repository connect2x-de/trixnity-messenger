package de.connect2x.trixnity.messenger.compose.view.search.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModel

interface UserSearchFieldNewSearchView {
    @Composable fun create(userSearchViewModel: UserSearchViewModel)
}

@Composable
fun UserSearchFieldNewSearch(userSearchViewModel: UserSearchViewModel) {
    with(DI.get<UserSearchFieldNewSearchView>()) { create(userSearchViewModel) }
}

class UserSearchFieldNewSearchViewImpl : UserSearchFieldNewSearchView {
    @Composable
    override fun create(userSearchViewModel: UserSearchViewModel) {
        val i18n = DI.get<I18nView>()

        var userSearchTerm by userSearchViewModel.searchTerm.collectAsTextFieldValueState()
        val isSearching by userSearchViewModel.isSearching.collectAsState()

        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        OutlinedTextField(
            value = userSearchTerm,
            onValueChange = { userSearchTerm = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            leadingIcon = {
                if (isSearching) {
                    ThemedProgressIndicator(Modifier, MaterialTheme.components.smallCircularProgressIndicator)
                } else {
                    Icon(Icons.Default.Search, i18n.userSearchSearchPeople())
                }
            },
            trailingIcon = {
                if (userSearchTerm.text.isNotBlank())
                    Icon(
                        Icons.Default.Clear,
                        i18n.commonDelete(),
                        Modifier.clickable { userSearchTerm = TextFieldValue("") }.buttonPointerModifier(),
                    )
            },
            label = { Text(i18n.userSearchNameOrMatrixId()) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
            maxLines = 1,
        )
    }
}

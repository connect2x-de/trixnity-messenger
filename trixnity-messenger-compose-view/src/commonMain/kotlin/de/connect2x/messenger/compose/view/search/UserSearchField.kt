package de.connect2x.messenger.compose.view.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.collectAsStateForTextField
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.UserSearchHandler

// TODO TIM
interface UserSearchFieldView {
    @Composable
    fun create(userSearchHandler: UserSearchHandler)
}

@Composable
fun UserSearchField(userSearchHandler: UserSearchHandler) {
    DI.current.get<UserSearchFieldView>().create(userSearchHandler)
}

class UserSearchFieldViewImpl : UserSearchFieldView {
    @Composable
    override fun create(userSearchHandler: UserSearchHandler) {
        val i18n = DI.current.get<I18nView>()
        val userSearchTerm = userSearchHandler.searchTerm.collectAsStateForTextField().value

        OutlinedTextField(
            value = userSearchTerm,
            onValueChange = { userSearchHandler.searchTerm.value = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 20.dp),
            leadingIcon = { Icon(Icons.Default.Search, i18n.userSearchSearchPeople()) },
            trailingIcon = {
                if (userSearchTerm.isNotBlank()) Icon(
                    Icons.Default.Clear,
                    i18n.commonDelete(),
                    Modifier.clickable { userSearchHandler.searchTerm.value = "" }.buttonPointerModifier()
                )
            },
            placeholder = { Text(i18n.userSearchNameOrMatrixId()) },
            keyboardOptions = KeyboardOptions(autoCorrect = false)
        )
    }
}
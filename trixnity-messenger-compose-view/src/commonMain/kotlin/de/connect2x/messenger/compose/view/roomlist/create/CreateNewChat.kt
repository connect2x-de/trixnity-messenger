package de.connect2x.messenger.compose.view.roomlist.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.MoreInfo
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.roomlist.search.SearchUsersView
import de.connect2x.messenger.compose.view.search.UserSearchResultListView
import de.connect2x.messenger.compose.view.search.collectUserSearchResult
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.AvatarContentIcon
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedAvatar
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize

interface CreateNewChatView {
    @Composable
    fun create(createNewChatViewModel: CreateNewChatViewModel)
}

@Composable
fun CreateNewChat(createNewChatViewModel: CreateNewChatViewModel) {
    DI.get<CreateNewChatView>().create(createNewChatViewModel)
}

class CreateNewChatViewImpl : CreateNewChatView {
    @Composable
    override fun create(createNewChatViewModel: CreateNewChatViewModel) {
        val i18n = DI.get<I18nView>()
        val isCreating by createNewChatViewModel.isCreating.collectAsState()
        val error = createNewChatViewModel.error.collectAsState().value
        val errorDetails = createNewChatViewModel.errorDetails.collectAsState().value
        val searchUsersView = DI.get<SearchUsersView>()
        val userSearchResultView = DI.get<UserSearchResultListView>()
        val searchUsersResults = collectUserSearchResult(createNewChatViewModel.createNewRoomViewModel.searchHandler)

        Box(Modifier.fillMaxSize()) {
            Box {
                if (error != null) {
                    ThemedModalDialog({ createNewChatViewModel.errorDismiss() }) {
                        ModalDialogHeader {
                            Text(i18n.anErrorHasOccurred())
                        }
                        ModalDialogContent {
                            Text(error)
                            if (errorDetails != null) {
                                MoreInfo(
                                    title = i18n.errorDetails(),
                                ) {
                                    Text(errorDetails, modifier = Modifier.padding(20.dp))
                                }
                            }
                        }
                        ModalDialogFooter {
                            ThemedButton(
                                style = MaterialTheme.components.primaryButton,
                                onClick = { createNewChatViewModel.errorDismiss() },
                            ) {
                                Text(i18n.actionOk())
                            }
                        }
                    }
                }

                Column {
                    Header(createNewChatViewModel::cancel, i18n.createNewChatTitle())
                    LazyColumn {
                        item(key = "CreatingIndicator") {
                            if (isCreating) {
                                ThemedProgressIndicator(
                                    Modifier.fillMaxWidth(),
                                    MaterialTheme.components.linearProgressIndicator
                                )
                            }
                        }
                        item(key = "AddOrSearchGroup") {
                            AddOrSearchGroup(createNewChatViewModel)
                            HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
                        }
                        searchUsersView.create(
                            createNewChatViewModel.createNewRoomViewModel,
                            createNewChatViewModel::onUserClick,
                            searchUsersResults,
                            userSearchResultView,
                            this,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddOrSearchGroup(createNewChatViewModel: CreateNewChatViewModel) {
    val i18n = DI.get<I18nView>()
    val isCreating by createNewChatViewModel.isCreating.collectAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.weight(1.0f, fill = true)
                .clickable(enabled = !isCreating) { createNewChatViewModel.createGroup() }
                .buttonPointerModifier()
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemedAvatar(avatarSize().dp) {
                    AvatarContentIcon(Icons.Default.GroupAdd, avatarSize().dp)
                }
                Spacer(Modifier.size(20.dp))
                Text(i18n.createNewGroupCreate())
            }
        }
        Spacer(Modifier.size(20.dp))
        Box(
            Modifier.weight(1.0f, fill = true)
                .clickable { createNewChatViewModel.searchGroup() }
                .buttonPointerModifier()
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemedAvatar(avatarSize().dp) {
                    AvatarContentIcon(Icons.Default.TravelExplore, avatarSize().dp)
                }
                Spacer(Modifier.size(20.dp))
                Text(i18n.createNewGroupSearch())
            }
        }
    }
}

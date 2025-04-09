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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import de.connect2x.messenger.compose.view.common.AvatarWithImage
import de.connect2x.messenger.compose.view.common.ErrorDialog
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.roomlist.search.SearchUsers
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModel

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
        val error = createNewChatViewModel.error.collectAsState()

        Box(Modifier.fillMaxSize()) {
            Box {
                if (error.value != null) {
                    ErrorDialog(error.value.orEmpty(), { createNewChatViewModel.errorDismiss() })
                }

                Column {
                    Header(createNewChatViewModel::cancel, i18n.createNewChatTitle())
                    if (isCreating) {
                        ThemedProgressIndicator(
                            Modifier.fillMaxWidth(),
                            MaterialTheme.components.linearProgressIndicator
                        )
                    }
                    AddOrSearchGroup(createNewChatViewModel)
                    HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
                    SearchUsers(
                        createNewChatViewModel.createNewRoomViewModel,
                        onUserClick = createNewChatViewModel::onUserClick
                    )
                }
            }
        }
    }
}

@Composable
fun AddOrSearchGroup(createNewChatViewModel: CreateNewChatViewModel) {
    val i18n = DI.get<I18nView>()
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.weight(1.0f, fill = true)
            .clickable { createNewChatViewModel.createGroup() }
            .buttonPointerModifier()
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarWithImage { Icon(Icons.Default.GroupAdd, i18n.createNewGroupCreate()) }
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
                AvatarWithImage { Icon(Icons.Default.TravelExplore, i18n.createNewGroupSearch()) }
                Spacer(Modifier.size(20.dp))
                Text(i18n.createNewGroupSearch())
            }
        }
    }
}

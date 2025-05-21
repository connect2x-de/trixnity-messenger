package de.connect2x.messenger.compose.view.roomlist.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.common.VerticalGrid
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.util.UserSearchHandler
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel

interface UsersInGroupView {
    @Composable
    fun create(userSearchHandler: UserSearchHandler)
}

@Composable
fun UsersInGroup(userSearchHandler: UserSearchHandler) {
    DI.get<UsersInGroupView>().create(userSearchHandler)
}

@Composable
fun UsersInGroup(createNewGroupViewModel: CreateNewGroupViewModel) {
    DI.get<UsersInGroupView>().create(createNewGroupViewModel.createNewRoomViewModel.searchHandler)
}

class UsersInGroupViewImpl : UsersInGroupView {
    @Composable
    override fun create(userSearchHandler: UserSearchHandler) {
        val i18n = DI.get<I18nView>()
        val isMobile = Platform.current.isMobile
        val selectedUsers = userSearchHandler.selectedUsers.collectAsState()
        if (selectedUsers.value.isNotEmpty()) {
            Box(Modifier.padding(horizontal = 10.dp, vertical = 15.dp)) {
                VerticalGrid(spacing = 10.dp) {
                    selectedUsers.value.map { groupUser ->
                        Column(
                            Modifier.requiredWidth(60.dp)
                                .then(if (isMobile) Modifier.clickable {
                                    userSearchHandler.unselectUser(groupUser)
                                } else Modifier),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ThemedUserAvatar(groupUser.initials, groupUser.image) {
                                Tooltip({ Text(i18n.commonRemove() )}) {
                                    ThemedIconButton(
                                        style = MaterialTheme.components.primaryIconButton,
                                        size = 15.dp,
                                        onClick = { userSearchHandler.unselectUser(groupUser) }
                                    ) {
                                        Icon(Icons.Default.Close, i18n.commonRemove())
                                    }
                                }
                            }
                            Text(groupUser.displayName, maxLines = 3, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.fillMaxWidth().width(1.dp).padding(horizontal = 10.dp))
        }
    }
}

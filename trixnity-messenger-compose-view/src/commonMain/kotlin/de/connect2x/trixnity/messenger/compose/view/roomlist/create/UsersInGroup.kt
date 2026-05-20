package de.connect2x.trixnity.messenger.compose.view.roomlist.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.HorizontalScrollbar
import de.connect2x.trixnity.messenger.compose.view.Platform
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.isMobile
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.util.UserSearchHandler
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel

interface UsersInGroupView {
    @Composable fun create(userSearchHandler: UserSearchHandler)
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
        val scrollState = rememberScrollState()
        val isMobile = Platform.current.isMobile
        val selectedUsers = userSearchHandler.selectedUsers.collectAsState()
        if (selectedUsers.value.isNotEmpty()) {
            Box {
                Box(Modifier.padding(horizontal = 10.dp, vertical = 15.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        selectedUsers.value.map { groupUser ->
                            key(groupUser.userId) {
                                Column(
                                    Modifier.requiredWidth(60.dp)
                                        .then(
                                            if (isMobile)
                                                Modifier.clickable { userSearchHandler.unselectUser(groupUser) }
                                            else Modifier
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    val presence = groupUser.presence.collectAsState().value
                                    ThemedUserAvatar(
                                        initials = groupUser.initials,
                                        image = groupUser.image,
                                        presence = presence,
                                    ) {
                                        Tooltip({ Text(i18n.commonRemove()) }) {
                                            ThemedIconButton(
                                                style = MaterialTheme.components.primaryIconButton,
                                                size = 15.dp,
                                                onClick = { userSearchHandler.unselectUser(groupUser) },
                                            ) {
                                                Icon(Icons.Default.Close, i18n.commonRemove())
                                            }
                                        }
                                    }
                                    Text(
                                        groupUser.displayName,
                                        maxLines = 3,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalScrollbar(
                    Modifier.align(Alignment.BottomCenter).padding(horizontal = 10.dp).fillMaxWidth(),
                    scrollState,
                )
            }
            HorizontalDivider(Modifier.fillMaxWidth().width(1.dp).padding(horizontal = 10.dp))
        }
    }
}

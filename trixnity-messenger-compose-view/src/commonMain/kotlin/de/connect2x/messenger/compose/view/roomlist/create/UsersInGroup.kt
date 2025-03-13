package de.connect2x.messenger.compose.view.roomlist.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.VerticalGrid
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel

interface UsersInGroupView {
    @Composable
    fun create(createNewGroupViewModel: CreateNewGroupViewModel)
}

@Composable
fun UsersInGroup(createNewGroupViewModel: CreateNewGroupViewModel) {
    DI.get<UsersInGroupView>().create(createNewGroupViewModel)
}

class UsersInGroupViewImpl : UsersInGroupView {
    @Composable
    override fun create(createNewGroupViewModel: CreateNewGroupViewModel) {
        val i18n = DI.get<I18nView>()
        val isMobile = Platform.current.isMobile
        val groupUsers = createNewGroupViewModel.groupUsers.collectAsState()
        if (groupUsers.value.isNotEmpty()) {
            Box(Modifier.padding(horizontal = 10.dp, vertical = 15.dp)) {
                VerticalGrid(spacing = 10.dp) {
                    groupUsers.value.map { groupUser ->
                        Column(
                            Modifier.requiredWidth(60.dp)
                                .then(if (isMobile) Modifier.clickable {
                                    createNewGroupViewModel.removeUserFromGroup(groupUser)
                                } else Modifier),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Avatar(groupUser.image, groupUser.initials) {
                                Icon(
                                    Icons.Default.Close,
                                    i18n.commonRemove(),
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .size(15.dp)
                                        .clickable { createNewGroupViewModel.removeUserFromGroup(groupUser) }
                                        .buttonPointerModifier(),
                                )
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

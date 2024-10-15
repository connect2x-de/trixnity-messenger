package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExtendedFloatingActionButton
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
import de.connect2x.messenger.compose.view.common.ErrorDialog
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.VerticalGrid
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isMobile
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersViewModel


@Composable
fun AddMembersContainer(addMembersViewModel: AddMembersViewModel) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            AddMembersToRoom(addMembersViewModel)
        }
    }
}

interface AddMembersToRoomView {
    @Composable
    fun create(addMembersViewModel: AddMembersViewModel)
}

@Composable
fun AddMembersToRoom(addMembersViewModel: AddMembersViewModel) {
    DI.get<AddMembersToRoomView>().create(addMembersViewModel)
}

class AddMembersToRoomViewImpl : AddMembersToRoomView {
    @Composable
    override fun create(addMembersViewModel: AddMembersViewModel) {
        val canAddMembers = addMembersViewModel.canAddMembers.collectAsState()
        val error = addMembersViewModel.error.collectAsState()
        val errorCause = addMembersViewModel.errorCause.collectAsState().value
        val i18n = DI.get<I18nView>()

        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Column {
                    Header(addMembersViewModel::back, i18n.addMembers())
                    if (error.value != null) {
                        ErrorDialog(error.value.orEmpty(), { addMembersViewModel.errorDismiss() }, errorCause = errorCause)
                    }
                    UsersInGroup(addMembersViewModel)
                    SearchUsersSettings(
                        addMembersViewModel.potentialMembersViewModel,
                        addMembersViewModel::onUserClick
                    )
                }
                if (canAddMembers.value) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 20.dp, end = 20.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            text = { Text(i18n.addMembers()) },
                            icon = { Icon(Icons.Default.Check, i18n.addMembers()) },
                            onClick = { addMembersViewModel.addMembers() },
                            modifier = Modifier.buttonPointerModifier(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.UsersInGroup(addMembersToRoom: AddMembersViewModel) {
    val i18n = DI.get<I18nView>()
    val isMobile = Platform.current.isMobile
    val groupUsers = addMembersToRoom.groupUsers.collectAsState()
    if (groupUsers.value.isNotEmpty()) {
        Box(Modifier.padding(horizontal = 10.dp, vertical = 20.dp)) {
            VerticalGrid(spacing = 10.dp) {
                groupUsers.value.map { groupUser ->
                    Column(
                        Modifier.requiredWidth(60.dp)
                            .then(if (isMobile) Modifier.clickable {
                                addMembersToRoom.removeUserFromGroup(groupUser)
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
                                    .size(15.dp)
                                    .clickable { addMembersToRoom.removeUserFromGroup(groupUser) }
                                    .buttonPointerModifier(),
                            )
                        }
                        Text(groupUser.displayName, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                    }
                }
            }
        }
        HorizontalDivider(Modifier.fillMaxWidth().width(1.dp).padding(horizontal = 10.dp))
    }
}

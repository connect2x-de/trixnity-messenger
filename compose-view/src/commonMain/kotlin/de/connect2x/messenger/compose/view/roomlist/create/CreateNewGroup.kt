package de.connect2x.messenger.compose.view.roomlist.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.collectAsStateForTextField
import de.connect2x.messenger.compose.view.common.ErrorDialog
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.MoreOptions
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.roomlist.search.SearchUsers
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel

// FIXME TIM
interface CreateNewGroupView {
    @Composable
    fun create(createNewGroupViewModel: CreateNewGroupViewModel)
}

@Composable
fun CreateNewGroup(createNewGroupViewModel: CreateNewGroupViewModel) {
    DI.current.get<CreateNewGroupView>().create(createNewGroupViewModel)
}

class CreateNewGroupViewImpl : CreateNewGroupView {
    @Composable
    override fun create(createNewGroupViewModel: CreateNewGroupViewModel) {
        val i18n = DI.current.get<I18nView>()
        val canCreateNewGroup = createNewGroupViewModel.canCreateNewGroup.collectAsState()
        val error = createNewGroupViewModel.error.collectAsState()
        val isPrivate by createNewGroupViewModel.isPrivate.collectAsState()
        val isEncrypted by createNewGroupViewModel.isEncrypted.collectAsState()
        val optionalRoomName by createNewGroupViewModel.optionalRoomName.collectAsStateForTextField()
        val optionalRoomTopic by createNewGroupViewModel.optionalGroupTopic.collectAsStateForTextField()

        val roomOptionsString = buildString {
            append(i18n.roomType())

            val roomType = when {
                isPrivate && isEncrypted -> "${i18n.roomTypePrivate()} & ${i18n.roomTypeEncrypted()}"
                !isPrivate && isEncrypted -> "${i18n.roomTypePublic()} & ${i18n.roomTypeEncrypted()}"
                !isPrivate && !isEncrypted -> "${i18n.roomTypePublic()} & ${i18n.roomTypeUnencrypted()}"
                else -> i18n.roomTypeForbidden()
            }
            append(roomType)
        }

        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {

                if (error.value != null) {
                    ErrorDialog(error.value.orEmpty(), { createNewGroupViewModel.errorDismiss() })
                }
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column {
                        Header(createNewGroupViewModel::back, {
                            Text(
                                i18n.createNewGroupNewGroup(),
                                fontWeight = Bold,
                                fontSize = 16.sp,
                            )
                        })
                        Spacer(Modifier.height(15.dp))
                        MoreOptions(roomOptionsString) {
                            CreateGroupOptions(createNewGroupViewModel)
                        }
                        Spacer(Modifier.height(15.dp))
                        OptionalRoomNameInput(createNewGroupViewModel, optionalRoomName)
                        Spacer(Modifier.height(15.dp))
                        OptionalRoomTopicInput(createNewGroupViewModel, optionalRoomTopic)
                        UsersInGroup(createNewGroupViewModel)
                        SearchUsers(
                            createNewGroupViewModel.createNewRoomViewModel,
                            createNewGroupViewModel::onUserClick,
                        )
                    }
                }
                if (canCreateNewGroup.value) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 20.dp, end = 20.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            text = { Text(i18n.createNewGroupCreate()) },
                            icon = { Icon(Icons.Default.Check, i18n.createNewGroupCreate()) },
                            onClick = { createNewGroupViewModel.createNewGroup() },
                            modifier = Modifier.buttonPointerModifier(),
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 20.dp, end = 20.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            text = { Text(i18n.createNewGroupCreate()) },
                            icon = { Icon(Icons.Default.Dangerous, i18n.createNewGroupCreate()) },
                            onClick = { },
                            containerColor = Color.LightGray,
                            modifier = Modifier.buttonPointerModifier(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OptionalRoomNameInput(
    createNewGroupViewModel: CreateNewGroupViewModel,
    optionalRoomName: String
) {
    val i18n = DI.current.get<I18nView>()
    OutlinedTextField(
        value = optionalRoomName,
        onValueChange = { createNewGroupViewModel.optionalRoomName.value = it },
        placeholder = { Text(i18n.optionalGroupNamePlaceholder()) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
    )
}

@Composable
fun OptionalRoomTopicInput(
    createNewGroupViewModel: CreateNewGroupViewModel,
    optionalRoomTopic: String
) {
    val i18n = DI.current.get<I18nView>()
    OutlinedTextField(
        value = optionalRoomTopic,
        onValueChange = { createNewGroupViewModel.optionalGroupTopic.value = it },
        placeholder = { Text(i18n.optionalGroupTopicPlaceholder()) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
    )
}

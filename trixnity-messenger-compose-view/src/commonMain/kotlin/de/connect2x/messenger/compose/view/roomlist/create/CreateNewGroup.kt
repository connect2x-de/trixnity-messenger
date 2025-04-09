package de.connect2x.messenger.compose.view.roomlist.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.ErrorDialog
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.LoadingBar
import de.connect2x.messenger.compose.view.common.MoreOptions
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.roomlist.search.SearchUsers
import de.connect2x.messenger.compose.view.theme.components.ThemedFloatingActionButton
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel

interface CreateNewGroupView {
    @Composable
    fun create(createNewGroupViewModel: CreateNewGroupViewModel)
}

@Composable
fun CreateNewGroup(createNewGroupViewModel: CreateNewGroupViewModel) {
    DI.get<CreateNewGroupView>().create(createNewGroupViewModel)
}

class CreateNewGroupViewImpl : CreateNewGroupView {
    @Composable
    override fun create(createNewGroupViewModel: CreateNewGroupViewModel) {
        val i18n = DI.get<I18nView>()
        val canCreateNewGroup = createNewGroupViewModel.canCreateNewGroup.collectAsState()
        val error = createNewGroupViewModel.error.collectAsState()
        val isPrivate by createNewGroupViewModel.isPrivate.collectAsState()
        val isEncrypted by createNewGroupViewModel.isEncrypted.collectAsState()
        val isCreating by createNewGroupViewModel.isCreating.collectAsState()
        val optionalRoomName = createNewGroupViewModel.optionalRoomName.collectAsTextFieldValueState()
        val optionalRoomTopic = createNewGroupViewModel.optionalGroupTopic.collectAsTextFieldValueState()

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

                    if (isCreating) {
                        LoadingBar()
                    }

                    Spacer(Modifier.height(15.dp))
                    LimitedSizeStickyHeaderColumn(
                        modifier = Modifier.fillMaxSize(),
                        header = {
                            MoreOptions(roomOptionsString, modifier = Modifier.padding(horizontal = 10.dp)) {
                                CreateGroupOptions(createNewGroupViewModel)
                            }
                            Spacer(Modifier.height(15.dp))
                            OptionalRoomNameInput(optionalRoomName)
                            Spacer(Modifier.height(15.dp))
                            OptionalRoomTopicInput(optionalRoomTopic)
                            UsersInGroup(createNewGroupViewModel)
                        },
                        body = { shouldScroll ->
                            SearchUsers(
                                createNewGroupViewModel.createNewRoomViewModel,
                                shouldScroll = shouldScroll,
                                createNewGroupViewModel::onUserClick,
                            )
                        }
                    )
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 20.dp, end = 20.dp)
            ) {
                ThemedFloatingActionButton(
                    expanded = true,
                    enabled = !isCreating && canCreateNewGroup.value,
                    onClick = { createNewGroupViewModel.createNewGroup() },
                    text = { Text(i18n.createNewGroupCreate()) },
                    icon = { Icon(Icons.Default.Check, i18n.createNewGroupCreate()) },
                )
            }
        }
    }
}

@Composable
fun OptionalRoomNameInput(
    value: MutableState<TextFieldValue>,
) {
    val i18n = DI.get<I18nView>()
    OutlinedTextField(
        value = value.value,
        onValueChange = { value.value = it },
        placeholder = { Text(i18n.optionalGroupNamePlaceholder()) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
    )
}

@Composable
fun OptionalRoomTopicInput(
    value: MutableState<TextFieldValue>,
) {
    val i18n = DI.get<I18nView>()
    OutlinedTextField(
        value = value.value,
        onValueChange = { value.value = it },
        placeholder = { Text(i18n.optionalGroupTopicPlaceholder()) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
        )
    )
}

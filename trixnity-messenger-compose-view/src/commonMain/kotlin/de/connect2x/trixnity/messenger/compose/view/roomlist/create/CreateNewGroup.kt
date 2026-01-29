package de.connect2x.trixnity.messenger.compose.view.roomlist.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.common.ExpandableSection
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.roomlist.search.SearchUsersView
import de.connect2x.trixnity.messenger.compose.view.search.SearchResultState
import de.connect2x.trixnity.messenger.compose.view.search.UserSearchResultListView
import de.connect2x.trixnity.messenger.compose.view.search.collectUserSearchResult
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedFloatingActionButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.util.inputFocusNavigation
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
        val error = createNewGroupViewModel.error.collectAsState().value
        val errorDetails = createNewGroupViewModel.errorDetails.collectAsState().value
        val isPrivate by createNewGroupViewModel.isPrivate.collectAsState()
        val isEncrypted by createNewGroupViewModel.isEncrypted.collectAsState()
        val isCreating by createNewGroupViewModel.isCreating.collectAsState()
        val optionalRoomName = createNewGroupViewModel.optionalRoomName.collectAsTextFieldValueState()
        val optionalRoomTopic = createNewGroupViewModel.optionalGroupTopic.collectAsTextFieldValueState()
        val userSearchView = DI.get<SearchUsersView>()
        val userSearchResultView = DI.get<UserSearchResultListView>()
        val userSearchResults = collectUserSearchResult(createNewGroupViewModel.createNewRoomViewModel.searchHandler)
        val selectedUsers = createNewGroupViewModel.createNewRoomViewModel.searchHandler.selectedUsers.collectAsState()

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

        var references by remember {
            mutableStateOf(listOf<String>())
        }

        LaunchedEffect(userSearchResults, selectedUsers.value) {
            if (userSearchResults is SearchResultState.Results) {
                references =
                    userSearchResults.users.map { it.userId.full }.minus(selectedUsers.value.map { it.userId.full }
                        .toSet())
            }
        }
        references.firstOrNull()

        Box(Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Header(createNewGroupViewModel::back, {
                    Text(
                        i18n.createNewGroupNewGroup(),
                        fontWeight = Bold,
                        fontSize = 16.sp,
                    )
                })
                Box(Modifier.fillMaxSize()) {
                    if (isCreating) {
                        ThemedProgressIndicator(
                            Modifier.fillMaxWidth(),
                            MaterialTheme.components.linearProgressIndicator
                        )
                    }
                    val listState = rememberLazyListState()

                    LazyColumn(Modifier.fillMaxSize().rovingFocusContainer(), listState) {
                        item(key = "MoreOptions") {
                            val expanded = rememberSaveable("MoreOptions") { mutableStateOf(false) }
                            val historyExpanded = rememberSaveable("MoreOptions") { mutableStateOf(false) }

                            Column {
                                ExpandableSection(
                                    roomOptionsString,
                                    expanded,
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    icon = Icons.Default.Settings,
                                ) {
                                    CreateGroupOptions(createNewGroupViewModel, historyExpanded)
                                }
                                Spacer(Modifier.height(15.dp))
                            }
                        }
                        item(key = "RoomNameInput") {
                            OptionalRoomNameInput(optionalRoomName)
                            Spacer(Modifier.height(15.dp))
                        }
                        item(key = "RoomTopic") {
                            OptionalRoomTopicInput(optionalRoomTopic)
                        }
                        item(key = "UsersInGroup") {
                            UsersInGroup(createNewGroupViewModel)
                        }
                        userSearchView.create(
                            createNewGroupViewModel.createNewRoomViewModel,
                            { user -> createNewGroupViewModel.onUserClick(user) },
                            userSearchResults,
                            userSearchResultView,
                            this
                        )
                    }

                    VerticalScrollbar(
                        Modifier.fillMaxHeight().align(Alignment.CenterEnd),
                        listState,
                        false
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

        if (error != null) {
            ThemedModalDialog({ createNewGroupViewModel.errorDismiss() }) {
                ModalDialogHeader {
                    Text(i18n.anErrorHasOccurred())
                }
                ModalDialogContent {
                    Text(error)
                    if (errorDetails != null) {
                        ExpandableSection(heading = i18n.errorDetails(), icon = Icons.Default.Info) {
                            Text(errorDetails, modifier = Modifier.padding(20.dp))
                        }
                    }
                }
                ModalDialogFooter {
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = { createNewGroupViewModel.errorDismiss() },
                    ) {
                        Text(i18n.actionOk())
                    }
                }
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
        label = { Text(i18n.optionalGroupNameLabel()) },
        modifier = Modifier
            .inputFocusNavigation()
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        maxLines = 2,
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
        label = { Text(i18n.optionalGroupTopicLabel()) },
        modifier = Modifier
            .inputFocusNavigation()
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
        ),
        maxLines = 2,
    )
}

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.common.ErrorDialog
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.roomlist.search.SearchUsersView
import de.connect2x.trixnity.messenger.compose.view.search.SearchResultState
import de.connect2x.trixnity.messenger.compose.view.search.UserSearchResultListView
import de.connect2x.trixnity.messenger.compose.view.search.UsersInGroup
import de.connect2x.trixnity.messenger.compose.view.search.collectUserSearchResult
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.util.inputFocusNavigation
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel

interface CreateNewGroupView {
    @Composable fun create(createNewGroupViewModel: CreateNewGroupViewModel)
}

@Composable
fun CreateNewGroup(createNewGroupViewModel: CreateNewGroupViewModel) {
    DI.get<CreateNewGroupView>().create(createNewGroupViewModel)
}

private val log = Logger("de.connect2x.trixnity.messenger.compose.view.roomlist.create.CreateNewGroupViewImpl")

class CreateNewGroupViewImpl : CreateNewGroupView {
    @Composable
    override fun create(createNewGroupViewModel: CreateNewGroupViewModel) {
        val i18n = DI.get<I18nView>()
        val error = createNewGroupViewModel.error.collectAsState().value
        val errorDetails = createNewGroupViewModel.errorDetails.collectAsState().value
        val isCreating by createNewGroupViewModel.isCreating.collectAsState()
        val optionalRoomName = createNewGroupViewModel.optionalRoomName.collectAsTextFieldValueState()
        val optionalRoomTopic = createNewGroupViewModel.optionalGroupTopic.collectAsTextFieldValueState()
        val userSearchView = DI.get<SearchUsersView>()
        val userSearchResultView = DI.get<UserSearchResultListView>()
        val userSearchResults = collectUserSearchResult(createNewGroupViewModel.createNewRoomViewModel.searchHandler)

        Box(Modifier.fillMaxSize()) {
            Column(verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
                Header(
                    createNewGroupViewModel::back,
                    { Text(i18n.createNewGroupNewGroup(), fontWeight = Bold, fontSize = 16.sp) },
                )
                if (isCreating) {
                    ThemedProgressIndicator(Modifier.fillMaxWidth(), MaterialTheme.components.linearProgressIndicator)
                }
                Box(Modifier.fillMaxSize()) {
                    val listState = rememberLazyListState()

                    val focusedItem =
                        remember(userSearchResults) {
                            mutableStateOf(
                                if (userSearchResults is SearchResultState.Results) {
                                    userSearchResults.users.firstOrNull()?.userId?.full
                                } else {
                                    null
                                }
                            )
                        }

                    LazyColumn(
                        Modifier.fillMaxSize().rovingFocusContainer(listState = listState, focusedItem = focusedItem),
                        listState,
                    ) {
                        item(key = "MoreOptions") { CreateNewGroupOptions(createNewGroupViewModel) }
                        item(key = "RoomNameInput") {
                            OptionalRoomNameInput(optionalRoomName)
                            Spacer(Modifier.height(15.dp))
                        }
                        item(key = "RoomTopic") { OptionalRoomTopicInput(optionalRoomTopic) }
                        item(key = "UsersInGroup") {
                            UsersInGroup(createNewGroupViewModel.createNewRoomViewModel.searchHandler)
                        }
                        userSearchView.create(
                            createNewGroupViewModel.createNewRoomViewModel,
                            { user -> createNewGroupViewModel.onUserClick(user) },
                            userSearchResults,
                            userSearchResultView,
                            this,
                            focusedItem,
                        )
                    }

                    VerticalScrollbar(Modifier.fillMaxHeight().align(Alignment.CenterEnd), listState, false)
                }
            }
            CreateNewGroupButton(createNewGroupViewModel)
        }

        ErrorDialog(error, errorDetails, onDismiss = { createNewGroupViewModel.errorDismiss() })
    }
}

@Composable
fun OptionalRoomNameInput(value: MutableState<TextFieldValue>) {
    val i18n = DI.get<I18nView>()
    OutlinedTextField(
        value = value.value,
        onValueChange = { value.value = it },
        label = { Text(i18n.optionalGroupNameLabel()) },
        modifier = Modifier.inputFocusNavigation().fillMaxWidth().padding(horizontal = 10.dp),
        maxLines = 2,
    )
}

@Composable
fun OptionalRoomTopicInput(value: MutableState<TextFieldValue>) {
    val i18n = DI.get<I18nView>()
    OutlinedTextField(
        value = value.value,
        onValueChange = { value.value = it },
        label = { Text(i18n.optionalGroupTopicLabel()) },
        modifier = Modifier.inputFocusNavigation().fillMaxWidth().padding(horizontal = 10.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        maxLines = 2,
    )
}

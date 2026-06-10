package de.connect2x.trixnity.messenger.compose.view.roomlist.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.common.ErrorDialog
import de.connect2x.trixnity.messenger.compose.view.common.ExpandableSection
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.VerySmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.search.user.SearchTerm
import de.connect2x.trixnity.messenger.compose.view.search.user.UsersInGroup
import de.connect2x.trixnity.messenger.compose.view.search.user.searchResults
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupNewSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel

class CreateNewGroupNewSearchViewImpl : CreateNewGroupView {
    private val log =
        Logger("de.connect2x.trixnity.messenger.compose.view.roomlist.create.CreateNewGroupNewSearchViewImpl")

    @Composable
    override fun create(createNewGroupViewModel: CreateNewGroupViewModel) {
        if (createNewGroupViewModel is CreateNewGroupNewSearchViewModel) {
            val i18n = DI.get<I18nView>()
            val error = createNewGroupViewModel.error.collectAsState().value
            val errorDetails = createNewGroupViewModel.errorDetails.collectAsState().value
            val isPrivate by createNewGroupViewModel.isPrivate.collectAsState()
            val isEncrypted by createNewGroupViewModel.isEncrypted.collectAsState()
            val isCreating by createNewGroupViewModel.isCreating.collectAsState()
            val searchResultList by createNewGroupViewModel.userSearchViewModel.searchResultList.collectAsState()
            val noResultsFound by createNewGroupViewModel.userSearchViewModel.noResultsFound.collectAsState()
            val optionalRoomName = createNewGroupViewModel.optionalRoomName.collectAsTextFieldValueState()
            val optionalRoomTopic = createNewGroupViewModel.optionalGroupTopic.collectAsTextFieldValueState()

            val roomOptionsString = buildString {
                append(i18n.roomType())

                val roomType =
                    when {
                        isPrivate && isEncrypted -> "${i18n.roomTypePrivate()} & ${i18n.roomTypeEncrypted()}"
                        !isPrivate && isEncrypted -> "${i18n.roomTypePublic()} & ${i18n.roomTypeEncrypted()}"
                        !isPrivate && !isEncrypted -> "${i18n.roomTypePublic()} & ${i18n.roomTypeUnencrypted()}"
                        else -> i18n.roomTypeForbidden()
                    }
                append(roomType)
            }

            Box(Modifier.fillMaxSize()) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Header(
                        createNewGroupViewModel::back,
                        { Text(i18n.createNewGroupNewGroup(), fontWeight = Bold, fontSize = 16.sp) },
                    )
                    if (isCreating) {
                        ThemedProgressIndicator(
                            Modifier.fillMaxWidth(),
                            MaterialTheme.components.linearProgressIndicator,
                        )
                    }
                    Box(Modifier.fillMaxSize()) {
                        val listState = rememberLazyListState()
                        LazyColumn(Modifier.fillMaxSize().rovingFocusContainer(), listState) { // FIXME
                            item(key = "MoreOptions") {
                                val expanded = rememberSaveable("MoreOptions") { mutableStateOf(false) }
                                val historyExpanded = rememberSaveable("MoreOptions") { mutableStateOf(false) }

                                SmallSpacer()
                                Column {
                                    ExpandableSection(
                                        roomOptionsString,
                                        expanded,
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        icon = Icons.Default.Settings,
                                    ) {
                                        CreateGroupOptions(createNewGroupViewModel, historyExpanded)
                                    }
                                }
                            }
                            item(key = "RoomNameInput") {
                                VerySmallSpacer()
                                OptionalRoomNameInput(optionalRoomName)
                            }
                            item(key = "RoomTopic") {
                                VerySmallSpacer()
                                OptionalRoomTopicInput(optionalRoomTopic)
                            }
                            item(key = "UsersInGroup") {
                                VerySmallSpacer()
                                UsersInGroup(createNewGroupViewModel.groupUsersNewSearch) {
                                    createNewGroupViewModel.removeUserFromGroup(it)
                                }
                            }
                            stickyHeader("searchTerm") { SearchTerm(createNewGroupViewModel.userSearchViewModel) }
                            searchResults(
                                searchProviders = createNewGroupViewModel.userSearchViewModel.searchProviders,
                                onUserClick = createNewGroupViewModel::onUserClick,
                                searchResultList = searchResultList,
                                noResultsFound = noResultsFound,
                                focusedItem = mutableStateOf(null), // FIXME
                            )
                        }

                        VerticalScrollbar(Modifier.fillMaxHeight().align(Alignment.CenterEnd), listState, false)
                    }
                }
                CreateNewGroupButton(createNewGroupViewModel)
            }

            ErrorDialog(error, errorDetails, onDismiss = { createNewGroupViewModel.errorDismiss() })
        } else {
            log.warn {
                "Expected CreateNewGroupNewSearchViewModel, but got ${createNewGroupViewModel::class.simpleName}"
            }
        }
    }
}

package de.connect2x.trixnity.messenger.compose.view.roomlist.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.HorizontalScrollbar
import de.connect2x.trixnity.messenger.compose.view.Platform
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.common.ExpandableSection
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.VerySmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.isMobile
import de.connect2x.trixnity.messenger.compose.view.roomlist.search.SearchUserProviderSettings
import de.connect2x.trixnity.messenger.compose.view.roomlist.search.searchResults
import de.connect2x.trixnity.messenger.compose.view.search.searchTerm
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedFloatingActionButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupNewSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.provider.homeserver.HomeserverUserSearchResult

class CreateNewGroupNewSearchViewImpl : CreateNewGroupView {
    @Composable
    override fun create(createNewGroupViewModel: CreateNewGroupViewModel) {
        if (createNewGroupViewModel is CreateNewGroupNewSearchViewModel) {
            val i18n = DI.get<I18nView>()
            val canCreateNewGroup = createNewGroupViewModel.canCreateNewGroup.collectAsState()
            val error = createNewGroupViewModel.error.collectAsState().value
            val errorDetails = createNewGroupViewModel.errorDetails.collectAsState().value
            val isPrivate by createNewGroupViewModel.isPrivate.collectAsState()
            val isEncrypted by createNewGroupViewModel.isEncrypted.collectAsState()
            val isCreating by createNewGroupViewModel.isCreating.collectAsState()
            val optionalRoomName = createNewGroupViewModel.optionalRoomName.collectAsTextFieldValueState()
            val optionalRoomTopic = createNewGroupViewModel.optionalGroupTopic.collectAsTextFieldValueState()

            val searchResultList = createNewGroupViewModel.searchUserViewModel.searchResultList.collectAsState().value
            val providerSearchActive =
                createNewGroupViewModel.searchUserViewModel.providerSearchActive.collectAsState().value
            val searchUserProviderSettings = remember { mutableStateOf(false) }

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
                                UsersInGroup(createNewGroupViewModel)
                            }
                            searchTerm(createNewGroupViewModel.searchUserViewModel)
                            searchResults(
                                searchUserProviders = createNewGroupViewModel.searchUserViewModel.searchUserProviders,
                                onUserClick = createNewGroupViewModel::onUserClick,
                                providerSearchActive = providerSearchActive,
                                providerSearchSetActive = { index, active ->
                                    createNewGroupViewModel.searchUserViewModel.setProvider(index, active)
                                },
                                searchResultList = searchResultList,
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

            CreateNewRoomErrorDialog(error, errorDetails, onDismiss = { createNewGroupViewModel.errorDismiss() })

            if (searchUserProviderSettings.value) {
                SearchUserProviderSettings(createNewGroupViewModel.searchUserViewModel.searchUserProviders) {
                    searchUserProviderSettings.value = false
                }
            }
        }
    }
}

// FIXME change the API?
@Composable
private fun UsersInGroup(createNewGroupViewModel: CreateNewGroupNewSearchViewModel) {
    val i18n = DI.get<I18nView>()
    val scrollState = rememberScrollState()
    val isMobile = Platform.current.isMobile
    val selectedUsers by createNewGroupViewModel.groupUsersNewSearch.collectAsState()
    if (selectedUsers.isNotEmpty()) {
        Box {
            Box(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    selectedUsers.map { groupUser ->
                        key(groupUser.userId) {
                            Column(
                                Modifier.requiredWidth(60.dp)
                                    .then(if (isMobile) Modifier.clickable {
                                        createNewGroupViewModel.removeUserFromGroup(groupUser)
                                    } else Modifier),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val presence =
                                    if (groupUser is HomeserverUserSearchResult) groupUser.presence.collectAsState().value
                                    else null
                                ThemedUserAvatar(
                                    initials = groupUser.initials,
                                    image = groupUser.image,
                                    presence = presence,
                                ) {
                                    Tooltip({ Text(i18n.commonRemove()) }) {
                                        ThemedIconButton(
                                            style = MaterialTheme.components.primaryIconButton,
                                            size = 15.dp,
                                            onClick = { createNewGroupViewModel.removeUserFromGroup(groupUser) }
                                        ) {
                                            Icon(Icons.Default.Close, i18n.commonRemove())
                                        }
                                    }
                                }
                                Text(
                                    groupUser.displayName ?: "",
                                    maxLines = 3,
                                    style = MaterialTheme.typography.labelMedium
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

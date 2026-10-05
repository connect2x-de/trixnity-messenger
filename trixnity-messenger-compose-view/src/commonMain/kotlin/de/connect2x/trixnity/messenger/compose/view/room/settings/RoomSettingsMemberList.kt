package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.common.ToggleableFilterChip
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItem
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel

interface RoomSettingsMemberListView {
    @Composable fun create(roomSettingsViewModel: RoomSettingsViewModel)
}

@Composable
fun ColumnScope.RoomSettingsMemberList(roomSettingsViewModel: RoomSettingsViewModel) {
    DI.get<RoomSettingsMemberListView>().create(roomSettingsViewModel)
}

class RoomSettingsMemberListViewImpl : RoomSettingsMemberListView {
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun create(roomSettingsViewModel: RoomSettingsViewModel) {
        val i18n = DI.get<I18nView>()
        val hasPowerToInvite = roomSettingsViewModel.hasPowerToInvite.collectAsState().value
        val memberListViewModel = roomSettingsViewModel.memberListViewModel
        val memberListElementViewModels = memberListViewModel.elements.collectAsState().value
        val searchTerm by memberListViewModel.searchTerm.collectAsState()
        var showFilters by remember(memberListViewModel) { mutableStateOf(searchTerm.isNotEmpty()) }
        val focusRequester = remember { FocusRequester() }
        fun closeFilters() {
            showFilters = false
            memberListViewModel.searchTerm.value = ""
        }
        val joinedMemberCount = memberListViewModel.membershipCounts.collectAsState().value[Membership.JOIN]

        Column {
            ThemedListItem(
                style = MaterialTheme.components.settingsItem,
                headlineContent = {
                    Text(
                        "${i18n.roomSettingsMembers()} ${joinedMemberCount?.let { "($it)" }}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val filterAction =
                            if (showFilters) i18n.settingsRoomMemberListHideFilters()
                            else i18n.settingsRoomMemberListShowFilters()
                        Tooltip(tooltip = { Text(filterAction) }) {
                            ThemedIconButton(
                                style = MaterialTheme.components.commonIconButton,
                                onClick = { if (showFilters) closeFilters() else showFilters = true },
                            ) {
                                Icon(
                                    if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                                    filterAction,
                                )
                            }
                        }
                        if (hasPowerToInvite) {
                            Tooltip(tooltip = { Text(i18n.addMembers()) }) {
                                ThemedIconButton(
                                    style = MaterialTheme.components.commonIconButton,
                                    onClick = { roomSettingsViewModel.openAddMembersView() },
                                ) {
                                    Icon(Icons.Default.PersonAdd, i18n.addMembers())
                                }
                            }
                        }
                    }
                },
            )

            AnimatedVisibility(showFilters, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    Modifier.onKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                            closeFilters()
                            true
                        } else false
                    }
                ) {
                    OutlinedTextField(
                        value = searchTerm,
                        onValueChange = { memberListViewModel.searchTerm.value = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        leadingIcon = { Icon(Icons.Default.Search, i18n.userSearchSearchPeople()) },
                        label = { Text(i18n.userSearchNameOrMatrixId()) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        val membershipFilters =
                            listOf(
                                Membership.JOIN to i18n.settingsRoomMemberListJoined(),
                                Membership.KNOCK to i18n.settingsRoomMemberListKnocking(),
                                Membership.INVITE to i18n.settingsRoomMemberListInvited(),
                                Membership.BAN to i18n.settingsRoomMemberListBanned(),
                            )
                        membershipFilters.forEach { (membership, label) ->
                            ToggleableFilterChip(memberListViewModel.filterByMemberships, setOf(membership)) {
                                Text(
                                    label,
                                    Modifier.semantics { text = AnnotatedString(i18n.filterBy() + " " + label) },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }
                LaunchedEffect(showFilters) { if (showFilters) focusRequester.requestFocus() }
            }

            if (memberListElementViewModels.isNotEmpty()) {
                MemberList(memberListViewModel, onClickUser = { roomSettingsViewModel.openUserProfile(it) })
            }
        }
    }
}

@Composable
fun MemberList(memberListViewModel: MemberListViewModel, onClickUser: (UserId) -> Unit) {
    val members by memberListViewModel.elements.collectAsState()
    val state = rememberLazyListState()
    val showLoadingSpinner = memberListViewModel.showLoadingSpinner.collectAsState().value

    val focusedItem = remember(members) { mutableStateOf(members.firstOrNull()?.memberUserId?.full) }

    Box(Modifier.heightIn(min = 100.dp, max = 320.dp)) {
        LazyColumn(
            Modifier.fillMaxWidth().rovingFocusContainer(listState = state, focusedItem = focusedItem).semantics {
                collectionInfo = CollectionInfo(rowCount = members.size, columnCount = 1)
            },
            state,
        ) {
            itemsIndexed(members, key = { _, item -> item.memberUserId.full }) { index, member ->
                RoomSettingsMemberListElement(
                    memberListViewModel,
                    member.memberUserId,
                    member,
                    modifier =
                        Modifier.rovingFocusItem(
                                isFocused = { focusedItem.value == member.memberUserId.full },
                                onFocus = { focusedItem.value = member.memberUserId.full },
                            )
                            .semantics {
                                collectionItemInfo =
                                    CollectionItemInfo(rowIndex = index, rowSpan = 1, columnIndex = 0, columnSpan = 1)
                            },
                    onClick = { onClickUser(member.memberUserId) },
                )
            }
            if (showLoadingSpinner) {
                item(key = "loadingSpinner") { LoadingSpinner() }
            }
        }
        if (state.canScrollForward || state.canScrollBackward) {
            VerticalScrollbar(Modifier.align(Alignment.CenterEnd), state, false)
        }
    }
}

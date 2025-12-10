package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.ToggleableFilterChip
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedListItem
import de.connect2x.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.Membership

interface RoomSettingsMemberListView {
    @Composable
    fun create(roomSettingsViewModel: RoomSettingsViewModel)
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
        val memberListElementViewModels =
            memberListViewModel.elements.collectAsState().value
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
                trailingContent = if (hasPowerToInvite) {
                    {
                        Tooltip(
                            tooltip = { Text(i18n.addMembers()) }
                        ) {
                            ThemedIconButton(
                                style = MaterialTheme.components.commonIconButton,
                                onClick = { roomSettingsViewModel.openAddMembersView() },
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    i18n.addMembers(),
                                )
                            }
                        }
                    }
                } else null
            )

            FlowRow(Modifier.fillMaxWidth()) {
                ToggleableFilterChip(
                    memberListViewModel.filterByMemberships,
                    setOf(Membership.JOIN)
                ) {
                    Text(i18n.settingsRoomMemberListJoined(), Modifier.semantics {
                        text = AnnotatedString(i18n.filterBy() + " " + i18n.settingsRoomMemberListJoined())
                    })
                }
                Spacer(Modifier.size(5.dp))
                ToggleableFilterChip(
                    memberListViewModel.filterByMemberships,
                    setOf(Membership.KNOCK)
                ) {
                    Text(i18n.settingsRoomMemberListKnocking(), Modifier.semantics {
                        text =
                            AnnotatedString(i18n.filterBy() + " " + i18n.settingsRoomMemberListKnocking())
                    })
                }
                Spacer(Modifier.size(5.dp))
                ToggleableFilterChip(
                    memberListViewModel.filterByMemberships,
                    setOf(Membership.INVITE)
                ) {
                    Text(i18n.settingsRoomMemberListInvited(), Modifier.semantics {
                        text =
                            AnnotatedString(i18n.filterBy() + " " + i18n.settingsRoomMemberListInvited())
                    })
                }
                Spacer(Modifier.size(5.dp))
                ToggleableFilterChip(
                    memberListViewModel.filterByMemberships,
                    setOf(Membership.BAN)
                ) {
                    Text(i18n.settingsRoomMemberListBanned(), Modifier.semantics {
                        text =
                            AnnotatedString(i18n.filterBy() + " " + i18n.settingsRoomMemberListBanned())
                    })
                }
            }

            if (memberListElementViewModels.isNotEmpty()) {
                MemberList(memberListViewModel, onClickUser = { roomSettingsViewModel.openUserProfile(it) })
            }
        }
    }
}

@Composable
fun MemberList(
    memberListViewModel: MemberListViewModel,
    onClickUser: (UserId) -> Unit,
) {
    val members by memberListViewModel.elements.collectAsState()
    val state = rememberLazyListState()
    val showLoadingSpinner = memberListViewModel.showLoadingSpinner.collectAsState().value

    var focusedItem by remember(members) { mutableStateOf(members.map { it.memberUserId }.firstOrNull()) }

    Box(Modifier.heightIn(min = 100.dp, max = 320.dp)) {
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .rovingFocusContainer()
                .semantics {
                    collectionInfo = CollectionInfo(rowCount = members.size, columnCount = 1)
                },
            state
        ) {
            itemsIndexed(members, key = { _, item -> item.memberUserId.full }) { i, member ->
                RoomSettingsMemberListElement(
                    memberListViewModel,
                    member.memberUserId,
                    member,
                    modifier = Modifier
                        .rovingFocusItem(
                            isFocused = focusedItem == member.memberUserId,
                            onFocus = { focusedItem = member.memberUserId },
                        )
                        .semantics {
                            collectionItemInfo =
                                CollectionItemInfo(rowIndex = i, rowSpan = 1, columnIndex = 0, columnSpan = 1)
                        },
                    onClick = {
                        onClickUser(member.memberUserId)
                    },
                )
            }
            if (showLoadingSpinner) {
                item(key = "loadingSpinner") {
                    LoadingSpinner()
                }
            }
        }
        if (state.canScrollForward || state.canScrollBackward) {
            VerticalScrollbar(Modifier.align(Alignment.CenterEnd), state, false)
        }
    }
}

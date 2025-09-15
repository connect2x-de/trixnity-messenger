package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${i18n.roomSettingsMembers()} ${joinedMemberCount?.let { "($it)" }}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1.0f, false).fillMaxWidth(),
            )
            if (hasPowerToInvite) {
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
        }

        FlowRow(Modifier.fillMaxWidth()) {
            ToggleableFilterChip(
                memberListViewModel.filterByMemberships,
                setOf(Membership.JOIN)
            ) {
                Text(i18n.settingsRoomMemberListJoined())
            }
            Spacer(Modifier.size(5.dp))
            ToggleableFilterChip(
                memberListViewModel.filterByMemberships,
                setOf(Membership.KNOCK)
            ) {
                Text(i18n.settingsRoomMemberListKnocking())
            }
            Spacer(Modifier.size(5.dp))
            ToggleableFilterChip(
                memberListViewModel.filterByMemberships,
                setOf(Membership.INVITE)
            ) {
                Text(i18n.settingsRoomMemberListInvited())
            }
            Spacer(Modifier.size(5.dp))
            ToggleableFilterChip(
                memberListViewModel.filterByMemberships,
                setOf(Membership.BAN)
            ) {
                Text(i18n.settingsRoomMemberListBanned())
            }
        }

        if (memberListElementViewModels.isNotEmpty()) {
            MemberList(memberListViewModel, onClickUser = { roomSettingsViewModel.openUserProfile(it) })
        }
    }
}

@Composable
fun MemberList(
    memberListViewModel: MemberListViewModel,
    onClickUser: (UserId) -> Unit,
) {
    val members = memberListViewModel.elements.collectAsState().value
    val state = rememberLazyListState()
    val showLoadingSpinner = memberListViewModel.showLoadingSpinner.collectAsState().value

    Box(Modifier.heightIn(min = 100.dp, max = 320.dp)) {
        LazyColumn(Modifier.fillMaxWidth(), state) {
            members.forEach { memberListElementViewModel ->
                val userId = memberListElementViewModel.memberUserId
                item(key = userId.full) {
                    RoomSettingsMemberListElement(
                        memberListViewModel,
                        userId,
                        memberListElementViewModel,
                        onClick = {
                            onClickUser(userId)
                        },
                    )
                }
            }
            if (showLoadingSpinner) {
                item(key = "loadingSpinner") {
                    LoadingSpinner()
                }
            }
        }

        // The VerticalScrollbar causes the size of the box to always be maximum and thus no longer adapts to the content.
        // TODO: Consider using the approach used in UnifiedTimelineElementMetadata.kt for the user interactions list.
        if (members.count() > 4) {
            VerticalScrollbar(
                Modifier,
                state,
                false,
            )
        }
    }
    LaunchedEffect(members) {
        if (state.layoutInfo.visibleItemsInfo.any { it.index == 1 }) { // This has been the first element before.
            state.animateScrollToItem(0)
        }
    }
}

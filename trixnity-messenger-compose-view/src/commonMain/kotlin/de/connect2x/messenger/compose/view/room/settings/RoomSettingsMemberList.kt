package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel

interface RoomSettingsMemberListView {
    @Composable
    fun create(roomSettingsViewModel: RoomSettingsViewModel)
}

@Composable
fun RoomSettingsMemberList(roomSettingsViewModel: RoomSettingsViewModel) {
    DI.get<RoomSettingsMemberListView>().create(roomSettingsViewModel)
}

class RoomSettingsMemberListViewImpl : RoomSettingsMemberListView {
    @Composable
    override fun create(roomSettingsViewModel: RoomSettingsViewModel) {
        val i18n = DI.get<I18nView>()
        val hasPowerToInvite = roomSettingsViewModel.hasPowerToInvite.collectAsState().value
        val memberListViewModels =
            roomSettingsViewModel.memberListViewModel.memberListElementViewModels.collectAsState().value
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${i18n.roomSettingsMembers().capitalize(Locale.current)} (${memberListViewModels.count()})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1.0f, false).fillMaxWidth(),
            )
            if (hasPowerToInvite) {
                IconButton(
                    onClick = { roomSettingsViewModel.openAddMembersView() },
                    modifier = Modifier.buttonPointerModifier()
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        i18n.addMembers(),
                    )
                }
            }
        }
        MemberList(roomSettingsViewModel.memberListViewModel)
    }
}

@Composable
fun MemberList(memberListViewModel: MemberListViewModel) {
    val members = memberListViewModel.memberListElementViewModels.collectAsState().value
    val clickedUser = remember { mutableStateOf<MemberListElementViewModel.MemberElement?>(null) }
    val state = rememberLazyListState()
    val showLoadingSpinner = memberListViewModel.showLoadingSpinner.collectAsState().value

    Box(Modifier.heightIn(min = 100.dp, max = 320.dp)) {
        LazyColumn(Modifier.fillMaxWidth(), state) {
            members.forEach { (userId, memberListElementViewModel) ->
                item(key = userId.full) {
                    RoomSettingsMemberListElement(
                        memberListViewModel,
                        userId,
                        memberListElementViewModel,
                        clickedUser,
                    )
                }
            }
            if (showLoadingSpinner) item(key = "loadingSpinner") {
                LoadingSpinner()
            }
        }

        //the VerticalScrollbar causes the size of the box to always be maximum and thus no longer adapts to the content
        //this is the only solution found for now
        if (members.count() > 4) {
            VerticalScrollbar(
                Modifier.align(Alignment.CenterEnd),
                state,
                false
            )
        }
    }
    LaunchedEffect(members) {
        if (state.layoutInfo.visibleItemsInfo.any { it.index == 1 }) { // this has been the first element before
            state.animateScrollToItem(0)
        }
    }
}

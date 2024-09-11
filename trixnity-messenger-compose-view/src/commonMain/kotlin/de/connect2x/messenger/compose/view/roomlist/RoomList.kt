package de.connect2x.messenger.compose.view.roomlist

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.roomlist.header.AccountMenuItem
import de.connect2x.messenger.compose.view.roomlist.header.SelectAccountHeader
import de.connect2x.messenger.compose.view.roomlist.room.RoomListElementContainer
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }

interface RoomListView {
    @Composable
    fun create(roomListViewModel: RoomListViewModel)
}

@Composable
fun RoomList(roomListViewModel: RoomListViewModel) {
    DI.get<RoomListView>().create(roomListViewModel)
}

class RoomListViewImpl : RoomListView {
    @Composable
    override fun create(roomListViewModel: RoomListViewModel) {
        val state = rememberLazyListState()
        val initialSyncFinished = roomListViewModel.initialSyncFinished.collectAsState().value
        val allRooms = roomListViewModel.sortedRoomListElementViewModels.collectAsState().value
        val canCreateNewRoomWithAccount = roomListViewModel.canCreateNewRoomWithAccount.collectAsState().value
        val i18n = DI.get<I18nView>()
        Surface {
            Box(
                Modifier
                    .fillMaxSize(),
            ) {
                log.debug { "rendering room list items" }
                if (allRooms.isEmpty() && canCreateNewRoomWithAccount) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            Text(i18n.roomListNoRoom())
                            Spacer(Modifier.size(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { roomListViewModel.createNewRoom() },
                                    Modifier.buttonPointerModifier(),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Chat, i18n.accountCreateNewRoom())
                                        Spacer(Modifier.size(10.dp))
                                        Text(i18n.roomListCreateRoom(), modifier = Modifier.weight(1.0f, fill = false))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize(), state) {
                        items(
                            allRooms,
                            { (roomId, _) -> roomId.full }
                        ) { roomListElement ->
                            RoomListElementContainer(
                                roomListElement.roomId,
                                roomListViewModel,
                                roomListElement.viewModel,
                            )
                        }
                    }
                    VerticalScrollbar(
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        state,
                        false,
                    )
                }
                CreateRoomFloatingButton(roomListViewModel)
            }
        }

        LaunchedEffect(initialSyncFinished) {
            log.debug { "initialSyncFinished -> scroll room list to top" }
            state.scrollToItem(0)
        }
        LaunchedEffect(allRooms) {
            if (state.layoutInfo.visibleItemsInfo.any { it.index == 1 }) { // this has been the first element before
                state.animateScrollToItem(0)
            }
        }
    }
}


@Composable
fun BoxScope.CreateRoomFloatingButton(roomListViewModel: RoomListViewModel) {
    val i18n = DI.get<I18nView>()
    val canCreateNewRoomWithAccount = roomListViewModel.canCreateNewRoomWithAccount.collectAsState().value
    val accounts = roomListViewModel.accountViewModel.accounts.collectAsState().value
    val selectActiveAccount = remember { mutableStateOf(false) }

    Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp)) {
        Tooltip(
            tooltip = { TooltipText(i18n.accountCreateNewRoom()) },
        ) {
            FloatingActionButton(
                onClick = {
                    if (canCreateNewRoomWithAccount) {
                        roomListViewModel.createNewRoom()
                    } else {
                        selectActiveAccount.value = true
                    }
                },
                modifier = Modifier
                    .buttonPointerModifier()
                    .indication(indication = null, interactionSource = MutableInteractionSource()),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, i18n.accountCreateNewRoom())
            }
        }

        DropdownMenu(
            expanded = selectActiveAccount.value,
            onDismissRequest = { selectActiveAccount.value = false },
        ) {
            SelectAccountHeader(i18n.accountSelectAccount())
            accounts.forEach { account ->
                AccountMenuItem(account) { roomListViewModel.createNewRoomFor(account.userId) }
            }
        }
    }
}

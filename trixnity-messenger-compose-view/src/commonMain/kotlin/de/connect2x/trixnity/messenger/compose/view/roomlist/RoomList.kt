package de.connect2x.trixnity.messenger.compose.view.roomlist

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.roomlist.header.AccountMenuItem
import de.connect2x.trixnity.messenger.compose.view.roomlist.header.SelectAccountHeader
import de.connect2x.trixnity.messenger.compose.view.roomlist.room.RoomListElementContainer
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedDropdownMenu
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedFloatingActionButton
import de.connect2x.trixnity.messenger.compose.view.util.LocalRovingFocus
import de.connect2x.trixnity.messenger.compose.view.util.RovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.util.RovingFocusItem
import de.connect2x.trixnity.messenger.compose.view.util.verticalRovingFocus
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
        val allRoomState = roomListViewModel.elements.collectAsState()
        val allRooms = allRoomState.value
        val canCreateNewRoomWithAccount = roomListViewModel.canCreateNewRoomWithAccount.collectAsState().value
        val searchResultsEmpty = roomListViewModel.searchResultsEmpty.collectAsState().value
        val i18n = DI.get<I18nView>()
        Box(Modifier.fillMaxSize()) {
            log.debug { "rendering room list items" }
            if (allRooms.isEmpty() && canCreateNewRoomWithAccount && !searchResultsEmpty) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text(i18n.roomListNoRoom())
                        Spacer(Modifier.size(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ThemedButton(
                                style = MaterialTheme.components.commonButton,
                                onClick = { roomListViewModel.createNewRoom() },
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    i18n.accountCreateNewRoom(),
                                    modifier = Modifier.Companion.size(MaterialTheme.components.primaryButton.iconSize)
                                )
                                Spacer(Modifier.Companion.size(MaterialTheme.components.primaryButton.iconSpacing))
                                Text(i18n.roomListCreateRoom())
                            }
                        }
                    }
                }
            } else if (searchResultsEmpty) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text(i18n.roomListNoSearchResults())
                    }
                }
            } else {
                val selectedRoomId = roomListViewModel.selectedRoomId.collectAsState()

                RovingFocusContainer {
                    val rovingFocusState = LocalRovingFocus.current
                    val defaultItem = derivedStateOf {
                        selectedRoomId.value ?: allRoomState.value.firstOrNull()?.roomId
                    }

                    LaunchedEffect(rovingFocusState, defaultItem.value) {
                        rovingFocusState?.selectItem(defaultItem.value) {
                            val index = allRooms.indexOfFirst { it.roomId == defaultItem.value }
                            if (index != -1) {
                                state.scrollToItem(index)
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().verticalRovingFocus(
                            default = defaultItem.value,
                            scroll = { item ->
                                val index = allRooms.indexOfFirst { it.roomId == item }
                                if (index != -1) {
                                    state.scrollToItem(index)
                                }
                            },
                            up = {
                                val currentItem = activeRef.value ?: defaultItem.value
                                val currentIndex = allRooms.indexOfFirst { it.roomId == currentItem }
                                val nextIndex = currentIndex.minus(1).coerceIn(allRooms.indices)
                                allRooms[nextIndex].roomId
                            },
                            down = {
                                val currentItem = activeRef.value ?: defaultItem.value
                                val currentIndex = allRooms.indexOfFirst { it.roomId == currentItem }
                                val nextIndex = currentIndex.plus(1).coerceIn(allRooms.indices)
                                allRooms[nextIndex].roomId
                            },
                        ),
                        state,
                    ) {
                        items(
                            allRooms,
                            { it.roomId.full }
                        ) { roomListElement ->
                            RovingFocusItem(roomListElement.roomId, selectedRoomId) {
                                RoomListElementContainer(
                                    roomListElement.roomId,
                                    roomListViewModel,
                                    roomListElement,
                                )
                            }
                        }
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
        ThemedFloatingActionButton(
            onClick = {
                if (canCreateNewRoomWithAccount) {
                    roomListViewModel.createNewRoom()
                } else {
                    selectActiveAccount.value = true
                }
            },
            text = { Text(i18n.accountCreateNewRoom()) },
            icon = { Icon(Icons.AutoMirrored.Filled.Chat, i18n.accountCreateNewRoom()) },
        )

        ThemedDropdownMenu(
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

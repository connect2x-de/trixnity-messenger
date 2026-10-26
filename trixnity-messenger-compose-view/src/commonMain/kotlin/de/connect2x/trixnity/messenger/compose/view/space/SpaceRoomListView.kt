package de.connect2x.trixnity.messenger.compose.view.space

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.navigation.LocalAppNavigationRailVisible
import de.connect2x.trixnity.messenger.compose.view.roomlist.RoomListItems
import de.connect2x.trixnity.messenger.compose.view.roomlist.header.AccountAvatar
import de.connect2x.trixnity.messenger.compose.view.roomlist.header.AccountOptions
import de.connect2x.trixnity.messenger.compose.view.roomlist.header.SearchRoomsBanner
import de.connect2x.trixnity.messenger.compose.view.roomlist.header.ShowSearch
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceContentState
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceContentViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize

@Composable
internal fun SpaceRoomListView(
    viewModel: SpaceContentViewModel,
    roomListViewModel: RoomListViewModel,
    userId: UserId,
    exploreSelected: Boolean,
    onOpenExplore: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val state by viewModel.state.collectAsState()
    val content = (state as? SpaceContentState.Ready)?.content
    val listState = rememberLazyListState()
    val initialSyncFinished = roomListViewModel.initialSyncFinished.collectAsState().value
    LaunchedEffect(initialSyncFinished) { listState.scrollToItem(0) }
    val rooms by roomListViewModel.elements.collectAsState()
    val searchTerm by roomListViewModel.searchTerm.collectAsState()
    LaunchedEffect(rooms) { if (!listState.canScrollBackward) listState.requestScrollToItem(0) }
    val spacing = MaterialTheme.messengerDpConstants
    val subspaces = content?.items.orEmpty().filter { it.joined && it.isSpace }
    ThemedSurface(style = MaterialTheme.components.sidebar) {
        Column(Modifier.fillMaxSize()) {
            SpaceHeader(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                if (!LocalAppNavigationRailVisible.current) SpaceBackButton(onBack = viewModel::back)
                AccountAvatar(roomListViewModel.accountViewModel)
                ShowSearch(roomListViewModel)
                AccountOptions(roomListViewModel.accountViewModel, roomListViewModel)
            }
            SearchRoomsBanner(roomListViewModel, i18n.spacesSearchJoinedRooms())
            Box(Modifier.weight(1f)) {
                RoomListItems(
                    roomListViewModel,
                    listState,
                    header = {
                        item("SpaceTitle") {
                            Row(
                                Modifier.fillMaxWidth()
                                    .padding(
                                        start = spacing.middle,
                                        end = spacing.middle,
                                        top = spacing.middle,
                                        bottom = spacing.small,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                            ) {
                                Box(Modifier.size(avatarSize().dp)) {
                                    if (content != null) {
                                        SpaceAvatar(name = content.name.orEmpty(), image = viewModel.spaceImage)
                                    }
                                }
                                Text(
                                    content?.name.orEmpty(),
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        item("SpaceExplore") {
                            Box(
                                Modifier.padding(start = spacing.middle, end = spacing.middle, bottom = spacing.middle)
                            ) {
                                SpaceNavigationRow(exploreSelected, onOpenExplore) {
                                    Icon(Icons.Default.Explore, null)
                                    Text(
                                        i18n.spacesExplore(),
                                        Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                                }
                            }
                        }
                        if (state == SpaceContentState.Error)
                            item("SpaceError") { Column(Modifier.padding(spacing.middle)) { SpaceError(viewModel) } }
                        item("SpaceRooms") { SpaceSectionHeader(i18n.spacesRooms()) }
                        if (rooms.isEmpty() && content != null)
                            item("EmptyRoomList") {
                                Text(
                                    if (searchTerm.text.isNotBlank()) i18n.roomListNoSearchResults()
                                    else i18n.spacesNoJoinedRooms(),
                                    Modifier.padding(horizontal = spacing.middle),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                    },
                    footer = {
                        if (subspaces.isNotEmpty())
                            item("SpaceSubspaces") { SpaceSectionHeader(i18n.spacesSubspaces()) }
                        spaceListItems(viewModel, subspaces)
                    },
                )
                VerticalScrollbar(Modifier.align(Alignment.CenterEnd).fillMaxHeight(), listState, false)
            }
        }
    }
}

@Composable
private fun SpaceNavigationRow(selected: Boolean, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .buttonPointerModifier()
            .padding(MaterialTheme.messengerDpConstants.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.small),
    ) {
        content()
    }
}

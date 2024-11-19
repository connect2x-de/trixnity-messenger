package de.connect2x.messenger.compose.view.roomlist.header

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.AvatarWithImage
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.common.collectAsStateForTextField
import de.connect2x.messenger.compose.view.common.thenNullable
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import net.folivo.trixnity.core.model.RoomId

@Composable
fun Banner(
    visible: Boolean,
    background: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    AnimatedVisibility(
        visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .thenNullable(onClick?.let {
                    Modifier
                        .buttonPointerModifier()
                        .clickable(interactionSource, indication = null) { onClick() }
                }),
            content = content,
        )
    }
}

@Composable
fun SyncErrorBanner(roomListViewModel: RoomListViewModel) {
    val i18n = DI.get<I18nView>()
    val syncStateError = roomListViewModel.syncStateError.collectAsState().value
    val allSyncError = roomListViewModel.allSyncError.collectAsState().value
    val visible = syncStateError.any { (_, error) -> error }
    Banner(
        visible,
        background = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, i18n.roomListSyncErrorNoConnection())
            Spacer(Modifier.size(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                if (allSyncError) {
                    Text(
                        i18n.roomListSyncErrorNoInternet(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                } else {
                    Text(
                        i18n.roomListSyncErrorAccounts(syncStateError.keys.joinToString { it.full }),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    i18n.roomListSyncErrorSendMessages(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun NotVerifiedBanner(roomListViewModel: RoomListViewModel) {
    val i18n = DI.get<I18nView>()
    val firstUserNotVerified = roomListViewModel.unverifiedAccounts.collectAsState().value.firstOrNull()
    Banner(
        firstUserNotVerified != null,
        background = MaterialTheme.colorScheme.errorContainer,
        onClick = {
            if (firstUserNotVerified != null) roomListViewModel.verifyAccount(firstUserNotVerified)
        },
    ) {
        Row(
            Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Warning, i18n.roomListAccountNotVerifiedIcon(), tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.size(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                if (firstUserNotVerified != null) Text(
                    i18n.roomListAccountNotVerifiedMessage(firstUserNotVerified),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun SearchRoomsBanner(roomListViewModel: RoomListViewModel) {
    val i18n = DI.get<I18nView>()
    val showSearch = roomListViewModel.showSearch.collectAsState().value
    val searchTerm = roomListViewModel.searchTerm.collectAsStateForTextField().value
    val focusRequester = remember { FocusRequester() }
    Banner(
        showSearch,
        background = MaterialTheme.colorScheme.surface,
    ) {
        Surface(tonalElevation = 8.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchTerm,
                    onValueChange = { roomListViewModel.searchTerm.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .heightIn(40.dp)
                        .focusRequester(focusRequester)
                        .onKeyEvent {
                            if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                                roomListViewModel.showSearch.value = false
                                true
                            } else {
                                false
                            }
                        },
                    shape = RoundedCornerShape(8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, i18n.roomListSearch()) },
                    placeholder = {
                        Text(
                            text = "${i18n.roomListSearch()}...",
                            color = Color.Gray,
                        )
                    }
                )
                HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
            }
        }
    }
    LaunchedEffect(showSearch) {
        if (showSearch) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun SpacesBanner(roomListViewModel: RoomListViewModel) {
    val i18n = DI.get<I18nView>()
    val spaces = roomListViewModel.spaces.collectAsState().value
    val showSpaces = roomListViewModel.showSpaces.collectAsState().value
    val listState = rememberLazyListState()
    Banner(
        spaces.isNotEmpty() && showSpaces,
        background = MaterialTheme.colorScheme.surface,
    ) {
        Surface(tonalElevation = 8.dp) {
            Column(Modifier.fillMaxWidth()) {
                Box {
                    LazyRow(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.Start),
                        userScrollEnabled = true, // only works in Android
                        state = listState, // for desktop
                    ) {
                        item {
                            Tooltip(
                                tooltip = { TooltipText(i18n.roomListNoFilter()) },
                                modifier = Modifier
                                    .clickable(indication = null, interactionSource = MutableInteractionSource()) {
                                        roomListViewModel.activeSpace.value = null
                                    }
                                    .buttonPointerModifier(),
                                onClick = { // on android the long press "eats" the click
                                    roomListViewModel.activeSpace.value = null
                                }
                            ) {
                                SelectedSpace(roomListViewModel, thisSpace = null) {
                                    AvatarWithImage(avatarSize().minus(4).dp) {
                                        Icon(Icons.Default.FilterListOff, i18n.roomListNoFilter())
                                    }
                                }
                            }
                        }
                        items(spaces, { space -> space.roomId.full }) { space ->
                            Tooltip(
                                tooltip = { TooltipText(space.name) },
                                modifier = Modifier
                                    .clickable(indication = null, interactionSource = MutableInteractionSource()) {
                                        roomListViewModel.activeSpace.value = space.roomId
                                    }
                                    .buttonPointerModifier(),
                                onClick = { // on android the long press "eats" the click
                                    roomListViewModel.activeSpace.value = space.roomId
                                }
                            ) {
                                SelectedSpace(roomListViewModel, space.roomId) {
                                    Avatar(
                                        image = space.image,
                                        initials = space.initials,
                                        size = avatarSize().minus(4).dp,
                                    )
                                }
                            }
                        }
                    }
                    HorizontalScrollbar(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        listState,
                        reverseLayout = false,
                    )
                }
                HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
            }
        }
    }
}

@Composable
fun SelectedSpace(roomListViewModel: RoomListViewModel, thisSpace: RoomId?, content: @Composable () -> Unit) {
    val activeSpace = roomListViewModel.activeSpace.collectAsState().value
    Box(
        Modifier
            .size(avatarSize().dp)
            .border(
                4.dp,
                if (activeSpace == thisSpace) {
                    Brush.radialGradient(
                        0.8f to MaterialTheme.colorScheme.primary,
                        1.0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                } else {
                    Brush.radialGradient(
                        0.8f to MaterialTheme.colorScheme.secondaryContainer,
                        1.0f to MaterialTheme.colorScheme.secondaryContainer.copy(0.1f)
                    )
                },
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

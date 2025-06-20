package de.connect2x.messenger.compose.view.roomlist.header

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.maxLength
import de.connect2x.messenger.compose.view.common.thenNullable
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.SurfaceStyle
import de.connect2x.messenger.compose.view.theme.components.ThemedHorizontalDivider
import de.connect2x.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel

@Composable
fun Banner(
    style: SurfaceStyle = MaterialTheme.components.commonBanner,
    visible: Boolean,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val modifier = onClick?.let {
        Modifier
            .buttonPointerModifier()
            .clickable(interactionSource, indication = ripple(bounded = true), onClick = onClick)
    }

    AnimatedVisibility(
        visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        ThemedSurface(
            style = style,
            modifier = Modifier.fillMaxWidth().thenNullable(modifier),
        ) {
            content()
        }
    }
}

@Composable
fun SyncErrorBanner(roomListViewModel: RoomListViewModel) {
    val i18n = DI.get<I18nView>()
    val syncStates = roomListViewModel.syncStates.collectAsState().value
    Banner(
        style = MaterialTheme.components.errorBanner,
        visible = syncStates.failedFor.isNotEmpty(),
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, i18n.roomListSyncErrorNoConnection())
            Spacer(Modifier.size(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                if (syncStates.failedForAll) {
                    Text(
                        i18n.roomListSyncErrorNoInternet(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                } else {
                    Text(
                        i18n.roomListSyncErrorAccounts(syncStates.joinFailedToString()),
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
        style = MaterialTheme.components.errorBanner,
        visible = firstUserNotVerified != null,
        onClick = {
            if (firstUserNotVerified != null) roomListViewModel.verifyAccount(firstUserNotVerified)
        },
    ) {
        Row(
            Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Warning,
                i18n.roomListAccountNotVerifiedIcon(),
            )
            Spacer(Modifier.size(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                if (firstUserNotVerified != null) {
                    Text(
                        i18n.roomListAccountNotVerifiedMessage(firstUserNotVerified),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun SearchRoomsBanner(roomListViewModel: RoomListViewModel) {
    val i18n = DI.get<I18nView>()
    val showSearch = roomListViewModel.showSearch.collectAsState().value
    val (searchTerm, maxLength) = roomListViewModel.searchTerm.collectAsTextFieldValueState()
    val focusRequester = remember { FocusRequester() }
    Banner(
        style = MaterialTheme.components.commonBanner,
        visible = showSearch,
    ) {
        Column(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = searchTerm.value,
                onValueChange = { searchTerm.value = it.maxLength(maxLength) },
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
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    autoCorrectEnabled = false
                )
            )
            ThemedHorizontalDivider()
        }
    }
    LaunchedEffect(showSearch) {
        if (showSearch) {
            focusRequester.requestFocus()
        }
    }
}

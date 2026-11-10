package de.connect2x.trixnity.messenger.compose.view.space

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.common.AccountColorBar
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.common.LocalAccountColorsEnabled
import de.connect2x.trixnity.messenger.compose.view.common.ThemedLoadingButton
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.roomlist.header.AccountAvatar
import de.connect2x.trixnity.messenger.compose.view.roomlist.header.AccountOptions
import de.connect2x.trixnity.messenger.compose.view.roomlist.room.RoomName
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.AvatarStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedHorizontalDivider
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceBrowserItem
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceContentState
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceContentViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceMembershipActionState

@Composable
fun SpaceBrowserView(viewModel: SpaceContentViewModel, accountViewModel: AccountViewModel) {
    val i18n = DI.get<I18nView>()
    val state by viewModel.state.collectAsState()
    val lazyListState = rememberLazyListState()
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchTerm by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val query = if (showSearch) searchTerm.text.trim() else ""
    ThemedSurface(style = MaterialTheme.components.sidebar) {
        Column(Modifier.fillMaxSize()) {
            SpaceHeader {
                AccountAvatar(accountViewModel)
                SpaceSearchButton(showSearch) { showSearch = it }
                AccountOptions(accountViewModel)
            }
            SpaceSearchBanner(showSearch, searchTerm, { searchTerm = it }, { showSearch = false }, i18n.spacesSearch())
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxSize()) {
                    when (val current = state) {
                        SpaceContentState.Loading -> LoadingSpinner()
                        SpaceContentState.Error -> SpaceError(viewModel)
                        is SpaceContentState.Ready -> {
                            val content = current.content
                            if (content.items.isEmpty()) {
                                Text(
                                    i18n.spacesEmptyList(),
                                    Modifier.padding(MaterialTheme.messengerDpConstants.middle),
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                LazyColumn(state = lazyListState) {
                                    val filtered = content.items.filter { it.name.contains(query, ignoreCase = true) }
                                    if (filtered.isEmpty() && query.isNotEmpty()) {
                                        item {
                                            Text(
                                                i18n.spacesNoSearchResults(),
                                                Modifier.padding(MaterialTheme.messengerDpConstants.middle),
                                            )
                                        }
                                    }
                                    val (invitations, spaces) = filtered.partition { it.invited }
                                    if (invitations.isNotEmpty()) {
                                        item { SpaceSectionHeader(i18n.spacesInvitations()) }
                                        spaceListItems(viewModel, invitations, showAccount = content.userId == null)
                                    }
                                    spaceListItems(viewModel, spaces, showAccount = content.userId == null)
                                }
                                VerticalScrollbar(
                                    Modifier.align(Alignment.CenterEnd),
                                    lazyListState,
                                    reverseLayout = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SpaceBrowserItemView(
    viewModel: SpaceContentViewModel,
    item: SpaceBrowserItem,
    showAccount: Boolean = false,
    showOpenAction: Boolean = false,
) {
    val i18n = DI.get<I18nView>()
    val canRequestMembership = item.canJoin
    val available = item.joined || item.invited || canRequestMembership
    Box {
        Box(Modifier.matchParentSize().padding(start = 2.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)) {
            AccountColorBar(item.accountColor, Modifier.fillMaxHeight())
        }
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .alpha(if (available) 1f else 0.38f)
                    .clickable(enabled = item.joined, role = Role.Button) { viewModel.select(item) }
                    .heightIn(min = 72.dp)
                    .padding(
                        start = if (LocalAccountColorsEnabled.current && item.accountColor != null) 20.dp else 14.dp,
                        end = 10.dp,
                        top = 10.dp,
                        bottom = 10.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SpaceAvatar(
                name = item.name,
                image = rememberSpaceAvatar(viewModel, item.userId, item.roomId),
                style = if (item.isSpace) AvatarStyle.space() else MaterialTheme.components.avatar,
            )
            Column(Modifier.weight(1f)) {
                RoomName(item.name)
                if (showAccount) SpaceAccountName(item.userId)
                item.memberCount?.let { Text(i18n.spacesMemberCount(it), style = MaterialTheme.typography.bodySmall) }
                item.childCount?.let {
                    Text(
                        i18n.spacesChildCounts(
                            it - item.subspaceCount - item.unknownChildCount,
                            item.subspaceCount,
                            item.unknownChildCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (!item.joined && item.invited)
                    Text(i18n.spacesInvitations(), style = MaterialTheme.typography.bodySmall)
                if (item.membershipActionState == SpaceMembershipActionState.Failed) {
                    Text(
                        i18n.spacesMembershipError(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            SpaceMembershipButtons(viewModel, item)
            if (showOpenAction && item.joined) {
                ThemedIconButton(onClick = { viewModel.select(item) }, modifier = Modifier.buttonPointerModifier()) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, i18n.spacesOpen())
                }
            }
        }
    }
}

@Composable
internal fun SpaceError(viewModel: SpaceContentViewModel) {
    val i18n = DI.get<I18nView>()
    Text(i18n.spacesLoadError())
    ThemedButton(onClick = viewModel::refresh, style = MaterialTheme.components.primaryButton) {
        Text(i18n.spacesRetry())
    }
}

@Composable
internal fun SpaceMembershipButtons(viewModel: SpaceContentViewModel, item: SpaceBrowserItem) {
    val i18n = DI.get<I18nView>()
    if (item.invited) {
        ThemedLoadingButton(
            isLoading = item.membershipActionState == SpaceMembershipActionState.Rejecting,
            onClick = { viewModel.reject(item) },
            enabled = !item.membershipActionState.isBusy,
            style = MaterialTheme.components.commonButton,
        ) {
            Text(i18n.spacesDecline())
        }
    }
    if (item.canJoin && !item.joined) SpaceJoinButton(viewModel, item)
}

@Composable
private fun SpaceJoinButton(viewModel: SpaceContentViewModel, item: SpaceBrowserItem) {
    val i18n = DI.get<I18nView>()
    ThemedLoadingButton(
        isLoading = item.membershipActionState == SpaceMembershipActionState.Joining,
        onClick = { viewModel.join(item) },
        enabled = !item.membershipActionState.isBusy,
        style = MaterialTheme.components.primaryButton,
    ) {
        Text(
            when (item.membershipActionState) {
                SpaceMembershipActionState.Idle -> i18n.roomListJoin()
                SpaceMembershipActionState.Joining -> i18n.spacesJoining()
                SpaceMembershipActionState.Rejecting -> i18n.spacesDecline()
                SpaceMembershipActionState.Failed -> i18n.spacesRetry()
            }
        )
    }
}

internal fun LazyListScope.spaceListItems(
    viewModel: SpaceContentViewModel,
    spaces: List<SpaceBrowserItem>,
    showAccount: Boolean = false,
    showOpenAction: Boolean = false,
) {
    itemsIndexed(spaces, key = { _, item -> "space/${item.userId.full}/${item.roomId.full}" }) { index, item ->
        Box {
            SpaceBrowserItemView(viewModel, item, showAccount = showAccount, showOpenAction = showOpenAction)
            if (index > 0) ThemedHorizontalDivider(style = MaterialTheme.components.roomListDivider)
        }
    }
}

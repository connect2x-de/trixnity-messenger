package de.connect2x.trixnity.messenger.compose.view.space

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.root.IsSinglePane
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.AvatarStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceBrowserItem
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceContentState
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceContentViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceExploreViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceMembershipActionState

@Composable
internal fun SpaceExploreView(viewModel: SpaceExploreViewModel) {
    val i18n = DI.get<I18nView>()
    val state by viewModel.state.collectAsState()
    val content = (state as? SpaceContentState.Ready)?.content
    val invitation = content?.takeIf { it.invited }?.items?.singleOrNull()
    ThemedSurface(style = MaterialTheme.components.sidebar) {
        Column(Modifier.fillMaxSize()) {
            SpaceHeader(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                if (IsSinglePane.current) SpaceBackButton(onBack = viewModel::back)
                Text(
                    i18n.spacesExplore(),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ThemedIconButton(onClick = viewModel::openSettings, enabled = content != null && invitation == null) {
                    Icon(Icons.Default.Settings, i18n.spacesSettings())
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (invitation != null) SpaceInvitation(viewModel, invitation) else SpaceExploreRooms(viewModel, state)
            }
        }
    }
}

@Composable
private fun SpaceInvitation(viewModel: SpaceContentViewModel, invitation: SpaceBrowserItem) {
    val i18n = DI.get<I18nView>()
    val spacing = MaterialTheme.messengerDpConstants
    Box(Modifier.fillMaxSize().padding(spacing.middle), contentAlignment = Alignment.Center) {
        Column(
            modifier =
                Modifier.widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = spacing.middle),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.middle),
        ) {
            SpaceAvatar(name = invitation.name, image = viewModel.spaceImage, size = 72.dp)
            Text(invitation.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(i18n.settingsRoomMemberListInvited(), style = MaterialTheme.typography.bodyLarge)
            SpaceAccountName(invitation.userId)
            invitation.memberCount?.let { Text(i18n.spacesMemberCount(it), style = MaterialTheme.typography.bodySmall) }
            if (invitation.membershipActionState == SpaceMembershipActionState.Failed) {
                Text(
                    i18n.spacesMembershipError(),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.middle)) {
                SpaceMembershipButtons(viewModel, invitation)
            }
        }
    }
}

@Composable
private fun SpaceExploreRooms(viewModel: SpaceContentViewModel, state: SpaceContentState) {
    val i18n = DI.get<I18nView>()
    val spacing = MaterialTheme.messengerDpConstants
    var search by remember { mutableStateOf("") }
    when (state) {
        SpaceContentState.Loading -> LoadingSpinner()
        SpaceContentState.Error -> Column(Modifier.padding(spacing.middle)) { SpaceError(viewModel) }
        is SpaceContentState.Ready -> {
            val content = state.content
            val filtered = content.items.filter { it.name.contains(search.trim(), ignoreCase = true) }
            val (subspaces, rooms) = filtered.partition { it.isSpace }
            val listState = rememberLazyListState()
            Box(Modifier.fillMaxSize()) {
                LazyColumn(state = listState) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(spacing.middle),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.middle),
                        ) {
                            SpaceAvatar(
                                name = content.name.orEmpty(),
                                image = viewModel.spaceImage,
                                size = 72.dp,
                                style = AvatarStyle.space(),
                            )
                            Text(content.name.orEmpty(), style = MaterialTheme.typography.headlineSmall)
                            SpaceAccountName(content.userId)
                            content.topic?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            content.memberCount?.let {
                                Text(i18n.spacesMemberCount(it), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            label = { Text(i18n.spacesSearchRooms()) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.middle),
                        )
                    }
                    if (filtered.isEmpty() && search.isNotBlank())
                        item { Text(i18n.spacesNoSearchResults(), Modifier.padding(spacing.middle)) }
                    if (content.hierarchyFailed)
                        item { Column(Modifier.padding(spacing.middle)) { SpaceError(viewModel) } }
                    item { SpaceSectionHeader("${i18n.spacesRooms()} (${rooms.size})") }
                    if (content.items.isEmpty() && !content.loadingHierarchy && !content.hierarchyFailed)
                        item { Text(i18n.spacesEmptySpace(), Modifier.padding(horizontal = spacing.middle)) }
                    spaceListItems(viewModel, rooms, showOpenAction = true)
                    if (subspaces.isNotEmpty())
                        item { SpaceSectionHeader("${i18n.spacesSubspaces()} (${subspaces.size})") }
                    spaceListItems(viewModel, subspaces, showOpenAction = true)
                    if (content.loadingHierarchy) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = spacing.middle),
                                contentAlignment = Alignment.Center,
                            ) {
                                LoadingSpinner()
                            }
                        }
                    }
                }
                VerticalScrollbar(Modifier.align(Alignment.CenterEnd), listState, reverseLayout = false)
            }
        }
    }
}

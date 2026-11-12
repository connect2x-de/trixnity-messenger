package de.connect2x.trixnity.messenger.compose.view.space

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.compose.view.common.AccountColorBar
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.navigation.AppNavigationRailItem
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.AvatarStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceContentState
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceContentViewModel

internal fun LazyListScope.spaceNavigationItems(
    viewModel: SpaceContentViewModel,
    state: SpaceContentState,
    selectedSpace: Pair<UserId, RoomId>?,
    i18n: I18nView,
) {
    when (state) {
        SpaceContentState.Loading ->
            item { ThemedProgressIndicator(Modifier.size(24.dp), MaterialTheme.components.circularProgressIndicator) }
        SpaceContentState.Error ->
            item {
                AppNavigationRailItem(i18n.spacesRetry(), false, viewModel::refresh) {
                    Icon(Icons.Default.Refresh, null)
                }
            }
        is SpaceContentState.Ready ->
            items(
                state.content.items.filter { it.joined || it.invited },
                key = { "${it.userId.full}/${it.roomId.full}" },
            ) { item ->
                val label =
                    when {
                        item.isInLoop -> "${item.name}: ${i18n.spacesLoopWarning()}"
                        item.invited -> "${item.name} (${i18n.spacesInvitations()})"
                        else -> item.name
                    }
                AppNavigationRailItem(
                    label,
                    selectedSpace == (item.userId to item.roomId),
                    { viewModel.select(item) },
                ) {
                    Box {
                        AccountColorBar(item.accountColor, Modifier.height(36.dp).offset(x = (-10).dp))
                        BadgedBox(
                            badge = {
                                if (item.isInLoop) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    ) {
                                        Icon(Icons.Default.Warning, null, Modifier.size(12.dp))
                                    }
                                } else if (item.invited) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) {
                                        Icon(Icons.Default.Mail, null, Modifier.size(12.dp))
                                    }
                                }
                            }
                        ) {
                            SpaceAvatar(
                                name = item.name,
                                image = rememberSpaceAvatar(viewModel, item.userId, item.roomId),
                                style = AvatarStyle.space(),
                                size = 36.dp,
                            )
                        }
                    }
                }
            }
    }
}

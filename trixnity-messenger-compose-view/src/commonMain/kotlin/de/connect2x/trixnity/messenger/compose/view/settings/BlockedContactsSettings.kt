package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.modifier.focusHighlighting
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItem
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.settings.BlockedContact
import de.connect2x.trixnity.messenger.viewmodel.settings.BlockedContactsSettingsViewModel

interface BlockedContactsSettingsView {
    @Composable
    fun create(blockedContactsSettingsViewModel: BlockedContactsSettingsViewModel)
}

@Composable
fun BlockedContactsSettings(blockedContactsSettingsViewModel: BlockedContactsSettingsViewModel) {
    DI.get<BlockedContactsSettingsView>().create(blockedContactsSettingsViewModel)
}

class BlockedContactsSettingsViewImpl : BlockedContactsSettingsView {
    @Composable
    override fun create(blockedContactsSettingsViewModel: BlockedContactsSettingsViewModel) {
        val userList by blockedContactsSettingsViewModel.blockedContactsList.collectAsState()
        val i18n = DI.get<I18nView>()
        val state = rememberLazyListState()
        val focusedItem = remember(userList) { mutableStateOf(userList.firstOrNull()?.userId) }

        Column {
            Header(blockedContactsSettingsViewModel::back, i18n.blockedContactsHeader())
            if (userList.isEmpty()) {
                ThemedListItem(
                    style = MaterialTheme.components.settingsItem,
                    headlineContent = {
                        Text(
                            i18n.blockedContactsAccountLabel(blockedContactsSettingsViewModel.account.full),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                )
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.PersonOff,
                            null,
                            modifier = Modifier.requiredSize(72.dp),
                            tint = MaterialTheme.colorScheme.primaryContainer,
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            i18n.blockedContactsEmptyListLabel(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            } else {
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        Modifier.fillMaxSize().rovingFocusContainer(
                            listState = state,
                            focusedItem = focusedItem
                        ),
                        state
                    ) {
                        item("header") {
                            ThemedListItem(
                                style = MaterialTheme.components.settingsItem,
                                headlineContent = {
                                    Text(
                                        i18n.blockedContactsAccountLabel(blockedContactsSettingsViewModel.account.full),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            )
                        }
                        items(userList, key = { value -> value.userId }) { user ->
                            IgnoredUserListElement(
                                user = user, i18n = i18n,
                                isFocused = { focusedItem.value == user.userId },
                                onFocus = { focusedItem.value = user.userId },
                            ) {
                                val userListWithoutThisOne = userList.filter { it.userId != user.userId }
                                blockedContactsSettingsViewModel.unblockContact(user.userId)
                                if (userListWithoutThisOne.isEmpty()) return@IgnoredUserListElement
                                focusedItem.value = userListWithoutThisOne[0].userId
                            }
                        }
                    }
                    if (state.canScrollForward || state.canScrollBackward) {
                        VerticalScrollbar(Modifier.align(Alignment.CenterEnd), state, false)
                    }
                }
            }
        }
    }
}

@Composable
fun IgnoredUserListElement(
    user: BlockedContact,
    i18n: I18nView,
    isFocused: () -> Boolean,
    onFocus: () -> Unit,
    onRemove: () -> Unit
) {
    ThemedListItem(
        style = MaterialTheme.components.settingsItem,
        modifier = Modifier.rovingFocusItem(isFocused, onFocus).focusHighlighting(),
        leadingContent = { Icon(Icons.Default.PersonOff, null) },
        headlineContent = { Text(user.userId.full) },
        trailingContent = {
            if (user.isUnblocking) {
                ThemedProgressIndicator(style = MaterialTheme.components.extraSmallCircularProgressIndicator)
            } else {
                Tooltip({ Text(i18n.unblockContactDescription()) }) {
                    ThemedIconButton(
                        style = MaterialTheme.components.commonIconButton,
                        onClick = onRemove,
                    ) {
                        Icon(Icons.Default.RemoveCircle, i18n.unblockContactDescription())
                    }
                }
            }
        },
    )
}

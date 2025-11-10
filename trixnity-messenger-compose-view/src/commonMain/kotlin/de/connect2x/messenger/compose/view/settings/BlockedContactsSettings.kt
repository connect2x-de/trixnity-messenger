package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedListItem
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.messenger.compose.view.theme.messengerFocusIndicator
import de.connect2x.messenger.compose.view.util.LocalRovingFocus
import de.connect2x.messenger.compose.view.util.RovingFocusContainer
import de.connect2x.messenger.compose.view.util.RovingFocusItem
import de.connect2x.messenger.compose.view.util.rovingFocusItem
import de.connect2x.messenger.compose.view.util.verticalRovingFocus
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
        val references = remember(userList) { userList.map { it.userId } }
        val defaultItem = references.firstOrNull()
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
                    RovingFocusContainer {
                        LazyColumn(
                            Modifier.fillMaxSize().verticalRovingFocus(
                                default = defaultItem,
                                scroll = { item ->
                                    val index = references.indexOf(item)
                                    if (index != -1) {
                                        state.scrollToItem(index)
                                    }
                                },
                                up = {
                                    val currentItem = activeRef.value ?: defaultItem
                                    val currentIndex = references.indexOf(currentItem)
                                    val nextIndex = currentIndex.minus(1).coerceIn(references.indices)
                                    references[nextIndex]
                                },
                                down = {
                                    val currentItem = activeRef.value ?: defaultItem
                                    val currentIndex = references.indexOf(currentItem)
                                    val nextIndex = currentIndex.plus(1).coerceIn(references.indices)
                                    references[nextIndex]
                                },
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
                            itemsIndexed(userList, key = { _, value -> value.userId }) { index, user ->
                                RovingFocusItem(user.userId, defaultItem) {
                                    val focusContainer = LocalRovingFocus.current

                                    IgnoredUserListElement(user, i18n) {
                                         val referencesWithoutThisOne = references - user.userId
                                         blockedContactsSettingsViewModel.unblockContact(user.userId)

                                         if (referencesWithoutThisOne.isEmpty()) return@IgnoredUserListElement

                                         focusContainer?.selectItem(
                                             referencesWithoutThisOne[index.coerceIn(referencesWithoutThisOne.indices)]
                                         )
                                    }
                                }
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
    onRemove: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused = interactionSource.collectIsFocusedAsState()
    val focusedBorder =
        if (IsFocusHighlighting.current && focused.value) {
            Modifier.border(
                width = MaterialTheme.messengerFocusIndicator.borderWidth,
                color = MaterialTheme.colorScheme.onBackground,
            )
        } else Modifier

    ThemedListItem(
        style = MaterialTheme.components.settingsItem,
        modifier = Modifier.then(focusedBorder),
        leadingContent = { Icon(Icons.Default.PersonOff, null) },
        headlineContent = { Text(user.userId.full) },
        trailingContent = {
            if (user.isUnblocking) {
                ThemedProgressIndicator(style = MaterialTheme.components.extraSmallCircularProgressIndicator)
            } else {
                Tooltip({ Text(i18n.unblockContactDescription()) }) {
                    ThemedIconButton(
                        style = MaterialTheme.components.commonIconButton,
                        modifier = Modifier.rovingFocusItem(),
                        interactionSource = interactionSource,
                        onClick = onRemove,
                    ) {
                        Icon(Icons.Default.RemoveCircle, i18n.unblockContactDescription())
                    }
                }
            }
        },
    )
}

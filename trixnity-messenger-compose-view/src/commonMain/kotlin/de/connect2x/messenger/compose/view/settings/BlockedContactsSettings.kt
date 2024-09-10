package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.settings.BlockedContact
import de.connect2x.trixnity.messenger.viewmodel.settings.BlockedContactsSettingsViewModel

interface BlockedContactsSettingsView {
    @Composable
    fun create(blockedContactsSettingsViewModel: BlockedContactsSettingsViewModel)
}

@Composable
fun BlockedContactsSettings(blockedContactsSettingsViewModel: BlockedContactsSettingsViewModel) {
    DI.current.get<BlockedContactsSettingsView>().create(blockedContactsSettingsViewModel)
}

class BlockedContactsSettingsViewImpl : BlockedContactsSettingsView {
    @Composable
    override fun create(blockedContactsSettingsViewModel: BlockedContactsSettingsViewModel) {
        val userList = blockedContactsSettingsViewModel.blockedContactsList.collectAsState().value
        val i18n = DI.current.get<I18nView>()
        val scroll = rememberScrollState()
        Box(Modifier.fillMaxSize()) {
            Column {
                Header(blockedContactsSettingsViewModel::back, i18n.blockedContactsHeader())
                Text(
                    i18n.blockedContactsAccountLabel(blockedContactsSettingsViewModel.account.full),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(16.dp),
                )
                when (userList.isNotEmpty()) {
                    true -> Box {
                        Column(Modifier.padding(10.dp).verticalScroll(scroll)) {
                            userList.map { user ->
                                IgnoredUserListElement(user, i18n, blockedContactsSettingsViewModel)
                            }
                        }
                        VerticalScrollbar(
                            Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            scroll,
                        )
                    }

                    false -> Box(Modifier.fillMaxWidth()) {
                        Text(
                            i18n.blockedContactsEmptyListLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 100.dp,
                            ).align(Alignment.Center),
                        )
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
    viewModel: BlockedContactsSettingsViewModel,
) = Box(Modifier.fillMaxWidth()) {
    val rowInteractionSource = remember { MutableInteractionSource() }
    val isRowHovered by rowInteractionSource.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight(unbounded = true)
            .hoverable(rowInteractionSource)
            .background(if (isRowHovered) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val buttonInteractionSource = remember { MutableInteractionSource() }
        val isButtonHovered by buttonInteractionSource.collectIsHoveredAsState()
        Icon(
            Icons.Default.PersonOff,
            contentDescription = i18n.blockedContactDescription(),
            modifier = Modifier.wrapContentSize(unbounded = true),
        )
        Text(
            user.userId.full,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(8.dp).weight(1.0f, true),
        )
        Box(
            Modifier
                .size(24.dp) // To keep child elements centered to the same pivot.
                .wrapContentSize(unbounded = true)
        ) {
            when (user.isUnblocking) {
                true -> CircularProgressIndicator(
                    Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
                )

                else -> IconButton(
                    onClick = { viewModel.unblockContact(user.userId) },
                    Modifier
                        .align(Alignment.Center)
                        .hoverable(buttonInteractionSource)
                        .buttonPointerModifier()
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircle,
                        contentDescription = i18n.unblockContactDescription(),
                        modifier = Modifier.align(Alignment.Center),
                        tint = if (isButtonHovered) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceTint,
                    )
                }
            }
        }
    }
}

package de.connect2x.trixnity.messenger.compose.view.roomlist.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.ExpandableSection
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.icons.HelpIcon
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.i18n.getExplanation
import de.connect2x.trixnity.messenger.compose.view.i18n.getExplanationWhenEncrypted
import de.connect2x.trixnity.messenger.compose.view.i18n.getStateName
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemRadioButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel

interface CreateNewGroupOptionsView {
    @Composable fun create(createNewGroupViewModel: CreateNewGroupViewModel, historyExpanded: MutableState<Boolean>)
}

@Composable
fun CreateNewGroupOptions(
    createNewGroupViewModel: CreateNewGroupViewModel,
    historyExpanded: MutableState<Boolean> = remember { mutableStateOf(false) },
) {
    DI.get<CreateNewGroupOptionsView>().create(createNewGroupViewModel, historyExpanded)
}

class CreateNewGroupOptionsViewImpl : CreateNewGroupOptionsView {
    @Composable
    override fun create(createNewGroupViewModel: CreateNewGroupViewModel, historyExpanded: MutableState<Boolean>) {
        val i18n = DI.get<I18nView>()
        val isPrivate by createNewGroupViewModel.isPrivate.collectAsState()
        val isEncrypted by createNewGroupViewModel.isEncrypted.collectAsState()

        val roomOptionsString = buildString {
            val roomType =
                when {
                    isPrivate && isEncrypted -> "${i18n.roomTypePrivate()} & ${i18n.roomTypeEncrypted()}"
                    !isPrivate && isEncrypted -> "${i18n.roomTypePublic()} & ${i18n.roomTypeEncrypted()}"
                    !isPrivate && !isEncrypted -> "${i18n.roomTypePublic()} & ${i18n.roomTypeUnencrypted()}"
                    else -> i18n.roomTypeForbidden()
                }
            append(roomType)
        }

        val expanded = rememberSaveable("moreOptions") { mutableStateOf(false) }

        SmallSpacer()
        Column {
            ExpandableSection(
                roomOptionsString,
                expanded,
                modifier = Modifier.padding(horizontal = 10.dp),
                icon = Icons.Default.Settings,
            ) {
                Column {
                    GroupIsPrivate(createNewGroupViewModel)
                    GroupDirectoryVisibility(createNewGroupViewModel)
                    GroupEncryption(createNewGroupViewModel)
                    GroupHistoryVisibility(createNewGroupViewModel)
                }
            }
        }
    }
}

@Composable
fun GroupIsPrivate(createNewGroupViewModel: CreateNewGroupViewModel) {
    val i18n = DI.get<I18nView>()
    val isPrivate by createNewGroupViewModel.isPrivate.collectAsState()

    ThemedListItemSwitch(
        headlineContent = {
            Text(i18n.roomVisibility() + if (isPrivate) i18n.roomTypePrivate() else i18n.roomTypePublic())
        },
        selected = isPrivate,
        onChange = { createNewGroupViewModel.setIsPrivate(it) },
        leadingContent = { HelpIcon(if (isPrivate) i18n.roomTypePrivateInfo() else i18n.roomTypePublicInfo()) },
        style = MaterialTheme.components.settingsItem,
    )
}

@Composable
fun GroupDirectoryVisibility(createNewGroupViewModel: CreateNewGroupViewModel) {
    val i18n = DI.get<I18nView>()
    val isPrivate by createNewGroupViewModel.isPrivate.collectAsState()
    val directoryVisibilityIsPublic by createNewGroupViewModel.directoryVisibilityIsPublic.collectAsState()
    ThemedListItemSwitch(
        headlineContent = { Text(i18n.roomDirectoryVisibility()) },
        selected = directoryVisibilityIsPublic,
        onChange = { createNewGroupViewModel.setDirectoryVisibilityIsPublic(it) },
        leadingContent = { HelpIcon(i18n.roomDirectoryVisibilityInfo()) },
        supportingContent = { if (isPrivate) Text(i18n.roomDirectoryVisibilityExplanation()) },
        enabled = !isPrivate,
        style = MaterialTheme.components.settingsItem,
    )
}

@Composable
fun GroupEncryption(createNewGroupViewModel: CreateNewGroupViewModel) {
    val i18n = DI.get<I18nView>()
    val isPrivate by createNewGroupViewModel.isPrivate.collectAsState()
    val isEncrypted by createNewGroupViewModel.isEncrypted.collectAsState()
    ThemedListItemSwitch(
        headlineContent = {
            Row {
                Text(i18n.roomEncryption())
                Icon(if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen, null)
            }
        },
        selected = isEncrypted,
        onChange = { createNewGroupViewModel.changeEncryptionStatus(it) },
        leadingContent = {
            HelpIcon(if (isEncrypted) i18n.roomTypeEncryptedInfo() else i18n.roomTypeUnencryptedInfo())
        },
        supportingContent = { if (!isPrivate) Text(i18n.roomEncryptionExplanation()) },
        enabled = isPrivate,
        style = MaterialTheme.components.settingsItem,
    )
}

@Composable
fun GroupHistoryVisibility(createNewGroupViewModel: CreateNewGroupViewModel) {
    val i18n = DI.get<I18nView>()

    val historyVisibility by createNewGroupViewModel.optionalRoomHistoryVisibility.collectAsState()

    val historyExpanded = rememberSaveable("moreOptions") { mutableStateOf(false) }

    /**
     * this cannot be a [de.connect2x.trixnity.messenger.compose.view.common.RadioSetting] because expanded is
     * controlled via an argument
     */
    ExpandableSection(
        heading =
            i18n.chatHistoryVisibility() +
                ": " +
                (historyVisibility?.getStateName(i18n)
                    ?: HistoryVisibilityEventContent.HistoryVisibility.SHARED.getStateName(i18n)),
        expanded = historyExpanded,
        icon = Icons.Default.Settings,
    ) {
        for (visibility in createNewGroupViewModel.availableRoomHistoryVisibilities) {
            val historyVisibility by createNewGroupViewModel.optionalRoomHistoryVisibility.collectAsState()
            val isEncrypted = createNewGroupViewModel.isEncrypted.collectAsState().value
            ThemedListItemRadioButton(
                headlineContent = { Text(visibility.getStateName(i18n)) },
                selected = (historyVisibility ?: HistoryVisibilityEventContent.HistoryVisibility.SHARED) == visibility,
                onChange = { createNewGroupViewModel.changeOptionalHistoryVisibility(visibility) },
                enabled = createNewGroupViewModel.historyVisibilityCanBeChangedTo(visibility),
                leadingContent = {
                    HelpIcon(
                        if (isEncrypted) visibility.getExplanationWhenEncrypted(i18n)
                        else visibility.getExplanation(i18n)
                    )
                },
                style = MaterialTheme.components.settingsItem,
            )
        }
    }
}

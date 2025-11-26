package de.connect2x.messenger.compose.view.roomlist.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.icons.HelpIcon
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.i18n.getExplanation
import de.connect2x.messenger.compose.view.i18n.getExplanationWhenEncrypted
import de.connect2x.messenger.compose.view.i18n.getStateName
import de.connect2x.messenger.compose.view.common.ExpandableSection
import de.connect2x.messenger.compose.view.theme.components.ThemedSwitch
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent

interface CreateGroupOptionsView {
    @Composable
    fun create(createNewGroupViewModel: CreateNewGroupViewModel, historyExpanded: MutableState<Boolean>)
}

@Composable
fun CreateGroupOptions(
    createNewGroupViewModel: CreateNewGroupViewModel,
    historyExpanded: MutableState<Boolean> = remember { mutableStateOf(false) }
) {
    DI.get<CreateGroupOptionsView>().create(createNewGroupViewModel, historyExpanded)
}

class CreateGroupOptionsViewImpl : CreateGroupOptionsView {
    @Composable
    override fun create(createNewGroupViewModel: CreateNewGroupViewModel, historyExpanded: MutableState<Boolean>) {
        val isPrivate by createNewGroupViewModel.isPrivate.collectAsState()
        val isEncrypted by createNewGroupViewModel.isEncrypted.collectAsState()
        val historyVisibility by createNewGroupViewModel.optionalRoomHistoryVisibility.collectAsState()
        val i18n = DI.get<I18nView>()

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val visibilityInfoText = if (isPrivate) i18n.roomTypePrivateInfo() else i18n.roomTypePublicInfo()
                val visibilityType = if (isPrivate) i18n.roomTypePrivate() else i18n.roomTypePublic()

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                )
                {
                    HelpIcon(visibilityInfoText)
                    Text(i18n.roomVisibility())
                    Text(visibilityType)
                }

                ThemedSwitch(
                    checked = isPrivate,
                    onCheckedChange = { createNewGroupViewModel.isPrivate.value = it }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen
                val infoText = if (isEncrypted) i18n.roomTypeEncryptedInfo() else i18n.roomTypeUnencryptedInfo()

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HelpIcon(infoText)
                    Text(i18n.roomEncryption())
                    Icon(icon, null)
                }

                ThemedSwitch(
                    checked = isEncrypted,
                    onCheckedChange = { createNewGroupViewModel.changeEncryptionStatus(it) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        ExpandableSection(
                            heading = "${i18n.chatHistoryVisibility()}: ${
                                historyVisibility?.getStateName(i18n)
                                    ?: HistoryVisibilityEventContent.HistoryVisibility.SHARED.getStateName(i18n)
                            }",
                            expanded = historyExpanded,
                            icon = Icons.Default.Settings,
                        ) {
                            for (visibility in createNewGroupViewModel.availableRoomHistoryVisibilities) {
                                CreateGroupVisibilityOption(visibility, createNewGroupViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGroupVisibilityOption(
    visibility: HistoryVisibilityEventContent.HistoryVisibility,
    createNewGroupViewModel: CreateNewGroupViewModel
) {
    val i18n = DI.get<I18nView>()
    val historyVisibility by createNewGroupViewModel.optionalRoomHistoryVisibility.collectAsState()
    val isEncrypted = createNewGroupViewModel.isEncrypted.collectAsState().value
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clickable(enabled = createNewGroupViewModel.historyVisibilityCanBeChangedTo(visibility)) {
                createNewGroupViewModel.changeOptionalHistoryVisibility(
                    visibility
                )
            }
            .buttonPointerModifier(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HelpIcon(if (isEncrypted) visibility.getExplanationWhenEncrypted(i18n) else visibility.getExplanation(i18n))
        Text(visibility.getStateName(i18n), modifier = Modifier.weight(1.0f, fill = true))
        RadioButton(
            selected = (historyVisibility ?: HistoryVisibilityEventContent.HistoryVisibility.SHARED) == visibility,
            onClick = { createNewGroupViewModel.changeOptionalHistoryVisibility(visibility) },
            enabled = createNewGroupViewModel.historyVisibilityCanBeChangedTo(visibility)
        )
    }
}

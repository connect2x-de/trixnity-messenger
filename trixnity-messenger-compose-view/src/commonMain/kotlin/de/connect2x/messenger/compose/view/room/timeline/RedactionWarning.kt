package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.MiddleSpacer
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedCheckbox
import de.connect2x.messenger.compose.view.theme.components.ThemedListItemCheckbox
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel

interface RedactionWarningView {
    @Composable
    fun create(holder: BaseTimelineElementHolderViewModel, showRedactWarning: MutableState<Boolean>)
}

@Composable
fun RedactionWarning(holder: BaseTimelineElementHolderViewModel, showRedactWarning: MutableState<Boolean>) {
    DI.get<RedactionWarningView>().create(holder, showRedactWarning)
}

class RedactionWarningViewImpl : RedactionWarningView {
    @Composable
    override fun create(holder: BaseTimelineElementHolderViewModel, showRedactWarning: MutableState<Boolean>) {
        if (holder !is TimelineElementHolderViewModel) {
            return
        }
        val i18n = DI.get<I18nView>()
        val warningEnabled = holder.redactionWarningIsEnabled.collectAsState().value
        val disableWarning = remember { mutableStateOf(false) }
        if (showRedactWarning.value && warningEnabled) {
            ThemedModalDialog(onDismissRequest = { showRedactWarning.value = false }) {
                ModalDialogHeader { Text(i18n.redactionWarningInfoTitle()) }
                ModalDialogContent {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            i18n.commonWarning(),
                            tint = MaterialTheme.messengerColors.warning
                        )
                        MiddleSpacer()
                        Column {
                            Text(i18n.redactionWarningInfo())
                            SmallSpacer()
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                                    .clickable(role = Role.Checkbox) { disableWarning.value = !disableWarning.value }
                                    .buttonPointerModifier()
                            ) {
                                Text(i18n.redactionWarningDisable())
                                ThemedCheckbox(disableWarning.value, { newValue -> disableWarning.value = newValue })
                            }
                        }
                    }
                }
                ModalDialogFooter {
                    ThemedButton(
                        onClick = {
                            disableWarning.value = false
                            showRedactWarning.value = false
                        },
                        style = MaterialTheme.components.commonButton
                    ) {
                        Text(i18n.commonCancel())
                    }
                    ThemedButton(onClick = {
                        if (disableWarning.value) {
                            holder.disableRedactWarning()
                        }
                        holder.redact()
                    }, style = MaterialTheme.components.primaryButton) {
                        Text(i18n.commonConfirm())
                    }
                }
            }
        }
    }
}

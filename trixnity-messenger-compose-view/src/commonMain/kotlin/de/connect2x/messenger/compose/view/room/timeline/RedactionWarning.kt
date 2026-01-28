package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.MiddleSpacer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
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
        if (showRedactWarning.value) {
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
                        Text(i18n.redactionWarningInfo())
                    }
                }
                ModalDialogFooter {
                    ThemedButton(
                        onClick = {
                            showRedactWarning.value = false
                        },
                        style = MaterialTheme.components.commonButton
                    ) {
                        Text(i18n.commonCancel())
                    }
                    ThemedButton(onClick = {
                        holder.redact()
                    }, style = MaterialTheme.components.primaryButton) {
                        Text(i18n.commonConfirm())
                    }
                }
            }
        }
    }
}

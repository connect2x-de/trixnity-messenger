package de.connect2x.trixnity.messenger.compose.view.room.timeline

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.compose.view.util.inputFocusNavigation
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReportMessageViewModel

@Composable
fun MessageReport(
    reportToMessageViewModel: ReportMessageViewModel,
) {
    val i18n = DI.get<I18nView>()
    val focusRequester = remember { FocusRequester() }
    val reason = reportToMessageViewModel.messageReportReason.collectAsTextFieldValueState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ThemedModalDialog({ reportToMessageViewModel.closeReportMessageDialog() }) {
        ModalDialogHeader {
            Text(i18n.reportMessageHeader())
        }
        ModalDialogContent {
            OutlinedTextField(
                modifier = Modifier.Companion.inputFocusNavigation()
                    .focusRequester(focusRequester)
                    .fillMaxWidth(),
                value = reason.value,
                onValueChange = { reason.value = it },
                minLines = 3,
                maxLines = 5,
                label = { Text(i18n.reportMessageLabel()) },
            )
        }
        ModalDialogFooter {
            ThemedButton(
                style = MaterialTheme.components.commonButton,
                onClick = { reportToMessageViewModel.closeReportMessageDialog() },
            ) {
                Text(i18n.actionCancel())
            }
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = { reportToMessageViewModel.submitReportToMessage() },
            ) {
                Text(i18n.reportMessage())
            }
        }
    }
}

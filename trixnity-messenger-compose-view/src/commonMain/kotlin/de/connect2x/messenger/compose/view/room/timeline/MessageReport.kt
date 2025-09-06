package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReportMessageViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MessageReport(
    reportToMessageViewModel: ReportMessageViewModel,
) {

    val i18n = DI.get<I18nView>()
    val focusRequester = remember { FocusRequester() }
    var reason by reportToMessageViewModel.messageReportReason.collectAsTextFieldValueState()

    LaunchedEffect(Unit) {
        delay(500.milliseconds)
        focusRequester.requestFocus()
    }

    ThemedModalDialog({ reportToMessageViewModel.closeReportMessageDialog() }) {
        ModalDialogHeader {
            Text(i18n.reportMessageHeader())
        }
        ModalDialogContent {
            OutlinedTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth(),
                value = reason,
                onValueChange = { reason = it },
                minLines = 3,
                maxLines = 5,
                textStyle = MaterialTheme.typography.bodyMedium,
                label = { Text(i18n.reportMessageLabel()) }
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

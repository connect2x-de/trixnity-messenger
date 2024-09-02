package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.NextButton
import de.connect2x.messenger.compose.view.common.MessengerModal
import de.connect2x.messenger.compose.view.common.MessengerModalButtonRow
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReportMessageViewModel

@Composable
fun MessageReport(
    reportToMessageViewModel: ReportMessageViewModel,
) {

    val i18n = DI.current.get<I18nView>()
    val focusRequester = remember { FocusRequester() }
    val reason = reportToMessageViewModel.messageReportReason.collectAsState().value

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    MessengerModal(
        onDismiss = { reportToMessageViewModel.closeReportMessageDialog() },
        title = i18n.reportMessageHeader(),
    ) {

        OutlinedTextField(
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            value = reason ?: "",
            onValueChange = { reportToMessageViewModel.messageReportReason.value = it },
            minLines = 3,
            maxLines = 5,
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = { Text(i18n.reportMessageLabel()) }
        )

        MessengerModalButtonRow({
            NextButton(text = i18n.reportMessage()) { reportToMessageViewModel.submitReportToMessage() }
        })
    }

}

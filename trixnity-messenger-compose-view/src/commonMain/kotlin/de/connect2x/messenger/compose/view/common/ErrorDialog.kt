package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView

@Composable
fun ErrorDialog(
    errorMessage: String,
    dismissAction: () -> Unit,
    confirmAction: () -> Unit = {},
    confirmText: String? = null,
    errorCause: String? = null
) {
    val i18n = DI.get<I18nView>()
    AlertDialog(
        onDismissRequest = { dismissAction() },
        confirmButton = {
            confirmText?.let {
                Button({ confirmAction() }, Modifier.buttonPointerModifier()) {
                    Text(it)
                }
            }
        },
        modifier = Modifier.defaultMinSize(minWidth = 400.dp)
            .background(MaterialTheme.colorScheme.errorContainer),
        dismissButton = {
            Button({ dismissAction() }, Modifier.buttonPointerModifier()) {
                Text(i18n.commonOk())
            }
        },
        title = { Text(i18n.anErrorHasOccurred()) },
        text = { ErrorText(errorMessage, errorCause) },
        shape = RoundedCornerShape(4.dp),
    )
}


@Composable
fun ErrorText(errorMessage: String, errorCause: String?) {
    if (errorCause == null) Text(errorMessage) else {
        Column {
            Text(errorMessage)
            Spacer(Modifier.size(10.dp))
            HorizontalDivider(Modifier)
            Spacer(Modifier.size(10.dp))
            Text(color = MaterialTheme.colorScheme.error, text = errorCause)
        }
    }
}

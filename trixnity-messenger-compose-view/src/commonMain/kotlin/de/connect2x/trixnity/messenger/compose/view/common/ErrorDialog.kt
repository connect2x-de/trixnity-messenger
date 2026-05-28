package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog

@Composable
fun ErrorDialog(error: String?, errorDetails: String?, onDismiss: () -> Unit) {
    val i18n = DI.get<I18nView>()

    if (error != null) {
        ThemedModalDialog({ onDismiss() }) {
            ModalDialogHeader { Text(i18n.anErrorHasOccurred()) }
            ModalDialogContent {
                Text(error)
                if (errorDetails != null) {
                    ExpandableSection(heading = i18n.errorDetails(), icon = Icons.Default.Info) {
                        Text(errorDetails, modifier = Modifier.padding(20.dp))
                    }
                }
            }
            ModalDialogFooter {
                ThemedButton(style = MaterialTheme.components.primaryButton, onClick = { onDismiss() }) {
                    Text(i18n.actionOk())
                }
            }
        }
    }
}

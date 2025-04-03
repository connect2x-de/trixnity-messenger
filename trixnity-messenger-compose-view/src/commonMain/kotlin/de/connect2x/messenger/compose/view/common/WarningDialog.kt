package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton

@Composable
fun WarningDialog(
    title: String,
    message: @Composable () -> Unit,
    dismissButtonText: String,
    confirmButtonText: String,
    dismissAction: () -> Unit,
    confirmAction: () -> Unit = {},
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val i18n = DI.get<I18nView>()
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = { dismissAction() },
        confirmButton = {
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = { confirmAction() },
                modifier = Modifier
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Enter) {
                            confirmAction()
                            true
                        } else false
                    }
                    .focusable(true)
                    .focusRequester(focusRequester)
            ) {
                Text(confirmButtonText)
            }
        },
        modifier = Modifier
            .defaultMinSize(minWidth = 400.dp),
        dismissButton = {
            ThemedButton(
                style = MaterialTheme.components.commonButton,
                onClick = { dismissAction() },
            ) {
                Text(dismissButtonText)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, i18n.commonWarning(), tint = iconColor)
                Spacer(Modifier.size(20.dp))
                Text(title)
            }
        },
        text = message,
        shape = RoundedCornerShape(4.dp),
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

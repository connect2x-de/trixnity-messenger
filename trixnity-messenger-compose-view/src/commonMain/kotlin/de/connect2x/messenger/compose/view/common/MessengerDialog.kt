package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedSurface

@Composable
fun MessengerDialog(
    onDismissRequest: () -> Unit,
    text: @Composable (() -> Unit),
) {
    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        ThemedSurface(
            style = MaterialTheme.components.dialog,
            modifier = Modifier.defaultMinSize(400.dp).wrapContentHeight().width(IntrinsicSize.Min),
        ) {
            Box(Modifier.padding(15.dp).wrapContentHeight()) {
                text()
            }
        }
    }
}

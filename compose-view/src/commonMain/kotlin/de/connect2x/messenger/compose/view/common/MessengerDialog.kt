package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun MessengerDialog(
    onDismissRequest: () -> Unit,
    text: @Composable (() -> Unit),
) {
    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = Modifier.defaultMinSize(400.dp).wrapContentHeight().width(IntrinsicSize.Min),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Box(Modifier.padding(15.dp).wrapContentHeight()) {
                text()
            }
        }
    }
}
package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton

// FIXME can be removed/refactored to better reflect its remaining usages once the old wizard framework is gone
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.MessengerModalButtonRow(
    button1: @Composable RowScope.() -> Unit,
    button2: (@Composable RowScope.() -> Unit)? = null,
    button3: (@Composable RowScope.() -> Unit)? = null,
) {
    Spacer(Modifier.size(20.dp))
    Column(
        Modifier.fillMaxWidth().weight(1.0f, fill = false),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
    ) {
        FlowRow(horizontalArrangement = Arrangement.SpaceEvenly) {
            button1()
            if (button2 != null) {
                Spacer(Modifier.size(10.dp))
                button2()
            }
            if (button3 != null) {
                Spacer(Modifier.size(10.dp))
                button3()
            }
        }
    }
}

@Composable
fun RowScope.CloseModalButton(closeModalAction: () -> Unit, caption: String? = null) {
    val i18n = DI.get<I18nView>()
    ThemedButton(
        style = MaterialTheme.components.destructiveButton,
        onClick = { closeModalAction() },
        modifier =
            Modifier.weight(1.0f, fill = false).width(IntrinsicSize.Max), // avoid wrapping button text if possible
    ) {
        Text(caption ?: i18n.commonClose())
    }
}

@Composable
fun RowScope.CloseMessengerButton(closeMessengerAction: () -> Unit) {
    val i18n = DI.get<I18nView>()
    val appName = DI.get<MatrixMessengerConfiguration>().appName
    ThemedButton(
        style = MaterialTheme.components.destructiveButton,
        onClick = { closeMessengerAction() },
        modifier = Modifier.weight(1.0f, fill = false),
    ) {
        Text(i18n.closeApp(appName))
    }
}

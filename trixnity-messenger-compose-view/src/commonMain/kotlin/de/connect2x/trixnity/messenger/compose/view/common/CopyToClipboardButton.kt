package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton

@Composable
fun CopyToClipboardButton(
    value: String,
    tooltip: String,
    icon:  @Composable () -> Unit = {Icon(Icons.Default.CopyAll, tooltip)}
){
    @Suppress("DEPRECATION") // TODO: New clipboard API is not usable from common code, fix this eventually..
    val clipboard = LocalClipboardManager.current

    Tooltip({ Text(tooltip) }, Modifier.semantics(mergeDescendants = true) {}) {
        ThemedIconButton(
            style = MaterialTheme.components.commonIconButton,
            onClick = { clipboard.setText(AnnotatedString(value)) },
            content = icon
        )
    }
}

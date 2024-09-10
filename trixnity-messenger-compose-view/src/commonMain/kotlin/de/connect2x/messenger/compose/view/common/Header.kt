package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.i18n.I18nView

@Composable
fun Header(backAction: () -> Unit, title: String, additionalButtons: @Composable (() -> Unit)? = null) {
    Header(backAction, { Text(title, style = MaterialTheme.typography.titleMedium) }, additionalButtons)
}

@Composable
fun Header(backAction: () -> Unit, title: @Composable () -> Unit, additionalButtons: @Composable (() -> Unit)? = null) {
    val i18n = DI.current.get<I18nView>()
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(backAction, Modifier.buttonPointerModifier()) {
                    Icon(Icons.Default.ArrowBack, i18n.commonBack())
                }
                Spacer(Modifier.size(10.dp))
                title()
                if (additionalButtons != null) {
                    Spacer(Modifier.weight(1.0f, false).fillMaxWidth())
                    additionalButtons()
                }
            }
            HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
        }
    }
}


package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.common.modifier.customClickable
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView


@Composable
internal fun LegalFooter() {
    val i18n = DI.get<I18nView>()

    var showPrivacy by remember { mutableStateOf(false) }
    if (showPrivacy) Privacy { showPrivacy = false }

    var showImprint by remember { mutableStateOf(false) }
    if (showImprint) Imprint { showImprint = false }

    var showLicenses by remember { mutableStateOf(false) }
    if (showLicenses) Licenses { showLicenses = false }

    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        TooltipText(i18n.appInfoPrivacy(), i18n.appInfoPrivacyLink()) { showPrivacy = true }
        TooltipText(i18n.appInfoImprint(), i18n.appInfoImprintLink()) { showImprint = true }
        TooltipText(i18n.appInfoLicenses(), i18n.appInfoLicensesLink()) { showLicenses = true }
    }
}

@Composable
private fun TooltipText(text: String, tooltip: String, onClick: () -> Unit) {
    Tooltip(tooltip = { Text(tooltip) }) {
        Text(text, Modifier.customClickable(onClick = onClick))
    }
}


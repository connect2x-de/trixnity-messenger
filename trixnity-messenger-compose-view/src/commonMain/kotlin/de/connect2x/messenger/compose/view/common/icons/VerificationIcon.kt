package de.connect2x.messenger.compose.view.common.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.common.icons.VerificationLevel.*
import de.connect2x.messenger.compose.view.theme.messengerColors

enum class VerificationLevel {
    DEVICE, USER, TIMELINE_EVENT
}

@Composable
fun VerifiedIcon(verificationLevel: VerificationLevel, size: Dp = 24.dp) {
    val i18n = DI.current.get<I18nView>()
    Tooltip(tooltip = {
        when (verificationLevel) {
            DEVICE -> TooltipText(i18n.verificationVerifiedDevice())
            USER -> TooltipText(i18n.verificationVerifiedUser())
            TIMELINE_EVENT -> Box {}
        }
    }) {
        Shield(size) {
            Icon(
                Icons.Default.GppGood,
                i18n.verificationTrusted(),
                modifier = Modifier.size(size),
                tint = MaterialTheme.messengerColors.verificationTrusted
            )
        }
    }
}

@Composable
fun NotVerifiedIcon(verificationLevel: VerificationLevel, size: Dp = 24.dp) {
    val i18n = DI.current.get<I18nView>()
    Tooltip(tooltip = {
        when (verificationLevel) {
            DEVICE -> TooltipText(i18n.verificationNotVerifiedDevice())
            USER -> TooltipText(i18n.verificationNotVerifiedUser())
            TIMELINE_EVENT -> Box { }
        }
    }) {
        Shield(size) {
            Icon(
                Icons.Default.GppBad,
                i18n.verificationNotTrusted(),
                modifier = Modifier.size(size),
                tint = MaterialTheme.messengerColors.verificationUntrusted
            )
        }
    }
}

@Composable
fun NeutralVerifiedIcon(verificationLevel: VerificationLevel, size: Dp = 24.dp) {
    val i18n = DI.current.get<I18nView>()
    Tooltip(tooltip = {
        when (verificationLevel) {
            DEVICE -> Box { }
            USER -> TooltipText(i18n.verificationNeutralUser())
            TIMELINE_EVENT -> Box { }
        }
    }) {
        Shield(size) {
            Icon(
                Icons.Default.Shield,
                i18n.verificationNotVerifiedYet(),
                modifier = Modifier.size(size),
                tint = MaterialTheme.messengerColors.verificationNeutral
            )
        }
    }
}

@Composable
private fun Shield(size: Dp, content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            Icons.Default.Shield,
            "",
            modifier = Modifier.size(size + 2.dp),
            tint = Color.Black,
        )
        Icon(
            Icons.Default.Shield,
            "",
            modifier = Modifier.size(size - 4.dp),
            tint = Color.White,
        )
        content()
    }
}

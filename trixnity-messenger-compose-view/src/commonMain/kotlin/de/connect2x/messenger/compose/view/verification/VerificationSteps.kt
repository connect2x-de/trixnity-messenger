package de.connect2x.messenger.compose.view.verification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.verification.SelectVerificationMethodViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepCompareViewModel
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod

// FIXME all configurable?


@Composable
fun VerificationWaitForOtherContent() {
    val i18n = DI.get<I18nView>()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LoadingSpinner()
        Spacer(Modifier.size(20.dp))
        Text(i18n.verificationWait())
    }
}

@Composable
fun SelectVerificationMethodContent(
    selectVerificationMethodViewModel: SelectVerificationMethodViewModel,
    selectedVerificationMethod: MutableState<VerificationMethod?>
) {
    val verificationMethods = selectVerificationMethodViewModel.verificationMethods

    Column {
        verificationMethods.forEach { (verificationMethod, explanation) ->
            if (selectVerificationMethodViewModel.hasSelection) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { selectedVerificationMethod.value = verificationMethod }
                ) {
                    RadioButton(
                        selected = selectedVerificationMethod.value == verificationMethod,
                        onClick = { selectedVerificationMethod.value = verificationMethod },
                    )
                    Text(
                        text = explanation,
                    )
                }
            } else {
                Text(
                    text = explanation,
                )
            }
        }
    }
}


@Composable
fun BoxScope.CompareEmojisOrNumbersContent(verificationStepCompareViewModel: VerificationStepCompareViewModel) {
    val i18n = DI.get<I18nView>()
    val emojis = verificationStepCompareViewModel.emojis
    val decimals = verificationStepCompareViewModel.decimals
    Column(
        Modifier.fillMaxWidth().align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (emojis.isNotEmpty()) {
            Text(i18n.verificationEmojiComparison())
            Spacer(Modifier.size(20.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    emojis.take(4).map { Emoji(it, this@BoxWithConstraints.maxWidth / 4) }
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    emojis.drop(4).map { Emoji(it, this@BoxWithConstraints.maxWidth / 4) }
                }
            }
        } else {
            Text(i18n.verificationNumberComparison())
            Spacer(Modifier.size(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                decimals.take(2).map { Number(it) }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                decimals.drop(2).map { Number(it) }
            }
        }
    }
}


@Composable
fun VerificationRejectedContent(
    deviceVerification: Boolean = true,
) {
    val i18n = DI.get<I18nView>()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Cancel,
            i18n.commonCancelled(),
            Modifier.size(50.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(text = i18n.verificationRejected(if (deviceVerification) i18n.deviceVerification() else i18n.userVerification()))
    }

}

@Composable
fun VerificationTimeoutContent(
    deviceVerification: Boolean = true,
) {
    val i18n = DI.get<I18nView>()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Cancel,
            i18n.commonCancelled(),
            Modifier.size(50.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(i18n.verificationTimeout(if (deviceVerification) i18n.deviceVerification() else i18n.userVerification()))
    }

}

@Composable
fun VerificationCancelledContent(
    deviceVerification: Boolean = true,
) {
    val i18n = DI.get<I18nView>()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Cancel,
            i18n.commonCancelled(),
            Modifier.size(50.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(text = i18n.verificationCancelled(if (deviceVerification) i18n.deviceVerification() else i18n.userVerification()))
    }

}

@Composable
private fun Emoji(emoji: Pair<String, Map<String, String?>>, maxWidth: Dp) {
    val i18n = DI.get<I18nView>()
    Column(
        Modifier
            .padding(10.dp)
            .sizeIn(maxWidth = maxWidth - 10.dp), // - padding
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji.first, style = MaterialTheme.typography.bodyLarge)
        Text(
            emoji.second[Locale.current.language.lowercase()] ?: i18n.commonUnknown(),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun Number(number: Int) {
    Text(number.toString(), fontSize = 28.sp)
}



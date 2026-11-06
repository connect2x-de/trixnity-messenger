package de.connect2x.trixnity.messenger.compose.view.verification.v2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationMethod
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedRadioButton
import de.connect2x.trixnity.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.VerificationViewModel

private val log = Logger("de.connect2x.trixnity.messenger.compose.view.verification.v2.VerificationStepsContentKt")

@Composable
fun VerificationSelectVerificationMethodContent(
    verificationViewModel: VerificationViewModel,
    selectedVerificationMethod: MutableState<VerificationMethod?>,
) {
    val verificationMethods = verificationViewModel.verificationMethods.collectAsState().value
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            verificationMethods == null -> {
                log.warn { "No verification methods found" }
                LoadingSpinner()
            }
            (verificationMethods.size > 1) -> {
                verificationMethods.forEach { (verificationMethod, explanation) ->
                    key(verificationMethod) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedVerificationMethod.value = verificationMethod },
                        ) {
                            ThemedRadioButton(
                                selected = selectedVerificationMethod.value == verificationMethod,
                                onClick = { selectedVerificationMethod.value = verificationMethod },
                            )
                            Text(explanation)
                        }
                    }
                }
            }
            else -> {
                val singleVerificationMethod by remember { mutableStateOf(verificationMethods.firstOrNull()) }
                LaunchedEffect(singleVerificationMethod) {
                    selectedVerificationMethod.value = singleVerificationMethod?.first
                }
                val explanation = singleVerificationMethod?.second
                if (explanation != null) {
                    Text(explanation)
                }
            }
        }
    }
}

@Composable
fun VerificationCancelledContent(isDeviceVerification: Boolean) {
    val i18n = DI.get<I18nView>()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Cancel,
            i18n.commonCancelled(),
            Modifier.size(MaterialTheme.messengerDpConstants.large),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text =
                i18n.verificationCancelled(
                    if (isDeviceVerification) i18n.deviceVerification() else i18n.userVerification()
                )
        )
    }
}

@Composable
fun BoxScope.VerificationCompareEmojisOrNumbersContent(verificationViewModel: VerificationViewModel) {
    val emojis = verificationViewModel.emojis.collectAsState().value
    val decimals = verificationViewModel.decimals.collectAsState().value
    val i18n = DI.get<I18nView>()
    Column(Modifier.fillMaxWidth().align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
        if (emojis?.isNotEmpty() == true) {
            Text(i18n.verificationEmojiComparison())
            MiddleSpacer()
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    emojis.take(4).forEach { Emoji(it, this@BoxWithConstraints.maxWidth / 4) }
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    emojis.drop(4).forEach { Emoji(it, this@BoxWithConstraints.maxWidth / 4) }
                }
            }
        } else if (decimals != null) {
            Text(i18n.verificationNumberComparison())
            MiddleSpacer()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                decimals.take(2).forEach { Number(it) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                decimals.drop(2).forEach { Number(it) }
            }
        } else {
            LoadingSpinner()
        }
    }
}

@Composable
fun VerificationRejectedContent(isDeviceVerification: Boolean) {
    val i18n = DI.get<I18nView>()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Cancel,
            i18n.commonCancelled(),
            Modifier.size(MaterialTheme.messengerDpConstants.large),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text =
                i18n.verificationRejected(
                    if (isDeviceVerification) i18n.deviceVerification() else i18n.userVerification()
                )
        )
    }
}

@Composable
fun VerificationTimeoutContent(isDeviceVerification: Boolean) {
    val i18n = DI.get<I18nView>()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Cancel,
            i18n.commonCancelled(),
            Modifier.size(MaterialTheme.messengerDpConstants.large),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(i18n.verificationTimeout(if (isDeviceVerification) i18n.deviceVerification() else i18n.userVerification()))
    }
}

@Composable
fun VerificationRequestContent(verificationViewModel: VerificationViewModel, isDeviceVerification: Boolean) {
    val i18n = DI.get<I18nView>()
    val deviceDisplayName by verificationViewModel.theirDeviceDisplayName.collectAsState()
    val theirDisplayName by verificationViewModel.theirDisplayName.collectAsState()

    Column {
        if (isDeviceVerification) {
            theirDisplayName?.let { Text(i18n.deviceVerificationInitiatedBy(it)) }
            deviceDisplayName?.let { Text(i18n.deviceVerificationToAccount(it)) }
        } else {
            theirDisplayName?.let { Text(i18n.userVerificationRequest(it)) }
        }
    }
}

@Composable
fun VerificationWaitForOtherContent() {
    val i18n = DI.get<I18nView>()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LoadingSpinner()
        MiddleSpacer()
        Text(i18n.verificationWait())
    }
}

@Composable
fun VerificationSuccessContent(isDeviceVerification: Boolean) {
    val i18n = DI.get<I18nView>()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isDeviceVerification) {
                Text(i18n.verificationSuccess())
            } else {
                Text(i18n.userVerificationSuccessMessage())
            }
            Icon(Icons.Default.CheckCircle, i18n.commonSuccess(), tint = MaterialTheme.messengerColors.success)
        }
    }
}

@Composable
fun VerificationAcceptSasStartContent() {
    val i18n = DI.get<I18nView>()
    Text(i18n.verificationStartEmoji())
}

@Composable
fun VerificationAcceptedByOtherClientContent() {
    val i18n = DI.get<I18nView>()
    Text(i18n.userVerificationOtherDevice())
}

@Composable
private fun Emoji(emoji: Pair<String, Map<String, String?>>, maxWidth: Dp) {
    val i18n = DI.get<I18nView>()
    Column(
        Modifier.padding(MaterialTheme.messengerDpConstants.small)
            .sizeIn(maxWidth = maxWidth - MaterialTheme.messengerDpConstants.small), // - padding
        horizontalAlignment = Alignment.CenterHorizontally,
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

package de.connect2x.trixnity.messenger.compose.view.verification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.common.CloseMessengerButton
import de.connect2x.trixnity.messenger.compose.view.common.ErrorView
import de.connect2x.trixnity.messenger.compose.view.common.LargeSpacer
import de.connect2x.trixnity.messenger.compose.view.common.Paragraphs
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.Wizard
import de.connect2x.trixnity.messenger.compose.view.common.WizardImage
import de.connect2x.trixnity.messenger.compose.view.common.WizardNavigationButton
import de.connect2x.trixnity.messenger.compose.view.common.WizardStep
import de.connect2x.trixnity.messenger.compose.view.copyToClipboard
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSelectionContainer
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view.generated.resources.Res
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view.generated.resources.recoverykey
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view.generated.resources.vault
import de.connect2x.trixnity.messenger.viewmodel.verification.CrossSigningBootstrapViewModel
import kotlinx.coroutines.launch

const val RECOVERY_KEY_EXPLANATION = "RECOVERY_KEY_EXPLANATION"
const val RECOVERY_KEY = "RECOVERY_KEY"
const val FINISHED = "FINISHED"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrossSigningBootstrapWizard(crossSigningBootstrapViewModel: CrossSigningBootstrapViewModel) {
    val i18n = DI.get<I18nView>()
    val wizardSteps =
        remember(crossSigningBootstrapViewModel, i18n) {
            listOf(
                WizardStep(
                    id = RECOVERY_KEY_EXPLANATION,
                    title = { i18n.bootstrapRecoveryKeyExplanationTitle() },
                    content = { boxWithConstraintsScope ->
                        val isBootstrapRunning =
                            crossSigningBootstrapViewModel.isBootstrapRunning.collectAsState().value
                        val error = crossSigningBootstrapViewModel.error.collectAsState().value
                        Paragraphs {
                            Text(i18n.bootstrapRecoveryKeyExplanation1())
                            Text(i18n.bootstrapRecoveryKeyExplanation2())
                            WizardImage(Res.drawable.vault, i18n.bootstrapVault(), 300.dp, boxWithConstraintsScope)
                            if (error != null) {
                                ErrorView(error)
                            }
                            if (isBootstrapRunning) {
                                Row {
                                    Spacer(Modifier.weight(1.0f, fill = true))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(i18n.bootstrapRecoveryKeyVaultCreation())
                                        Spacer(Modifier.size(20.dp))
                                        ThemedProgressIndicator(
                                            style = MaterialTheme.components.extraSmallCircularProgressIndicator
                                        )
                                    }
                                }
                            }
                        }
                    },
                    additionalButton = {
                        val isBootstrapRunning =
                            crossSigningBootstrapViewModel.isBootstrapRunning.collectAsState().value
                        val recoveryKey = crossSigningBootstrapViewModel.recoveryKey.collectAsState().value
                        if (isBootstrapRunning.not() && recoveryKey == null) {
                            ThemedButton(
                                style = MaterialTheme.components.primaryButton,
                                onClick = { crossSigningBootstrapViewModel.startCrossSigningBootstrap() },
                            ) {
                                Text(i18n.bootstrapRecoveryKeyCreateVault())
                            }
                        }
                    },
                    nextButton = {
                        WizardNavigationButton.Custom {
                            val recoveryKey = crossSigningBootstrapViewModel.recoveryKey.collectAsState().value

                            LaunchedEffect(recoveryKey) {
                                if (recoveryKey != null) {
                                    nextStep?.let { currentStepId.value = it }
                                }
                            }
                        }
                    },
                ),
                WizardStep(
                    id = RECOVERY_KEY,
                    title = { i18n.bootstrapRecoveryKeyTitle() },
                    content = {
                        val recoveryKey = crossSigningBootstrapViewModel.recoveryKey.collectAsState().value
                        val recoveryKeyPart1 = crossSigningBootstrapViewModel.recoveryKeyPart1.collectAsState().value
                        val recoveryKeyPart2 = crossSigningBootstrapViewModel.recoveryKeyPart2.collectAsState().value
                        val copiedToClipBoard = remember { mutableStateOf(false) }
                        val recoveryKeyCopied = crossSigningBootstrapViewModel.recoveryKeyCopied.collectAsState().value
                        Paragraphs {
                            Text(text = i18n.bootstrapRecoveryKeyHandling())
                            Text(i18n.bootstrapRecoveryKeyWarning())
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                Icon(
                                    Icons.Default.Warning,
                                    i18n.commonWarning(),
                                    modifier = Modifier.padding(end = 10.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    i18n.bootstrapRecoveryKeyAttention(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Text(text = i18n.bootstrapRecoveryKeyOnlyOnce(), fontWeight = FontWeight.Bold)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                WizardImage(Res.drawable.recoverykey, i18n.bootstrapRecoveryKey(), 60.dp)
                                LargeSpacer()
                                ThemedSelectionContainer(
                                    MaterialTheme.components.selectionOnSurface,
                                    Modifier.weight(1.0f),
                                ) {
                                    Column {
                                        Text(
                                            text = recoveryKeyPart1 ?: "",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                        Spacer(Modifier.size(15.dp))
                                        Text(
                                            text = recoveryKeyPart2 ?: "",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    }
                                }
                                Spacer(Modifier.size(40.dp))
                                Tooltip({ Text(i18n.bootstrapRecoveryKeyCopyToClipboard()) }) {
                                    Column(Modifier.width(IntrinsicSize.Min)) {
                                        val scope = rememberCoroutineScope()
                                        val di = DI.current
                                        Button(
                                            {
                                                scope.launch {
                                                    copyToClipboard(recoveryKey ?: "", di)
                                                    copiedToClipBoard.value = true
                                                }
                                            },
                                            Modifier.buttonPointerModifier(),
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Assignment,
                                                i18n.bootstrapRecoveryKeyCopyToClipboard(),
                                            )
                                        }
                                        AnimatedVisibility(copiedToClipBoard.value) {
                                            Text(
                                                i18n.commonCopied(),
                                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                                                textAlign = TextAlign.Center,
                                                color = Color.White,
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.size(40.dp))
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    crossSigningBootstrapViewModel.confirmRecoveryKeyCopied()
                                },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = recoveryKeyCopied,
                                    { crossSigningBootstrapViewModel.confirmRecoveryKeyCopied() },
                                )
                                Spacer(Modifier.size(10.dp))
                                Text(i18n.bootstrapRecoveryKeySafe())
                            }
                        }
                    },
                    additionalButton = { CloseMessengerButton(crossSigningBootstrapViewModel::closeMessenger) },
                    nextButton = {
                        WizardNavigationButton.Standard(
                            enabled = { crossSigningBootstrapViewModel.recoveryKeyCopied.collectAsState().value }
                        )
                    },
                ),
                WizardStep(
                    id = FINISHED,
                    title = { i18n.bootstrapFinished() },
                    content = {
                        // TODO content?
                    },
                    nextButton = {
                        WizardNavigationButton.Custom {
                            ThemedButton(
                                style = MaterialTheme.components.primaryButton,
                                onClick = { crossSigningBootstrapViewModel.close() },
                            ) {
                                Text(i18n.commonConfirm())
                            }
                        }
                    },
                ),
            )
        }

    Wizard(wizardSteps = wizardSteps, wizardId = "CrossSigningBootstrapWizard")
}

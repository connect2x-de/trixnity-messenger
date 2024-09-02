package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerDpConstants

typealias StepId = String

interface WizardNextButton {
    data class Standard(val enabled: @Composable () -> Boolean = { true }) : WizardNextButton
    data object None : WizardNextButton
    data class Custom(val button: @Composable RowScope.() -> Unit) : WizardNextButton
}

@Immutable
data class WizardStep(
    val id: StepId,
    val title: @Composable () -> String,
    val content: @Composable (BoxWithConstraintsScope) -> Unit,
    val additionalButton: (@Composable RowScope.((StepId) -> Unit) -> Unit)? = null,
    val nextButton: WizardNextButton = WizardNextButton.Standard(),
)

@Composable
fun Wizard(wizardSteps: List<WizardStep>) {
    val currentStepId = remember { mutableStateOf(wizardSteps.getOrNull(0)?.id ?: "unknown") }

    val wizardStep = wizardSteps.find { it.id == currentStepId.value }
    if (wizardStep != null) {
        Surface(
            Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.messengerDpConstants.large),
                contentAlignment = Alignment.Center,
            ) {
                // this is necessary to have a scroll position saved on every step, but not being linked (https://kotlinlang.slack.com/archives/CJLTWPH7S/p1715854224165609?thread_ts=1715852960.082249&cid=CJLTWPH7S)
                val savableStateHolder = rememberSaveableStateHolder()
                savableStateHolder.SaveableStateProvider(key = wizardStep.id) {
                    val scrollState = rememberScrollState()
                    WizardContainer(wizardSteps, wizardStep, currentStepId, scrollState)
                }
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.WizardContainer(
    wizardSteps: List<WizardStep>,
    wizardStep: WizardStep,
    currentStep: MutableState<StepId>,
    scrollState: ScrollState,
) {
    val boxWithConstraints = this
    Surface(
        Modifier
            .widthIn(max = 800.dp)
            .heightIn(min = max(1200.dp, this.maxHeight))
            .padding(
                if (boxWithConstraints.maxWidth < 500.dp) MaterialTheme.messengerDpConstants.small
                else MaterialTheme.messengerDpConstants.large
            )
            .clip(RoundedCornerShape(MaterialTheme.messengerDpConstants.small)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(
                        if (boxWithConstraints.maxWidth < 500.dp) MaterialTheme.messengerDpConstants.small
                        else MaterialTheme.messengerDpConstants.large
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column {
                    WizardHeading(wizardStep)
                    WizardContent(wizardStep, scrollState, boxWithConstraints)
                    WizardButtons(wizardSteps, wizardStep, currentStep)
                }
            }
            VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState)
        }
    }
}

@Composable
private fun WizardHeading(wizardStep: WizardStep) {
    Text(
        wizardStep.title(),
        style = MaterialTheme.typography.titleLarge,
    )
    MiddleSpacer()
}

@Composable
private fun ColumnScope.WizardContent(
    wizardStep: WizardStep,
    scrollState: ScrollState,
    boxWithConstraints: BoxWithConstraintsScope,
) {
    Surface(
        Modifier.Companion
            .weight(1.0f, fill = true)
    ) {
        Box(
            Modifier
                .fillMaxSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                wizardStep.content(boxWithConstraints)
            }
        }
    }
}

@Composable
private fun WizardButtons(
    wizardSteps: List<WizardStep>,
    wizardStep: WizardStep,
    currentStep: MutableState<StepId>,
) {
    val nextStep = wizardSteps.getOrNull(wizardSteps.indexOf(wizardStep) + 1)?.id
    val previousStep = wizardSteps.getOrNull(wizardSteps.indexOf(wizardStep) - 1)?.id
    val additionalButton = wizardStep.additionalButton
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Bottom) {
        if (previousStep != null) {
            if (additionalButton != null) {
                MessengerModalButtonRow(
                    button1 = { additionalButton { stepId -> currentStep.value = stepId } },
                    button2 = { BackButton(currentStep, previousStep) },
                    button3 = { NextButton(wizardStep, nextStep, currentStep) },
                )
            } else {
                MessengerModalButtonRow(
                    button1 = { BackButton(currentStep, previousStep) },
                    button2 = { NextButton(wizardStep, nextStep, currentStep) },
                )
            }
        } else {
            if (additionalButton != null) {
                MessengerModalButtonRow(
                    button1 = { additionalButton { stepId -> currentStep.value = stepId } },
                    button2 = { NextButton(wizardStep, nextStep, currentStep) },
                )
            } else {
                MessengerModalButtonRow(
                    button1 = { NextButton(wizardStep, nextStep, currentStep) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.NextButton(
    wizardStep: WizardStep,
    nextStep: StepId?,
    currentStep: MutableState<StepId>,
) {
    when (val nextButton = wizardStep.nextButton) {
        is WizardNextButton.Standard -> {
            nextStep?.let { NextButtonImpl(currentStep, nextStep, nextButton.enabled) }
        }

        is WizardNextButton.None -> {}

        is WizardNextButton.Custom -> {
            nextButton.button(this@NextButton)
        }
    }
}

@Composable
private fun NextButtonImpl(currentStep: MutableState<StepId>, nextStep: StepId, enabled: @Composable () -> Boolean) {
    val i18n = DI.current.get<I18nView>()
    Button(
        onClick = { currentStep.value = nextStep },
        modifier = Modifier.buttonPointerModifier(),
        enabled = enabled(),
    ) {
        Text(i18n.commonNext())
    }
}

@Composable
private fun BackButton(currentStep: MutableState<StepId>, previousStep: StepId) {
    val i18n = DI.current.get<I18nView>()
    Button(
        onClick = { currentStep.value = previousStep },
        modifier = Modifier.buttonPointerModifier(),
    ) {
        Text(i18n.commonBack())
    }
}

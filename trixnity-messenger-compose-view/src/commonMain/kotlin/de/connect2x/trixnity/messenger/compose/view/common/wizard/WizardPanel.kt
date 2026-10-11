package de.connect2x.trixnity.messenger.compose.view.common.wizard

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.common.VerySmallSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.settings.LegalFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.SurfaceStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants

private val wizardMaxWidth = 800.dp
private val wizardMinHeight = 1200.dp
private val wizardSurfacePaddingThreshold = 500.dp

data class WizardPanelContent(
    val title: @Composable () -> String,
    val subTitle: (@Composable () -> String)? = null,
    val content: @Composable (BoxWithConstraintsScope) -> Unit,
    val buttons: (@Composable RowScope.() -> Unit)?,
)

@Composable
fun WizardPanel(
    title: @Composable () -> String,
    subTitle: (@Composable () -> String)? = null,
    content: @Composable (BoxWithConstraintsScope) -> Unit,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    wizardId: String? = null,
) {
    WizardPanel(WizardPanelContent(title, subTitle, content, buttons), wizardId)
}

@Composable
fun WizardPanel(wizardStep: WizardPanelContent, wizardId: String?) {
    ThemedSurface(
        modifier = Modifier.fillMaxSize().then(if (wizardId != null) Modifier.testTag(wizardId) else Modifier),
        style = SurfaceStyle.default(color = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = MaterialTheme.messengerDpConstants.small),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                WizardContainer(wizardStep)
            }
            LegalFooter()
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.WizardContainer(wizardStep: WizardPanelContent) {
    val boxWithConstraints = this
    val scrollState = rememberScrollState()
    val isUnderThreshold = boxWithConstraints.maxWidth < wizardSurfacePaddingThreshold
    ThemedSurface(
        modifier =
            Modifier.widthIn(max = wizardMaxWidth)
                .heightIn(min = max(wizardMinHeight, this.maxHeight))
                .padding(
                    if (isUnderThreshold) MaterialTheme.messengerDpConstants.small
                    else MaterialTheme.messengerDpConstants.large
                )
                .clip(RoundedCornerShape(MaterialTheme.messengerDpConstants.small)),
        style = SurfaceStyle.default(color = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .padding(
                        if (isUnderThreshold) MaterialTheme.messengerDpConstants.middle
                        else MaterialTheme.messengerDpConstants.large
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(Modifier.semantics { dialog() }) {
                    WizardHeading(wizardStep)
                    WizardContent(wizardStep, scrollState, boxWithConstraints)
                    WizardButtons(wizardStep)
                }
            }
            VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState)
        }
    }
}

@Composable
private fun WizardHeading(wizardStep: WizardPanelContent) {
    Text(wizardStep.title(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    wizardStep.subTitle?.invoke()?.let { subTitle ->
        VerySmallSpacer()
        Text(subTitle, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
    }
    MiddleSpacer()
}

@Composable
private fun ColumnScope.WizardContent(
    wizardStep: WizardPanelContent,
    scrollState: ScrollState,
    boxWithConstraints: BoxWithConstraintsScope,
) {
    Box(Modifier.weight(1.0f, true).verticalScroll(scrollState)) { wizardStep.content(boxWithConstraints) }
}

@Composable
private fun ColumnScope.WizardButtons(wizardStep: WizardPanelContent) {
    wizardStep.buttons?.let { buttons ->
        MiddleSpacer()
        FlowRow(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement =
                Arrangement.spacedBy(MaterialTheme.messengerDpConstants.small, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.Bottom,
        ) {
            buttons()
        }
    }
}

@Composable
fun NextButton(enabled: Boolean = true, text: String? = null, nextAction: () -> Unit) {
    val i18n = DI.get<I18nView>()
    ThemedButton(style = MaterialTheme.components.primaryButton, onClick = nextAction, enabled = enabled) {
        Text(text ?: i18n.actionNext())
    }
}

@Composable
fun BackButton(enabled: Boolean = true, text: String? = null, onBack: () -> Unit) {
    val i18n = DI.get<I18nView>()
    ThemedButton(style = MaterialTheme.components.commonButton, onClick = onBack, enabled = enabled) {
        Text(text ?: i18n.actionBack())
    }
}

package de.connect2x.messenger.compose.view.uia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.util.collectAsStateForLoadingIndicator
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepDummyViewModel

interface UiaDummyStepView {
    @Composable
    fun create(uiaStepDummyViewModel: UiaStepDummyViewModel)
}

@Composable
fun UiaDummyStep(uiaStepDummyViewModel: UiaStepDummyViewModel) {
    DI.get<UiaDummyStepView>().create(uiaStepDummyViewModel)
}

class UiaDummyStepViewImpl : UiaDummyStepView {
    @Composable
    override fun create(uiaStepDummyViewModel: UiaStepDummyViewModel) {
        val i18n = DI.get<I18nView>()
        val isSubmitting = uiaStepDummyViewModel.isLoading.collectAsState().value
        val showIsSubmitting = uiaStepDummyViewModel.isLoading.collectAsStateForLoadingIndicator().value && isSubmitting

        val error = uiaStepDummyViewModel.error.collectAsState().value
        UiaModalBox {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                UiaHeading(i18n.uiaPasswordTitle())
                if (error != null) {
                    ErrorView(error)
                }
                if (showIsSubmitting) LoadingSpinner()
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ThemedButton(
                        style = MaterialTheme.components.commonButton,
                        onClick = uiaStepDummyViewModel::cancel,
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        enabled = !isSubmitting,
                        onClick = uiaStepDummyViewModel::next,
                    ) {
                        Text(i18n.uiaDummyButtonNext().capitalize(Locale.current))
                    }
                }
            }
        }
    }
}

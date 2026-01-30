package de.connect2x.trixnity.messenger.compose.view.uia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.ErrorView
import de.connect2x.trixnity.messenger.compose.view.common.LargeSpacer
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepFallbackViewModel

interface UiaFallbackFlowView {
    @Composable
    fun create(uiaStepFallbackViewModel: UiaStepFallbackViewModel)
}

@Composable
fun UiaFallbackFlow(uiaStepFallbackViewModel: UiaStepFallbackViewModel) {
    DI.get<UiaFallbackFlowView>().create(uiaStepFallbackViewModel)
}

class UiaFallbackFlowViewImpl : UiaFallbackFlowView {
    @Composable
    override fun create(uiaStepFallbackViewModel: UiaStepFallbackViewModel) {
        val i18n = DI.get<I18nView>()
        val error = uiaStepFallbackViewModel.error.collectAsState().value
        val isAwaiting = uiaStepFallbackViewModel.waitForResult.collectAsState().value
        val authenticationType = uiaStepFallbackViewModel.authenticationTypeString
        UiaModalBox {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                UiaHeading(i18n.uiaFallbackTitle())
                if (error != null) {
                    ErrorView(error)
                }
                MiddleSpacer()
                Text(authenticationType, Modifier.align(Alignment.CenterHorizontally))
                MiddleSpacer()
                if (isAwaiting) LoadingSpinner()
                LargeSpacer()
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ThemedButton(
                        style = MaterialTheme.components.commonButton,
                        onClick = uiaStepFallbackViewModel::cancel,
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = uiaStepFallbackViewModel::openFallbackUrl,
                    ) {
                        Text(i18n.uiaFallbackButtonRedirect().capitalize(Locale.current))
                    }
                }
            }
        }
    }
}

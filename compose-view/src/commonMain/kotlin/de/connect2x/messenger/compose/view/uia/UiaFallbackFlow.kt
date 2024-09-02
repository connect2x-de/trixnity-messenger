package de.connect2x.messenger.compose.view.uia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepFallbackViewModel

interface UiaFallbackFlowView {
    @Composable
    fun create(uiaStepFallbackViewModel: UiaStepFallbackViewModel)
}

@Composable
fun UiaFallbackFlow(uiaStepFallbackViewModel: UiaStepFallbackViewModel) {
    DI.current.get<UiaFallbackFlowView>().create(uiaStepFallbackViewModel)
}

class UiaFallbackFlowViewImpl : UiaFallbackFlowView {
    @Composable
    override fun create(uiaStepFallbackViewModel: UiaStepFallbackViewModel) {
        val i18n = DI.current.get<I18nView>()
        val error = uiaStepFallbackViewModel.error.collectAsState().value
        val isAwaiting = uiaStepFallbackViewModel.waitForResult.collectAsState().value
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
                if (isAwaiting) LoadingSpinner()
                Spacer(Modifier.height(40.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    OutlinedButton(
                        onClick = uiaStepFallbackViewModel::cancel,
                        modifier = Modifier.buttonPointerModifier(),
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                    Button(
                        onClick = uiaStepFallbackViewModel::openFallbackUrl,
                        modifier = Modifier.buttonPointerModifier(),
                    ) {
                        Text(i18n.uiaFallbackButtonRedirect().capitalize(Locale.current))
                    }
                }
            }
        }
    }
}
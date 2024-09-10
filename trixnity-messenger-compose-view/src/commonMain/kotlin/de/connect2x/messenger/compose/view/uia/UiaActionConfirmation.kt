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
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaActionConfirmationViewModel

interface UiaActionConfirmationView {
    @Composable
    fun create(uiaActionConfirmationViewModel: UiaActionConfirmationViewModel)
}

@Composable
fun UiaActionConfirmation(uiaActionConfirmationViewModel: UiaActionConfirmationViewModel) {
    DI.current.get<UiaActionConfirmationView>().create(uiaActionConfirmationViewModel)
}

class UiaActionConfirmationViewImpl : UiaActionConfirmationView {
    @Composable
    override fun create(uiaActionConfirmationViewModel: UiaActionConfirmationViewModel) {
        val i18n = DI.current.get<I18nView>()
        val message = uiaActionConfirmationViewModel.confirmationMessage ?: "" // TODO shouldn't be nullable
        val isPerformingAction = uiaActionConfirmationViewModel.isPerformingAction.collectAsState().value
        UiaModalBox {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                UiaHeading(message)
                if (isPerformingAction) LoadingSpinner()
                Spacer(Modifier.height(40.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    OutlinedButton(
                        onClick = uiaActionConfirmationViewModel::cancel,
                        modifier = Modifier.buttonPointerModifier(),
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                    Button(
                        enabled = !isPerformingAction,
                        onClick = uiaActionConfirmationViewModel::next,
                        modifier = Modifier.buttonPointerModifier(),
                    ) {
                        Text(i18n.commonOk().capitalize(Locale.current))
                    }
                }
            }
        }
    }
}
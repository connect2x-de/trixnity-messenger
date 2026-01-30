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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.ErrorView
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepEmailIdentityViewModel

interface UiaEmailIdentityStepView {
    @Composable
    fun create(uiaStepEmailIdentityViewModel: UiaStepEmailIdentityViewModel)
}

@Composable
fun UiaEmailIdentityStep(uiaStepEmailIdentityViewModel: UiaStepEmailIdentityViewModel) {
    DI.get<UiaEmailIdentityStepView>().create(uiaStepEmailIdentityViewModel)
}

class UiaEmailIdentityStepViewImpl : UiaEmailIdentityStepView {
    @Composable
    override fun create(uiaStepEmailIdentityViewModel: UiaStepEmailIdentityViewModel) {
        val i18n = DI.get<I18nView>()
        val error = uiaStepEmailIdentityViewModel.error.collectAsState().value
        UiaModalBox {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                UiaHeading(i18n.uiaEmailTitle())
                if (error != null) {
                    ErrorView(error)
                }
                MiddleSpacer()
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ThemedButton(
                        style = MaterialTheme.components.commonButton,
                        onClick = uiaStepEmailIdentityViewModel::cancel,
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                }
            }
        }
    }

}

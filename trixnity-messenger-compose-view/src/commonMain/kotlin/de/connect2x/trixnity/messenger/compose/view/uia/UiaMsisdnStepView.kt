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
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepMsisdnViewModel

interface UiaMsisdnStepView {
    @Composable
    fun create(uiaStepMsisdnViewModel: UiaStepMsisdnViewModel)
}

@Composable
fun UiaMsisdnStep(uiaStepMsisdnViewModel: UiaStepMsisdnViewModel) {
    DI.get<UiaMsisdnStepView>().create(uiaStepMsisdnViewModel)
}

class UiaMsisdnStepViewImpl: UiaMsisdnStepView {
    @Composable
    override fun create(uiaStepMsisdnViewModel: UiaStepMsisdnViewModel) {
        val i18n = DI.get<I18nView>()
        val error = uiaStepMsisdnViewModel.error.collectAsState().value
        UiaModalBox {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                UiaHeading(i18n.uiaMsisdnTitle())
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
                        onClick = uiaStepMsisdnViewModel::cancel,
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                }
            }
        }
    }

}

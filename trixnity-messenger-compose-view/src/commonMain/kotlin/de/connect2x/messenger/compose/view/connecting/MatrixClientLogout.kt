package de.connect2x.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.connecting.RemoveMatrixAccountViewModel

interface RemoveMatrixAccountView {
    @Composable
    fun create(removeMatrixAccountViewModel: RemoveMatrixAccountViewModel)
}

@Composable
fun RemoveMatrixAccount(removeMatrixAccountViewModel: RemoveMatrixAccountViewModel) {
    DI.get<RemoveMatrixAccountView>().create(removeMatrixAccountViewModel)
}

class RemoveMatrixAccountViewImpl : RemoveMatrixAccountView {
    @Composable
    override fun create(removeMatrixAccountViewModel: RemoveMatrixAccountViewModel) {
        val error = removeMatrixAccountViewModel.error.collectAsState().value
        val i18n = DI.get<I18nView>()

        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.Center)) {
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.size(20.dp))
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = removeMatrixAccountViewModel::close,
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                } else {
                    ThemedProgressIndicator(
                        Modifier.align(Alignment.CenterHorizontally),
                        MaterialTheme.components.circularProgressIndicator
                    )
                    Text(i18n.matrixClientLogout(removeMatrixAccountViewModel.userId.full))
                }
            }
        }
    }
}

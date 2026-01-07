package de.connect2x.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountState
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel

interface AddMatrixAccountView {
    @Composable
    fun create(addMatrixAccountViewModel: AddMatrixAccountViewModel)
}

@Composable
fun AddMatrixAccount(addMatrixAccountViewModel: AddMatrixAccountViewModel) {
    DI.get<AddMatrixAccountView>().create(addMatrixAccountViewModel)
}

class AddMatrixAccountViewImpl : AddMatrixAccountView {
    @Composable
    override fun create(addMatrixAccountViewModel: AddMatrixAccountViewModel) {
        val i18n = DI.get<I18nView>()
        Column {
            ServerInputField(addMatrixAccountViewModel)
            Spacer(Modifier.height(20.dp))
            ServerDiscoveryState(addMatrixAccountViewModel)
            Surface {
                val isMultiProfile = addMatrixAccountViewModel.isMultiProfile.collectAsState().value
                Column {
                    Text(i18n.accountOverviewWarning())
                    if (isMultiProfile) Text(i18n.accountOverviewWarningMultipleAccounts())
                }
            }
        }
    }
}

internal fun AddMatrixAccountState.inputEnabled() =
    this is AddMatrixAccountState.None || this is AddMatrixAccountState.Failure

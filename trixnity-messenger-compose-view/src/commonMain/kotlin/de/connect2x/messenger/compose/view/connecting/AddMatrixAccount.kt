package de.connect2x.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
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
        Column {
            ServerInputField(addMatrixAccountViewModel)
            Spacer(Modifier.height(20.dp))
            ServerDiscoveryState(addMatrixAccountViewModel)
        }
    }
}

internal fun AddMatrixAccountState.inputEnabled() =
    this is AddMatrixAccountState.None || this is AddMatrixAccountState.Failure

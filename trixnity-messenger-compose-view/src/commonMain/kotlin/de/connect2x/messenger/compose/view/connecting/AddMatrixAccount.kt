package de.connect2x.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.MiddleSpacer
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.messenger.compose.view.theme.messengerDpConstants
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
        val isFirstMatrixClient = addMatrixAccountViewModel.isFirstMatrixClient.collectAsState().value
        Column {
            ServerInputField(addMatrixAccountViewModel)
            Spacer(Modifier.height(20.dp))
            ServerDiscoveryState(addMatrixAccountViewModel)
            MiddleSpacer()
            if (isFirstMatrixClient == false) {
                ThemedSurface(style = MaterialTheme.components.details.copy(shape = MaterialTheme.shapes.medium)) {
                    val isMultiProfile = addMatrixAccountViewModel.isMultiProfile.collectAsState().value
                    Column(
                        Modifier.padding(MaterialTheme.messengerDpConstants.middle),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                i18n.commonWarning(),
                                tint = MaterialTheme.messengerColors.warning
                            )
                            MiddleSpacer()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(i18n.accountOverviewWarning())
                                SmallSpacer()
                                if (isMultiProfile) Text(i18n.accountOverviewWarningMultipleAccounts())
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun AddMatrixAccountState.inputEnabled() =
    this is AddMatrixAccountState.None || this is AddMatrixAccountState.Failure

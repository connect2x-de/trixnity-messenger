package de.connect2x.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.MatrixUsername
import de.connect2x.messenger.compose.view.common.PasswordField
import de.connect2x.messenger.compose.view.common.TabInTextField
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterMatrixAccountViewModel


interface RegisterNewAccountView {
    @Composable
    fun create(registerMatrixAccountViewModel: RegisterMatrixAccountViewModel)
}

@Composable
fun RegisterNewAccount(registerMatrixAccountViewModel: RegisterMatrixAccountViewModel) {
    DI.get<RegisterNewAccountView>().create(registerMatrixAccountViewModel)
}

class RegisterNewAccountViewImpl : RegisterNewAccountView {
    @Composable
    override fun create(registerMatrixAccountViewModel: RegisterMatrixAccountViewModel) {
        val i18n = DI.get<I18nView>()
        val canRegisterNewUser = registerMatrixAccountViewModel.canRegisterNewUser.collectAsState().value
        val error = registerMatrixAccountViewModel.error.collectAsState().value

        Column {
            if (error != null) {
                ErrorView(error)
            }
            Spacer(Modifier.size(20.dp))
            val tabToNextAndEnterSend =
                TabInTextField(canRegisterNewUser, registerMatrixAccountViewModel::register)
            MatrixUsername(
                username = registerMatrixAccountViewModel.username.collectAsTextFieldValueState(),
                label = i18n.registrationUsername(),
                enabled = true,
            ) {
                Tooltip({ TooltipText(i18n.profileUserNameInfo()) }) {
                    Icon(
                        Icons.Default.Info,
                        i18n.profileUserNameInfo(),
                    )
                }
            }
            Spacer(Modifier.size(20.dp))
            PasswordField(
                password = registerMatrixAccountViewModel.password.collectAsTextFieldValueState(),
                modifier = tabToNextAndEnterSend,
            ) { Text(i18n.registrationPassword()) }
        }
    }
}


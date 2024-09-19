package de.connect2x.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.MatrixUsername
import de.connect2x.messenger.compose.view.common.PasswordField
import de.connect2x.messenger.compose.view.common.TabInTextField
import de.connect2x.messenger.compose.view.common.collectAsStateForTextField
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountState
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModel


interface PasswordLoginView {
    @Composable
    fun create(passwordLoginViewModel: PasswordLoginViewModel)
}

@Composable
fun PasswordLogin(passwordLoginViewModel: PasswordLoginViewModel) {
    DI.get<PasswordLoginView>().create(passwordLoginViewModel)
}

class PasswordLoginViewImpl : PasswordLoginView {
    @Composable
    override fun create(passwordLoginViewModel: PasswordLoginViewModel) {
        val state = passwordLoginViewModel.addMatrixAccountState.collectAsState().value
        val canLogin = passwordLoginViewModel.canLogin.collectAsState().value
        val tabToNextAndEnterSend = TabInTextField(canLogin, passwordLoginViewModel::tryLogin)
        val i18n = DI.get<I18nView>()

        Column {
            MatrixUsername(
                username = passwordLoginViewModel.username.collectAsStateForTextField(),
                setUsername = { passwordLoginViewModel.username.value = it },
                label = i18n.addMatrixClientMatrixUsername(),
                enabled = passwordLoginViewModel.addMatrixAccountState.collectAsState().value.inputEnabled(),
                modifier = tabToNextAndEnterSend,
            )
            Spacer(Modifier.height(20.dp))
            PasswordField(
                password = passwordLoginViewModel.password.collectAsStateForTextField(),
                onPasswordChange = { passwordLoginViewModel.password.value = it },
                enabled = passwordLoginViewModel.addMatrixAccountState.collectAsState().value.inputEnabled(),
                modifier = tabToNextAndEnterSend,
            ) { Text(i18n.addMatrixClientPassword()) }
            Spacer(Modifier.height(20.dp))
            PasswordLoginState(state)
        }
    }
}

@Composable
fun PasswordLoginState(state: AddMatrixAccountState) {
    Box(Modifier.defaultMinSize(minHeight = 20.dp)) {
        when (state) {
            AddMatrixAccountState.None -> {}
            AddMatrixAccountState.Connecting -> LinearProgressIndicator(Modifier.fillMaxWidth())
            is AddMatrixAccountState.Failure ->
                Text(state.message, color = MaterialTheme.colorScheme.error)

            AddMatrixAccountState.Success -> {}
        }
    }
}

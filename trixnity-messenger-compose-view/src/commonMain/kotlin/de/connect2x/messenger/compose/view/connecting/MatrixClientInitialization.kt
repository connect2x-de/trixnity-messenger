package de.connect2x.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModel

interface MatrixClientInitializationView {
    @Composable
    fun create(matrixClientInitializationViewModel: MatrixClientInitializationViewModel)
}

@Composable
fun MatrixClientInitialization(matrixClientInitializationViewModel: MatrixClientInitializationViewModel) {
    DI.get<MatrixClientInitializationView>().create(matrixClientInitializationViewModel)
}

class MatrixClientInitializationViewImpl : MatrixClientInitializationView {
    @Composable
    override fun create(matrixClientInitializationViewModel: MatrixClientInitializationViewModel) {
        val currentState = matrixClientInitializationViewModel.currentState.collectAsState()
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.Center)) {
                ThemedProgressIndicator(
                    Modifier.align(Alignment.CenterHorizontally),
                    MaterialTheme.components.circularProgressIndicator
                )
                Text(currentState.value)
            }
        }
    }
}

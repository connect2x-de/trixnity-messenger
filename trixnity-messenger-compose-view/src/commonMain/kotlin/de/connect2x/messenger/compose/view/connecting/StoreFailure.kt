package de.connect2x.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.RunningText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.CloseApp
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationFailureViewModel
import kotlinx.coroutines.delay

interface MatrixClientInitializationFailureView {
    @Composable
    fun create(matrixClientInitializationFailureViewModel: MatrixClientInitializationFailureViewModel)
}

@Composable
fun MatrixClientInitializationFailure(matrixClientInitializationFailureViewModel: MatrixClientInitializationFailureViewModel) {
    DI.get<MatrixClientInitializationFailureView>().create(matrixClientInitializationFailureViewModel)
}

class MatrixClientInitializationFailureViewImpl : MatrixClientInitializationFailureView {
    @Composable
    override fun create(matrixClientInitializationFailureViewModel: MatrixClientInitializationFailureViewModel) {
        val i18n = DI.get<I18nView>()
        val deleteEnabled = matrixClientInitializationFailureViewModel.deleteEnabled
        val scroll = rememberScrollState()

        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(20.dp)
                    .widthIn(max = 800.dp)
                    .heightIn(max = 600.dp)
                    .verticalScroll(scroll)
            ) {
                val appName = DI.get<MatrixMessengerConfiguration>().appName
                if (deleteEnabled.not()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            i18n.commonWarning(),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 5.dp)
                        )
                        Text(text = i18n.storeFailureAlreadyOpen(appName))
                    }
                } else {
                    Text(text = i18n.storeFailureLocalDbNotLoaded())
                    Text(text = i18n.storeFailureLocalDbSelect())
                    Spacer(Modifier.size(20.dp))
                    Text(text = i18n.closeApp(appName), fontWeight = FontWeight.Bold)
                    Text(text = i18n.storeFailureLocalDbRestart(appName))
                    Spacer(Modifier.size(20.dp))
                    Button(
                        { matrixClientInitializationFailureViewModel.closeApplication() },
                        Modifier.buttonPointerModifier(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(i18n.closeApp(appName))
                    }
                    Spacer(Modifier.size(40.dp))
                    Text(text = i18n.storeFailureDeleteLocalDb(), fontWeight = FontWeight.Bold)
                    Text(
                        text = "${i18n.commonWarning().capitalize(Locale.current)}!",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(text = " ${i18n.storeFailureDeleteLocalDbSelect()}")
                    RunningText(text = i18n.storeFailureDeleteLocalDbRecoveryKey())
                    Text(text = i18n.storeFailureDeleteLocalDbOtherDevice())
                    Text(
                        text = i18n.storeFailureDeleteLocalDbWarning(),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Spacer(Modifier.size(20.dp))
                    Button(
                        { matrixClientInitializationFailureViewModel.delete() },
                        Modifier.buttonPointerModifier(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(i18n.storeFailureDeleteDb())
                    }
                }
            }
            VerticalScrollbar(
                Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                scroll,
            )
        }

        val closeApp = DI.get<CloseApp>()
        LaunchedEffect(Unit) {
            if (deleteEnabled.not()) {
                delay(5_000)
                closeApp()
            }
        }
    }
}

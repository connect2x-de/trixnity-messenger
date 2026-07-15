package de.connect2x.trixnity.messenger.compose.view.connecting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.MatrixClientInitializationException
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.LargeSpacer
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.common.RunningText
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationFailureViewModel

interface MatrixClientInitializationFailureView {
    @Composable fun create(matrixClientInitializationFailureViewModel: MatrixClientInitializationFailureViewModel)
}

@Composable
fun MatrixClientInitializationFailure(
    matrixClientInitializationFailureViewModel: MatrixClientInitializationFailureViewModel
) {
    DI.get<MatrixClientInitializationFailureView>().create(matrixClientInitializationFailureViewModel)
}

class MatrixClientInitializationFailureViewImpl : MatrixClientInitializationFailureView {
    @Composable
    override fun create(matrixClientInitializationFailureViewModel: MatrixClientInitializationFailureViewModel) {
        val initializationException = matrixClientInitializationFailureViewModel.initializationException
        val scroll = rememberScrollState()

        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(20.dp)
                    .widthIn(max = 800.dp)
                    .heightIn(max = 600.dp)
                    .verticalScroll(scroll)
            ) {
                when (initializationException) {
                    is MatrixClientInitializationException.DatabaseKeysManipulatedException -> {
                        StoreFailureKeysManipulation(matrixClientInitializationFailureViewModel)
                    }
                    is MatrixClientInitializationException.DatabaseCannotBeDecryptedException -> {
                        StoreFailureCannotDecryptDatabase(matrixClientInitializationFailureViewModel)
                    }
                    else -> {
                        StoreFailureShowError(matrixClientInitializationFailureViewModel)
                    }
                }
            }
            VerticalScrollbar(Modifier.align(Alignment.CenterEnd).fillMaxHeight(), scroll)
        }
    }
}

@Composable
fun StoreFailureKeysManipulation(
    matrixClientInitializationFailureViewModel: MatrixClientInitializationFailureViewModel
) {
    val i18n = DI.get<I18nView>()
    StoreFailureWarning(
        i18n.storeFailureKeysManipulationWarning(matrixClientInitializationFailureViewModel.userId.full)
    )
    LargeSpacer()
    Text(i18n.storeFailureKeysManipulationExplanation())
    MiddleSpacer()
    Text(i18n.storeFailureDeleteAccount())
    MiddleSpacer()
    ThemedButton(
        style = MaterialTheme.components.primaryButton,
        onClick = { matrixClientInitializationFailureViewModel.confirmDeletion() },
    ) {
        Text(i18n.commonConfirm().capitalize(Locale.current))
    }
}

@Composable
fun StoreFailureCannotDecryptDatabase(
    matrixClientInitializationFailureViewModel: MatrixClientInitializationFailureViewModel
) {
    val i18n = DI.get<I18nView>()
    StoreFailureWarning(
        i18n.storeFailureCannotDecryptDatabaseWarning(matrixClientInitializationFailureViewModel.userId.full)
    )
    LargeSpacer()
    Text(i18n.storeFailureCannotDecryptDatabaseExplanation())
    MiddleSpacer()
    Text(i18n.storeFailureDeleteAccount())
    MiddleSpacer()
    ThemedButton(
        style = MaterialTheme.components.primaryButton,
        onClick = { matrixClientInitializationFailureViewModel.confirmDeletion() },
    ) {
        Text(i18n.commonConfirm().capitalize(Locale.current))
    }
}

@Composable
fun StoreFailureShowError(matrixClientInitializationFailureViewModel: MatrixClientInitializationFailureViewModel) {
    val i18n = DI.get<I18nView>()
    val appName = DI.get<MatrixMessengerConfiguration>().appName

    val deleteEnabled = matrixClientInitializationFailureViewModel.deleteEnabled

    if (deleteEnabled.not()) {
        StoreFailureWarning(i18n.storeFailureAlreadyOpen(appName))
    } else {
        Text(text = i18n.storeFailureLocalDbNotLoaded(matrixClientInitializationFailureViewModel.userId.full))
        Text(text = i18n.storeFailureLocalDbSelect())
        MiddleSpacer()
        Text(text = i18n.closeApp(appName), fontWeight = FontWeight.Bold)
        Text(text = i18n.storeFailureLocalDbRestart(appName))
        MiddleSpacer()
        ThemedButton(
            style = MaterialTheme.components.destructiveButton,
            onClick = { matrixClientInitializationFailureViewModel.closeApplication() },
        ) {
            Text(i18n.closeApp(appName))
        }
        LargeSpacer()
        Text(text = i18n.storeFailureDeleteLocalDb(), fontWeight = FontWeight.Bold)
        Text(
            text = "${i18n.commonWarning().capitalize(Locale.current)}!",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
        )
        Text(text = " ${i18n.storeFailureDeleteLocalDbSelect()}")
        RunningText(text = i18n.storeFailureDeleteLocalDbRecoveryKey())
        Text(text = i18n.storeFailureDeleteLocalDbOtherDevice())
        Text(
            text = i18n.storeFailureDeleteLocalDbWarning(),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp),
        )
        MiddleSpacer()
        ThemedButton(
            style = MaterialTheme.components.destructiveButton,
            onClick = { matrixClientInitializationFailureViewModel.delete() },
        ) {
            Text(i18n.storeFailureDeleteDb())
        }
    }
}

@Composable
fun StoreFailureWarning(message: String) {
    val i18n = DI.get<I18nView>()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Warning,
            i18n.commonWarning(),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(end = 5.dp),
        )
        Text(text = message)
    }
}

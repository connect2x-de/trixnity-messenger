package de.connect2x.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.SINGLE_PANE_THRESHOLD
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.getOrNull
import de.connect2x.messenger.compose.view.theme.DefaultSizes
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel

private val MAX_WIDTH: Dp = 1600.dp

interface MainView {
    @Composable
    fun create(mainViewModel: MainViewModel)
}

@Composable
fun Main(mainViewModel: MainViewModel) {
    DI.get<MainView>().create(mainViewModel)
}

class MainViewImpl : MainView {
    @Composable
    override fun create(mainViewModel: MainViewModel) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val settings = DI.getOrNull<MatrixMessengerSettingsHolder>()?.collectAsState()?.value
            val defaultSizes = DI.get<DefaultSizes>()
            val sizeCoefficient =
                defaultSizes.maxDisplaySize - (settings?.base?.displaySize ?: defaultSizes.displaySize)
            Box(
                Modifier.fillMaxSize()
                    // TODO: remove the padding and adjust the bubble layout as it makes things look broken.
                    .padding(
                        horizontal = if ((maxWidth * sizeCoefficient) - MAX_WIDTH > 0.dp)
                            ((maxWidth * sizeCoefficient) - MAX_WIDTH) / 2 else 0.dp
                    )
            ) {
                val isSinglePane = this@BoxWithConstraints.maxWidth < SINGLE_PANE_THRESHOLD.dp
                InitialSyncSwitch(mainViewModel, isSinglePane)
            }
        }
        DeviceVerificationSwitch(mainViewModel)
        AvatarCutterSwitch(mainViewModel)
        AccountSetupSwitch(mainViewModel)
        SelfVerificationSwitch(mainViewModel)
        DeviceVerificationSwitch(mainViewModel)
        ShareDataSwitch(mainViewModel)
    }
}

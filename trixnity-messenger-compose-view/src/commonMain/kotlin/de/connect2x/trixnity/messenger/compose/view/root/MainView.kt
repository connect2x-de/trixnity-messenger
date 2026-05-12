package de.connect2x.trixnity.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.SINGLE_PANE_THRESHOLD
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel

val IsSinglePane = compositionLocalOf<Boolean> { error("compositionLocal not defined") }

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
            Box(Modifier.fillMaxSize()) {
                val isSinglePane = this@BoxWithConstraints.maxWidth < SINGLE_PANE_THRESHOLD.dp
                CompositionLocalProvider(IsSinglePane provides isSinglePane) {
                    InitialSyncSwitch(mainViewModel, isSinglePane)
                }
            }
        }
        AvatarCutterSwitch(mainViewModel)
        AccountSetupSwitch(mainViewModel)
        SelfVerificationSwitch(mainViewModel)
        DeviceVerificationSwitch(mainViewModel)
        ShareDataSwitch(mainViewModel)
    }
}

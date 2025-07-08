package de.connect2x.messenger.compose.view.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.InternalDecomposeApi
import com.arkivanov.decompose.lifecycle.MergedLifecycle
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.createRoot
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerProfileSettings
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }

@OptIn(InternalDecomposeApi::class)
@Composable
internal fun rememberRootViewModel(
    matrixMessenger: MatrixMessenger?,
    deviceLifecycle: Lifecycle
): RootViewModel? {
    val ownLifecycle = remember(matrixMessenger) { LifecycleRegistry() }
    val rootViewModel = remember(matrixMessenger) {
        matrixMessenger?.createRoot(DefaultComponentContext(MergedLifecycle(ownLifecycle, deviceLifecycle)))
    }
    DisposableEffect(matrixMessenger) {
        ownLifecycle.start()
        onDispose {
            ownLifecycle.destroy()
        }
    }
    return rootViewModel
}

@Composable
fun WithProfileSelection(
    matrixMultiMessenger: MatrixMultiMessenger,
    componentContext: ComponentContext,
    activeMessengerOnce: (MatrixMessenger, RootViewModel) -> Unit,
    activeMessenger: @Composable (MatrixMessenger, RootViewModel) -> Unit,
    nonActiveMessenger: @Composable (Map<String, MatrixMultiMessengerProfileSettings>) -> Unit,
) {
    val activeMatrixMessenger by matrixMultiMessenger.activeMatrixMessenger.collectAsState()
    val existingProfiles by matrixMultiMessenger.profiles.collectAsState()
    val rootViewModel: RootViewModel? = rememberRootViewModel(activeMatrixMessenger, componentContext.lifecycle)

    LaunchedEffect(rootViewModel) { // only execute the registration once during composition
        if (rootViewModel != null) {
            activeMatrixMessenger?.let { matrixMessenger ->
                activeMessengerOnce(matrixMessenger, rootViewModel)
            }
        }
    }
    activeMatrixMessenger?.let { activeMatrixMessengerNonNull ->
        rootViewModel?.let { rootViewModelNonNull ->
            activeMessenger(activeMatrixMessengerNonNull, rootViewModelNonNull)
        } ?: log.warn { "RootViewModel is `null`" }
    }
    if (activeMatrixMessenger == null) {
        nonActiveMessenger(existingProfiles)
    }
}

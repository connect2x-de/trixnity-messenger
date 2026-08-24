package de.connect2x.trixnity.messenger.internal.compose.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.profiles.WithProfileSelectionView
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerProfileSettings
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel

internal fun WithProfileSelectionView(): WithProfileSelectionView {
    return WithProfileSelectionViewImpl()
}

@TrixnityMessengerPrivateApi
private class WithProfileSelectionViewImpl : WithProfileSelectionView {
    @Composable
    override fun create(
        matrixMultiMessenger: MatrixMultiMessenger,
        componentContext: ComponentContext,
        activeMessengerOnce: (MatrixMessenger, RootViewModel) -> Unit,
        activeMessenger: @Composable (MatrixMessenger, RootViewModel) -> Unit,
        nonActiveMessenger: @Composable (Map<String, MatrixMultiMessengerProfileSettings>) -> Unit,
    ) {
        val activeMatrixMessenger by matrixMultiMessenger.activeMatrixMessenger.collectAsState()

        when (val matrixMessenger = activeMatrixMessenger) {
            null -> Inactive(matrixMultiMessenger, nonActiveMessenger)
            else -> Active(matrixMessenger, componentContext, activeMessengerOnce, activeMessenger)
        }
    }
}

@Composable
private fun Active(
    matrixMessenger: MatrixMessenger,
    componentContext: ComponentContext,
    activeMessengerOnce: (MatrixMessenger, RootViewModel) -> Unit,
    activeMessenger: @Composable (MatrixMessenger, RootViewModel) -> Unit,
) {
    LaunchedEffect(matrixMessenger) { activeMessengerOnce(matrixMessenger, UnsupportedRootViewModel) }
    SynchronizeLifecycle(componentContext, matrixMessenger)

    activeMessenger(matrixMessenger, UnsupportedRootViewModel)
}

@Composable
private fun SynchronizeLifecycle(componentContext: ComponentContext, matrixMessenger: MatrixMessenger) {
    val parentLifecycle = componentContext.lifecycle
    val childLifecycleRegistry = remember(matrixMessenger.di) { matrixMessenger.di.get<LifecycleRegistry>() }

    DisposableEffect(parentLifecycle, childLifecycleRegistry) {
        parentLifecycle.subscribe(childLifecycleRegistry)
        onDispose { parentLifecycle.unsubscribe(childLifecycleRegistry) }
    }
}

@Composable
private fun Inactive(
    matrixMultiMessenger: MatrixMultiMessenger,
    nonActiveMessenger: @Composable (Map<String, MatrixMultiMessengerProfileSettings>) -> Unit,
) {
    val existingProfiles by matrixMultiMessenger.profiles.collectAsState()
    nonActiveMessenger(existingProfiles)
}

private object UnsupportedRootViewModel : RootViewModel {
    override val uiaStack
        get() = error("unsupported")

    override val stack
        get() = error("unsupported")
}

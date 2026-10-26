package de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationbar

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.scene.SceneDecoratorStrategy
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.SINGLE_PANE_THRESHOLD
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.SceneDecoratorStrategyFactory

@TrixnityMessengerPrivateApi interface NavigationBarSceneDecoratorStrategyFactory : SceneDecoratorStrategyFactory

internal fun NavigationBarSceneDecoratorStrategyFactory(): NavigationBarSceneDecoratorStrategyFactory =
    NavigationBarSceneDecoratorStrategyFactoryImpl()

private class NavigationBarSceneDecoratorStrategyFactoryImpl : NavigationBarSceneDecoratorStrategyFactory {
    @Composable
    override fun <T : Any> create(): SceneDecoratorStrategy<T> {
        val enabled =
            DI.get<MatrixMessengerConfiguration>().features.enableSpaces &&
                !currentWindowAdaptiveInfo(true).windowSizeClass.isWidthAtLeastBreakpoint(SINGLE_PANE_THRESHOLD)
        return remember(enabled) { NavigationBarSceneDecoratorStrategy(enabled) }
    }
}

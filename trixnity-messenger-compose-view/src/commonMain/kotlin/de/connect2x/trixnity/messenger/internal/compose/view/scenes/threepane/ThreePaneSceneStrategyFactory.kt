package de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.SceneStrategyFactory

@TrixnityMessengerPrivateApi
interface ThreePaneSceneStrategyFactory : SceneStrategyFactory {
    @Composable override fun <T : Any> create(): ThreePaneSceneStrategy<T>
}

internal fun ThreePaneSceneStrategyFactory(): ThreePaneSceneStrategyFactory {
    return ThreePaneSceneStrategyFactoryImpl()
}

private class ThreePaneSceneStrategyFactoryImpl : ThreePaneSceneStrategyFactory {
    @Composable
    override fun <T : Any> create(): ThreePaneSceneStrategy<T> {
        val windowSizeClass = currentWindowAdaptiveInfo(true).windowSizeClass

        return remember(windowSizeClass) { ThreePaneSceneStrategy(windowSizeClass) }
    }
}

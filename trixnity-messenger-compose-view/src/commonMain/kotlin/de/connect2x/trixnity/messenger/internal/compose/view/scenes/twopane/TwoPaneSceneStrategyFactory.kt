package de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.getOrNull
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.SceneStrategyFactory

@TrixnityMessengerPrivateApi
interface TwoPaneSceneStrategyFactory : SceneStrategyFactory {
    @Composable override fun <T : Any> create(): TwoPaneSceneStrategy<T>
}

internal fun TwoPaneSceneStrategyFactory(): TwoPaneSceneStrategyFactory {
    return TwoPaneSceneStrategyFactoryImpl()
}

private class TwoPaneSceneStrategyFactoryImpl : TwoPaneSceneStrategyFactory {
    @Composable
    override fun <T : Any> create(): TwoPaneSceneStrategy<T> {
        val windowSizeClass = currentWindowAdaptiveInfo(true).windowSizeClass
        val placeholder = DI.getOrNull<TwoPaneScenePlaceholder>()
        return remember(windowSizeClass, placeholder) { TwoPaneSceneStrategy(windowSizeClass, placeholder) }
    }
}

package de.connect2x.trixnity.messenger.internal.compose.view.scenes.singlepane

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.SceneStrategyFactory

@TrixnityMessengerPrivateApi
interface SinglePaneSceneStrategyFactory : SceneStrategyFactory {
    @Composable override fun <T : Any> create(): SinglePaneSceneStrategy<T>
}

internal fun SinglePaneSceneStrategyFactory(): SinglePaneSceneStrategyFactory {
    return SinglePaneSceneStrategyFactoryImpl()
}

private class SinglePaneSceneStrategyFactoryImpl : SinglePaneSceneStrategyFactory {
    @Composable
    override fun <T : Any> create(): SinglePaneSceneStrategy<T> {
        return remember { SinglePaneSceneStrategy() }
    }
}

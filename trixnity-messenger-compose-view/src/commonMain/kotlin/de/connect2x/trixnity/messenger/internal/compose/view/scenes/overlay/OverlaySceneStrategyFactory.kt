package de.connect2x.trixnity.messenger.internal.compose.view.scenes.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.SceneStrategyFactory

@TrixnityMessengerPrivateApi
interface OverlaySceneStrategyFactory : SceneStrategyFactory {
    @Composable override fun <T : Any> create(): OverlaySceneStrategy<T>
}

internal fun OverlaySceneStrategyFactory(): OverlaySceneStrategyFactory {
    return OverlaySceneStrategyFactoryImpl()
}

private class OverlaySceneStrategyFactoryImpl : OverlaySceneStrategyFactory {
    @Composable
    override fun <T : Any> create(): OverlaySceneStrategy<T> {
        return remember { OverlaySceneStrategy() }
    }
}

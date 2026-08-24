package de.connect2x.trixnity.messenger.internal.compose.view.scenes.overlay

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.contains
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
interface OverlaySceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): OverlayScene<T>?
}

internal fun <T : Any> OverlaySceneStrategy(): OverlaySceneStrategy<T> {
    return OverlaySceneStrategyImpl()
}

private class OverlaySceneStrategyImpl<T : Any> : OverlaySceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): OverlayScene<T>? {
        val overlayEntry = entries.lastOrNull() ?: return null
        if (!overlayEntry.metadata.contains(OverlayScene.OverlaySceneMetadataKey)) return null

        return OverlayScene(entry = overlayEntry, previousEntries = entries.dropLast(1))
    }
}

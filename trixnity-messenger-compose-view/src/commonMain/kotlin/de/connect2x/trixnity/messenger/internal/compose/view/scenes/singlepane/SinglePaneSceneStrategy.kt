package de.connect2x.trixnity.messenger.internal.compose.view.scenes.singlepane

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
interface SinglePaneSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): SinglePaneScene<T>?
}

internal fun <T : Any> SinglePaneSceneStrategy(): SinglePaneSceneStrategy<T> {
    return SinglePaneSceneStrategyImpl()
}

private class SinglePaneSceneStrategyImpl<T : Any> : SinglePaneSceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): SinglePaneScene<T>? {
        val entry = entries.lastOrNull() ?: return null

        return SinglePaneScene(entry = entry, previousEntries = entries.dropLast(1))
    }
}

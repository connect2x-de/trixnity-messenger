package de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.contains
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.window.core.layout.WindowSizeClass
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.ROOM_WEIGHT
import de.connect2x.trixnity.messenger.compose.view.TWO_PANE_THRESHOLD

@TrixnityMessengerPrivateApi
interface ThreePaneSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): ThreePaneScene<T>?
}

internal fun <T : Any> ThreePaneSceneStrategy(windowSizeClass: WindowSizeClass): ThreePaneSceneStrategy<T> {
    return ThreePaneSceneStrategyImpl(windowSizeClass = windowSizeClass)
}

private class ThreePaneSceneStrategyImpl<T : Any>(private val windowSizeClass: WindowSizeClass) :
    ThreePaneSceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): ThreePaneScene<T>? {
        if (!windowSizeClass.isWidthAtLeastBreakpoint((TWO_PANE_THRESHOLD / ROOM_WEIGHT).toInt())) return null

        val lastEntry = entries.lastOrNull() ?: return null

        val (leftEntry, middleEntry, rightEntry) =
            if (lastEntry.metadata.contains(ThreePaneScene.ThreePaneSceneLeftMetadataKey)) {
                val middleEntry =
                    entries.findLast { it.metadata.contains(ThreePaneScene.ThreePaneSceneMiddleMetadataKey) }
                        ?: return null
                val rightEntry =
                    entries.findLast { it.metadata.contains(ThreePaneScene.ThreePaneSceneRightMetadataKey) }
                        ?: return null

                listOf(lastEntry, middleEntry, rightEntry)
            } else if (lastEntry.metadata.contains(ThreePaneScene.ThreePaneSceneMiddleMetadataKey)) {
                val leftEntry =
                    entries.findLast { it.metadata.contains(ThreePaneScene.ThreePaneSceneLeftMetadataKey) }
                        ?: return null
                val rightEntry =
                    entries.findLast { it.metadata.contains(ThreePaneScene.ThreePaneSceneRightMetadataKey) }
                        ?: return null

                listOf(leftEntry, lastEntry, rightEntry)
            } else if (lastEntry.metadata.contains(ThreePaneScene.ThreePaneSceneRightMetadataKey)) {
                val leftEntry =
                    entries.findLast { it.metadata.contains(ThreePaneScene.ThreePaneSceneLeftMetadataKey) }
                        ?: return null
                val middleEntry =
                    entries.findLast { it.metadata.contains(ThreePaneScene.ThreePaneSceneMiddleMetadataKey) }
                        ?: return null

                listOf(leftEntry, middleEntry, lastEntry)
            } else return null

        return ThreePaneScene(
            leftEntry = leftEntry,
            middleEntry = middleEntry,
            rightEntry = rightEntry,
            previousEntries = entries.dropLast(1),
        )
    }
}

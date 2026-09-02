package de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.contains
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.window.core.layout.WindowSizeClass
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.SINGLE_PANE_THRESHOLD

@TrixnityMessengerPrivateApi
interface TwoPaneSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): TwoPaneScene<T>?
}

internal fun <T : Any> TwoPaneSceneStrategy(
    windowSizeClass: WindowSizeClass,
    emptyPlaceholder: TwoPaneScenePlaceholder?,
): TwoPaneSceneStrategy<T> {
    return TwoPaneSceneStrategyImpl(windowSizeClass = windowSizeClass, emptyPlaceholder = emptyPlaceholder)
}

private class TwoPaneSceneStrategyImpl<T : Any>(
    private val windowSizeClass: WindowSizeClass,
    private val emptyPlaceholder: TwoPaneScenePlaceholder?,
) : TwoPaneSceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): TwoPaneScene<T>? {
        if (!windowSizeClass.isWidthAtLeastBreakpoint(SINGLE_PANE_THRESHOLD)) return null

        val lastEntry = entries.lastOrNull() ?: return null

        val (leftEntry, rightEntry) =
            if (lastEntry.metadata.contains(TwoPaneScene.TwoPaneSceneLeftMetadataKey)) {
                val rightEntry = entries.findLast { it.metadata.contains(TwoPaneScene.TwoPaneSceneRightMetadataKey) }
                lastEntry to rightEntry
            } else if (lastEntry.metadata.contains(TwoPaneScene.TwoPaneSceneRightMetadataKey)) {
                val leftEntry =
                    entries.findLast { it.metadata.contains(TwoPaneScene.TwoPaneSceneLeftMetadataKey) } ?: return null

                leftEntry to lastEntry
            } else return null

        return TwoPaneScene(
            leftEntry = leftEntry,
            rightEntry = rightEntry,
            previousEntries = entries.dropLast(1),
            emptyPlaceholder = emptyPlaceholder,
        )
    }
}

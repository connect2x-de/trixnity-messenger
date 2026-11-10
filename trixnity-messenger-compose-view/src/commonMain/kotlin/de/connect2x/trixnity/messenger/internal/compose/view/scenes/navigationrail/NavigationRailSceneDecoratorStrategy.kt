package de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationrail

import androidx.navigation3.runtime.get
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility

internal class NavigationRailSceneDecoratorStrategy<T : Any>(private val rail: NavigationRailView) :
    SceneDecoratorStrategy<T> {
    override fun SceneDecoratorStrategyScope<T>.decorateScene(scene: Scene<T>): Scene<T> {
        if (scene is OverlayScene || !NavigationVisibility.resolve(scene.entries).wide) return scene
        return NavigationRailScene(
            scene,
            rail,
            scene.entries.mapNotNull { it.metadata[NavigationRailScene.SelectionKey] }.lastOrNull(),
        )
    }
}

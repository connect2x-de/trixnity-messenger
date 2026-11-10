package de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationbar

import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility

internal class NavigationBarSceneDecoratorStrategy<T : Any>(private val enabled: Boolean) : SceneDecoratorStrategy<T> {
    override fun SceneDecoratorStrategyScope<T>.decorateScene(scene: Scene<T>): Scene<T> =
        if (enabled && scene !is OverlayScene && NavigationVisibility.resolve(scene.entries).compact)
            NavigationBarScene(scene)
        else scene
}

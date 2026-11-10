package de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import de.connect2x.trixnity.messenger.compose.view.space.SpaceNavigationBar

internal data class NavigationBarScene<T : Any>(val paneScene: Scene<T>) : Scene<T> {
    override val key: Any = paneScene::class to paneScene.key

    override val entries: List<NavEntry<T>> = paneScene.entries

    override val previousEntries: List<NavEntry<T>> = paneScene.previousEntries

    override val metadata: Map<String, Any> = paneScene.metadata

    override val content: @Composable () -> Unit = {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { key(paneScene::class, paneScene.key) { paneScene.content() } }
            SpaceNavigationBar()
        }
    }
}

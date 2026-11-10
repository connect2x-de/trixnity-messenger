package de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationrail

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation3.scene.SceneDecoratorStrategy
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.SINGLE_PANE_THRESHOLD
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.internal.component.CloseableComponentFactory
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.EntryProvider
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.SceneDecoratorStrategyFactory
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.clear
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceBrowserRoute

@TrixnityMessengerPrivateApi interface NavigationRailSceneDecoratorStrategyFactory : SceneDecoratorStrategyFactory

internal fun NavigationRailSceneDecoratorStrategyFactory(): NavigationRailSceneDecoratorStrategyFactory =
    NavigationRailSceneDecoratorStrategyFactoryImpl()

private class NavigationRailSceneDecoratorStrategyFactoryImpl : NavigationRailSceneDecoratorStrategyFactory {
    @Composable
    override fun <T : Any> create(): SceneDecoratorStrategy<T> {
        val navigation = DI.get<RouteNavigation>()
        val items by navigation.routes.collectAsState()
        val entryProvider = DI.get<EntryProvider>()
        val visibility =
            remember(items, entryProvider) { NavigationVisibility.resolve(items.map(entryProvider::createEntry)) }
        val windowSizeClass = currentWindowAdaptiveInfo(true).windowSizeClass
        val enabled =
            DI.get<MatrixMessengerConfiguration>().features.enableSpaces &&
                windowSizeClass.isWidthAtLeastBreakpoint(SINGLE_PANE_THRESHOLD) &&
                RoomListRoute in items &&
                visibility.wide
        if (!enabled) return remember { SceneDecoratorStrategy { it } }
        LaunchedEffect(navigation) { navigation.updateNavigation { clear<SpaceBrowserRoute>() } }
        val rail = rememberNavigationRailView(DI.get<CloseableComponentFactory>(), navigation)
        return remember(rail) { NavigationRailSceneDecoratorStrategy(rail) }
    }
}

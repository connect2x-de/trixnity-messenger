@file:OptIn(ExperimentalDecomposeApi::class)

package de.connect2x.trixnity.messenger.internal.compose.view

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.NavDisplay
import com.arkivanov.decompose.ExperimentalDecomposeApi
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.EntryDecoratorFactory
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.EntryProvider
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.SceneStrategyFactory
import de.connect2x.trixnity.messenger.internal.navigation.GetRouteNavigation
import de.connect2x.trixnity.messenger.util.BackHandler

@TrixnityMessengerPrivateApi
interface NavigationView {
    @Composable fun Content(modifier: Modifier = Modifier)
}

internal fun NavigationView(
    backHandler: BackHandler,
    navigation: GetRouteNavigation,
    entryDecorators: List<EntryDecoratorFactory>,
    sceneStrategies: List<SceneStrategyFactory>,
    entryProvider: EntryProvider,
): NavigationView {
    return NavigationViewImpl(
        backHandler = backHandler,
        navigation = navigation,
        entryDecorators = entryDecorators,
        sceneStrategies = sceneStrategies,
        entryProvider = entryProvider,
    )
}

@TrixnityMessengerPrivateApi
private class NavigationViewImpl(
    private val backHandler: BackHandler,
    private val navigation: GetRouteNavigation,
    private val entryDecorators: List<EntryDecoratorFactory>,
    private val sceneStrategies: List<SceneStrategyFactory>,
    private val entryProvider: EntryProvider,
) : NavigationView {
    @Composable
    override fun Content(modifier: Modifier) {
        val items by navigation.routes.collectAsState()

        NavDisplay(
            backStack = items,
            modifier = modifier.fillMaxSize(),
            onBack = { backHandler.goBack() },
            entryDecorators = entryDecorators.map { it.create() },
            sceneStrategies = sceneStrategies.map { it.create() },
            entryProvider = { entryProvider.createEntry(it) },
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        )
    }
}

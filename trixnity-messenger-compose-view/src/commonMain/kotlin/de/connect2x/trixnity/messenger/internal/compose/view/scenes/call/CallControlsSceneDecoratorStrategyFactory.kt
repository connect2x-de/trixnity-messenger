package de.connect2x.trixnity.messenger.internal.compose.view.scenes.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.scene.SceneDecoratorStrategy
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallService
import de.connect2x.trixnity.messenger.compose.view.room.call.CallControlsView
import de.connect2x.trixnity.messenger.compose.view.room.call.livekit.CallPermissionHost
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.SceneDecoratorStrategyFactory
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScenePlaceholder
import de.connect2x.trixnity.messenger.internal.navigation.GetRouteNavigation
import de.connect2x.trixnity.messenger.viewmodel.room.call.CallControlsViewModelFactory

@TrixnityMessengerCallApi internal interface CallControlsSceneDecoratorStrategyFactory : SceneDecoratorStrategyFactory

@TrixnityMessengerCallApi
internal fun CallControlsSceneDecoratorStrategyFactory(
    service: CallService,
    navigation: GetRouteNavigation,
    view: CallControlsView,
    viewModelFactory: CallControlsViewModelFactory,
    placeholder: TwoPaneScenePlaceholder?,
): CallControlsSceneDecoratorStrategyFactory {
    return CallControlsSceneDecoratorStrategyFactoryImpl(service, navigation, view, viewModelFactory, placeholder)
}

@TrixnityMessengerCallApi
private class CallControlsSceneDecoratorStrategyFactoryImpl(
    private val service: CallService,
    private val navigation: GetRouteNavigation,
    private val view: CallControlsView,
    private val viewModelFactory: CallControlsViewModelFactory,
    private val placeholder: TwoPaneScenePlaceholder?,
) : CallControlsSceneDecoratorStrategyFactory {
    @Composable
    override fun <T : Any> create(): SceneDecoratorStrategy<T> {
        CallPermissionHost(service = service)
        val coroutineScope = rememberCoroutineScope()
        val viewModel = remember(viewModelFactory, coroutineScope) { viewModelFactory.create(coroutineScope) }
        return remember(service, navigation, view, viewModel, placeholder) {
            CallControlsSceneDecoratorStrategy(service.session, navigation, view, viewModel, placeholder)
        }
    }
}

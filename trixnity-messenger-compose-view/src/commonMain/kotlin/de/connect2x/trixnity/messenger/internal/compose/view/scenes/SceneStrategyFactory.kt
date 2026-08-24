package de.connect2x.trixnity.messenger.internal.compose.view.scenes

import androidx.compose.runtime.Composable
import androidx.navigation3.scene.SceneStrategy
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.sort.SortableScope
import de.connect2x.trixnity.messenger.internal.sort.sorted
import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.module.dsl.new
import org.koin.dsl.bind

@TrixnityMessengerPrivateApi
interface SceneStrategyFactory {
    @Composable fun <T : Any> create(): SceneStrategy<T>
}

@TrixnityMessengerPrivateApi
inline fun <reified T : SceneStrategyFactory> Module.sceneStrategy(
    noinline definition: Definition<T>,
    noinline configure: SortableScope<SceneStrategyFactory>.() -> Unit = {},
): KoinDefinition<out SceneStrategyFactory> {
    return single<T>(definition = definition).bind<SceneStrategyFactory>().sorted(configure)
}

@TrixnityMessengerPrivateApi
inline fun <reified T : SceneStrategyFactory> Module.sceneStrategyOf(
    noinline constructor: () -> T,
    noinline configure: SortableScope<SceneStrategyFactory>.() -> Unit = {},
): KoinDefinition<out SceneStrategyFactory> {
    return sceneStrategy(definition = { new(constructor) }, configure = configure)
}

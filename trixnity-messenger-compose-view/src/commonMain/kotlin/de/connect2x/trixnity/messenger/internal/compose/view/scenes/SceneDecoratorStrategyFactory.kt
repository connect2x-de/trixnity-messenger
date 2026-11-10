package de.connect2x.trixnity.messenger.internal.compose.view.scenes

import androidx.compose.runtime.Composable
import androidx.navigation3.scene.SceneDecoratorStrategy
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.sort.SortableScope
import de.connect2x.trixnity.messenger.internal.sort.sorted
import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.module.dsl.new
import org.koin.dsl.bind

@TrixnityMessengerPrivateApi
interface SceneDecoratorStrategyFactory {
    @Composable fun <T : Any> create(): SceneDecoratorStrategy<T>
}

@TrixnityMessengerPrivateApi
inline fun <reified T : SceneDecoratorStrategyFactory> Module.sceneDecoratorStrategy(
    noinline definition: Definition<T>,
    noinline configure: SortableScope<SceneDecoratorStrategyFactory>.() -> Unit = {},
): KoinDefinition<out SceneDecoratorStrategyFactory> {
    return single<T>(definition = definition).bind<SceneDecoratorStrategyFactory>().sorted(configure)
}

@TrixnityMessengerPrivateApi
inline fun <reified T : SceneDecoratorStrategyFactory> Module.sceneDecoratorStrategyOf(
    noinline constructor: () -> T,
    noinline configure: SortableScope<SceneDecoratorStrategyFactory>.() -> Unit = {},
): KoinDefinition<out SceneDecoratorStrategyFactory> {
    return sceneDecoratorStrategy(definition = { new(constructor) }, configure = configure)
}

package de.connect2x.trixnity.messenger.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import kotlin.reflect.KClass
import kotlinx.coroutines.Dispatchers
import org.koin.core.Koin
import org.koin.core.parameter.parameterSetOf

@TrixnityMessengerPrivateApi
interface CloseableComponentFactory {
    fun <T : Any> create(clazz: KClass<out T>, parameters: Set<Any?>): CloseableComponent<T>
}

internal fun CloseableComponentFactory(appLifecycle: Lifecycle, koin: Koin): CloseableComponentFactory {
    return CloseableComponentFactoryImpl(appLifecycle = appLifecycle, koin = koin)
}

private class CloseableComponentFactoryImpl(private val appLifecycle: Lifecycle, private val koin: Koin) :
    CloseableComponentFactory {
    override fun <T : Any> create(clazz: KClass<out T>, parameters: Set<Any?>): CloseableComponent<T> {
        val componentContext = CloseableComponentContext(parentLifecycle = appLifecycle)
        val viewModelContext = ViewModelContext(koin, componentContext)

        val baseParameters = setOf(componentContext, viewModelContext)
        val allParameters = (baseParameters + parameters).toTypedArray()

        val value = koin.get<T>(clazz) { parameterSetOf(parameters = allParameters) }

        return CloseableComponent(value = value, onClose = componentContext::close)
    }
}

private class CloseableComponentContext(
    private val parentLifecycle: Lifecycle,
    private val lifecycleRegistry: LifecycleRegistry,
    private val stateKeeperDispatcher: StateKeeperDispatcher,
    private val instanceKeeperDispatcher: InstanceKeeperDispatcher,
    private val backDispatcher: BackDispatcher,
) :
    ComponentContext by DefaultComponentContext(
        lifecycle = lifecycleRegistry,
        stateKeeper = stateKeeperDispatcher,
        instanceKeeper = instanceKeeperDispatcher,
        backHandler = backDispatcher,
    ),
    AutoCloseable {

    init {
        parentLifecycle.subscribe(lifecycleRegistry)
    }

    constructor(
        parentLifecycle: Lifecycle
    ) : this(
        parentLifecycle = parentLifecycle,
        lifecycleRegistry = LifecycleRegistry(),
        stateKeeperDispatcher = StateKeeperDispatcher(),
        instanceKeeperDispatcher = InstanceKeeperDispatcher(),
        backDispatcher = BackDispatcher(),
    )

    override fun close() {
        parentLifecycle.unsubscribe(lifecycleRegistry)
        lifecycleRegistry.destroy()
        instanceKeeperDispatcher.destroy()
    }
}

private fun ViewModelContext(koin: Koin, componentContext: ComponentContext): ViewModelContext {
    return ViewModelContextImpl(
        di = koin,
        componentContext = componentContext,
        coroutineContext = Dispatchers.Main,
        name = "",
    )
}

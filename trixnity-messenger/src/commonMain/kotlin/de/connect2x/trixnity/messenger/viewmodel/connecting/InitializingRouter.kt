package de.connect2x.trixnity.messenger.viewmodel.connecting

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.util.bringToFrontSuspending
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable


class InitializingRouter<T, U>(
    componentContext: ComponentContext,
    private val initializingObject: StateFlow<T?>,
    private val viewModelBuilder: (T, ComponentContext) -> U,
) {
    private val navigation = StackNavigation<Config>()
    val stack = componentContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Initialization,
        key = "InitializingRouter-${uuid4()}",
        childFactory = { _, c -> createChild(c) },
    )

    private fun createChild(componentContext: ComponentContext): InitializationWrapper<U> {
        val serviceValue = initializingObject.value
        return if (serviceValue == null) {
            InitializationWrapper.Initialization()
        } else {
            InitializationWrapper.View(getView(serviceValue, componentContext))
        }
    }

    private var view: Pair<T, U>? = null

    private fun getView(serviceValue: T, componentContext: ComponentContext): U {
        val currentView = view
        return if (currentView != null && currentView.first == serviceValue) currentView.second
        else viewModelBuilder(serviceValue, componentContext).also { view = serviceValue to it }
    }

    suspend fun showInitialization() {
        navigation.bringToFrontSuspending(Config.Initialization)
    }

    suspend fun showView() {
        navigation.bringToFrontSuspending(Config.View)
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object Initialization : Config()

        @Serializable
        data object View : Config()
    }

    sealed class InitializationWrapper<U> {
        class Initialization<U> : InitializationWrapper<U>()
        class View<U>(val viewModel: U) : InitializationWrapper<U>()
    }
}
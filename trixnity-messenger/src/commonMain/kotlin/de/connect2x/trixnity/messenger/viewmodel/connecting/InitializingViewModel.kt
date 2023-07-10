package de.connect2x.trixnity.messenger.viewmodel.connecting

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.connecting.InitializingRouter.InitializationConfig
import de.connect2x.trixnity.messenger.viewmodel.connecting.InitializingRouter.InitializationWrapper
import de.connect2x.trixnity.messenger.viewmodel.util.coroutineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger { }

open class InitializingViewModel<T, U>(
    componentContext: ComponentContext,
    initializingObject: StateFlow<T?>,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    viewModelBuilder: (T, ComponentContext) -> U,
) : ComponentContext by componentContext,
    CoroutineScope by componentContext.coroutineScope(coroutineContext, "InitializingViewModel") {

    private val router = InitializingRouter(
        componentContext = this,
        initializingObject = initializingObject,
        viewModelBuilder = viewModelBuilder
    )
    val initializingStack: Value<ChildStack<InitializationConfig, InitializationWrapper<U>>> = router.stack

    init {
        launch {
            if (initializingObject.value == null) router.showInitialization()
            initializingObject
                .onEach {
                    if (it == null) {
                        log.debug { "show initialization" }
                        router.showInitialization()
                    } else {
                        log.debug { "show view" }
                        router.showView()
                    }
                }.collect()
        }
    }
}
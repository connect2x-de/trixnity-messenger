package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.RootRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.Koin
import kotlin.coroutines.CoroutineContext

interface RootViewModelFactory {
    fun create(
        componentContext: ComponentContext,
        di: Koin,
    ): RootViewModel = RootViewModelImpl(
        componentContext = componentContext,
        di = di,
        coroutineContext = di.get<CoroutineScope>().coroutineContext
    )

    companion object : RootViewModelFactory
}

interface RootViewModel {
    val stack: Value<ChildStack<Config, RootRouter.Wrapper>>
    val uiaStack: Value<ChildStack<UiaRouter.Config, UiaRouter.Wrapper>>
}

class RootViewModelImpl(
    componentContext: ComponentContext,
    di: Koin,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : ComponentContext by componentContext, RootViewModel {
    private val uiaRouter = UiaRouter(ViewModelContextImpl(di, componentContext, coroutineContext, "UIA"))
    override val uiaStack = uiaRouter.stack

    private val router = RootRouter(
        viewModelContext = ViewModelContextImpl(di, componentContext, coroutineContext, "Root"),
    ).apply { showInitialization() }
    override val stack: Value<ChildStack<Config, RootRouter.Wrapper>> = router.stack
}

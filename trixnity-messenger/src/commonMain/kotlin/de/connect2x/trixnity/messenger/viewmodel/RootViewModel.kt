package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.util.coroutineScope
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
    )

    companion object : RootViewModelFactory
}

interface RootViewModel {
    val rootStack: Value<ChildStack<RootRouter.Config, RootRouter.RootWrapper>>
}


open class RootViewModelImpl(
    componentContext: ComponentContext,
    di: Koin,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : ComponentContext by componentContext, RootViewModel {

    protected val scope: CoroutineScope = coroutineScope(coroutineContext)

    private val router = RootRouter(
        viewModelContext = ViewModelContextImpl(di, componentContext),
    )
    override val rootStack: Value<ChildStack<RootRouter.Config, RootRouter.RootWrapper>> = router.stack
}
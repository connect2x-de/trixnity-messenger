package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.InternalDecomposeApi
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.lifecycle.MergedLifecycle
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.Lifecycle
import de.connect2x.lognity.api.context.Context
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.BackHandler
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

interface ViewModelContext : KoinComponent, ComponentContext {
    data class Name(val name: String) : Context.Element {
        companion object Key : Context.Key<Name>

        override val key: Context.Key<*> = Key
    }

    /** This should be used carefully, because it can lead to leaks when not used on the top level. */
    val coroutineScope: CoroutineScope

    val trixnityMessengerBackHandler: BackHandler

    val log: Logger

    @Deprecated(
        "Don't use this, use trixnityMessengerBackHandler or registerBackCallback for lifecycle based callback registration",
        replaceWith = ReplaceWith("trixnityMessengerBackHandler"),
    )
    override val backHandler: com.arkivanov.essenty.backhandler.BackHandler

    fun childContext(key: String): ViewModelContext

    fun childContext(name: String, componentContext: ComponentContext): ViewModelContext

    fun childContext(key: String, userId: UserId): MatrixClientViewModelContext

    fun childContext(name: String, componentContext: ComponentContext, userId: UserId): MatrixClientViewModelContext

    /** TODO This is just a temporary workaround until decompose allows to destroy children. */
    fun childContextWithOwnLifecycle(name: String, lifecycle: Lifecycle, userId: UserId): MatrixClientViewModelContext

    fun registerBackCallback(backCallback: BackCallback)
}

interface MatrixClientViewModelContext : ViewModelContext {
    val matrixClient: MatrixClient
    val userId: UserId

    override fun childContext(key: String): MatrixClientViewModelContext

    override fun childContext(name: String, componentContext: ComponentContext): MatrixClientViewModelContext

    /** TODO This is just a temporary workaround until decompose allows to destroy children. */
    fun childContextWithOwnLifecycle(name: String, lifecycle: Lifecycle): MatrixClientViewModelContext
}

val ViewModelContext.i18n: I18n
    get() = get<I18n>()

val ViewModelContext.matrixClients: MatrixClients
    get() = get<MatrixClients>()

fun ViewModelContext.getMatrixClient(userId: UserId) =
    checkNotNull(get<MatrixClients>().value[userId]) { "cannot find MatrixClient for $userId" }

open class ViewModelContextImpl(
    private val di: Koin,
    componentContext: ComponentContext,
    protected val coroutineContext: CoroutineContext = Dispatchers.Default,
    protected val name: String,
) : ViewModelContext, ComponentContext by componentContext {
    final override val coroutineScope: CoroutineScope = instanceKeeper.getOrCreate {
        ViewModelCoroutineScope(coroutineContext)
    }

    override val log: Logger = Logger("VM:$name") { value(ViewModelContext.Name(name)) }

    override fun getKoin(): Koin = di

    override val trixnityMessengerBackHandler: BackHandler = di.get<BackHandler>()

    override fun registerBackCallback(backCallback: BackCallback) {
        with(trixnityMessengerBackHandler) { lifecycle.registerBackCallbackWithLifecycle(backCallback) }
    }

    override fun childContext(key: String): ViewModelContext {
        val componentContext = this as ComponentContext
        return childContext(key, componentContext.childContext(key))
    }

    override fun childContext(name: String, componentContext: ComponentContext): ViewModelContext {
        return ViewModelContextImpl(getKoin(), componentContext, coroutineContext, "${this.name}:$name")
    }

    override fun childContext(key: String, userId: UserId): MatrixClientViewModelContext {
        val componentContext = this as ComponentContext
        return childContext(key, componentContext.childContext(key), userId)
    }

    override fun childContext(
        name: String,
        componentContext: ComponentContext,
        userId: UserId,
    ): MatrixClientViewModelContext {
        return MatrixClientViewModelContextImpl(
            getKoin(),
            componentContext,
            userId,
            coroutineContext,
            "${this.name}:$name",
        )
    }

    @OptIn(InternalDecomposeApi::class)
    override fun childContextWithOwnLifecycle(
        name: String,
        lifecycle: Lifecycle,
        userId: UserId,
    ): MatrixClientViewModelContext =
        childContext(name, DefaultComponentContext(MergedLifecycle(this.lifecycle, lifecycle)), userId)
}

open class MatrixClientViewModelContextImpl(
    di: Koin,
    componentContext: ComponentContext,
    override val userId: UserId,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    name: String,
) : MatrixClientViewModelContext, ViewModelContextImpl(di, componentContext, coroutineContext, name) {
    override val matrixClient by lazy { getMatrixClient(userId) }

    override fun childContext(key: String): MatrixClientViewModelContext {
        val componentContext = this as ComponentContext
        return childContext(key, componentContext.childContext(key))
    }

    @OptIn(InternalDecomposeApi::class)
    override fun childContextWithOwnLifecycle(name: String, lifecycle: Lifecycle): MatrixClientViewModelContext =
        childContext(name, DefaultComponentContext(MergedLifecycle(this.lifecycle, lifecycle)))

    override fun childContext(name: String, componentContext: ComponentContext): MatrixClientViewModelContext {
        return MatrixClientViewModelContextImpl(
            getKoin(),
            componentContext,
            userId,
            coroutineContext,
            "${this.name}:$name",
        )
    }
}

/**
 * Creates a new child coroutine scope
 *
 * This scope is retained across configuration changes via instance keeper It properly propagates destruction to all
 * coroutine jobs
 */
private class ViewModelCoroutineScope(coroutineContext: CoroutineContext) : InstanceKeeper.Instance, CoroutineScope {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.viewmodel.ViewModelCoroutineScope")
    }

    val handler = CoroutineExceptionHandler { _, exception ->
        log.error(exception) { "coroutine scope with lifecycle has been cancelled" }
    }

    private val scope: CoroutineScope =
        CoroutineScope(coroutineContext + SupervisorJob(coroutineContext[Job]) + handler)

    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext

    override fun onDestroy() {
        scope.cancel()
    }
}

package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.InternalDecomposeApi
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.lifecycle.MergedLifecycle
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.Lifecycle
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.BackHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.UserId
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger { }

interface ViewModelContext : KoinComponent, ComponentContext {
    /**
     * This should be used carefully, because it can lead to leaks when not used on the top level.
     */
    val coroutineScope: CoroutineScope

    val trixnityMessengerBackHandler: BackHandler
    fun childContext(key: String): ViewModelContext
    fun childContext(componentContext: ComponentContext): ViewModelContext
    fun childContext(key: String, userId: UserId): MatrixClientViewModelContext

    fun childContext(componentContext: ComponentContext, userId: UserId): MatrixClientViewModelContext

    /**
     * TODO This is just a temporary workaround until decompose allows to destroy children.
     */
    fun childContextWithOwnLifecycle(lifecycle: Lifecycle, userId: UserId): MatrixClientViewModelContext

    fun registerBackCallback(backCallback: BackCallback)
}

interface MatrixClientViewModelContext : ViewModelContext {
    val matrixClient: MatrixClient
    val userId: UserId

    override fun childContext(key: String): MatrixClientViewModelContext
    override fun childContext(componentContext: ComponentContext): MatrixClientViewModelContext

    /**
     * TODO This is just a temporary workaround until decompose allows to destroy children.
     */
    fun childContextWithOwnLifecycle(lifecycle: Lifecycle): MatrixClientViewModelContext
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
) : ViewModelContext, ComponentContext by componentContext {
    final override val coroutineScope: CoroutineScope =
        instanceKeeper.getOrCreate { ViewModelCoroutineScope(coroutineContext) }

    override fun getKoin(): Koin = di

    override val trixnityMessengerBackHandler: BackHandler = di.get<BackHandler>()

    override fun registerBackCallback(backCallback: BackCallback) {
        with(trixnityMessengerBackHandler) { lifecycle.registerBackCallbackWithLifecycle(backCallback) }
    }

    override fun childContext(key: String): ViewModelContext {
        val componentContext = this as ComponentContext
        return childContext(componentContext.childContext(key))
    }

    override fun childContext(componentContext: ComponentContext): ViewModelContext {
        return ViewModelContextImpl(
            getKoin(),
            componentContext,
            coroutineContext
        )
    }

    override fun childContext(key: String, userId: UserId): MatrixClientViewModelContext {
        val componentContext = this as ComponentContext
        return childContext(componentContext.childContext(key), userId)
    }

    override fun childContext(componentContext: ComponentContext, userId: UserId): MatrixClientViewModelContext {
        return MatrixClientViewModelContextImpl(
            getKoin(),
            componentContext,
            userId,
            coroutineContext
        )
    }

    @OptIn(InternalDecomposeApi::class)
    override fun childContextWithOwnLifecycle(lifecycle: Lifecycle, userId: UserId): MatrixClientViewModelContext =
        childContext(DefaultComponentContext(MergedLifecycle(this.lifecycle, lifecycle)), userId)
}

open class MatrixClientViewModelContextImpl(
    di: Koin,
    componentContext: ComponentContext,
    override val userId: UserId,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : MatrixClientViewModelContext, ViewModelContextImpl(di, componentContext, coroutineContext) {
    override val matrixClient by lazy { getMatrixClient(userId) }

    override fun childContext(key: String): MatrixClientViewModelContext {
        val componentContext = this as ComponentContext
        return childContext(componentContext.childContext(key))
    }

    @OptIn(InternalDecomposeApi::class)
    override fun childContextWithOwnLifecycle(lifecycle: Lifecycle): MatrixClientViewModelContext =
        childContext(DefaultComponentContext(MergedLifecycle(this.lifecycle, lifecycle)))

    override fun childContext(componentContext: ComponentContext): MatrixClientViewModelContext {
        return MatrixClientViewModelContextImpl(
            getKoin(),
            componentContext,
            userId,
            coroutineContext
        )
    }
}

/**
 * Creates a new child coroutine scope
 *
 * This scope is retained across configuration changes via instance keeper
 * It properly propagates destruction to all coroutine jobs
 */
private class ViewModelCoroutineScope(
    coroutineContext: CoroutineContext,
) : InstanceKeeper.Instance, CoroutineScope {
    val handler = CoroutineExceptionHandler { _, exception ->
        log.error(exception) { "coroutine scope with lifecycle has been cancelled" }
    }

    private val scope: CoroutineScope =
        CoroutineScope(coroutineContext + SupervisorJob(coroutineContext[Job]) + handler)

    override val coroutineContext: CoroutineContext get() = scope.coroutineContext

    override fun onDestroy() {
        scope.cancel()
    }
}

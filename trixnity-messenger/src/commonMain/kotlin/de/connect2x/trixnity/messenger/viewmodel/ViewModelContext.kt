package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import de.connect2x.trixnity.messenger.NamedMatrixClient
import de.connect2x.trixnity.messenger.NamedMatrixClients
import de.connect2x.trixnity.messenger.util.I18n
import de.connect2x.trixnity.messenger.viewmodel.util.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.MatrixClient
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.coroutines.CoroutineContext

interface ViewModelContext : KoinComponent, ComponentContext {
    /**
     * This should be used carefully, because it can lead to leaks when not used on the top level.
     */
    val coroutineScope: CoroutineScope
    fun childContext(key: String, lifecycle: Lifecycle? = null): ViewModelContext
    fun childContext(componentContext: ComponentContext): ViewModelContext
    fun childContext(key: String, lifecycle: Lifecycle? = null, accountName: String): MatrixClientViewModelContext

    fun childContext(componentContext: ComponentContext, accountName: String): MatrixClientViewModelContext
}

interface MatrixClientViewModelContext : ViewModelContext {
    val matrixClient: MatrixClient
    val accountName: String
    override fun childContext(key: String, lifecycle: Lifecycle?): MatrixClientViewModelContext
    override fun childContext(componentContext: ComponentContext): MatrixClientViewModelContext
}

val ViewModelContext.i18n: I18n
    get() = get<I18n>()

val ViewModelContext.namedMatrixClients: StateFlow<List<NamedMatrixClient>>
    get() = get<NamedMatrixClients>().list

val ViewModelContext.matrixClients: StateFlow<List<MatrixClient>>
    get() = namedMatrixClients.map { namedMatrixClients ->
        namedMatrixClients.map {
            it.matrixClient.value ?: throw IllegalStateException("Cannot find MatrixClient for ${it.accountName}.")
        }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())

fun ViewModelContext.getMatrixClient(accountName: String) =
    get<NamedMatrixClients>().list.value.find { it.accountName == accountName }?.matrixClient?.value
        ?: throw IllegalStateException("Cannot find MatrixClient for ${accountName}.")

open class ViewModelContextImpl(
    private val di: Koin,
    componentContext: ComponentContext,
    protected val coroutineContext: CoroutineContext = Dispatchers.Default,
) : ViewModelContext,
    ComponentContext by componentContext {
    final override val coroutineScope: CoroutineScope = componentContext.coroutineScope(coroutineContext)

    override fun getKoin(): Koin = di

    override fun childContext(key: String, lifecycle: Lifecycle?): ViewModelContext {
        val componentContext = this as ComponentContext
        return childContext(componentContext.childContext(key, lifecycle))
    }

    override fun childContext(componentContext: ComponentContext): ViewModelContext {
        return ViewModelContextImpl(
            getKoin(),
            componentContext,
            coroutineContext
        )
    }

    override fun childContext(key: String, lifecycle: Lifecycle?, accountName: String): MatrixClientViewModelContext {
        val componentContext = this as ComponentContext
        return childContext(componentContext.childContext(key, lifecycle), accountName)
    }

    override fun childContext(componentContext: ComponentContext, accountName: String): MatrixClientViewModelContext {
        return MatrixClientViewModelContextImpl(
            getKoin(),
            componentContext,
            accountName,
            coroutineContext
        )
    }
}

open class MatrixClientViewModelContextImpl(
    di: Koin,
    componentContext: ComponentContext,
    override val accountName: String,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : MatrixClientViewModelContext, ViewModelContextImpl(di, componentContext, coroutineContext) {
    override val matrixClient by lazy { getMatrixClient(accountName) }

    override fun childContext(key: String, lifecycle: Lifecycle?): MatrixClientViewModelContext {
        val componentContext = this as ComponentContext
        return childContext(componentContext.childContext(key, lifecycle))
    }

    override fun childContext(componentContext: ComponentContext): MatrixClientViewModelContext {
        return MatrixClientViewModelContextImpl(
            getKoin(),
            componentContext,
            accountName,
            coroutineContext
        )
    }
}
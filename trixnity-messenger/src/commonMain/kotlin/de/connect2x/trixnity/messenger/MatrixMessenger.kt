package de.connect2x.trixnity.messenger

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.flattenValues
import net.folivo.trixnity.client.room
import org.koin.core.Koin
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger {}

interface MatrixMessenger {
    companion object

    val di: Koin

    val notificationCount: StateFlow<Long>

    /**
     * Stop this [MatrixMessenger] and its [CoroutineScope].
     * It should be called to clean up all resources used by [MatrixMessenger].
     *
     * After calling this, this instance should not be used anymore!
     */
    fun stop()
}

class MatrixMessengerImpl private constructor(
    override val di: Koin,
) : MatrixMessenger {
    companion object {
        suspend operator fun invoke(
            coroutineContext: CoroutineContext = Dispatchers.Default,
            configuration: MatrixMessengerConfiguration.() -> Unit,
        ): MatrixMessengerImpl {
            val config = MatrixMessengerConfiguration().apply(configuration)
            val exceptionHandler = CoroutineExceptionHandler { exceptionCoroutineContext, throwable ->
                log.error(throwable) { "Exception in global CoroutineScope $exceptionCoroutineContext" }
            }
            val coroutineScope =
                CoroutineScope(coroutineContext + CoroutineName("trixnity-messenger-global") + SupervisorJob() + exceptionHandler)
            val di = koinApplication {
                modules(module {
                    single { coroutineScope }
                    single { config }.bind<MatrixMessengerBaseConfiguration>()
                })
                modules(config.modules)
            }.koin
            val settingsHolder = di.getAll<SettingsHolder<*>>()
            settingsHolder.forEach {
                log.debug { "initialize SettingsHolder ($it)" }
                it.init()
            }
            return MatrixMessengerImpl(di)
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    override val notificationCount = di.get<MatrixClients>().map { it.values }.flatMapLatest { matrixClients ->
        combine(
            matrixClients.map { matrixClient ->
                matrixClient.room.getAll().flattenValues().map { rooms -> rooms.sumOf { it.unreadMessageCount } }
            }
        ) {
            it.sum()
        }
    }.stateIn(di.get(), SharingStarted.WhileSubscribed(), 0L)

    override fun stop() {
        di.get<CoroutineScope>().cancel()
        di.get<MatrixClients>().value.values.forEach { it.stop() }
    }
}

fun MatrixMessenger.createRoot(
    componentContext: ComponentContext = DefaultComponentContext(LifecycleRegistry())
): RootViewModel =
    di.get<RootViewModelFactory>().create(
        componentContext = componentContext,
        di = di,
    )
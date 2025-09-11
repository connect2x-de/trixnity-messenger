package de.connect2x.trixnity.messenger

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.settings.SettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.flattenValues
import net.folivo.trixnity.client.room
import org.koin.core.Koin
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger {}

interface MatrixMessenger : AutoCloseable {
    companion object

    val di: Koin

    val notificationCount: StateFlow<Long>

    /**
     * This will wait for the cancel() operations of child CoroutineScopes. Use this, when the app goes into the
     * background to be sure that every operation in Trixnity Messenger is finished.
     */
    suspend fun closeSuspending()
}

class MatrixMessengerImpl private constructor(
    override val di: Koin,
) : MatrixMessenger {
    companion object {
        suspend operator fun invoke(
            coroutineContext: CoroutineContext = Dispatchers.Default,
            configuration: MatrixMessengerConfiguration.() -> Unit,
        ): MatrixMessengerImpl {
            log.debug { "create MatrixMessengerImpl" }
            val config = MatrixMessengerConfiguration().apply(configuration)
            val exceptionHandler = CoroutineExceptionHandler { exceptionCoroutineContext, throwable ->
                log.error(throwable) { "Exception in global CoroutineScope $exceptionCoroutineContext" }
            }
            val coroutineScope =
                CoroutineScope(
                    coroutineContext
                            + CoroutineName("trixnity-messenger-global")
                            + SupervisorJob(coroutineContext[Job])
                            + exceptionHandler
                )
            val di = koinApplication {
                modules(module {
                    single<CoroutineScope> { coroutineScope }
                    single { config }.bind<MatrixMessengerBaseConfiguration>()
                })
                modules(config.modulesFactories.map { it.invoke() })
            }.koin
            val settingsHolder = di.getAll<SettingsHolder<*>>()
            settingsHolder.forEach {
                log.debug { "initialize SettingsHolder ($it)" }
                it.init()
            }
            val worker = di.getAll<MatrixMessengerWorker>()
            worker.forEach { work ->
                log.debug { "start worker $work" }
                coroutineScope.launch { work() }
            }
            log.debug { "created MatrixMessengerImpl" }
            return MatrixMessengerImpl(di)
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    override val notificationCount by lazy {
        di.get<MatrixClients>().map { it.values }.flatMapLatest { matrixClients ->
            combine(
                matrixClients.map { matrixClient ->
                    matrixClient.room.getAll().flattenValues().map { rooms -> rooms.sumOf { it.unreadMessageCount } }
                }
            ) {
                it.sum()
            }
        }.stateIn(di.get(), SharingStarted.WhileSubscribed(), 0L)
    }

    override fun close() {
        di.getAll<AutoCloseable>().forEach { it.close() }
        di.get<CoroutineScope>().apply {
            cancel("stopped MatrixMessenger")
        }
        di.get<MatrixClients>().value.values.forEach { it.close() }
        if (di.getOrNull<MatrixMultiMessengerConfiguration>() == null) {
            di.get<MatrixMessengerConfiguration>().httpClientEngine?.close()
        }
    }

    override suspend fun closeSuspending() {
        val job = di.get<CoroutineScope>().coroutineContext.job
        close()
        job.join()
    }
}

fun MatrixMessenger.createRoot(
    componentContext: ComponentContext = DefaultComponentContext(LifecycleRegistry())
): RootViewModel {
    log.debug { "create RootViewModel" }
    return di.get<RootViewModelFactory>().create(
        componentContext = componentContext,
        di = di,
    )
}

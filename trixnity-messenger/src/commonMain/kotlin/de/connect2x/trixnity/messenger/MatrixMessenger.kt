package de.connect2x.trixnity.messenger

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.settings.SettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelFactory
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import de.connect2x.trixnity.client.notification
import org.koin.core.Koin
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

interface MatrixMessenger : AutoCloseable {
    companion object

    val di: Koin

    val notificationCount: StateFlow<Int>

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
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.MatrixMessengerImpl")

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
            val worker = di.getAll<Worker>()
            worker.forEach { worker ->
                log.debug { "start worker $worker" }
                coroutineScope.launch { worker.doWork() }
            }
            log.debug { "created MatrixMessengerImpl" }
            return MatrixMessengerImpl(di)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val notificationCount by lazy {
        di.get<MatrixClients>().map { it.values }.flatMapLatest { matrixClients ->
            if (matrixClients.isEmpty()) flowOf(0)
            else combine(
                matrixClients.map { it.notification.getCount() }
            ) {
                it.sum()
            }
        }.stateIn(di.get(), SharingStarted.WhileSubscribed(), 0)
    }

    override fun close() {
        di.getAll<AutoCloseable>().forEach { it.close() }
        di.get<CoroutineScope>().apply {
            cancel("stopped MatrixMessenger")
        }
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
    return di.get<RootViewModelFactory>().create(
        componentContext = componentContext,
        di = di,
    )
}

package de.connect2x.trixnity.messenger

import com.arkivanov.decompose.ComponentContext
import de.connect2x.trixnity.messenger.util.UrlHandler
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.flattenValues
import net.folivo.trixnity.client.room
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

private val log = KotlinLogging.logger {}

// FIXME README
interface MatrixMessenger {
    companion object

    val root: RootViewModel
    val di: Koin

    val notificationCount: StateFlow<Long>

    /**
     * Stop the MatrixMessenger and its [CoroutineScope].
     * It should be called to clean up all resources used by [MatrixMessenger].
     *
     * After calling this, this instance should not be used anymore!
     */
    fun stop()
}

internal suspend fun MatrixMessenger.Companion.create(
    componentContext: ComponentContext,
    configuration: MatrixMessengerConfiguration.() -> Unit,
    defaultModule: Module.() -> Unit = {},
): MatrixMessenger {
    val config = MatrixMessengerConfiguration().apply(configuration)
    val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        log.error(throwable) { "Exception in global CoroutineScope $coroutineContext" }
    }
    val coroutineScope =
        CoroutineScope(Dispatchers.Default + CoroutineName("trixnity-messenger-global") + SupervisorJob() + exceptionHandler)
    val koinApplication = koinApplication {
        modules(module {
            single { coroutineScope }
            single { config }
            defaultModule()
        })
        modules(config.modules)
    }
    val matrixMessengerSettingsHolder = koinApplication.koin.get<CreateMatrixMessengerSettingsHolder>()()
    koinApplication.modules(
        module {
            single { matrixMessengerSettingsHolder }
        }
    )

    return MatrixMessengerImpl(
        componentContext = componentContext,
        di = koinApplication.koin,
    )
}

class MatrixMessengerImpl internal constructor(
    componentContext: ComponentContext,
    override val di: Koin,
) : MatrixMessenger {
    override val root: RootViewModel by lazy {
        di.get<RootViewModelFactory>().create(
            componentContext = componentContext,
            di = di,
        )
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


val MatrixMessenger.urlHandler: UrlHandler
    get() = di.get<UrlHandler>()
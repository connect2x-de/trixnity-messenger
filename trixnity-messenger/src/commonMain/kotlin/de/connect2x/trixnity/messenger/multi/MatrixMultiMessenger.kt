package de.connect2x.trixnity.messenger.multi

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.settings.SettingsHolder
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.koin.core.Koin
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger {}

interface MatrixMultiMessenger : ProfileManager, AutoCloseable {
    companion object

    val di: Koin

    /**
     * This will wait for the cancel() operations of child CoroutineScopes. Use this, when the app goes into the
     * background to sure that every operation in Trixnity Messenger is finished.
     */
    suspend fun closeAndWait()
}

class MatrixMultiMessengerImpl private constructor(
    override val di: Koin,
    private val profileManager: ProfileManager,
    private val supervisorJob: Job,
) : MatrixMultiMessenger, ProfileManager by profileManager {

    companion object {
        @DefaultArgumentInterop.Enabled
        suspend operator fun invoke(
            coroutineContext: CoroutineContext = Dispatchers.Default,
            configuration: MatrixMultiMessengerConfiguration.() -> Unit,
        ): MatrixMultiMessengerImpl {
            val config = MatrixMultiMessengerConfiguration().apply(configuration)
            val exceptionHandler = CoroutineExceptionHandler { exceptionCoroutineContext, throwable ->
                log.error(throwable) { "Exception in global CoroutineScope $exceptionCoroutineContext" }
            }
            val supervisorJob = SupervisorJob()
            val coroutineScope =
                CoroutineScope(coroutineContext + CoroutineName("trixnity-multi-messenger-global") + supervisorJob + exceptionHandler)
            val di = koinApplication {
                modules(
                    module {
                        single<CoroutineScope> { coroutineScope }
                        single { config }.bind<MatrixMessengerBaseConfiguration>()
                    },
                )
                modules(config.modulesFactories.map { it.invoke() })
            }.koin
            val settingsHolder = di.getAll<SettingsHolder<*>>()
            settingsHolder.forEach {
                log.debug { "initialize SettingsHolder ($it)" }
                it.init()
            }
            di.get<MatrixMultiMessengerSettingsHolder>().update<MatrixMultiMessengerSettingsBase> { oldSettings ->
                if (oldSettings.forgetActiveProfileOnStart) oldSettings.copy(activeProfile = null)
                else oldSettings
            }
            val worker = di.getAll<MatrixMultiMessengerWorker>()
            worker.forEach { work ->
                log.debug { "start worker $work" }
                coroutineScope.launch { work() }
            }
            val matrixMultiMessengerImpl = MatrixMultiMessengerImpl(
                di = di,
                profileManager = di.get(),
                supervisorJob = supervisorJob,
            )

            return matrixMultiMessengerImpl
        }
    }

    override fun close() {
        di.get<CoroutineScope>().apply {
            cancel("stopped MatrixMultiMessenger")
        }
        activeMatrixMessenger.value?.close()
        di.get<MatrixMultiMessengerConfiguration>().httpClientEngine?.close()
        di.close()
    }

    override suspend fun closeAndWait() {
        supervisorJob.cancelAndJoin()
        close()
    }
}

suspend fun MatrixMultiMessenger.singleModeMatrixMessenger(): Flow<MatrixMessenger> {
    if (activeProfile.value == null) {
        val profile = profiles.value.keys.firstOrNull() ?: createProfile()
        selectProfile(profile)
    }
    return activeMatrixMessenger.filterNotNull()
}

suspend fun MatrixMultiMessenger.singleMode(block: suspend (MatrixMessenger) -> Unit) {
    singleModeMatrixMessenger().collectLatest(block)
}


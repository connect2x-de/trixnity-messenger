package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.settings.SettingsHolder
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import org.koin.core.Koin
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger {}

interface MatrixMultiMessenger : ProfileManager {
    companion object

    val di: Koin

    /**
     * Stop this [MatrixMultiMessenger], its [CoroutineScope] ant the active [MatrixMessenger].
     * It should be called to clean up all resources used by [MatrixMultiMessenger].
     *
     * After calling this, this instance should not be used anymore!
     */
    suspend fun stop()
}

class MatrixMultiMessengerImpl private constructor(
    override val di: Koin,
    private val profileManager: ProfileManager,
) : MatrixMultiMessenger, ProfileManager by profileManager {

    companion object {
        suspend operator fun invoke(
            coroutineContext: CoroutineContext = Dispatchers.Default,
            configuration: MatrixMultiMessengerConfiguration.() -> Unit,
        ): MatrixMultiMessengerImpl {
            val config = MatrixMultiMessengerConfiguration().apply(configuration)
            val exceptionHandler = CoroutineExceptionHandler { exceptionCoroutineContext, throwable ->
                log.error(throwable) { "Exception in global CoroutineScope $exceptionCoroutineContext" }
            }
            val coroutineScope =
                CoroutineScope(coroutineContext + CoroutineName("trixnity-multi-messenger-global") + SupervisorJob() + exceptionHandler)
            val di = koinApplication {
                modules(
                    module {
                        single<CoroutineScope> { coroutineScope }
                        single { config }.bind<MatrixMessengerBaseConfiguration>()
                    },
                )
                modules(config.modules)
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
            return MatrixMultiMessengerImpl(
                di = di,
                profileManager = di.get()
            )
        }
    }

    override suspend fun stop() {
        di.get<CoroutineScope>().apply {
            cancel("stopped MatrixMultiMessenger")
            coroutineContext.job.join()
        }
        activeMatrixMessenger.value?.stop()
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


package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.SettingsHolder
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import org.koin.core.Koin
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
    fun stop()
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
                        single { coroutineScope }
                        single { config }
                    },
                )
                modules(config.modules)
            }.koin
            val settingsHolder = di.getAll<SettingsHolder<*>>()
            settingsHolder.forEach {
                log.debug { "initialize SettingsHolder ($it)" }
                it.init()
            }
            di.get<MatrixMultiMessengerSettingsHolder>().update { oldSettings ->
                if (oldSettings.forgetActiveProfileOnStart) oldSettings.copy(activeProfile = null)
                else oldSettings
            }
            return MatrixMultiMessengerImpl(
                di = di,
                profileManager = di.get()
            )
        }
    }

    override fun stop() {
        activeMatrixMessenger.value?.stop()
        di.get<CoroutineScope>().cancel()
    }
}

suspend fun MatrixMultiMessenger.singleMode(block: suspend (MatrixMessenger) -> Unit) {
    if (activeProfile.value == null) {
        val firstProfile = profiles.value.keys.firstOrNull()
            ?: createProfile()
        selectProfile(firstProfile)
    }
    activeMatrixMessenger.filterNotNull().collectLatest(block)
}
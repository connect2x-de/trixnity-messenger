package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import io.ktor.client.*
import io.ktor.client.engine.*
import org.koin.core.module.Module

data class MatrixMultiMessengerConfiguration(
    override var appName: String = "Trixnity Messenger",
    override var appId: String = "de.connect2x.messenger",
    override var urlProtocol: String = appId,
    override var urlHost: String = "localhost",
    override var sendLogsEmailAddress: String? = null,
    override var privacyInfoUrl: String? = null,
    override var imprintUrl: String? = null,
    override var licenses: String? = null,
    override var pushUrl: String? = null,
    /**
     * Consider using [messengerConfiguration], as it can be called multiple times.
     */
    var messenger: MatrixMessengerConfiguration.() -> Unit = { },

    /**
     * Specify a [HttpClientEngine]. This should be reused in an application.
     */
    override var httpClientEngine: HttpClientEngine? = null,

    /**
     * Configure the underlying [HttpClient].
     */
    override var httpClientConfig: (HttpClientConfig<*>.() -> Unit)? = null,
    /**
     * Inject and override modules.
     */
    var modules: List<Module> = createDefaultTrixnityMultiMessengerModules(),

    /**
     * Simultaneously use multiple profiles
     */
    var multiProfile: Boolean = true,
) : MatrixMessengerBaseConfiguration {
    val messengerWithBase: MatrixMessengerConfiguration.() -> Unit
        get() = {
            this@MatrixMultiMessengerConfiguration.copyTo(this)
            this@MatrixMultiMessengerConfiguration.messenger(this)
        }

    /**
     * By default, all [MatrixMessengerBaseConfiguration] fields, defined in [MatrixMultiMessengerConfiguration] are copied
     * to [messenger]. Therefore, you don't need to define e.g. [appName] twice.
     */
    fun messengerConfiguration(config: MatrixMessengerConfiguration.() -> Unit) {
        val original = messenger
        messenger = {
            original()
            config()
        }
    }
}



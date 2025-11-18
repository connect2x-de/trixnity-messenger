package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.TrixnityMessengerDsl
import io.ktor.client.*
import io.ktor.client.engine.*
import net.folivo.trixnity.client.ModuleFactory

@TrixnityMessengerDsl
data class MatrixMultiMessengerConfiguration(
    override var appName: String = "Trixnity Messenger",
    override var appId: String = "de.connect2x.messenger",
    override var appVersion: String? = null,
    override var urlProtocol: String = appId,
    override var urlHost: String = "localhost",
    override var sendLogsEmailAddress: String? = null,
    override var privacyInfo: String? = null,
    override var imprint: String? = null,
    override var licenses: String? = null,

    /**
     * Consider using [messengerConfiguration], as it can be called multiple times.
     */
    var messenger: MatrixMessengerConfiguration.() -> Unit = { },

    /**
     * Specify a [HttpClientEngine]. It is highly recommended to set it and share it within an application.
     */
    override var httpClientEngine: HttpClientEngine? = null,

    /**
     * Configure the underlying [HttpClient].
     */
    override var httpClientConfig: (HttpClientConfig<*>.() -> Unit)? = null,

    /**
     * Inject and override modules into Trixnity Messenger. By default, this is [createTrixnityMultiMessengerDefaultModuleFactories].
     *
     * Be aware to always create new modules because a module stores your class instances and therefore is reused, which we don't want!
     *
     * For example:
     * ```kotlin
     * modulesFactories += ::createCustomModule
     * ```
     */
    var modulesFactories: List<ModuleFactory> = createTrixnityMultiMessengerDefaultModuleFactories(),

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



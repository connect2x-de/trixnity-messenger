package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import org.koin.core.module.Module

data class MatrixMultiMessengerConfiguration(
    override var appName: String = "Trixnity Messenger",
    override var packageName: String = "de.connect2x",
    override var urlProtocol: String = "trixnity",
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
     * Inject and override modules.
     */
    var modules: List<Module> = createDefaultTrixnityMultiMessengerModules(),
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



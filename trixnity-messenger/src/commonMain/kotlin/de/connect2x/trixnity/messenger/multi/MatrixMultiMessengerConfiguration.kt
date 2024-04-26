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
    /**
     * By default, all [MatrixMessengerBaseConfiguration] fields, defined in [MatrixMultiMessengerConfiguration] are copied
     * to [messenger]. Therefore, you don't need to define e.g. [appName] twice.
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
            messenger()
        }
}

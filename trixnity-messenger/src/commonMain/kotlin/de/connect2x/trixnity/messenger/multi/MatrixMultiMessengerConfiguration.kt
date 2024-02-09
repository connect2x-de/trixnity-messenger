package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import org.koin.core.module.Module

data class MatrixMultiMessengerConfiguration(
    var messenger: MatrixMessengerConfiguration.() -> Unit = { MatrixMessengerConfiguration() },
    /**
     * Inject and override modules.
     */
    var modules: List<Module> = createDefaultTrixnityMultiMessengerModules(),
)
package de.connect2x.trixnity.messenger

import org.koin.core.module.Module

data class MatrixMessengerConfiguration(
    override var appName: String = "Trixnity Messenger",
    override var packageName: String = "de.connect2x",

    var encryptLocalData: Boolean = true,

    override var urlProtocol: String = "trixnity",
    override var urlHost: String = "localhost",
    var ssoRedirectPath: String = "sso",

    var generateInitialAccountColor: (suspend (alreadyUsedColors: Set<Long>) -> Long)? = null,

    // for privacy-first, override these values
    var defaultPresenceIsPublic: Boolean = true,
    var defaultReadMarkerIsPublic: Boolean = true,
    var defaultTypingIsPublic: Boolean = true,

    val timelineAutoLoadBefore: Boolean = true,

    /**
     * The maximum size of attachments that can be sent in *MegaByte*.
     */
    var attachmentMaxSize: Int = 1_000,

    /**
     * The maximum size of image attachments that are processed to change their rotation before upload in *MegaByte*.
     */
    val imageAttachmentMaxProcessingSize : Int = 50,

    var defaultHomeServer: String? = null,

    override var sendLogsEmailAddress: String? = null,

    /**
     * Inject and override modules.
     */
    var modules: List<Module> = createDefaultTrixnityMessengerModules(),
) : MatrixMessengerBaseConfiguration

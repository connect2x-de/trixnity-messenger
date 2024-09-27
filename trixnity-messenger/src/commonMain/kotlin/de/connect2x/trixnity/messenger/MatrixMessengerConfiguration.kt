package de.connect2x.trixnity.messenger

import org.koin.core.module.Module

private val colors =
    listOf(
        0xAAB749C7,
        0xAAC77849,
        0xAA59C749,
        0xAAC7B849,
        0xAA49C7C6,
        0xAAC7494B,
        0xAA49C771,
    )

data class MatrixMessengerConfiguration(
    override var appName: String = "Trixnity Messenger",
    override var packageName: String = "de.connect2x",

    var encryptLocalData: Boolean = true,

    override var urlProtocol: String = "trixnity",
    override var urlHost: String = "localhost",
    var ssoRedirectPath: String = "sso",

    var generateInitialAccountColor: (suspend (alreadyUsedColors: Set<Long>) -> Long)? = { alreadyUsedColors: Set<Long> ->
        colors.firstOrNull { alreadyUsedColors.contains(it).not() } ?: 0x00000000
    },

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
    val imageAttachmentMaxProcessingSize: Int = 50,

    var defaultHomeServer: String? = null,

    override var sendLogsEmailAddress: String? = null,

    override var privacyInfoUrl: String? = null,
    override var imprintUrl: String? = null,
    override var licenses: String? = null,

    override var pushUrl: String? = null,

    /**
     * Inject and override modules.
     */
    var modules: List<Module> = createDefaultTrixnityMessengerModules(),
) : MatrixMessengerBaseConfiguration

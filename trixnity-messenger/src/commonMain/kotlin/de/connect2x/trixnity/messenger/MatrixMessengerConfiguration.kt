package de.connect2x.trixnity.messenger

import org.koin.core.module.Module

data class MatrixMessengerConfiguration(
    var appName: String = "Trixnity Messenger",
    var packageName: String = "de.connect2x",

    var encryptLocalData: Boolean = true,

    var urlProtocol: String = "trixnity",
    var urlHost: String = "localhost",
    var ssoRedirectPath: String = "sso",

    var generateInitialAccountColor: (suspend (alreadyUsedColors: Set<Long>) -> Long)? = null,

    // for privacy-first, override these values
    var defaultPushMode: PushMode = PushMode.PUSH,
    var defaultPresenceIsPublic: Boolean = true,
    var defaultReadMarkerIsPublic: Boolean = true,
    var defaultTypingIsPublic: Boolean = true,

    // notifications defaults
    var defaultNotificationPlaySound: Boolean = true,
    var defaultNotificationShowPopup: Boolean = true,
    var defaultNotificationShowText: Boolean = true,

    val timelineAutoLoadBefore: Boolean = true,

    /**
     * The maximum size of attachments that can be sent in *MegaByte*.
     */
    var attachmentMaxSize: Int = 500,

    var defaultHomeServer: String? = null,

    var sendLogsEmailAddress: String? = null,

    /**
     * Inject and override modules into Trixnity Messenger.
     */
    var modules: List<Module> = createDefaultTrixnityMessengerModules(),
)

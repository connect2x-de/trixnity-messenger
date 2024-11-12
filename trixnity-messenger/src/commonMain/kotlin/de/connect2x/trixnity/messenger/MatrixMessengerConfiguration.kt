package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.gb
import de.connect2x.trixnity.messenger.util.mb
import io.ktor.client.*
import io.ktor.client.engine.*
import net.folivo.trixnity.client.ModuleFactory

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
    override var appId: String = "de.connect2x.messenger",

    var encryptLocalData: Boolean = true,

    override var urlProtocol: String = appId,
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
     * The maximum size of attachments that can be sent in *Bytes*.
     */
    var attachmentMaxSize: Long = 1.gb(),

    /**
     * The maximum size of image attachments that are processed to change their rotation before upload in *Bytes*.
     */
    val imageAttachmentMaxProcessingSize: Long = 50.mb(),

    /**
     * The maximum size of clipboard files that can be uploaded
     */
    val filePreviewMaxSize: Long = 50.mb(),

    /**
     * The maximum size of avatars that can be uploaded in *Bytes*
     */
    var avatarMaxSize: Long = 10.mb(),

    var defaultHomeServer: String? = null,

    override var sendLogsEmailAddress: String? = null,

    override var privacyInfoUrl: String? = null,
    override var imprintUrl: String? = null,
    override var licenses: String? = null,

    override var pushUrl: String? = null,

    /**
     * Specify a [HttpClientEngine]. It is highly recommended to set it and share it within an application.
     */
    override var httpClientEngine: HttpClientEngine? = null,

    /**
     * Configure the underlying [HttpClient].
     */
    override var httpClientConfig: (HttpClientConfig<*>.() -> Unit)? = null,

    /**
     * Inject and override modules into Trixnity Messenger. By default, this is [createTrixnityMessengerDefaultModuleFactories].
     *
     * Be aware to always create new modules because a module stores your class instances and therefore is reused, which we don't want!
     *
     * For example:
     * ```kotlin
     * modulesFactories += ::createCustomModule
     * ```
     */
    var modulesFactories: List<ModuleFactory> = createTrixnityMessengerDefaultModuleFactories(),
) : MatrixMessengerBaseConfiguration

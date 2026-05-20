package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.client.MatrixClientConfiguration
import de.connect2x.trixnity.client.ModuleFactory
import de.connect2x.trixnity.messenger.util.mb
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private val colors = listOf(0xAAB749C7, 0xAAC77849, 0xAA59C749, 0xAAC7B849, 0xAA49C7C6, 0xAAC7494B, 0xAA49C771)

@TrixnityMessengerDsl
data class MatrixMessengerConfiguration(
    override var appName: String = "Trixnity Messenger",
    override var appId: String = "de.connect2x.trixnity.messenger",
    override var appVersion: String? = null,
    override var appIcon: String? = null,
    override var appUri: String = "$appId:",
    var appUriSsoRedirect: String = "sso",
    var appUriOAuth2Redirect: String = "oAuth2",
    override var oAuth2ClientUrl: String = "https://messenger.trixnity.connect2x.de",
    var generateInitialAccountColor: (suspend (alreadyUsedColors: Set<Long>) -> Long)? =
        { alreadyUsedColors: Set<Long> ->
            colors.firstOrNull { alreadyUsedColors.contains(it).not() } ?: 0x00000000
        },

    // for the full experience; override these values for privacy-first
    var defaultPresenceIsPublic: Boolean = true,
    var defaultReadMarkerIsPublic: Boolean = true,
    var defaultTypingIsPublic: Boolean = true,
    var defaultRedactionWarningIsEnabled: Boolean = true,
    var databaseEncryptionEnabled: Boolean = true,
    val features: Features =
        Features(
            enablePdfReader = true,
            enablePowerlevelEventConfigurationInRoomSettings = true,
            enableMessageDrafts = true,
            enableAudioRecorder = true,
            enableMediaPlayer = true,
        ),

    /** The number of elements that should be loaded before and after the viewport. */
    var timelineBuffer: Int = 20,
    /** The number of elements that should be fetched from the server when not locally available. */
    var timelineFetchSize: Int = 100,
    /** The number of elements that should be loaded initially. */
    var timelineInitialSize: Int = 20,
    /** The maximum number of elements that should be kept in the timeline list (plus 2 * [timelineBuffer]) */
    var timelineMaxSize: Int = 160,

    /** The maximum amount of time until message are seperated by extra space */
    var showBigGapBeforeThreshold: Duration = 1.hours,

    /** The maximum size of files that can be loaded into memory in *Bytes* */
    var maxMediaSizeInMemory: Long = 50.mb(),
    var defaultHomeServer: String? = null,

    /**
     * Whether the [de.connect2x.trixnity.messenger.compose.view.settings.AccountSetupWizard] is used to setup new
     * accounts.
     *
     * Alternatively, the [de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModel] and others
     * can be used to manually guide the user through the setup process.
     *
     * Default is `true`.
     */
    var useAccountSetupWizard: Boolean = true,
    override var sendLogsEmailAddress: String? = null,

    /** The privacy info of the application in a Markdown format */
    override var privacyInfo: String? = null,
    /** The imprint of the application in a Markdown format */
    override var imprint: String? = null,
    override var licenses: String? = null,
    var downloadsDisabled: Boolean = false,

    /** Whether the messenger uses refresh tokens or access tokens. */
    var useRefreshTokens: Boolean = false,
    var cryptoDriver: CryptoDriver = CryptoDriver.VODOZEMAC,

    /** Consider using [clientConfiguration], as it can be called multiple times. */
    var client: MatrixClientConfiguration.() -> Unit = {},

    /** Specify a [HttpClientEngine]. It is highly recommended to set it and share it within an application. */
    override var httpClientEngine: HttpClientEngine? = null,

    /** Configure the underlying [HttpClient]. */
    override var httpClientConfig: (HttpClientConfig<*>.() -> Unit)? = null,

    /**
     * Inject and override modules into Trixnity Messenger. By default, this is
     * [createTrixnityMessengerDefaultModuleFactories].
     *
     * Be aware to always create new modules because a module stores your class instances and therefore is reused, which
     * we don't want!
     *
     * For example:
     * ```kotlin
     * modulesFactories += ::createCustomModule
     * ```
     */
    var modulesFactories: List<ModuleFactory> = createTrixnityMessengerDefaultModuleFactories(),
) : MatrixMessengerBaseConfiguration {

    fun clientConfiguration(config: MatrixClientConfiguration.() -> Unit) {
        val original = client
        client = {
            original()
            config()
        }
    }

    /** A list of feature toggles for the Matrix Messenger. */
    data class Features(
        /** If true, the PDF reader details view in the timeline will be enabled. */
        var enablePdfReader: Boolean = true,
        var enablePowerlevelEventConfigurationInRoomSettings: Boolean = true,
        var enableMessageDrafts: Boolean = true,
        /** Transitively enables voice messages */
        var enableAudioRecorder: Boolean = true,
        var enableMediaPlayer: Boolean = true,
    )

    enum class CryptoDriver {
        LIBOLM,
        VODOZEMAC,
    }
}

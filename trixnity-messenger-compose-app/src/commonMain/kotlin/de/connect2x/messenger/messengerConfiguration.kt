package de.connect2x.messenger

import de.connect2x.messenger.compose.view.composeViewModule
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.i18n.Languages
import de.connect2x.trixnity.messenger.i18n.platformGetSystemLangModule
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.platformMatrixMessengerSettingsHolderModule
import de.connect2x.trixnity.messenger.util.RootPath
import org.koin.dsl.module


fun messengerConfiguration(): MatrixMultiMessengerConfiguration.() -> Unit = {
    appName = BuildConfig.appName
    packageName = "de.connect2x.${BuildConfig.appNameCleaned}"
    privacyInfoUrl = "https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger"
    imprintUrl = "https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger"
    licenses = BuildConfig.licenses
    sendLogsEmailAddress = null
    urlProtocol = BuildConfig.appNameCleaned
    modules += listOf(
        composeViewModule(),
        // TODO this needs to be removed and fixed, as there is no MatrixMessengerSettingsHolderImpl at MultiMessenger level!
        platformMatrixMessengerSettingsHolderModule(),
        // TODO there should be a more clean way for I18n
        platformGetSystemLangModule(),
        module {
            single<Languages> { DefaultLanguages }
            single<I18n> { object : I18n(get(), get(), get()) {} }
        },
    )

    // MatrixMultiMessengerConfiguration flavors
    when (BuildConfig.flavor) {
        Flavor.PROD -> {}
        Flavor.DEV -> {
            modules += module {
                val devRootPath = getDevRootPath()
                if (devRootPath != null) single<RootPath> { devRootPath }
            }
        }
    }

    // MatrixMessengerConfiguration flavors
    messengerConfiguration {
        modules += listOf(
            composeViewModule(),
        )
        when (BuildConfig.flavor) {
            Flavor.PROD -> {}
            Flavor.DEV -> {
                // defaultHomeServer = "" // TODO your home server
            }
        }
    }
}

internal expect fun getDevRootPath(): RootPath?

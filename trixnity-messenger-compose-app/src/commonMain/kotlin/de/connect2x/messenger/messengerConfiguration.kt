package de.connect2x.messenger

import de.connect2x.messenger.compose.view.composeViewModule
import de.connect2x.messenger.compose.view.notifications.notificationsModule
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.i18n.Languages
import de.connect2x.trixnity.messenger.i18n.platformGetSystemLangModule
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.platformMatrixMessengerSettingsHolderModule
import de.connect2x.trixnity.messenger.util.RootPath
import kotlinx.datetime.TimeZone
import org.koin.dsl.module


fun messengerConfiguration(
    customConfig: MatrixMultiMessengerConfiguration.() -> Unit = {},
): MatrixMultiMessengerConfiguration.() -> Unit = multiMessengerConfig@{
    appName = BuildConfig.appName
    appId = BuildConfig.appId
    privacyInfo = "https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger"
    imprint = "https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger"
    pushUrl = "https://sygnal.demo.timmy-messenger.de/_matrix/push/v1/notify"
    licenses = BuildConfig.licenses
    sendLogsEmailAddress = null
    urlProtocol = BuildConfig.appId
    val notificationsDebugEnabled = BuildConfig.flavor == Flavor.DEV

    modulesFactories += listOf(
        { composeViewModule(null) },
        { notificationsModule(this@multiMessengerConfig, notificationsDebugEnabled) },
        // TODO this needs to be removed and fixed, as there is no MatrixMessengerSettingsHolderImpl at MultiMessenger level!
        ::platformMatrixMessengerSettingsHolderModule,
        // TODO there should be a more clean way for I18n
        ::platformGetSystemLangModule,
        {
            module {
                single<Languages> { DefaultLanguages }
                single<I18n> { object : I18n(get(), get(), get(), get<TimeZone>()) {} }
            }
        },
    )
    multiProfile = true

    // MatrixMultiMessengerConfiguration flavors
    when (BuildConfig.flavor) {
        Flavor.PROD -> {}
        Flavor.DEV -> {
            modulesFactories += {
                module {
                    val devRootPath = getDevRootPath()
                    if (devRootPath != null) single<RootPath> { devRootPath }
                }
            }
        }
    }

    // MatrixMessengerConfiguration flavors
    messengerConfiguration messengerConfig@{
        modulesFactories += { composeViewModule(this) }
        modulesFactories += { notificationsModule(this@messengerConfig, notificationsDebugEnabled) }
        defaultHomeServer = "demo.timmy-messenger.de"
    }
    customConfig()
}

internal expect fun getDevRootPath(): RootPath?

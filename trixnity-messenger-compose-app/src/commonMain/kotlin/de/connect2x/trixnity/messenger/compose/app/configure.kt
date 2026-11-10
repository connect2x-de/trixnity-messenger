package de.connect2x.trixnity.messenger.compose.app

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.DrawableResourceAppIcon
import de.connect2x.trixnity.messenger.compose.view.composeViewModule
import de.connect2x.trixnity.messenger.compose.view.typography.nunito.addNunitoThemeTypography
import de.connect2x.trixnity.messenger.i18n.platformGetSystemLangModule
import de.connect2x.trixnity.messenger.internal.compose.view.nav3ViewOptIn
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.platformMatrixMessengerSettingsHolderModule
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_app.generated.resources.Res
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_app.generated.resources.status_icon
import de.connect2x.trixnity.messenger.util.RootPath
import org.koin.dsl.module

fun MatrixMultiMessengerConfiguration.configure() {
    appName = BuildConfig.appName
    appId = BuildConfig.appId
    appVersion = BuildConfig.version
    icon = DrawableResourceAppIcon(Res.drawable.status_icon)
    licenses = BuildConfig.licenses
    sendLogsEmailAddress = null

    appUri = "$appId:"

    modulesFactories +=
        listOf(
            { composeViewModule(null) },
            // TODO this needs to be removed and fixed, as there is no MatrixMessengerSettingsHolderImpl at
            // MultiMessenger level!
            ::platformMatrixMessengerSettingsHolderModule,
            // TODO there should be a more clean way for I18n
            ::platformGetSystemLangModule,
        )

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
    messengerConfiguration {
        modulesFactories += { composeViewModule(this) }
        defaultHomeServer = "matrix.dev.connect2x.de"
        //        features.apply {
        //            enableNewSearch = false
        //        }
    }

    addNunitoThemeTypography()

    @OptIn(TrixnityMessengerPrivateApi::class)
    if (BuildConfig.useNav3) {
        nav3ViewOptIn()
    }
}

internal expect fun getDevRootPath(): RootPath?

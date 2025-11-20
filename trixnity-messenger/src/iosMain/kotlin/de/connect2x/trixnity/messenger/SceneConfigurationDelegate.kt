package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.uikit.ApplicationDelegateProtocol
import de.connect2x.trixnity.messenger.uikit.WindowSceneDelegate
import de.connect2x.trixnity.messenger.uikit.WithDefault
import kotlinx.cinterop.BetaInteropApi
import platform.UIKit.UIApplication
import platform.UIKit.UISceneConfiguration
import platform.UIKit.UISceneConnectionOptions
import platform.UIKit.UISceneSession
import platform.UIKit.UISceneSessionRole
import platform.UIKit.UIWindowScene

object SceneConfigurationDelegate : ApplicationDelegateProtocol {

    override fun configurationForConnecting(
        application: UIApplication,
        sceneSession: UISceneSession,
        options: UISceneConnectionOptions,
    ): WithDefault<UISceneConfiguration> = WithDefault.Value(makeConfig(sceneSession.role))

    @OptIn(BetaInteropApi::class)
    private fun makeConfig(role: UISceneSessionRole) = UISceneConfiguration(
        name = null,
        sessionRole = role,
    ).apply {
        delegateClass = WindowSceneDelegate
        sceneClass = UIWindowScene
    }
}

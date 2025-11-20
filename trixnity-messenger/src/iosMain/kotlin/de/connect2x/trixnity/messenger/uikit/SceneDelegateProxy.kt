package de.connect2x.trixnity.messenger.uikit

import platform.Foundation.NSError
import platform.Foundation.NSUserActivity
import platform.UIKit.UIOpenURLContext
import platform.UIKit.UIScene
import platform.UIKit.UISceneConnectionOptions
import platform.UIKit.UISceneSession

open class SceneDelegateProxy<T: SceneDelegateProtocol>(
    delegates: List<T>
) : Delegator<T>(delegates), SceneDelegateProtocol {

    override fun willConnect(
        scene: UIScene,
        session: UISceneSession,
        connectionOptions: UISceneConnectionOptions,
    ) = delegate {
        willConnect(
            scene = scene,
            session = session,
            connectionOptions = connectionOptions,
        )
    }

    override fun sceneDidDisconnect(
        scene: UIScene,
    ) = delegate {
        sceneDidDisconnect(
            scene = scene,
        )
    }

    override fun sceneWillEnterForeground(
        scene: UIScene,
    ) = delegate {
        sceneWillEnterForeground(
            scene = scene,
        )
    }

    override fun sceneDidBecomeActive(
        scene: UIScene,
    ) = delegate {
        sceneDidBecomeActive(
            scene = scene,
        )
    }

    override fun sceneWillResignActive(
        scene: UIScene,
    ) = delegate {
        sceneWillResignActive(
            scene = scene,
        )
    }

    override fun sceneDidEnterBackground(
        scene: UIScene,
    ) = delegate {
        sceneDidEnterBackground(
            scene = scene,
        )
    }

    override fun openUrlContexts(
        scene: UIScene,
        openUrlContexts: Set<UIOpenURLContext>,
    ) = delegate {
        openUrlContexts(
            scene = scene,
            openUrlContexts = openUrlContexts,
        )
    }

    override fun willContinueUserActivity(
        scene: UIScene,
        userActivityType: String,
    ) = delegate {
        willContinueUserActivity(
            scene = scene,
            userActivityType = userActivityType,
        )
    }

    override fun continueUserActivity(
        scene: UIScene,
        userActivity: NSUserActivity,
    ) = delegate {
        continueUserActivity(
            scene = scene,
            userActivity = userActivity,
        )
    }

    override fun didFailToContinueUserActivity(
        scene: UIScene,
        userActivityType: String,
        error: NSError,
    ) = delegate {
        didFailToContinueUserActivity(
            scene = scene,
            userActivityType = userActivityType,
            error = error,
        )
    }

    override fun stateRestorationActivity(
        scene: UIScene,
    ): WithDefault<NSUserActivity?> = delegateDefault {
        stateRestorationActivity(
            scene = scene,
        )
    }

    override fun restoreInteractionState(
        scene: UIScene,
        stateRestorationActivity: NSUserActivity,
    ) = delegate {
        restoreInteractionState(
            scene = scene,
            stateRestorationActivity = stateRestorationActivity,
        )
    }

    override fun didUpdate(
        scene: UIScene,
        userActivity: NSUserActivity,
    ) = delegate {
        didUpdate(
            scene = scene,
            userActivity = userActivity,
        )
    }

}

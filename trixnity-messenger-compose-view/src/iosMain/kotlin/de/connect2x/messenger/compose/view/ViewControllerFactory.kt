package de.connect2x.messenger.compose.view

import com.arkivanov.essenty.lifecycle.Lifecycle
import platform.UIKit.UIViewController

fun interface ViewControllerFactory {
    operator fun invoke(lifecycle: Lifecycle): UIViewController
}

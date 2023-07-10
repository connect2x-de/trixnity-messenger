package de.connect2x.trixnity.messenger

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry

val lifecycle = LifecycleRegistry()
val iosDefaultComponentContext = DefaultComponentContext(lifecycle)
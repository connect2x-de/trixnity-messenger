package de.connect2x.trixnity.messenger

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry

suspend fun createMatrixMessenger(
    componentContext: ComponentContext = DefaultComponentContext(LifecycleRegistry()),
    configuration: MatrixMessengerConfiguration.() -> Unit = {},
): MatrixMessenger = MatrixMessenger.create(componentContext, configuration)
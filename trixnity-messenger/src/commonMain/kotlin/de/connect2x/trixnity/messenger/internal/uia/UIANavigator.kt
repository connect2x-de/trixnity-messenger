package de.connect2x.trixnity.messenger.internal.uia

internal interface UIANavigator {
    suspend fun navigate(destination: UIADestination)
}

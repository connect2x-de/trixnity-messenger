@file:OptIn(ExperimentalMultiplatform::class)

package de.connect2x.trixnity.messenger.internal.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi @Immutable interface Route : NavKey

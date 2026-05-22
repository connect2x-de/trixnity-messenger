@file:OptIn(ExperimentalCoroutinesApi::class)

package de.connect2x.trixnity.messenger.integrationtests.messenger

import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.createRoot
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

class MatrixMessengerWithRoot(delegate: MatrixMessenger, val root: RootViewModel = delegate.createRoot()) :
    MatrixMessenger by delegate

package de.connect2x.trixnity.messenger.compose.view.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.compose.view.connecting.ConnectingWizard
import de.connect2x.trixnity.messenger.compose.view.connecting.MatrixClientInitialization
import de.connect2x.trixnity.messenger.compose.view.connecting.MatrixClientInitializationFailure
import de.connect2x.trixnity.messenger.compose.view.connecting.RemoveMatrixAccount
import de.connect2x.trixnity.messenger.viewmodel.RootRouter

@Composable
fun RootSwitch(stack: Value<ChildStack<*, RootRouter.Wrapper>>) {
    Children(
        stack = stack,
        animation = stackAnimation(fade()),
    ) {
        when (val child = it.instance) {
            is RootRouter.Wrapper.None -> Box {}
            is RootRouter.Wrapper.MatrixClientInitialization -> MatrixClientInitialization(child.viewModel)
            is RootRouter.Wrapper.MatrixClientInitializationFailure -> MatrixClientInitializationFailure(child.viewModel)
            is RootRouter.Wrapper.Main -> Main(child.viewModel)
            is RootRouter.Wrapper.AddMatrixAccount -> ConnectingWizard(child.viewModel)
            is RootRouter.Wrapper.OAuth2Login -> ConnectingWizard(child.viewModel)
            is RootRouter.Wrapper.RegisterMatrixAccount -> ConnectingWizard(child.viewModel)
            is RootRouter.Wrapper.RemoveMatrixAccount -> RemoveMatrixAccount(child.viewModel)
            is RootRouter.Wrapper.PasswordLogin -> ConnectingWizard(child.viewModel)
            is RootRouter.Wrapper.SSOLogin -> ConnectingWizard(child.viewModel)
        }
    }
}

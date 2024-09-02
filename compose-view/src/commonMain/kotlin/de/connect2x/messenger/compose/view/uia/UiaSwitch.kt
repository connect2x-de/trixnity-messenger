package de.connect2x.messenger.compose.view.uia

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaRouter

@Composable
fun UiaSwitch(stack: Value<ChildStack<*, UiaRouter.Wrapper>>) =
    Children(
        stack = stack,
        animation = stackAnimation(fade()),
    ) {
        when (val child = it.instance) {
            is UiaRouter.Wrapper.None -> Box {}
            is UiaRouter.Wrapper.UiaActionConfirmation -> UiaActionConfirmation(child.viewModel)
            is UiaRouter.Wrapper.UiaStepPassword -> UiaPasswordInput(child.viewModel)
            is UiaRouter.Wrapper.UiaStepFallback -> UiaFallbackFlow(child.viewModel)
            is UiaRouter.Wrapper.UiaStepRegistrationToken -> UiaRegistrationToken(child.viewModel)
            is UiaRouter.Wrapper.UiaStepDummy -> UiaDummyStep(child.viewModel)
        }.let {}
    }

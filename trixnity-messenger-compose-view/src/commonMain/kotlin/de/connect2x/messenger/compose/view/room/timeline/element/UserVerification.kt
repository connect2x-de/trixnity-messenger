package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.verification.AcceptSasStart
import de.connect2x.messenger.compose.view.verification.CompareEmojisOrNumbers
import de.connect2x.messenger.compose.view.verification.DeviceVerificationWaitForOther
import de.connect2x.messenger.compose.view.verification.SelectVerificationMethod
import de.connect2x.messenger.compose.view.verification.VerificationCancelled
import de.connect2x.messenger.compose.view.verification.VerificationRejected
import de.connect2x.messenger.compose.view.verification.VerificationTimeout
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.UserVerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepRequestViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel

interface UserVerificationView {
    @Composable
    fun create(userVerificationViewModel: UserVerificationViewModel    )
}

@Composable
fun UserVerification(userVerificationViewModel: UserVerificationViewModel) {
    DI.get<UserVerificationView>().create(userVerificationViewModel)
}

class UserVerificationViewImpl : UserVerificationView {
    @Composable
    override fun create(userVerificationViewModel: UserVerificationViewModel) {
        val i18n = DI.get<I18nView>()
        val sender = userVerificationViewModel.sender.collectAsState().value
        val isActive = userVerificationViewModel.isActive.collectAsState().value
        val reachedEndState = userVerificationViewModel.reachedEndState.collectAsState().value

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = min(maxWidth - 50.dp, 800.dp)
            Box(
                Modifier
                    .padding(horizontal = (maxWidth - width) / 2, vertical = 10.dp)
                    .align(Alignment.Center)
            ) {
                ProvideTextStyle(TextStyle(fontSize = 12.sp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Shield, "")
                                Spacer(Modifier.size(10.dp))
                                Text(
                                    text = i18n.userVerificationStarted(sender.name),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.0f, fill = true).padding(end = 10.dp)
                                )
                                if (isActive) {
                                    IconButton(
                                        onClick = userVerificationViewModel::cancel,
                                        modifier = Modifier.buttonPointerModifier()
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            i18n.commonCancel(),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                } else {
                                    Icon(Icons.Default.SportsScore, i18n.userVerificationDone())
                                }
                            }

                            if (isActive) {
                                Children(stack = userVerificationViewModel.stack) {
                                    when (val child = it.instance) {
                                        is VerificationRouter.Wrapper.Verification -> UserVerificationStepSwitch(
                                            child.viewModel
                                        )

                                        is VerificationRouter.Wrapper.None -> Box {}
                                    }
                                }
                            } else {
                                if (reachedEndState != null) {
                                    Spacer(Modifier.size(20.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        when (reachedEndState.first) {
                                            true -> Icon(
                                                Icons.Default.CheckCircle,
                                                i18n.userVerificationSuccess(),
                                            )

                                            false -> Icon(
                                                Icons.Default.Cancel,
                                                i18n.userVerificationNotSuccessful(),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Spacer(Modifier.size(10.dp))
                                        Text(text = reachedEndState.second)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserVerificationStepSwitch(viewModel: VerificationViewModel) {
    Column {
        Children(
            stack = viewModel.stack,
            animation = stackAnimation(fade())
        ) {
            Spacer(Modifier.size(20.dp))
            Box {
                when (val child = it.instance) {
                    is VerificationViewModel.Wrapper.Request -> UserVerificationRequest(child.viewModel)
                    is VerificationViewModel.Wrapper.Wait -> DeviceVerificationWaitForOther(viewModel::cancel)
                    is VerificationViewModel.Wrapper.SelectVerificationMethod -> SelectVerificationMethod(child.viewModel)
                    is VerificationViewModel.Wrapper.AcceptSasStart -> AcceptSasStart(child.viewModel)
                    is VerificationViewModel.Wrapper.CompareEmojisOrNumbers ->
                        CompareEmojisOrNumbers(child.viewModel)

                    is VerificationViewModel.Wrapper.Success -> UserVerificationSuccess()
                    is VerificationViewModel.Wrapper.Rejected -> VerificationRejected(child.viewModel)
                    is VerificationViewModel.Wrapper.Timeout -> VerificationTimeout(child.viewModel)
                    is VerificationViewModel.Wrapper.Cancelled -> VerificationCancelled(child.viewModel)
                    is VerificationViewModel.Wrapper.AcceptedByOtherClient -> VerificationAcceptedByOtherClient()
                    is VerificationViewModel.Wrapper.None -> Box {}
                }.let {}
            }
        }
    }
}

@Composable
fun UserVerificationRequest(verificationStepRequestViewModel: VerificationStepRequestViewModel) {
    val i18n = DI.get<I18nView>()
    val theirDisplayName =
        verificationStepRequestViewModel.theirDisplayName.collectAsState().value ?: i18n.commonUnknown()

    Column {
        Text(i18n.userVerificationRequest(theirDisplayName))
        Spacer(Modifier.size(20.dp))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1.0f, fill = true))
            Button(verificationStepRequestViewModel::next, Modifier.buttonPointerModifier()) {
                Text(i18n.commonNext().capitalize(Locale.current))
            }
        }
    }
}

@Composable
fun UserVerificationSuccess() {
    val i18n = DI.get<I18nView>()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(i18n.userVerificationSuccessMessage())
        Icon(Icons.Default.CheckCircle, i18n.userVerificationSuccess())
    }
}

@Composable
fun VerificationAcceptedByOtherClient() {
    val i18n = DI.get<I18nView>()
    Text(i18n.userVerificationOtherDevice())
}

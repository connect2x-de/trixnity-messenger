package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import de.connect2x.messenger.compose.view.common.CloseModalButton
import de.connect2x.messenger.compose.view.common.MessengerModalButtonRow
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.verification.CompareEmojisOrNumbersContent
import de.connect2x.messenger.compose.view.verification.SelectVerificationMethodContent
import de.connect2x.messenger.compose.view.verification.VerificationCancelledContent
import de.connect2x.messenger.compose.view.verification.VerificationRejectedContent
import de.connect2x.messenger.compose.view.verification.VerificationTimeoutContent
import de.connect2x.messenger.compose.view.verification.VerificationWaitForOtherContent
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.UserVerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.AcceptSasStartViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.SelectVerificationMethodViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepCancelledViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepCompareViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepRejectedViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepRequestViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepTimeoutViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel

interface UserVerificationView {
    @Composable
    fun create(userVerificationViewModel: UserVerificationViewModel)
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
                    is VerificationViewModel.Wrapper.Wait -> UserVerificationWaitForOther(viewModel::cancel)
                    is VerificationViewModel.Wrapper.SelectVerificationMethod -> UserVerificationSelectVerificationMethod(
                        child.viewModel
                    )

                    is VerificationViewModel.Wrapper.AcceptSasStart -> UserVerificationAcceptSasStart(child.viewModel)
                    is VerificationViewModel.Wrapper.CompareEmojisOrNumbers ->
                        UserVerificationCompareEmojisOrNumbers(child.viewModel)

                    is VerificationViewModel.Wrapper.Success -> UserVerificationSuccess()
                    is VerificationViewModel.Wrapper.Rejected -> UserVerificationRejected(child.viewModel)
                    is VerificationViewModel.Wrapper.Timeout -> UserVerificationTimeout(child.viewModel)
                    is VerificationViewModel.Wrapper.Cancelled -> UserVerificationCancelled(child.viewModel)
                    is VerificationViewModel.Wrapper.AcceptedByOtherClient -> UserVerificationAcceptedByOtherClient()
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
fun UserVerificationWaitForOther(cancelAction: (() -> Unit)? = null) {
    val i18n = DI.get<I18nView>()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        VerificationWaitForOtherContent()
        cancelAction?.let {
            MessengerModalButtonRow(
                {
                    VerificationWaitForOtherContent()
                    CloseModalButton(
                        caption = i18n.commonCancel(),
                        closeModalAction = cancelAction,
                    )
                }
            )
        }
    }
}

@Composable
fun UserVerificationSelectVerificationMethod(selectVerificationMethodViewModel: SelectVerificationMethodViewModel) {
    val verificationMethods = selectVerificationMethodViewModel.verificationMethods
    val selectedVerificationMethod =
        remember { mutableStateOf(verificationMethods.firstOrNull()?.first) }
    Column {
        SelectVerificationMethodContent(selectVerificationMethodViewModel, selectedVerificationMethod)
        Spacer(Modifier.size(20.dp))
        OkButton { selectedVerificationMethod.value?.let { selectVerificationMethodViewModel.acceptVerificationMethod(it) } }
    }
}


@Composable
fun UserVerificationAcceptSasStart(acceptSasStartViewModel: AcceptSasStartViewModel) {
    val i18n = DI.get<I18nView>()
    Column {
        Text(i18n.verificationStartEmoji())
        Spacer(Modifier.size(20.dp))
        OkButton(acceptSasStartViewModel::accept)
    }

}

@Composable
fun BoxScope.UserVerificationCompareEmojisOrNumbers(verificationStepCompareViewModel: VerificationStepCompareViewModel) {
    val i18n = DI.current.get<I18nView>()
    Column(Modifier.fillMaxWidth().align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            CompareEmojisOrNumbersContent(verificationStepCompareViewModel)
        }
        Spacer(Modifier.size(20.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                verificationStepCompareViewModel::decline,
                Modifier.buttonPointerModifier().weight(1.0f, fill = false),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(i18n.verificationNotMatch(), color = Color.White)
            }
            Spacer(Modifier.size(20.dp))
            Button(
                verificationStepCompareViewModel::accept,
                Modifier.buttonPointerModifier().weight(1.0f, fill = false)
            ) {
                Text(i18n.verificationMatch())
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
fun UserVerificationRejected(
    verificationStepRejectedViewModel: VerificationStepRejectedViewModel,

    ) {
    Column {
        VerificationRejectedContent(false)
        Spacer(Modifier.size(20.dp))
        OkButton(verificationStepRejectedViewModel::ok)
    }
}

@Composable
fun UserVerificationTimeout(
    verificationStepTimeoutViewModel: VerificationStepTimeoutViewModel,
) {
    Column {
        VerificationTimeoutContent(false)
        Spacer(Modifier.size(20.dp))
        OkButton(verificationStepTimeoutViewModel::ok)
    }
}

@Composable
fun UserVerificationCancelled(
    verificationStepCancelledViewModel: VerificationStepCancelledViewModel,
) {
    Column {
        VerificationCancelledContent(false)
        Spacer(Modifier.size(20.dp))
        OkButton(verificationStepCancelledViewModel::ok)
    }
}

@Composable
fun UserVerificationAcceptedByOtherClient() {
    val i18n = DI.get<I18nView>()
    Text(i18n.userVerificationOtherDevice())
}

@Composable
private fun OkButton(onClick: () -> Unit) {
    val i18n = DI.get<I18nView>()
    Row(Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1.0f, fill = true))
        Button(onClick, Modifier.buttonPointerModifier()) {
            Text(i18n.commonOk())
        }
    }
}

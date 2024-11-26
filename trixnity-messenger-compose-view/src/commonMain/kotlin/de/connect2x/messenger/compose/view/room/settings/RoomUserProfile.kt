package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.ErrorDialog
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.ADMIN
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.MODERATOR
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.USER
import de.connect2x.trixnity.messenger.viewmodel.room.settings.UserProfileViewModel
import net.folivo.trixnity.core.model.events.m.room.Membership

@Composable
fun RoomUserProfileContainer(memberListElementViewModel: UserProfileViewModel) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            RoomUserProfile(memberListElementViewModel)
        }
    }
}

interface RoomUserProfileView {
    @Composable
    fun create(memberListElementViewModel: UserProfileViewModel)
}

@Composable
fun RoomUserProfile(memberListElementViewModel: UserProfileViewModel) {
    DI.get<RoomUserProfileView>().create(memberListElementViewModel)
}

class RoomUserProfileViewImpl : RoomUserProfileView {
    @Composable
    override fun create(userProfileViewModel: UserProfileViewModel) {
        val error = userProfileViewModel.error.collectAsState()
        val i18n = DI.get<I18nView>()
        val member = userProfileViewModel.member.collectAsState().value
        val userId = userProfileViewModel.userId
        val iHavePowerToBanUser = userProfileViewModel.iHavePowerToBanUser.collectAsState().value
        val iHavePowerToUnbanUser = userProfileViewModel.iHavePowerToUnbanUser.collectAsState().value
        val iHavePowerToKickUser = userProfileViewModel.iHavePowerToKickUser.collectAsState().value
        val maxPowerLevel = userProfileViewModel.changePowerLevelViewModel.canSetPowerLevelToMax.collectAsState().value
        val blockingInProgress = userProfileViewModel.blockingInProgress.collectAsState().value
        val isUserBlocked = userProfileViewModel.isUserBlocked.collectAsState().value
        val membership = userProfileViewModel.membership.collectAsState().value
        val canSetRoleToAdmin =
            userProfileViewModel.changePowerLevelViewModel.canSetRoleToAdmin.collectAsState().value
        val canSetRoleToModerator =
            userProfileViewModel.changePowerLevelViewModel.canSetRoleToModerator.collectAsState().value
        val canSetRoleToUser =
            userProfileViewModel.changePowerLevelViewModel.canSetRoleToUser.collectAsState().value

        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Column {
                    Header(userProfileViewModel::back, "Profil")
                    if (error.value != null) {
                        ErrorDialog(error.value.orEmpty(), { userProfileViewModel.errorDismiss() }, errorCause = error.value)
                    }
                    error.value?.let {
                        Text("Error: $it")
                    }
                    Box(Modifier.width(100.dp).height(100.dp))
                    Text(member?.displayName ?: userId.full)
                    member?.displayName?.let {
                        Text(userId.full)
                    }

                    HorizontalDivider(Modifier.fillMaxWidth())
                    if (!blockingInProgress) {
                        if (isUserBlocked) {
                            Button(onClick = {
                                userProfileViewModel.unblockUser()
                            }) {
                                Text("unblock")
                            }
                        } else {
                            Button(onClick = {
                                userProfileViewModel.blockUser()
                            }) {
                                Text("block")
                            }
                        }
                    }
                    Button(onClick = {
                        TODO("Jap")
                    }) {
                        Text("contact")
                    }
                    Button(onClick = {
                        TODO("Jap")
                    }) {
                        Text("verify session")
                    }
                    HorizontalDivider(Modifier.fillMaxWidth())
                    if (iHavePowerToBanUser) {
                        Button(onClick = {
                            userProfileViewModel.openBanUserWarning()
                        }) {
                            Text("ban")
                        }
                    }
                    if (membership == Membership.BAN && iHavePowerToUnbanUser) {
                        Button(onClick = {
                            userProfileViewModel.openUnbanUserWarning()
                        }) {
                            Text("unban")
                        }
                    }
                    if (iHavePowerToKickUser) {
                        Button(onClick = {
                            userProfileViewModel.openKickUserWarning()
                        }) {
                            Text("kick")
                        }
                    }
                    HorizontalDivider(Modifier.fillMaxWidth())
                    if (canSetRoleToAdmin) {
                        Button(onClick = {
                            userProfileViewModel.changePowerLevelViewModel.openChangingRoleWarningDialog(ADMIN)
                        }) {
                            Text("Make Admin")
                        }
                    }
                    if (canSetRoleToModerator) {
                        Button(onClick = {
                            userProfileViewModel.changePowerLevelViewModel.openChangingRoleWarningDialog(MODERATOR)
                        }) {
                            Text("Make Mod")
                        }
                    }
                    if (canSetRoleToUser) {
                    Button(onClick = {
                        userProfileViewModel.changePowerLevelViewModel.openChangingRoleWarningDialog(USER)
                    }) {
                        Text("Make User")
                    }
                }
                    if (maxPowerLevel != null && maxPowerLevel != 0L) {
                        Button(onClick = {
                            userProfileViewModel.changePowerLevelViewModel.openChangingPowerLevelDialog()
                        }) {
                            Text("set specific powerlevel")
                        }
                    }
                }
            }
        }
    }
}

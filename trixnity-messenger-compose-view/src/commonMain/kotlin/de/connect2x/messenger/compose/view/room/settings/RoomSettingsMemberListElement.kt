package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.AvatarWithPresence
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.MessengerDialog
import de.connect2x.messenger.compose.view.common.UserState
import de.connect2x.messenger.compose.view.common.WarningDialog
import de.connect2x.messenger.compose.view.common.collectAsStateForTextField
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.ADMIN
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.MODERATOR
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel.Role.USER
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListViewModel
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.Membership

interface RoomSettingsMemberListElementView {
    @Composable
    fun create(
        memberListViewModel: MemberListViewModel,
        memberUserId: UserId,
        memberListElementViewModel: MemberListElementViewModel,
        clickedUser: MutableState<MemberListElementViewModel.MemberElement?>,
    )
}

@Composable
fun RoomSettingsMemberListElement(
    memberListViewModel: MemberListViewModel,
    memberUserId: UserId,
    memberListElementViewModel: MemberListElementViewModel,
    clickedUser: MutableState<MemberListElementViewModel.MemberElement?>,
) {
    DI.get<RoomSettingsMemberListElementView>()
        .create(memberListViewModel, memberUserId, memberListElementViewModel, clickedUser)
}

class RoomSettingsMemberListElementViewImpl : RoomSettingsMemberListElementView {
    @Composable
    override fun create(
        memberListViewModel: MemberListViewModel,
        memberUserId: UserId,
        memberListElementViewModel: MemberListElementViewModel,
        clickedUser: MutableState<MemberListElementViewModel.MemberElement?>,
    ) {
        val i18n = DI.get<I18nView>()
        val memberElement = memberListElementViewModel.member.collectAsState().value
        val memberOptionsOpen = memberListElementViewModel.memberOptionsOpen.collectAsState().value
        val role = memberListElementViewModel.role.collectAsState().value
        val powerLevel = memberListElementViewModel.powerLevel.collectAsState().value
        val showPowerLevel = memberListElementViewModel.showPowerLevel.collectAsState().value
        val showRole = memberListElementViewModel.showRole.collectAsState().value
        val isLastMember =
            memberListViewModel.memberListElementViewModels.collectAsState().value.lastOrNull()?.first == memberListElementViewModel.userId
        val presence = memberListElementViewModel.presence.collectAsState().value
        val membership = memberListElementViewModel.membership.collectAsState().value
        val membershipReason = memberListElementViewModel.membershipReason.collectAsState().value
        var bannedMemberReasonOpen by remember { mutableStateOf(false) }
        Box(
            Modifier
                .fillMaxWidth()
                .clickable {
                    clickedUser.value = memberElement; memberListElementViewModel.openMemberOptions()
                }
                .buttonPointerModifier(),
        ) {
            Column {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (memberElement == null) LoadingSpinner()
                    else {
                        AvatarWithPresence(memberElement.image, memberElement.initials, presence)
                        Spacer(Modifier.size(5.dp))
                        UserState(
                            memberListElementViewModel.userTrustLevel,
                            memberListElementViewModel.isUserBlocked,
                            memberListElementViewModel.membership,
                            memberListElementViewModel.iHavePowerToUnbanUser,
                        )
                        Spacer(Modifier.size(5.dp))
                        Text(
                            modifier = Modifier.fillMaxWidth().weight(1.0f, false),
                            text = memberElement.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (showRole || showPowerLevel) {
                            Text(
                                text = getRoomSettingsMemberRoleName(role, i18n)
                                        + if (showPowerLevel) " ($powerLevel)" else "",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                        if (!membershipReason.isNullOrBlank() && membership == Membership.BAN) {
                            Icon(
                                if (bannedMemberReasonOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                if (bannedMemberReasonOpen) i18n.commonCollapse() else i18n.commonExpand(),
                                Modifier.clickable { bannedMemberReasonOpen = !bannedMemberReasonOpen }
                            )
                        }
                    }
                }
                if (memberElement != null && memberOptionsOpen && memberElement == clickedUser.value) {
                    RoomSettingsMemberOptions(memberListElementViewModel, memberUserId, clickedUser)
                }
                if (bannedMemberReasonOpen) {
                    Column(
                        Modifier
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .clickable {
                                bannedMemberReasonOpen = false
                            }
                    ) {
                        membershipReason?.let {
                            Text(it)
                        }
                    }
                }
                if (isLastMember.not()) {
                    HorizontalDivider(Modifier.fillMaxWidth().width(1.dp).padding(horizontal = 10.dp))
                }
            }
        }
    }
}

@Composable
fun RoomSettingsMemberOptions(
    memberListElementViewModel: MemberListElementViewModel,
    userId: UserId,
    clickedUser: MutableState<MemberListElementViewModel.MemberElement?>
) {
    val i18n = DI.get<I18nView>()
    val iHavePowerToKickUser =
        memberListElementViewModel.iHavePowerToKickUser.collectAsState().value
    val iHavePowerToBanUser = memberListElementViewModel.iHavePowerToBanUser.collectAsState().value
    val iHavePowerToUnbanUer = memberListElementViewModel.iHavePowerToUnbanUser.collectAsState().value
    val canSetRoleToAdmin =
        memberListElementViewModel.changePowerLevelViewModel.canSetRoleToAdmin.collectAsState().value
    val canSetRoleToModerator =
        memberListElementViewModel.changePowerLevelViewModel.canSetRoleToModerator.collectAsState().value
    val canSetRoleToUser =
        memberListElementViewModel.changePowerLevelViewModel.canSetRoleToUser.collectAsState().value
    val changingRoleWarningOpen =
        memberListElementViewModel.changePowerLevelViewModel.changingRoleWarningDialogOpen.collectAsState().value
    val changingPowerLevelOpen =
        memberListElementViewModel.changePowerLevelViewModel.changingPowerLevelDialogOpen.collectAsState().value
    val maxPowerLevel =
        memberListElementViewModel.changePowerLevelViewModel.canSetPowerLevelToMax.collectAsState().value
    val kickUserWarningOpen =
        memberListElementViewModel.kickUserWarningOpen.collectAsState().value
    val banUserWarningOpen =
        memberListElementViewModel.banUserWarningOpen.collectAsState().value
    val unbanUserWarningOpen =
        memberListElementViewModel.unbanUserWarningOpen.collectAsState().value
    val isUserBlocked = memberListElementViewModel.isUserBlocked.collectAsState().value
    val blockingInProgress = memberListElementViewModel.blockingInProgress.collectAsState().value
    val membership = memberListElementViewModel.membership.collectAsState().value

    DropdownMenu(
        expanded = true,
        onDismissRequest = {
            clickedUser.value = null; memberListElementViewModel.closeMemberOptions()
        },
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    ) {
        if (membership == Membership.BAN) {
            if (iHavePowerToUnbanUer) {
                DropdownMenuItem(
                    text = {
                        Text(
                            i18n.memberListUnbanUser(),
                            Modifier.buttonPointerModifier(),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    onClick = { memberListElementViewModel.openUnbanUserWarning() },
                    contentPadding = PaddingValues(horizontal = 10.dp),
                )
            }

            return@DropdownMenu
        }
        if (canSetRoleToAdmin) {
            DropdownMenuItem(
                text = {
                    Text(
                        i18n.memberListChangeTo(i18n.memberListRoleAdministrator()),
                        Modifier.buttonPointerModifier(),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                onClick = {
                    memberListElementViewModel.changePowerLevelViewModel.openChangingRoleWarningDialog(
                        ADMIN
                    )
                },
                contentPadding = PaddingValues(horizontal = 10.dp),
            )
        }
        if (canSetRoleToModerator) {
            DropdownMenuItem(
                text = {
                    Text(
                        i18n.memberListChangeTo(i18n.memberListRoleModerator()),
                        Modifier.buttonPointerModifier(),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                onClick = {
                    memberListElementViewModel.changePowerLevelViewModel.openChangingRoleWarningDialog(
                        MODERATOR
                    )
                },
                contentPadding = PaddingValues(horizontal = 10.dp),
            )
        }
        if (canSetRoleToUser) {
            DropdownMenuItem(
                text = {
                    Text(
                        i18n.memberListChangeTo(i18n.memberListRoleUser()),
                        Modifier.buttonPointerModifier(),
                        MaterialTheme.colorScheme.onBackground,
                    )
                },
                onClick = {
                    memberListElementViewModel.changePowerLevelViewModel.openChangingRoleWarningDialog(
                        USER
                    )
                },
                contentPadding = PaddingValues(horizontal = 10.dp),
            )
        }
        if (maxPowerLevel != null && maxPowerLevel != 0L) {
            DropdownMenuItem(
                text = {
                    Text(
                        i18n.memberListChangePowerLevel(),
                        Modifier.buttonPointerModifier(),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                onClick = {
                    memberListElementViewModel.changePowerLevelViewModel.openChangingPowerLevelDialog()
                },
                contentPadding = PaddingValues(horizontal = 10.dp),
            )
        }
        if (iHavePowerToKickUser) {
            DropdownMenuItem(
                text = {
                    Text(
                        i18n.memberListRemoveUser(),
                        Modifier.buttonPointerModifier(),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                onClick = { memberListElementViewModel.openKickUserWarning() },
                contentPadding = PaddingValues(horizontal = 10.dp),
            )
        }
        if (iHavePowerToBanUser) {
            DropdownMenuItem(
                text = {
                    Text(
                        i18n.memberListBanUser(),
                        Modifier.buttonPointerModifier(),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                onClick = { memberListElementViewModel.openBanUserWarning() },
                contentPadding = PaddingValues(horizontal = 10.dp),
            )
        }
        if (blockingInProgress.not()) {
            if (isUserBlocked) {
                DropdownMenuItem(
                    text = {
                        Text(
                            i18n.roomHeaderUnblockUser(),
                            Modifier.buttonPointerModifier(),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    onClick = { memberListElementViewModel.unblockUser() },
                    contentPadding = PaddingValues(horizontal = 10.dp),
                )
            } else {
                DropdownMenuItem(
                    text = {
                        Text(
                            i18n.roomHeaderBlockUser(),
                            Modifier.buttonPointerModifier(),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    onClick = { memberListElementViewModel.blockUser() },
                    contentPadding = PaddingValues(horizontal = 10.dp),
                )
            }
        }
    }
    if (kickUserWarningOpen) RoomSettingsMemberKickUserWarning(memberListElementViewModel, userId)
    if (banUserWarningOpen) RoomSettingsMemberBanUserWarning(memberListElementViewModel)
    if (unbanUserWarningOpen) RoomSettingsMemberUnbanUserWarning(memberListElementViewModel)
    changingRoleWarningOpen?.let { RoomSettingsMemberChangingRoleWarning(memberListElementViewModel, it) }
    if (changingPowerLevelOpen) RoomSettingsMemberChangingPowerLevel(memberListElementViewModel)
}

@Composable
fun RoomSettingsMemberKickUserWarning(
    memberListElementViewModel: MemberListElementViewModel,
    userId: UserId
) {
    val i18n = DI.get<I18nView>()
    val kickUserWarningMessage =
        memberListElementViewModel.kickUserWarningMessage.collectAsState().value
    val kickUserWarningTitle =
        memberListElementViewModel.kickUserWarningTitle.collectAsState().value
    WarningDialog(
        title = kickUserWarningTitle,
        message = { Text(kickUserWarningMessage) },
        dismissButtonText = i18n.commonCancel().capitalize(Locale.current),
        confirmButtonText = i18n.memberListRemoveUserConfirmation(),
        dismissAction = { memberListElementViewModel.closeKickUserWarning() },
        confirmAction = {
            memberListElementViewModel.closeKickUserWarning()
            memberListElementViewModel.closeMemberOptions()
            memberListElementViewModel.kickUser(userId)
        }
    )
}

@Composable
fun RoomSettingsMemberBanUserWarning(
    memberListElementViewModel: MemberListElementViewModel,
) {
    val i18n = DI.current.get<I18nView>()
    var reason by remember { mutableStateOf("") }

    WarningDialog(
        title = i18n.memberListBanTitle(),
        message = {
            OutlinedTextField(
                value = reason,
                onValueChange = {
                    reason = it
                },
                label = {
                    Text(i18n.commonOptionalReason())
                },
                maxLines = 5
            )
        },
        dismissButtonText = i18n.commonCancel().capitalize(Locale.current),
        confirmButtonText = i18n.memberListBanUserConfirmation(),
        dismissAction = { memberListElementViewModel.closeBanUserWarning() },
        confirmAction = {
            memberListElementViewModel.closeBanUserWarning()
            memberListElementViewModel.closeMemberOptions()
            memberListElementViewModel.banUser(reason.ifBlank { null })
        })
}

@Composable
fun RoomSettingsMemberUnbanUserWarning(
    memberListElementViewModel: MemberListElementViewModel,
) {
    val i18n = DI.current.get<I18nView>()
    var reason by remember { mutableStateOf("") }

    WarningDialog(
        title = i18n.unbanTitle(),
        message = {
            OutlinedTextField(
                value = reason,
                onValueChange = {
                    reason = it
                },
                label = {
                    Text(i18n.commonOptionalReason())
                },
                maxLines = 5
            )
        },
        dismissButtonText = i18n.commonCancel().capitalize(Locale.current),
        confirmButtonText = i18n.unbanUserConfirmation(),
        dismissAction = { memberListElementViewModel.closeUnbanUserWarning() },
        confirmAction = {
            memberListElementViewModel.closeUnbanUserWarning()
            memberListElementViewModel.unbanUser(reason.ifBlank { null })
        }
    )
}

@Composable
fun RoomSettingsMemberChangingRoleWarning(memberListElementViewModel: MemberListElementViewModel, role: Role) {
    val i18n = DI.get<I18nView>()
    val newRole = getRoomSettingsMemberRoleName(role, i18n)
    val oldRole = getRoomSettingsMemberRoleName(memberListElementViewModel.role.collectAsState().value, i18n)
    val username = memberListElementViewModel.member.collectAsState().value?.displayName ?: i18n.commonUnknown()

    WarningDialog(
        title = i18n.memberListChangeRole(username, oldRole, newRole),
        message = { Text(i18n.memberListChangeRoleWarning()) },
        dismissButtonText = i18n.commonCancel().capitalize(Locale.current),
        confirmButtonText = when (role) {
            ADMIN -> i18n.memberListChangeTo(i18n.memberListRoleAdministrator())
            MODERATOR -> i18n.memberListChangeTo(i18n.memberListRoleModerator())
            USER -> i18n.memberListChangeTo(i18n.memberListRoleUser())
        },
        dismissAction = { memberListElementViewModel.changePowerLevelViewModel.closeChangingRoleWarningDialog() },
        confirmAction = {
            when (role) {
                ADMIN -> memberListElementViewModel.changePowerLevelViewModel.setRoleToAdmin()
                MODERATOR -> memberListElementViewModel.changePowerLevelViewModel.setRoleToModerator()
                USER -> memberListElementViewModel.changePowerLevelViewModel.setRoleToUser()
            }
            memberListElementViewModel.changePowerLevelViewModel.closeChangingRoleWarningDialog()
        }
    )
}


@Composable
fun RoomSettingsMemberChangingPowerLevel(memberListElementViewModel: MemberListElementViewModel) {
    val i18n = DI.get<I18nView>()
    val changePowerLevelInput =
        memberListElementViewModel.changePowerLevelViewModel.changingPowerLevelDialogInput.collectAsStateForTextField().value
    val showPowerLevelHelp =
        memberListElementViewModel.changePowerLevelViewModel.showPowerLevelHelp.collectAsState().value

    MessengerDialog(
        onDismissRequest = { memberListElementViewModel.changePowerLevelViewModel.closeChangingPowerLevelDialog() },
        text = {
            val state = rememberScrollState()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().verticalScroll(state)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1F, false).fillMaxWidth(),
                        text = i18n.memberListChangePowerLevel(),
                        style = MaterialTheme.typography.labelLarge
                    )
                    IconButton(
                        onClick = { if (showPowerLevelHelp) memberListElementViewModel.changePowerLevelViewModel.closePowerLevelHelp() else memberListElementViewModel.changePowerLevelViewModel.openPowerLevelHelp() },
                        Modifier.buttonPointerModifier()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Help, i18n.commonHelp())
                    }
                }
                Spacer(Modifier.size(10.dp))
                OutlinedTextField(
                    value = changePowerLevelInput.value,
                    onValueChange = {
                        memberListElementViewModel.changePowerLevelViewModel.onPowerLevelEntered(
                            it
                        )
                    },
                    isError = changePowerLevelInput.errorId != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )
                Spacer(Modifier.size(5.dp))
                changePowerLevelInput.errorId?.let {
                    Spacer(Modifier.size(5.dp))
                    Text(color = MaterialTheme.colorScheme.error, text = it)
                }
                Spacer(Modifier.size(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        { memberListElementViewModel.changePowerLevelViewModel.closeChangingPowerLevelDialog() },
                        Modifier.buttonPointerModifier().weight(0.4f)
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                    Spacer(Modifier.size(15.dp))
                    Button(
                        enabled = changePowerLevelInput.errorId == null && changePowerLevelInput.value != "",
                        onClick = {
                            memberListElementViewModel.changePowerLevelViewModel.setPowerLevelTo(
                                changePowerLevelInput.value.toLong()
                            )
                            memberListElementViewModel.changePowerLevelViewModel.closeChangingPowerLevelDialog()
                        },
                        modifier = Modifier.buttonPointerModifier().weight(0.4f)
                    ) { Text(text = i18n.memberListChangePowerLevel(), textAlign = TextAlign.Center) }
                }
                if (showPowerLevelHelp) {
                    Column(modifier = Modifier.align(alignment = Alignment.Start)) {
                        Spacer(Modifier.size(15.dp))
                        Text(i18n.memberListNote(), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.size(5.dp))
                        Text(text = i18n.memberListNoteText())
                    }
                }
            }
        }
    )
}

fun getRoomSettingsMemberRoleName(role: Role, i18n: I18nView): String {
    return when (role) {
        ADMIN -> i18n.memberListRoleAdministrator()
        MODERATOR -> i18n.memberListRoleModerator()
        USER -> i18n.memberListRoleUser()
    }
}

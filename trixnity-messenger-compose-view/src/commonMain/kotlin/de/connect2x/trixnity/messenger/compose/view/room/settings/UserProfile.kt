package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Wysiwyg
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.DoorSliding
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import de.connect2x.trixnity.client.user.PowerLevel
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.crypto.key.UserTrustLevel
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.common.CopyToClipboardButton
import de.connect2x.trixnity.messenger.compose.view.common.ErrorView
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.icons.BanIcon
import de.connect2x.trixnity.messenger.compose.view.common.icons.BlockIcon
import de.connect2x.trixnity.messenger.compose.view.common.icons.NeutralVerifiedIcon
import de.connect2x.trixnity.messenger.compose.view.common.icons.NotVerifiedIcon
import de.connect2x.trixnity.messenger.compose.view.common.icons.VerificationLevel
import de.connect2x.trixnity.messenger.compose.view.common.icons.VerifiedIcon
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.root.IsSinglePane
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedInfoChip
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItem
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSelectableText
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSuggestionChip
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.compose.view.util.inputFocusNavigation
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.UserProfileViewModel


@Composable
fun UserProfileContainer(userProfileViewModel: UserProfileViewModel) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            UserProfile(userProfileViewModel)
        }
    }
}

interface UserProfileView {
    @Composable
    fun create(userProfileViewModel: UserProfileViewModel)
}

@Composable
fun UserProfile(userProfileViewModel: UserProfileViewModel) {
    DI.get<UserProfileView>().create(userProfileViewModel)
}

class UserProfileViewImpl : UserProfileView {
    @Composable
    override fun create(userProfileViewModel: UserProfileViewModel) {
        val error = userProfileViewModel.error.collectAsState()
        val i18n = DI.get<I18nView>()
        val userInfoElement = userProfileViewModel.userInfo.collectAsState().value
        val image = userInfoElement?.image?.collectAsState(null)?.value
        val userId = userProfileViewModel.userId

        val membership = userProfileViewModel.membership.collectAsState().value
        val membershipReason = userProfileViewModel.membershipReason.collectAsState().value

        val userTrustLevel = userProfileViewModel.userTrustLevel.collectAsState().value

        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            DialogHandler(userProfileViewModel)
            Column(Modifier.weight(1f)) {
                Header(userProfileViewModel::back, i18n.profileTitle())
                error.value?.let {
                    ErrorView(it)
                }

                Column(
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp),
                    Arrangement.Center,
                    Alignment.CenterHorizontally
                ) {
                    if (userInfoElement != null) {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            Box(Modifier.align(Alignment.Center)) {
                                ThemedUserAvatar(
                                    initials = userInfoElement.initials,
                                    image = image,
                                    presence = null,
                                    size = min(
                                        this@BoxWithConstraints.maxWidth,
                                        this@BoxWithConstraints.maxHeight
                                    ).coerceAtMost(180.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        ThemedSelectableText(
                            userInfoElement.name,
                            MaterialTheme.components.selectionOnSurface,
                            style = MaterialTheme.typography.titleLarge
                        )

                        if (userInfoElement.name != userId.full) {
                            CopyableUserId(userId, MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        CopyableUserId(userId, MaterialTheme.typography.titleLarge)
                    }

                    Spacer(Modifier.height(5.dp))
                    when (userTrustLevel) {
                        is UserTrustLevel.CrossSigned -> {
                            if (userTrustLevel.verified) {
                                ThemedInfoChip(
                                    style = MaterialTheme.components.primaryChip,
                                    icon = { VerifiedIcon(VerificationLevel.USER) },
                                    label = { Text(i18n.userTrustSecureVerified()) },
                                )
                            } else {
                                ThemedInfoChip(
                                    style = MaterialTheme.components.secondaryChip,
                                    icon = { NeutralVerifiedIcon(VerificationLevel.USER) },
                                    label = { Text(i18n.userTrustSecureUnverified()) }
                                )
                            }
                        }

                        is UserTrustLevel.NotAllDevicesCrossSigned -> {
                            ThemedInfoChip(
                                style = MaterialTheme.components.destructiveChip,
                                icon = { NotVerifiedIcon(VerificationLevel.USER) },
                                label = { Text(i18n.userTrustInsecureUnverifiedDevices()) },
                            )
                        }

                        UserTrustLevel.Blocked -> {
                            ThemedInfoChip(
                                style = MaterialTheme.components.destructiveChip,
                                icon = { NotVerifiedIcon(VerificationLevel.USER) },
                                label = { Text(i18n.userTrustInsecureBlocked()) },
                            )
                        }

                        is UserTrustLevel.Invalid -> {
                            ThemedInfoChip(
                                style = MaterialTheme.components.destructiveChip,
                                icon = { NotVerifiedIcon(VerificationLevel.USER) },
                                label = { Text(i18n.userTrustInsecureInvalid()) },
                            )
                        }

                        UserTrustLevel.Unknown, null -> {
                            ThemedInfoChip(
                                style = MaterialTheme.components.destructiveChip,
                                icon = { NotVerifiedIcon(VerificationLevel.USER) },
                                label = { Text(i18n.userTrustInsecureUnknown()) },
                            )
                        }
                    }
                    if (membership == Membership.BAN) {
                        ThemedInfoChip(
                            style = MaterialTheme.components.destructiveChip,
                            icon = { BanIcon() },
                            label = { Text(membershipReason ?: i18n.banned()) },
                        )
                    }
                }
            }

            Column {
                SmallSpacer()
                RoomOptions(userProfileViewModel, i18n)
                UserOptions(userProfileViewModel, i18n)
            }
        }
    }
}

@Composable
fun CopyableUserId(userId: UserId, textStyle: TextStyle) {
    val i18n = DI.get<I18nView>()

    @Suppress("DEPRECATION") // TODO: New clipboard API is not usable from common code, fix this eventually..
    val clipboard = LocalClipboardManager.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        ThemedSelectableText(
            userId.full,
            MaterialTheme.components.selectionOnSurface,
            style = textStyle,
            overflow = TextOverflow.Visible
        )
        Spacer(Modifier.size(5.dp))
        CopyToClipboardButton(
            userId.full,
            i18n.userProfileCopyUserId()
        )
    }
}

@Composable
private fun RoomOptions(userProfileViewModel: UserProfileViewModel, i18n: I18nView) {
    val iHavePowerToAcceptKnock by userProfileViewModel.iHavePowerToAcceptKnock.collectAsState()
    val iHavePowerToRejectKnock by userProfileViewModel.iHavePowerToRejectKnock.collectAsState()
    val iHavePowerToKickUser by userProfileViewModel.iHavePowerToKickUser.collectAsState()
    val iHavePowerToBanUser by userProfileViewModel.iHavePowerToBanUser.collectAsState()
    val iHavePowerToUnbanUser by userProfileViewModel.iHavePowerToUnbanUser.collectAsState()
    val canSetPowerLevelToMax by userProfileViewModel.changePowerLevelViewModel.canSetPowerLevelToMax.collectAsState()
    val canSetRoleToAdmin by userProfileViewModel.changePowerLevelViewModel.canSetRoleToAdmin.collectAsState()
    val canSetRoleToModerator by userProfileViewModel.changePowerLevelViewModel.canSetRoleToModerator.collectAsState()
    val canSetRoleToUser by userProfileViewModel.changePowerLevelViewModel.canSetRoleToUser.collectAsState()
    val membership by userProfileViewModel.membership.collectAsState()

    val shouldShowChangePowerLevel = canSetRoleToUser || canSetRoleToModerator || canSetRoleToAdmin ||
            (canSetPowerLevelToMax != null && canSetPowerLevelToMax != 0L)
    val shouldShowBan = iHavePowerToBanUser || (iHavePowerToUnbanUser && membership == Membership.BAN)
    val shouldShowKnockOptions = membership == Membership.KNOCK

    if (shouldShowChangePowerLevel || shouldShowBan || iHavePowerToKickUser) {
        HorizontalDivider(Modifier.fillMaxWidth())

        ThemedListItem(
            headlineContent = {
                Text(i18n.userProfileRoomOptions(), style = MaterialTheme.typography.titleMedium)
            },
            style = MaterialTheme.components.settingsItem,
        )

        if (shouldShowChangePowerLevel) {
            ChangePowerLevelSection(userProfileViewModel, i18n)
        }

        if (!userProfileViewModel.isMyself) {
            if (shouldShowKnockOptions) {
                if (iHavePowerToAcceptKnock) {
                    AcceptKnockSection(userProfileViewModel, i18n)
                }
                if (iHavePowerToRejectKnock) {
                    RejectKnockSection(userProfileViewModel, i18n)
                }
            } else {
                if (iHavePowerToKickUser) {
                    KickUserSection(userProfileViewModel, i18n)
                }
            }

            if (shouldShowBan) {
                BanUserSection(userProfileViewModel, i18n)
            }
        }
    }
}

@Composable
private fun UserOptions(userProfileViewModel: UserProfileViewModel, i18n: I18nView) {
    if (!userProfileViewModel.isMyself) {
        val blockingInProgress = userProfileViewModel.blockingInProgress.collectAsState().value
        val isUserBlocked = userProfileViewModel.isUserBlocked.collectAsState().value
        val openingChat = userProfileViewModel.openingChat.collectAsState().value
        val verificationInThisRoom = userProfileViewModel.verificationIsRunningInThisRoom.collectAsState().value
        val canOpenChat = userProfileViewModel.canOpenChat.collectAsState().value
        val verificationAvailable = userProfileViewModel.canVerifyUser.collectAsState().value
        val verificationIsRunning = userProfileViewModel.verificationIsRunning.collectAsState().value

        ThemedListItem(
            headlineContent = {
                Text(i18n.userProfileUserOptions(), style = MaterialTheme.typography.titleMedium)
            },
            style = MaterialTheme.components.settingsItem,
        )
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            leadingContent = { BlockIcon() },
            headlineContent = { Text(i18n.userProfileBlockUser()) },
            selected = isUserBlocked,
            onChange = {
                if (isUserBlocked) {
                    userProfileViewModel.unblockUser()
                } else {
                    userProfileViewModel.blockUser()
                }
            },
            enabled = !blockingInProgress,
            thumbContent = {
                if (blockingInProgress) {
                    ThemedProgressIndicator(style = MaterialTheme.components.switchProgressIndicator)
                }
            }
        )
        if (canOpenChat) {
            ThemedListItemButton(
                style = MaterialTheme.components.settingsItem,
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        null,
                        tint = defaultColorForState(!openingChat),
                    )
                },
                headlineContent = {
                    Text(
                        text = i18n.userProfileContact(),
                        color = defaultColorForState(!openingChat)
                    )
                },
                onClick = { userProfileViewModel.openChat() },
            )
        }
        val isSinglePane = IsSinglePane.current
        if (verificationAvailable) {
            if (verificationIsRunning && !verificationInThisRoom) {
                Tooltip(
                    enabled = verificationIsRunning,
                    tooltip = { Text(i18n.verificationAlreadyRunningInAnotherRoom()) },
                ) {
                    ThemedListItemButton(
                        style = MaterialTheme.components.settingsItem,
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.Wysiwyg, null) },
                        headlineContent = { Text(i18n.userProfileNavigateToVerification()) },
                        onClick = { userProfileViewModel.openVerificationRoom() },
                    )
                }
            } else {
                Tooltip(
                    enabled = verificationIsRunning,
                    tooltip = { Text(i18n.verificationAlreadyRunning()) },
                ) {
                    ThemedListItemButton(
                        style = MaterialTheme.components.settingsItem,
                        leadingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.Wysiwyg, null,
                                tint = defaultColorForState(!verificationIsRunning)
                            )
                        },
                        headlineContent = {
                            Text(
                                i18n.userProfileVerification(),
                                color = defaultColorForState(!verificationIsRunning)
                            )
                        },
                        onClick = { userProfileViewModel.startVerification(isSinglePane) },
                        enabled = !verificationIsRunning,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangePowerLevelSection(userProfileViewModel: UserProfileViewModel, i18n: I18nView) {
    ThemedListItemButton(
        style = MaterialTheme.components.settingsItem,
        leadingContent = { Icon(Icons.Filled.Verified, null) },
        headlineContent = { Text(i18n.userProfileChangePowerLevel()) },
        onClick = { userProfileViewModel.changePowerLevelViewModel.openChangingPowerLevelDialog() },
    )
}

@Composable
private fun BanUserSection(userProfileViewModel: UserProfileViewModel, i18n: I18nView) {
    val membership = userProfileViewModel.membership.collectAsState().value
    val membershipChanging = userProfileViewModel.membershipChanging.collectAsState().value

    ThemedListItemSwitch(
        style = MaterialTheme.components.settingsItem,
        leadingContent = { BanIcon() },
        headlineContent = { Text(i18n.userProfileBanUser()) },
        selected = membership == Membership.BAN,
        onChange = {
            if (membership == Membership.BAN) {
                userProfileViewModel.openUnbanUserWarning()
            } else {
                userProfileViewModel.openBanUserWarning()
            }
        },
        enabled = !membershipChanging,
    )
}

@Composable
private fun KickUserSection(userProfileViewModel: UserProfileViewModel, i18n: I18nView) {
    ThemedListItemButton(
        style = MaterialTheme.components.settingsItem,
        leadingContent = {
            Icon(Icons.Filled.PersonOff, null)
        },
        headlineContent = {
            Text(i18n.userProfileRemoveUser())
        },
        onClick = { userProfileViewModel.openKickUserWarning() },
    )
}

@Composable
private fun AcceptKnockSection(userProfileViewModel: UserProfileViewModel, i18n: I18nView) {
    ThemedListItemButton(
        style = MaterialTheme.components.settingsItem,
        leadingContent = {
            Icon(Icons.Filled.DoorSliding, null)
        },
        headlineContent = {
            Text(i18n.userProfileAcceptKnock())
        },
        onClick = { userProfileViewModel.acceptKnock() },
    )
}

@Composable
private fun RejectKnockSection(userProfileViewModel: UserProfileViewModel, i18n: I18nView) {
    ThemedListItemButton(
        style = MaterialTheme.components.settingsItem,
        leadingContent = {
            Icon(Icons.Filled.DoorFront, null)
        },
        headlineContent = {
            Text(i18n.userProfileRejectKnock())
        },
        onClick = { userProfileViewModel.rejectKnock() },
    )
}

@Composable
private fun DialogHandler(userProfileViewModel: UserProfileViewModel) {
    if (userProfileViewModel.kickUserWarningOpen.collectAsState().value) {
        KickUserWarning(userProfileViewModel)
    }

    if (userProfileViewModel.banUserWarningOpen.collectAsState().value) {
        BanUserWarning(userProfileViewModel)
    }

    if (userProfileViewModel.unbanUserWarningOpen.collectAsState().value) {
        UnbanUserWarning(userProfileViewModel)
    }

    if (userProfileViewModel.changePowerLevelViewModel.changingPowerLevelDialogOpen.collectAsState().value) {
        ChangingPowerLevel(userProfileViewModel)
    }
}

@Composable
fun KickUserWarning(userProfileViewModel: UserProfileViewModel) {
    val i18n = DI.get<I18nView>()
    val kickUserReason = userProfileViewModel.kickUserReason.collectAsTextFieldValueState()
    val isDirect = userProfileViewModel.isDirect.collectAsState().value

    ThemedModalDialog({ userProfileViewModel.closeKickUserWarning() }) {
        ModalDialogHeader {
            Text(
                if (isDirect) i18n.settingsRoomMemberListKickUserWarningTitleChat(userProfileViewModel.userId.full)
                else i18n.settingsRoomMemberListKickUserWarningTitleGroup(userProfileViewModel.userId.full)
            )
        }
        ModalDialogContent {
            OutlinedTextField(
                value = kickUserReason.value,
                onValueChange = { kickUserReason.value = it },
                label = {
                    Text(i18n.commonOptionalReason())
                },
                maxLines = 5,
                modifier = Modifier.fillMaxWidth().inputFocusNavigation(),
            )
        }
        ModalDialogFooter {
            ThemedButton(
                style = MaterialTheme.components.commonButton,
                onClick = { userProfileViewModel.closeKickUserWarning() }
            ) {
                Text(i18n.actionCancel())
            }
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = {
                    userProfileViewModel.closeKickUserWarning()
                    userProfileViewModel.kickUser()
                }
            ) {
                Text(i18n.userProfileRemoveUserConfirmation())
            }
        }
    }
}

@Composable
fun BanUserWarning(userProfileViewModel: UserProfileViewModel) {
    val i18n = DI.get<I18nView>()
    val banUserReason = userProfileViewModel.banUserReason.collectAsTextFieldValueState()

    ThemedModalDialog({ userProfileViewModel.closeBanUserWarning() }) {
        ModalDialogHeader {
            Text(i18n.userProfileBanUserConfirmationSure())
        }
        ModalDialogContent {
            OutlinedTextField(
                value = banUserReason.value,
                onValueChange = { banUserReason.value = it },
                label = {
                    Text(i18n.commonOptionalReason())
                },
                maxLines = 5,
                modifier = Modifier.inputFocusNavigation().fillMaxWidth(),
            )
        }
        ModalDialogFooter {
            ThemedButton(
                style = MaterialTheme.components.commonButton,
                onClick = { userProfileViewModel.closeBanUserWarning() }
            ) {
                Text(i18n.actionCancel())
            }
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = {
                    userProfileViewModel.closeBanUserWarning()
                    userProfileViewModel.banUser()
                }
            ) {
                Text(i18n.userProfileBanUserConfirmation())
            }
        }
    }
}

@Composable
fun UnbanUserWarning(userProfileViewModel: UserProfileViewModel) {
    val i18n = DI.get<I18nView>()
    val unbanUserReason = userProfileViewModel.unbanUserReason.collectAsTextFieldValueState()

    ThemedModalDialog({ userProfileViewModel.closeUnbanUserWarning() }) {
        ModalDialogHeader {
            Text(i18n.unbanTitle())
        }
        ModalDialogContent {
            OutlinedTextField(
                value = unbanUserReason.value,
                onValueChange = { unbanUserReason.value = it },
                label = {
                    Text(i18n.commonOptionalReason())
                },
                maxLines = 5,
                modifier = Modifier.inputFocusNavigation().fillMaxWidth(),
            )
        }
        ModalDialogFooter {
            ThemedButton(
                style = MaterialTheme.components.commonButton,
                onClick = { userProfileViewModel.closeUnbanUserWarning() }
            ) {
                Text(i18n.actionCancel())
            }
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = {
                    userProfileViewModel.closeUnbanUserWarning()
                    userProfileViewModel.unbanUser()
                }
            ) {
                Text(i18n.unbanUserConfirmation())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChangingPowerLevel(userProfileViewModel: UserProfileViewModel) {
    val i18n = DI.get<I18nView>()
    val changingPowerLevelDialogError =
        userProfileViewModel.changePowerLevelViewModel.changingPowerLevelDialogError.collectAsState().value
    var changePowerLevelInput by
    userProfileViewModel.changePowerLevelViewModel.changingPowerLevelDialogInput.collectAsTextFieldValueState()
    val showPowerLevelHelp =
        userProfileViewModel.changePowerLevelViewModel.showPowerLevelHelp.collectAsState().value
    val canSetRoleToAdmin =
        userProfileViewModel.changePowerLevelViewModel.canSetRoleToAdmin.collectAsState().value
    val canSetRoleToModerator =
        userProfileViewModel.changePowerLevelViewModel.canSetRoleToModerator.collectAsState().value
    val canSetRoleToUser =
        userProfileViewModel.changePowerLevelViewModel.canSetRoleToUser.collectAsState().value

    ThemedModalDialog(userProfileViewModel.changePowerLevelViewModel::closeChangingPowerLevelDialog) {
        ModalDialogHeader {
            Text(i18n.userProfileChangePowerLevel())
        }
        ModalDialogContent {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1F, false).fillMaxWidth(),
                    text = i18n.userProfileChangePowerLevel(),
                    style = MaterialTheme.typography.labelLarge
                )
                Tooltip(
                    tooltip = { Text(i18n.commonHelp()) }
                ) {
                    ThemedIconButton(
                        style = MaterialTheme.components.commonIconButton,
                        onClick = {
                            if (showPowerLevelHelp) userProfileViewModel.changePowerLevelViewModel.closePowerLevelHelp()
                            else userProfileViewModel.changePowerLevelViewModel.openPowerLevelHelp()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Help, i18n.commonHelp())
                    }
                }
            }
            OutlinedTextField(
                value = changePowerLevelInput,
                onValueChange = { changePowerLevelInput = it },
                isError = changingPowerLevelDialogError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .inputFocusNavigation()
                    .fillMaxWidth(),
                maxLines = 1
            )
            FlowRow(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp, Alignment.Start), Arrangement.Top) {
                if (canSetRoleToUser) {
                    ThemedSuggestionChip(
                        onClick = {
                            changePowerLevelInput = TextFieldValue(
                                ChangePowerLevelViewModel.Role.USER.getMinPowerLevel().toLevelString()
                            )
                        },
                        label = {
                            Text(i18n.userProfileRoleUser())
                        }
                    )
                }
                if (canSetRoleToModerator) {
                    ThemedSuggestionChip(
                        onClick = {
                            changePowerLevelInput = TextFieldValue(
                                ChangePowerLevelViewModel.Role.MODERATOR.getMinPowerLevel().toLevelString()
                            )
                        },
                        label = {
                            Text(i18n.userProfileRoleModerator())
                        }
                    )
                }
                if (canSetRoleToAdmin) {
                    ThemedSuggestionChip(
                        onClick = {
                            changePowerLevelInput = TextFieldValue(
                                ChangePowerLevelViewModel.Role.ADMIN.getMinPowerLevel().toLevelString()
                            )
                        },
                        label = {
                            Text(i18n.userProfileRoleAdministrator())
                        }
                    )
                }
            }
            changingPowerLevelDialogError?.let {
                Text(color = MaterialTheme.colorScheme.error, text = it)
            }
            if (showPowerLevelHelp) {
                Text(i18n.userProfileNote(), style = MaterialTheme.typography.labelMedium)
                Text(text = i18n.userProfileNoteText())
            }
        }
        ModalDialogFooter {
            ThemedButton(
                style = MaterialTheme.components.commonButton,
                onClick = { userProfileViewModel.changePowerLevelViewModel.closeChangingPowerLevelDialog() },
            ) {
                Text(i18n.actionCancel())
            }
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                enabled = changingPowerLevelDialogError == null && changePowerLevelInput.text != "",
                onClick = {
                    userProfileViewModel.changePowerLevelViewModel.setPowerLevelTo(
                        changePowerLevelInput.text.toLong()
                    )
                    userProfileViewModel.changePowerLevelViewModel.closeChangingPowerLevelDialog()
                },
            ) {
                Text(i18n.userProfileChangePowerLevel())
            }
        }
    }
}

@Composable
private fun defaultColorForState(enabled: Boolean) =
    LocalContentColor.current.run {
        if (!enabled) copy(alpha = 0.6f) else this
    }

private fun PowerLevel.toLevelString() = (this as? PowerLevel.User)?.level?.toString() ?: ""

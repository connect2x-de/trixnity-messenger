package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Wysiwyg
import androidx.compose.material.icons.filled.CopyAll
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.MessengerDialog
import de.connect2x.messenger.compose.view.common.SelectableText
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.common.VerySmallSpacer
import de.connect2x.messenger.compose.view.common.WarningDialog
import de.connect2x.messenger.compose.view.common.icons.BanIcon
import de.connect2x.messenger.compose.view.common.icons.BlockIcon
import de.connect2x.messenger.compose.view.common.icons.NotVerifiedIcon
import de.connect2x.messenger.compose.view.common.icons.VerificationLevel
import de.connect2x.messenger.compose.view.common.icons.VerifiedIcon
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.root.IsSinglePane
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedInfoChip
import de.connect2x.messenger.compose.view.theme.components.ThemedSuggestionChip
import de.connect2x.messenger.compose.view.theme.components.ThemedSwitch
import de.connect2x.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.UserProfileViewModel
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.crypto.key.UserTrustLevel


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
                                    userInfoElement.initials,
                                    image,
                                    this@BoxWithConstraints.maxWidth.coerceAtMost(200.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        SelectableText(userInfoElement.name, style = MaterialTheme.typography.titleLarge)

                        if (userInfoElement.name != userId.full) {
                            CopyableUserId(userId, MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        CopyableUserId(userId, MaterialTheme.typography.titleLarge)
                    }

                    Spacer(Modifier.height(5.dp))
                    when (userTrustLevel) {
                        is UserTrustLevel.CrossSigned -> {
                            ThemedInfoChip(
                                style = MaterialTheme.components.primaryChip,
                                icon = { VerifiedIcon(VerificationLevel.USER) },
                                label = { Text(i18n.secure()) },
                            )
                        }

                        is UserTrustLevel.NotAllDevicesCrossSigned -> {
                            ThemedInfoChip(
                                style = MaterialTheme.components.destructiveChip,
                                icon = { NotVerifiedIcon(VerificationLevel.USER) },
                                label = { Text(i18n.insecure()) },
                            )
                        }

                        UserTrustLevel.Blocked, is UserTrustLevel.Invalid, UserTrustLevel.Unknown, null -> {
                            ThemedInfoChip(
                                style = MaterialTheme.components.destructiveChip,
                                icon = { NotVerifiedIcon(VerificationLevel.USER) },
                                label = { Text(i18n.roomNoEncryptionFound()) },
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
    val clipboard = LocalClipboardManager.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        SelectableText(userId.full, style = textStyle, overflow = TextOverflow.Visible)
        Spacer(Modifier.size(5.dp))
        Tooltip({ TooltipText(i18n.userProfileCopyUserId()) }) {
            ThemedIconButton(
                style = MaterialTheme.components.commonIconButton,
                onClick = { clipboard.setText(AnnotatedString(userId.full)) }
            ) {
                Icon(Icons.Default.CopyAll, i18n.userProfileCopyUserId())
            }
        }
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

        SmallSpacer()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.messengerDpConstants.small),
            Arrangement.Start,
            Alignment.CenterVertically
        ) {
            Text(i18n.userProfileRoomOptions(), style = MaterialTheme.typography.titleMedium)
        }
        SmallSpacer()

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
        val verifying = userProfileViewModel.verifying.collectAsState().value
        val canOpenChat = userProfileViewModel.canOpenChat.collectAsState().value

        VerySmallSpacer()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.messengerDpConstants.small),
            Arrangement.Start,
            Alignment.CenterVertically
        ) {
            Text(i18n.userProfileUserOptions(), style = MaterialTheme.typography.titleMedium)
        }
        VerySmallSpacer()

        MenuElement(arrangement = Arrangement.SpaceBetween) {
            Row {
                BlockIcon()
                Spacer(Modifier.size(10.dp))
                Text(i18n.userProfileBlockUser())
            }
            ThemedSwitch(
                checked = isUserBlocked,
                onCheckedChange = {
                    if (isUserBlocked) {
                        userProfileViewModel.unblockUser()
                    } else {
                        userProfileViewModel.blockUser()
                    }
                },
                enabled = !blockingInProgress
            )
        }
        if (canOpenChat) {
            MenuElement(Modifier.clickable {
                userProfileViewModel.openChat()
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    i18n.contact(),
                    Modifier.size(24.dp),
                    defaultColorForState(!openingChat)
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = i18n.userProfileContact(),
                    color = defaultColorForState(!openingChat)
                )
            }
        }
        val isSinglePane = IsSinglePane.current
        MenuElement(Modifier.clickable {
            userProfileViewModel.startVerification(isSinglePane)
        }) {
            Icon(
                Icons.AutoMirrored.Filled.Wysiwyg,
                i18n.userVerification(),
                Modifier.size(24.dp),
                defaultColorForState(!verifying)
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = i18n.userProfileVerification(),
                color = defaultColorForState(!verifying)
            )
        }
    }
}

@Composable
private fun ChangePowerLevelSection(userProfileViewModel: UserProfileViewModel, i18n: I18nView) {
    MenuElement(Modifier.clickable {
        userProfileViewModel.changePowerLevelViewModel.openChangingPowerLevelDialog()
    }) {
        Icon(
            Icons.Filled.Verified,
            i18n.userProfileChangePowerLevel(),
            Modifier.size(24.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(i18n.userProfileChangePowerLevel())
    }
}

@Composable
private fun BanUserSection(userProfileViewModel: UserProfileViewModel, i18n: I18nView) {
    val membership = userProfileViewModel.membership.collectAsState().value
    val membershipChanging = userProfileViewModel.membershipChanging.collectAsState().value

    MenuElement(arrangement = Arrangement.SpaceBetween) {
        Row {
            BanIcon()
            Spacer(Modifier.size(10.dp))
            Text(i18n.userProfileBanUser())
        }
        ThemedSwitch(
            checked = membership == Membership.BAN,
            onCheckedChange = {
                if (membership == Membership.BAN) {
                    userProfileViewModel.openUnbanUserWarning()
                } else {
                    userProfileViewModel.openBanUserWarning()
                }
            },
            enabled = !membershipChanging
        )
    }
}

@Composable
private fun KickUserSection(userProfileViewModel: UserProfileViewModel, i18n: I18nView) {
    MenuElement(Modifier.clickable {
        userProfileViewModel.openKickUserWarning()
    }) {
        Icon(
            Icons.Filled.PersonOff,
            i18n.userProfileRemoveUser(),
            Modifier.size(24.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(i18n.userProfileRemoveUser())
    }
}

@Composable
private fun AcceptKnockSection(userProfileViewModel: UserProfileViewModel, i18n: I18nView) {
    MenuElement(Modifier.clickable {
        userProfileViewModel.acceptKnock()
    }) {
        Icon(
            Icons.Filled.DoorSliding,
            i18n.userProfileAcceptKnock(),
            Modifier.size(24.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(i18n.userProfileAcceptKnock())
    }
}

@Composable
private fun RejectKnockSection(userProfileViewModel: UserProfileViewModel, i18n: I18nView) {
    MenuElement(Modifier.clickable {
        userProfileViewModel.rejectKnock()
    }) {
        Icon(
            Icons.Filled.DoorFront,
            i18n.userProfileRejectKnock(),
            Modifier.size(24.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(i18n.userProfileRejectKnock())
    }
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
    var kickUserReason by userProfileViewModel.kickUserReason.collectAsTextFieldValueState()
    val isDirect = userProfileViewModel.isDirect.collectAsState().value

    WarningDialog(
        title =
            if (isDirect) i18n.settingsRoomMemberListKickUserWarningTitleChat(userProfileViewModel.userId.full)
            else i18n.settingsRoomMemberListKickUserWarningTitleGroup(userProfileViewModel.userId.full),
        message = {
            OutlinedTextField(
                value = kickUserReason,
                onValueChange = { kickUserReason = it },
                label = {
                    Text(i18n.commonOptionalReason())
                },
                maxLines = 5
            )
        },
        dismissButtonText = i18n.commonCancel().capitalize(Locale.current),
        confirmButtonText = i18n.userProfileRemoveUserConfirmation(),
        dismissAction = { userProfileViewModel.closeKickUserWarning() },
        confirmAction = {
            userProfileViewModel.closeKickUserWarning()
            userProfileViewModel.kickUser()
        }
    )
}

@Composable
fun BanUserWarning(userProfileViewModel: UserProfileViewModel) {
    val i18n = DI.current.get<I18nView>()
    var banUserReason by userProfileViewModel.banUserReason.collectAsTextFieldValueState()

    WarningDialog(
        title = i18n.userProfileBanUserConfirmationSure(),
        message = {
            OutlinedTextField(
                value = banUserReason,
                onValueChange = { banUserReason = it },
                label = {
                    Text(i18n.commonOptionalReason())
                },
                maxLines = 5
            )
        },
        dismissButtonText = i18n.commonCancel().capitalize(Locale.current),
        confirmButtonText = i18n.userProfileBanUserConfirmation(),
        dismissAction = { userProfileViewModel.closeBanUserWarning() },
        confirmAction = {
            userProfileViewModel.closeBanUserWarning()
            userProfileViewModel.banUser()
        }
    )
}

@Composable
fun UnbanUserWarning(userProfileViewModel: UserProfileViewModel) {
    val i18n = DI.current.get<I18nView>()
    var unbanUserReason by userProfileViewModel.unbanUserReason.collectAsTextFieldValueState()

    WarningDialog(
        title = i18n.unbanTitle(),
        message = {
            OutlinedTextField(
                value = unbanUserReason,
                onValueChange = { unbanUserReason = it },
                label = {
                    Text(i18n.commonOptionalReason())
                },
                maxLines = 5
            )
        },
        dismissButtonText = i18n.commonCancel().capitalize(Locale.current),
        confirmButtonText = i18n.unbanUserConfirmation(),
        dismissAction = { userProfileViewModel.closeUnbanUserWarning() },
        confirmAction = {
            userProfileViewModel.closeUnbanUserWarning()
            userProfileViewModel.unbanUser()
        }
    )
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

    MessengerDialog(
        onDismissRequest = {
            userProfileViewModel.changePowerLevelViewModel.closeChangingPowerLevelDialog()
        },
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
                        text = i18n.userProfileChangePowerLevel(),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Tooltip(
                        tooltip = { Text(i18n.commonHelp()) }
                    ) {
                        ThemedIconButton(
                            style = MaterialTheme.components.commonIconButton,
                            onClick = { if (showPowerLevelHelp) userProfileViewModel.changePowerLevelViewModel.closePowerLevelHelp() else userProfileViewModel.changePowerLevelViewModel.openPowerLevelHelp() },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Help, i18n.commonHelp())
                        }
                    }
                }
                Spacer(Modifier.size(10.dp))
                OutlinedTextField(
                    value = changePowerLevelInput,
                    onValueChange = { changePowerLevelInput = it },
                    isError = changingPowerLevelDialogError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )
                FlowRow(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Arrangement.Top) {
                    if (canSetRoleToUser) {
                        ThemedSuggestionChip(
                            onClick = {
                                changePowerLevelInput = TextFieldValue(
                                    ChangePowerLevelViewModel.Role.USER
                                        .getMinPowerLevel().toString()
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
                                    ChangePowerLevelViewModel.Role.MODERATOR
                                        .getMinPowerLevel().toString()
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
                                    ChangePowerLevelViewModel.Role.ADMIN
                                        .getMinPowerLevel().toString()
                                )
                            },
                            label = {
                                Text(i18n.userProfileRoleAdministrator())
                            }
                        )
                    }
                }
                Spacer(Modifier.size(5.dp))
                changingPowerLevelDialogError?.let {
                    Spacer(Modifier.size(5.dp))
                    Text(color = MaterialTheme.colorScheme.error, text = it)
                }
                Spacer(Modifier.size(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = { userProfileViewModel.changePowerLevelViewModel.closeChangingPowerLevelDialog() },
                        modifier = Modifier.weight(0.4f)
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                    Spacer(Modifier.size(15.dp))
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        enabled = changingPowerLevelDialogError == null && changePowerLevelInput.text != "",
                        onClick = {
                            userProfileViewModel.changePowerLevelViewModel.setPowerLevelTo(
                                changePowerLevelInput.text.toLong()
                            )
                            userProfileViewModel.changePowerLevelViewModel.closeChangingPowerLevelDialog()
                        },
                        modifier = Modifier.weight(0.4f)
                    ) {
                        Text(i18n.userProfileChangePowerLevel())
                    }
                }
                if (showPowerLevelHelp) {
                    Column(modifier = Modifier.align(alignment = Alignment.Start)) {
                        Spacer(Modifier.size(15.dp))
                        Text(i18n.userProfileNote(), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.size(5.dp))
                        Text(text = i18n.userProfileNoteText())
                    }
                }
            }
        }
    )
}

@Composable
private fun MenuElement(
    modifier: Modifier = Modifier,
    arrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 10.dp).minimumInteractiveComponentSize(),
        arrangement,
        Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
private fun defaultColorForState(enabled: Boolean) =
    LocalContentColor.current.run {
        if (!enabled) copy(alpha = 0.6f) else this
    }

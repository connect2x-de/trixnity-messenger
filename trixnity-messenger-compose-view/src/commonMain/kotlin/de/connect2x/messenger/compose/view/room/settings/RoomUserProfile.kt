package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.MessengerDialog
import de.connect2x.messenger.compose.view.common.WarningDialog
import de.connect2x.messenger.compose.view.common.blockPointerInput
import de.connect2x.messenger.compose.view.common.collectAsStateForTextField
import de.connect2x.messenger.compose.view.common.icons.BanIcon
import de.connect2x.messenger.compose.view.common.icons.BlockIcon
import de.connect2x.messenger.compose.view.common.icons.NotVerifiedIcon
import de.connect2x.messenger.compose.view.common.icons.VerificationLevel
import de.connect2x.messenger.compose.view.common.icons.VerifiedIcon
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.UserProfileViewModel
import net.folivo.trixnity.client.key.UserTrustLevel
import net.folivo.trixnity.core.model.events.m.room.Membership

@Composable
fun RoomUserProfileContainer(userProfileViewModel: UserProfileViewModel) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            RoomUserProfile(userProfileViewModel)
        }
    }
}

interface RoomUserProfileView {
    @Composable
    fun create(userProfileViewModel: UserProfileViewModel)
}

@Composable
fun RoomUserProfile(userProfileViewModel: UserProfileViewModel) {
    DI.get<RoomUserProfileView>().create(userProfileViewModel)
}

class RoomUserProfileViewImpl : RoomUserProfileView {
    @Composable
    override fun create(userProfileViewModel: UserProfileViewModel) {
        val error = userProfileViewModel.error.collectAsState()
        val i18n = DI.get<I18nView>()
        val userInfoElement = userProfileViewModel.userInfo.collectAsState().value
        val image = userInfoElement?.image?.collectAsState(null)?.value
        val userId = userProfileViewModel.userId
        val membershipChanging = userProfileViewModel.membershipChanging.collectAsState().value
        val iHavePowerToBanUser = userProfileViewModel.iHavePowerToBanUser.collectAsState().value
        val iHavePowerToUnbanUser = userProfileViewModel.iHavePowerToUnbanUser.collectAsState().value
        val iHavePowerToKickUser = userProfileViewModel.iHavePowerToKickUser.collectAsState().value
        val maxPowerLevel = userProfileViewModel.changePowerLevelViewModel.canSetPowerLevelToMax.collectAsState().value
        val blockingInProgress = userProfileViewModel.blockingInProgress.collectAsState().value
        val isUserBlocked = userProfileViewModel.isUserBlocked.collectAsState().value
        val membership = userProfileViewModel.membership.collectAsState().value
        val membershipReason = userProfileViewModel.membershipReason.collectAsState().value
        val canSetRoleToAdmin =
            userProfileViewModel.changePowerLevelViewModel.canSetRoleToAdmin.collectAsState().value
        val canSetRoleToModerator =
            userProfileViewModel.changePowerLevelViewModel.canSetRoleToModerator.collectAsState().value
        val canSetRoleToUser =
            userProfileViewModel.changePowerLevelViewModel.canSetRoleToUser.collectAsState().value
        val userTrustLevel = userProfileViewModel.userTrustLevel.collectAsState().value
        val openingChat = userProfileViewModel.openingChat.collectAsState().value
        val verifying = userProfileViewModel.verifying.collectAsState().value

        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .blockPointerInput(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            DialogHandler(userProfileViewModel)
            Column(Modifier.weight(1f)) {
                Header(userProfileViewModel::back, i18n.profileTitle())
                error.value?.let {
                    ErrorView(it)
                }

                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    Arrangement.Center,
                    Alignment.CenterHorizontally
                ) {
                    if (userInfoElement != null) {
                        Avatar(
                            image,
                            userInfoElement.initials ?: "",
                            50.dp
                        )
                        Text(userInfoElement.name, style = MaterialTheme.typography.titleLarge)
                        if (userInfoElement.name != userId.full) {
                            Text(userId.full, style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        Text(userId.full, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(5.dp))
                    when (userTrustLevel) {
                        is UserTrustLevel.CrossSigned ->
                            StatusRow(i18n.secure(), true)

                        is UserTrustLevel.NotAllDevicesCrossSigned ->
                            StatusRow(i18n.insecure(), false)

                        UserTrustLevel.Blocked, is UserTrustLevel.Invalid, UserTrustLevel.Unknown, null ->
                            StatusRow(i18n.roomNoEncryptionFound(), false)
                    }
                }
            }

            Column {
                if (membership == Membership.BAN && membershipReason != null) {
                    HorizontalDivider(Modifier.fillMaxWidth())
                    MenuElement {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            i18n.contact(),
                            Modifier.size(24.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(i18n.userProfileBanReason())
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = 44.dp, end = 10.dp),
                        Arrangement.Start,
                        Alignment.Top
                    ) {
                        Text(membershipReason)
                    }
                }

                if (!userProfileViewModel.isMyself) {
                    HorizontalDivider(Modifier.fillMaxWidth())
                    MenuElement(arrangement = Arrangement.SpaceBetween) {
                        Row {
                            BlockIcon()
                            Spacer(Modifier.size(10.dp))
                            Text(i18n.userProfileBlockUser())
                        }
                        Switch(
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
                    MenuElement(Modifier.clickable {
                        userProfileViewModel.startVerification()
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
                    HorizontalDivider(Modifier.fillMaxWidth())
                    if (
                        canSetRoleToUser ||
                        canSetRoleToModerator ||
                        canSetRoleToAdmin ||
                        (maxPowerLevel != null && maxPowerLevel != 0L)
                    ) {
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
                    if (iHavePowerToBanUser || (iHavePowerToUnbanUser && membership == Membership.BAN)) {
                        MenuElement(arrangement = Arrangement.SpaceBetween) {
                            Row {
                                BanIcon()
                                Spacer(Modifier.size(10.dp))
                                Text(i18n.userProfileBanUser())
                            }
                            Switch(
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
                    if (iHavePowerToKickUser) {
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
                }
            }
        }
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
    val kickUserWarningMessage =
        userProfileViewModel.kickUserWarningMessage.collectAsState().value
    val kickUserWarningTitle =
        userProfileViewModel.kickUserWarningTitle.collectAsState().value
    WarningDialog(
        title = kickUserWarningTitle,
        message = { Text(kickUserWarningMessage) },
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
    val banReason = userProfileViewModel.banReason.collectAsStateForTextField().value

    WarningDialog(
        title = i18n.userProfileBanUserConfirmationSure(),
        message = {
            OutlinedTextField(
                value = banReason,
                onValueChange = {
                    userProfileViewModel.banReason.value = it
                },
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
    val unbanReason = userProfileViewModel.unbanReason.collectAsStateForTextField().value

    WarningDialog(
        title = i18n.unbanTitle(),
        message = {
            OutlinedTextField(
                value = unbanReason,
                onValueChange = {
                    userProfileViewModel.unbanReason.value = it
                },
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
    val changePowerLevelInput =
        userProfileViewModel.changePowerLevelViewModel.changingPowerLevelDialogInput.collectAsStateForTextField().value
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
                    IconButton(
                        onClick = { if (showPowerLevelHelp) userProfileViewModel.changePowerLevelViewModel.closePowerLevelHelp() else userProfileViewModel.changePowerLevelViewModel.openPowerLevelHelp() },
                        Modifier.buttonPointerModifier()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Help, i18n.commonHelp())
                    }
                }
                Spacer(Modifier.size(10.dp))
                OutlinedTextField(
                    value = changePowerLevelInput.value,
                    onValueChange = {
                        userProfileViewModel.changePowerLevelViewModel.onPowerLevelEntered(
                            it
                        )
                    },
                    isError = changePowerLevelInput.errorId != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )
                FlowRow(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Arrangement.Top) {
                    if (canSetRoleToUser) {
                        SuggestionChip(
                            onClick = {
                                userProfileViewModel.changePowerLevelViewModel.onPowerLevelEntered(
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
                        SuggestionChip(
                            onClick = {
                                userProfileViewModel.changePowerLevelViewModel.onPowerLevelEntered(
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
                        SuggestionChip(
                            onClick = {
                                userProfileViewModel.changePowerLevelViewModel.onPowerLevelEntered(
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
                        { userProfileViewModel.changePowerLevelViewModel.closeChangingPowerLevelDialog() },
                        Modifier.buttonPointerModifier().weight(0.4f)
                    ) {
                        Text(i18n.commonCancel().capitalize(Locale.current))
                    }
                    Spacer(Modifier.size(15.dp))
                    Button(
                        enabled = changePowerLevelInput.errorId == null && changePowerLevelInput.value != "",
                        onClick = {
                            userProfileViewModel.changePowerLevelViewModel.setPowerLevelTo(
                                changePowerLevelInput.value.toLong()
                            )
                            userProfileViewModel.changePowerLevelViewModel.closeChangingPowerLevelDialog()
                        },
                        modifier = Modifier.buttonPointerModifier().weight(0.4f)
                    ) { Text(text = i18n.userProfileChangePowerLevel(), textAlign = TextAlign.Center) }
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
    content: @Composable () -> Unit
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
private fun StatusRow(text: String, positive: Boolean = true) {
    SuggestionChip(
        enabled = false,
        onClick = {},
        label = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (positive) {
                    VerifiedIcon(VerificationLevel.TIMELINE_EVENT)
                } else {
                    NotVerifiedIcon(VerificationLevel.TIMELINE_EVENT)
                }

                Text(
                    text,
                    color = if (positive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        colors = SuggestionChipDefaults.suggestionChipColors().copy(
            disabledContainerColor = if (positive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            disabledLabelColor = if (positive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
        )
    )
}

@Composable
private fun defaultColorForState(enabled: Boolean) =
    LocalContentColor.current.run { if (!enabled) { copy(alpha = 0.6f) } else { this } }

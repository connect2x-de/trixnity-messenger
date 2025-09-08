package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.UserState
import de.connect2x.messenger.compose.view.common.modifier.focusHighlighting
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components.AvatarPresenceBadge
import de.connect2x.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModel.Role
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListViewModel
import net.folivo.trixnity.client.user.PowerLevel
import net.folivo.trixnity.core.model.UserId

interface RoomSettingsMemberListElementView {
    @Composable
    fun create(
        memberListViewModel: MemberListViewModel,
        memberUserId: UserId,
        memberListElementViewModel: MemberListElementViewModel,
        onClick: () -> Unit,
    )
}

@Composable
fun RoomSettingsMemberListElement(
    memberListViewModel: MemberListViewModel,
    memberUserId: UserId,
    memberListElementViewModel: MemberListElementViewModel,
    onClick: () -> Unit,
) {
    DI.get<RoomSettingsMemberListElementView>()
        .create(memberListViewModel, memberUserId, memberListElementViewModel, onClick)
}

class RoomSettingsMemberListElementViewImpl : RoomSettingsMemberListElementView {
    @Composable
    override fun create(
        memberListViewModel: MemberListViewModel,
        memberUserId: UserId,
        memberListElementViewModel: MemberListElementViewModel,
        onClick: () -> Unit,
    ) {
        val i18n = DI.get<I18nView>()
        val memberElement = memberListElementViewModel.member.collectAsState().value
        val role = memberListElementViewModel.role.collectAsState().value
        val powerLevel = memberListElementViewModel.powerLevel.collectAsState().value
        val showPowerLevel = memberListElementViewModel.showPowerLevel.collectAsState().value
        val showRole = memberListElementViewModel.showRole.collectAsState().value
        val isLastMember =
            memberListViewModel.elements.collectAsState().value.lastOrNull()?.memberUserId == memberListElementViewModel.memberUserId
        val presence = memberListElementViewModel.presence.collectAsState().value
        val image = memberElement?.image
        val isMemberElementLoading = memberElement == null
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            Modifier
                .fillMaxWidth()
                .clickable(interactionSource = interactionSource, indication = LocalIndication.current) {
                    memberListElementViewModel.openUserProfile()
                }
                .focusHighlighting(interactionSource)
                .buttonPointerModifier(),
        ) {
            Column {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMemberElementLoading) LoadingSpinner() else {
                        ThemedUserAvatar(initials = memberElement.initials, image = image, presence = presence) {
                            AvatarPresenceBadge(presence)
                        }
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
                                        + if (showPowerLevel && powerLevel is PowerLevel.User) " (${powerLevel.level})" else "",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
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

fun getRoomSettingsMemberRoleName(role: Role, i18n: I18nView): String {
    return when (role) {
        Role.CREATOR -> i18n.userProfileRoleCreator()
        Role.ADMIN -> i18n.userProfileRoleAdministrator()
        Role.MODERATOR -> i18n.userProfileRoleModerator()
        Role.USER -> i18n.userProfileRoleUser()
    }
}

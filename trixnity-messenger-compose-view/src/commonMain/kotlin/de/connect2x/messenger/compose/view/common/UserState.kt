package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.icons.BanIcon
import de.connect2x.messenger.compose.view.common.icons.BanIconType
import de.connect2x.messenger.compose.view.common.icons.KnockIcon
import de.connect2x.messenger.compose.view.common.icons.NeutralVerifiedIcon
import de.connect2x.messenger.compose.view.common.icons.NotVerifiedIcon
import de.connect2x.messenger.compose.view.common.icons.VerificationLevel
import de.connect2x.messenger.compose.view.common.icons.VerifiedIcon
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerColors
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.crypto.key.UserTrustLevel

@Composable
fun RowScope.UserState(
    userTrustLevelFlow: StateFlow<UserTrustLevel?>,
    isUserBlockedFlow: StateFlow<Boolean>,
    membershipFlow: StateFlow<Membership?>? = null,
    unbannableFlow: StateFlow<Boolean>? = null
) {
    val userTrustLevel = userTrustLevelFlow.collectAsState().value
    val isUserBlocked = isUserBlockedFlow.collectAsState().value
    val membership = membershipFlow?.collectAsState()?.value
    val unbannable = unbannableFlow?.collectAsState()?.value == true
    val i18n = DI.get<I18nView>()

    if (isUserBlocked) {
        Tooltip({ Text(i18n.roomHeaderUserIsBlocked()) }) {
            Icon(
                Icons.Default.Block,
                i18n.roomHeaderUserIsBlocked(),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.messengerColors.blockedUser,
            )
        }
        Spacer(Modifier.size(5.dp))
    } else if (membership == Membership.BAN) {
        BanIcon(if (unbannable) BanIconType.Unbannable else BanIconType.NotUnbannable, 16.dp)
    } else if (membership == Membership.KNOCK) {
        KnockIcon(16.dp)
    } else {
        when (userTrustLevel) {
            is UserTrustLevel.CrossSigned ->
                if (userTrustLevel.verified) {
                    VerifiedIcon(VerificationLevel.USER, 16.dp)
                } else {
                    NeutralVerifiedIcon(VerificationLevel.USER, 16.dp)
                }

            is UserTrustLevel.Unknown -> {
                NeutralVerifiedIcon(VerificationLevel.USER, 16.dp)
            }

            null -> Box { }
            else -> {
                NotVerifiedIcon(VerificationLevel.USER, 16.dp)
            }
        }
    }
}

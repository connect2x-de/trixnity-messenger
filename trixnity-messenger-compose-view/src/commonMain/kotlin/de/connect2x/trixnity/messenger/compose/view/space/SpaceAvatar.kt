package de.connect2x.trixnity.messenger.compose.view.space

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.AvatarStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceContentViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import kotlinx.coroutines.flow.Flow

@Composable
internal fun AvatarStyle.Companion.space() =
    MaterialTheme.components.avatar.copy(
        imageBackgroundColor = Color.Transparent,
        outerBorder = BorderStroke(0.dp, Color.Transparent),
        innerBorder = BorderStroke(0.dp, Color.Transparent),
        shape = RoundedCornerShape(20),
    )

@Composable
internal fun SpaceAvatar(
    name: String,
    image: Flow<ByteArray?>,
    size: Dp = avatarSize().dp,
    style: AvatarStyle = AvatarStyle.space(),
    modifier: Modifier = Modifier,
) {
    val initials = DI.get<Initials>().compute(name)
    key(image) {
        ThemedUserAvatar(
            initials = initials,
            image = image.collectAsState(null).value,
            size = size,
            style = style,
            modifier = modifier,
        )
    }
}

@Composable
internal fun rememberSpaceAvatar(viewModel: SpaceContentViewModel, userId: UserId, roomId: RoomId): Flow<ByteArray?> =
    remember(viewModel, userId, roomId) { viewModel.avatar(userId, roomId) }

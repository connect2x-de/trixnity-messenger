package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.util.GetAccountProfileDisplayName

@Composable
fun rememberAccountProfileDisplayName(userId: UserId): State<String> {
    val displayName = DI.get<GetAccountProfileDisplayName>()
    return remember(userId) { displayName.fromUserId(userId) }.collectAsStateWithLifecycle(userId.full)
}

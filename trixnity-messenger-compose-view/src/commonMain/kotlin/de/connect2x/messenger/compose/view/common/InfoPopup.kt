package de.connect2x.messenger.compose.view.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import de.connect2x.trixnity.messenger.util.ReactionKey
import de.connect2x.trixnity.messenger.util.ReadReceiptsHandle.Reader
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import kotlinx.coroutines.flow.StateFlow

@Composable
expect fun InfoPopup(
    isOpen: Boolean,
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    readers: Collection<Reader>,
    reactors: Map<ReactionKey, Collection<StateFlow<UserInfoElement?>>>,
    modifier: Modifier = Modifier,
)

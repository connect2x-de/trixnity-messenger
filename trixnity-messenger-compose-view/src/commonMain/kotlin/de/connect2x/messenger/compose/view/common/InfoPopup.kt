package de.connect2x.messenger.compose.view.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import de.connect2x.trixnity.messenger.util.ReadReceiptsRepository.ReadReceiptsHandle.Reader
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement

@Composable
expect fun InfoPopup(
    isOpen: Boolean,
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    readers: Set<Reader>,
    reactors: Map<String, List<UserInfoElement>>,
    modifier: Modifier = Modifier,
)

package de.connect2x.messenger.compose.view.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement

@Composable
expect fun ReactorListPopup(
    isOpen: Boolean,
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isByMe: Boolean,
    reactors: Map<String, List<UserInfoElement>>,
)

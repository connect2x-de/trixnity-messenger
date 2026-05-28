package de.connect2x.trixnity.messenger.compose.view.room.settings.addmembers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.ErrorDialog
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersNewSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersViewModel

class AddMembersToRoomNewSearchViewImpl : AddMembersToRoomView {
    private val log =
        Logger("de.connect2x.trixnity.messenger.compose.view.room.settings.AddMembersToRoomNewSearchViewImpl")

    @Composable
    override fun create(addMembersViewModel: AddMembersViewModel) {
        if (addMembersViewModel is AddMembersNewSearchViewModel) {
            val i18n = DI.get<I18nView>()

            val error by addMembersViewModel.error.collectAsState()
            val errorCause by addMembersViewModel.errorCause.collectAsState()

            Box(Modifier.fillMaxSize()) {
                Column {
                    Header(addMembersViewModel::back, i18n.addMembers())
                    SearchUsers(addMembersViewModel) { UndecryptableHistoryInfo(addMembersViewModel) }
                }
                AddMembersFloatingButton(addMembersViewModel)
                ErrorDialog(error, errorCause, onDismiss = { addMembersViewModel.errorDismiss() })
            }
        } else {
            log.warn {
                "Expected AddMembersToRoomNewSearchViewModel, but got: ${addMembersViewModel::class.simpleName}"
            }
        }
    }
}

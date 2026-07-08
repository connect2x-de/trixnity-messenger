package de.connect2x.trixnity.messenger.compose.view.roomlist.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.sp
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.common.ErrorDialog
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.VerySmallSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.search.user.SearchUsers
import de.connect2x.trixnity.messenger.compose.view.search.user.UsersInGroup
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupNewSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel

class CreateNewGroupNewSearchViewImpl : CreateNewGroupView {
    private val log =
        Logger("de.connect2x.trixnity.messenger.compose.view.roomlist.create.CreateNewGroupNewSearchViewImpl")

    @Composable
    override fun create(createNewGroupViewModel: CreateNewGroupViewModel) {
        if (createNewGroupViewModel is CreateNewGroupNewSearchViewModel) {
            val i18n = DI.get<I18nView>()
            val error by createNewGroupViewModel.error.collectAsState()
            val errorDetails by createNewGroupViewModel.errorDetails.collectAsState()
            val isCreating by createNewGroupViewModel.isCreating.collectAsState()

            Box(Modifier.fillMaxSize()) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Header(
                        createNewGroupViewModel::back,
                        { Text(i18n.createNewGroupNewGroup(), fontWeight = Bold, fontSize = 16.sp) },
                    )
                    if (isCreating) {
                        ThemedProgressIndicator(
                            Modifier.fillMaxWidth(),
                            MaterialTheme.components.linearProgressIndicator,
                        )
                    }
                    SearchUsers(
                        createNewGroupViewModel.userSearchViewModel,
                        { createNewGroupViewModel.onUserClick(it) },
                    ) {
                        val optionalRoomName = createNewGroupViewModel.optionalRoomName.collectAsTextFieldValueState()
                        val optionalRoomTopic =
                            createNewGroupViewModel.optionalGroupTopic.collectAsTextFieldValueState()
                        CreateNewGroupOptions(createNewGroupViewModel)
                        VerySmallSpacer()
                        OptionalRoomNameInput(optionalRoomName)
                        VerySmallSpacer()
                        OptionalRoomTopicInput(optionalRoomTopic)
                        VerySmallSpacer()
                        UsersInGroup(createNewGroupViewModel.groupUsersNewSearch) {
                            createNewGroupViewModel.removeUserFromGroup(it)
                        }
                    }
                }
                CreateNewGroupButton(createNewGroupViewModel)
            }

            ErrorDialog(error, errorDetails, onDismiss = { createNewGroupViewModel.errorDismiss() })
        } else {
            log.warn {
                "Expected CreateNewGroupNewSearchViewModel, but got ${createNewGroupViewModel::class.simpleName}"
            }
        }
    }
}

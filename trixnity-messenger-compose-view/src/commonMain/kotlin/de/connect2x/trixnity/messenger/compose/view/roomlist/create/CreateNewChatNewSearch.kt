package de.connect2x.trixnity.messenger.compose.view.roomlist.create

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.ErrorDialog
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.search.user.SearchUsers
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatNewSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModel

class CreateNewChatNewSearchViewImpl : CreateNewChatView {
    private val log =
        Logger("de.connect2x.trixnity.messenger.compose.view.roomlist.create.CreateNewChatNewSearchViewImpl")

    @Composable
    override fun create(createNewChatViewModel: CreateNewChatViewModel) {
        if (createNewChatViewModel is CreateNewChatNewSearchViewModel) {
            val i18n = DI.get<I18nView>()
            val isCreating by createNewChatViewModel.isCreating.collectAsState()
            val error by createNewChatViewModel.error.collectAsState()
            val errorDetails by createNewChatViewModel.errorDetails.collectAsState()
            Box(Modifier.fillMaxSize()) {
                Box {
                    Column {
                        Header(createNewChatViewModel::cancel, i18n.createNewChatTitle())
                        if (isCreating) {
                            ThemedProgressIndicator(
                                Modifier.fillMaxWidth(),
                                MaterialTheme.components.linearProgressIndicator,
                            )
                        }
                        SearchUsers(
                            createNewChatViewModel.userSearchViewModel,
                            { createNewChatViewModel.onUserClick(it) },
                        ) {
                            AddOrSearchGroup(createNewChatViewModel)
                            HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
                        }
                    }

                    ErrorDialog(error, errorDetails, onDismiss = { createNewChatViewModel.errorDismiss() })
                }
            }
        } else {
            log.warn { "Expected CreateNewChatNewSearchViewModel, but got ${createNewChatViewModel::class.simpleName}" }
        }
    }
}

package de.connect2x.trixnity.messenger.compose.view.roomlist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedFloatingActionButton

interface CreateNewChatOrGroupFloatingActionButton {
    @Composable
    fun create(onClick: () -> Unit)
}

@Composable
fun CreateNewChatOrGroupFloatingActionButton(onClick: () -> Unit) {
    DI.get<CreateNewChatOrGroupFloatingActionButton>().create(onClick)
}

class CreateNewChatOrGroupFloatingActionButtonImpl : CreateNewChatOrGroupFloatingActionButton {
    @Composable
    override fun create(onClick: () -> Unit) {
        val i18n = DI.get<I18nView>()
        ThemedFloatingActionButton(
            onClick = onClick,
            text = { Text(i18n.accountCreateNewRoom()) },
            icon = { Icon(Icons.AutoMirrored.Filled.Chat, i18n.accountCreateNewRoom()) },
        )
    }
}

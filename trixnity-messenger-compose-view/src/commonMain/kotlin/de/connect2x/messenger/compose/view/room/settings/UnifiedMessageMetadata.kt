package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageMetadataViewModel
import io.github.oshai.kotlinlogging.KotlinLogging


private val log = KotlinLogging.logger {}

@Composable
fun UnifiedMessageMetadata(viewModel: MessageMetadataViewModel) {
    // TODO
        Box(Modifier.fillMaxSize().background(Color.DarkGray)) {
            Column {
                Header(viewModel::back, "we need to go back")

                Text("metadata of ${viewModel.eventId}")
        }
    }
}

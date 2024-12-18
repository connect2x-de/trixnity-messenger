package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.room.settings.UnifiedMessageMetadataViewModel
import io.github.oshai.kotlinlogging.KotlinLogging


private val log = KotlinLogging.logger {}

@Composable
fun UnifiedMessageMetadata(viewModel: UnifiedMessageMetadataViewModel) {
    // TODO
    log.debug { "showing unified message metadata" }
    Text("metadata")
}

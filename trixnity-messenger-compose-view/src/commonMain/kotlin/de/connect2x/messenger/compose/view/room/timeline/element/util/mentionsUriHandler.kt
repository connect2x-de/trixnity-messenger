package de.connect2x.messenger.compose.view.room.timeline.element.util

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.UriHandler
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TextBasedViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.MessageMention
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

fun mentionsUriHandler(baseHandler: UriHandler, vm: TextBasedViewModel, mentions: List<MessageMention?>): State<UriHandler> {
    return mutableStateOf(object : UriHandler {
        override fun openUri(uri: String) {
            if (!uri.startsWith("timmy-data:")) {
                baseHandler.openUri(uri)
                return
            }

            val key = uri.removePrefix("timmy-data:").toInt()
            val mention = mentions[key]
            if (mention == null) {
                log.debug { mentions }
                log.error { "No data for mention $key not found" }
                return
            }

            // todo: implement and open user view (profile)
            // todo: implement and open event view
            vm.openMention(mention)
        }
    })
}

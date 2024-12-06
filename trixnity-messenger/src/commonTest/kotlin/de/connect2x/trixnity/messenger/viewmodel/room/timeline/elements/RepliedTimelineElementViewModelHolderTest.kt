package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import io.kotest.core.spec.style.ShouldSpec

class RepliedTimelineElementViewModelHolderTest : ShouldSpec() {
    init {
        context(TimelineElementHolderViewModel::showSender.name) {
            should("be false when room is direct") {
                TODO()
                cancelNeverEndingCoroutines()
            }
            should("be true when when room is not direct") {
                TODO()
                cancelNeverEndingCoroutines()
            }
        }
    }
}

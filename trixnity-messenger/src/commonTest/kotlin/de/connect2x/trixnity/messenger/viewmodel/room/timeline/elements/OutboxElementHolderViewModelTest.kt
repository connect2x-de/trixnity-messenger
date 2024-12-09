package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import io.kotest.core.spec.style.ShouldSpec

class OutboxElementHolderViewModelTest : ShouldSpec() {
    init {
        context(OutboxElementHolderViewModel::isFirstInUserSequence.name) {
            should("be true when last timeline event is not by us") {
                TODO()
                cancelNeverEndingCoroutines()
            }
            should("be true when last timeline event is by us") {
                TODO()
                cancelNeverEndingCoroutines()
            }
            should("ignore unsupported events") {
                TODO()
                cancelNeverEndingCoroutines()
            }
        }
        context(OutboxElementHolderViewModel::showSender.name) {
            should("be false when room is direct") {
                TODO()
                cancelNeverEndingCoroutines()
            }
            should("be false when not first in sequence") {
                TODO()
                cancelNeverEndingCoroutines()
            }
            should("be true when first in sequence") {
                TODO()
                cancelNeverEndingCoroutines()
            }
        }
    }
}

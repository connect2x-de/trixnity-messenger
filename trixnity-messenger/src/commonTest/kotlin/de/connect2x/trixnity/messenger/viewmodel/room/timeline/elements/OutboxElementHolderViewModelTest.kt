package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import io.kotest.core.spec.style.ShouldSpec

class OutboxElementHolderViewModelTest : ShouldSpec() {
    init {
        context(OutboxElementHolderViewModel::isFirstInUserSequence.name) {
            should("be true when last timeline event is not by us") {
                TODO()
            }
            should("be true when last timeline event is by us") {
                TODO()
            }
            should("ignore unsupported events") {
                TODO()
            }
        }
        context(OutboxElementHolderViewModel::showSender.name) {
            should("be false when room is direct") {
                TODO()
            }
            should("be false when not first in sequence") {
                TODO()
            }
            should("be true when first in sequence") {
                TODO()
            }
        }
    }
}

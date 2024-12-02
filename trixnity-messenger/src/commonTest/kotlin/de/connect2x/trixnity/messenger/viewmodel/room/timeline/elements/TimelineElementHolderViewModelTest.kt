package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import io.kotest.core.spec.style.ShouldSpec

class TimelineElementHolderViewModelTest : ShouldSpec() {

    init {
        context(TimelineElementHolderViewModel::isFirstInUserSequence.name) {
            should("be true when is first") {
                TODO()
            }
            should("false when not first") {
                TODO()
            }
            should("ignore unsupported events") {
                TODO()
            }
        }
        context(TimelineElementHolderViewModel::showSender.name) {
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
        context(TimelineElementHolderViewModel::isRead.name) {
            should("be true when read by third user") {
                TODO()
            }
            should("be true when message from third user after it") {
                TODO()
            }
            should("be false when not read by anyone and only the same user send message after it") {
                TODO()
            }
            should("be false when we read the event") {
                TODO()
            }
            should("be false when we send a message after it") {
                TODO()
            }
        }
        context(TimelineElementHolderViewModel::isReadBy.name) {
            should("contain users from read markers") {
                TODO()
            }
            should("not contain us or sender from read marker") {
                TODO()
            }
            should("contain sender of subsequent events") {
                TODO()
            }
            should("not contain us or sender from subsequent events") {
                TODO()
            }
        }
    }
}

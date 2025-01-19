package de.connect2x.trixnity.messenger.viewmodel.room.settings

import io.kotest.core.spec.style.ShouldSpec

class MessageMetadataViewModelTest : ShouldSpec() {

    init{
        should("return the message content"){TODO()}
        should("return the message attachment"){TODO()}
        should("return all reactions"){TODO()}
        should("return all readers"){TODO()}
        should("return the sent time and sender"){TODO()}
        should("return all message edits sorted by time"){TODO()}
        should("return if the message is by own user"){TODO()}
//        should(""){TODO()}

        context("read indication smoke tests"){
            should("see if and by whom a message has been read"){TODO()}
            should("see if a message hasn't been read"){TODO()}
        }

        context("message reactions smoke tests"){
            should("see if and by whom a message has received reactions from"){TODO()}
            should("see if a message hasn't received any reactions"){TODO()}
        }
    }
}

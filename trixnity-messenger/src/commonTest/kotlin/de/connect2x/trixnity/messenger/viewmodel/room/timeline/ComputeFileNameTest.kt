package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.ComputeFileName
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.ComputeFileNameImpl
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.FileInfo
import net.folivo.trixnity.core.model.events.m.room.ImageInfo
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased

class ComputeFileNameTest : ShouldSpec() {

    val cut: ComputeFileName = ComputeFileNameImpl(MatrixMessengerConfiguration())

    init {
        should("use filename when ${FileBased.File::class}") {
            cut(
                FileBased.File(
                    body = "",
                    fileName = "dino.pdf",
                    info = null,
                    url = "mxc://server.com/12345asdfg"
                )
            ) shouldBe "dino.pdf"
        }
        should("use encrypted file url") {
            cut(
                FileBased.File(
                    body = "",
                    info = FileInfo(mimeType = "application/pdf"),
                    file = EncryptedFile("mxc://example.org/encrypted123", EncryptedFile.JWK("key"), "iv", mapOf()),
                    url = "mxc://example.org/123" // ignore
                )
            ) shouldBe "Trixnity Messenger-bXhjOi8vZXhhbXBsZS5vcmcvZW5jcnlwdGVkMTIz.pdf"
        }
        should("use file url") {
            cut(
                FileBased.File(
                    body = "",
                    info = FileInfo(mimeType = "application/pdf"),
                    url = "mxc://example.org/123"
                )
            ) shouldBe "Trixnity Messenger-bXhjOi8vZXhhbXBsZS5vcmcvMTIz.pdf"
        }
        should("use not fail on unknown mimetype") {
            cut(
                FileBased.Image(
                    "image.png",
                    ImageInfo(mimeType = "image/unicorn"),
                    "mxc://server.com/12345asdfg"
                )
            ) shouldBe "Trixnity Messenger-bXhjOi8vc2VydmVyLmNvbS8xMjM0NWFzZGZn"
        }
    }
}
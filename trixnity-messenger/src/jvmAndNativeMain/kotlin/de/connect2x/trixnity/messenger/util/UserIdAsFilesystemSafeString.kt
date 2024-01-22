package de.connect2x.trixnity.messenger.util

import net.folivo.trixnity.core.model.UserId
import okio.ByteString.Companion.toByteString

fun UserId.asFilesystemSafeString() = full.encodeToByteArray().toByteString().sha256().base64Url()
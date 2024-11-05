package de.connect2x.messenger.android

import android.content.ClipData
import android.content.Context
import android.net.Uri
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.ManualFileDescriptor
import de.connect2x.trixnity.messenger.util.UriFileDescriptor
import io.ktor.http.*
import kotlinx.coroutines.flow.flowOf

sealed class SharedFile {
    abstract fun toFileDescriptor(context: Context, i18n: I18n): FileDescriptor

    data class SharedUri(val uri: Uri) : SharedFile() {
        override fun toFileDescriptor(context: Context, i18n: I18n) =
            UriFileDescriptor(context.applicationContext, uri, i18n)

    }

    data class SharedText(val text: CharSequence) : SharedFile() {
        override fun toFileDescriptor(context: Context, i18n: I18n) =
            ManualFileDescriptor(
                i18n.commonUnknown(),
                null,
                ContentType.Text.Plain,
                flowOf(text.toString().encodeToByteArray())
            )

    }

    companion object {
        fun of(item: ClipData.Item): SharedFile? =
            item.uri?.let(SharedFile::SharedUri) ?: item.text?.let(SharedFile::SharedText)
    }
}

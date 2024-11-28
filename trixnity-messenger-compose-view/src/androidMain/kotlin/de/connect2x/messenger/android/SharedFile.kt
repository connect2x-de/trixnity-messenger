package de.connect2x.messenger.android

import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.ManualFileDescriptor
import de.connect2x.trixnity.messenger.util.SharedData
import de.connect2x.trixnity.messenger.util.UriFileDescriptor
import io.ktor.http.*
import kotlinx.coroutines.flow.flowOf

abstract class SharedIntentData {
    abstract fun toSharedData(context: Context, i18n: I18n): SharedData?

    data class SharedText(val text: String) : SharedIntentData() {
        override fun toSharedData(context: Context, i18n: I18n): SharedData? = SharedData.PlainText(text)
    }

    data class SharedUrl(val url: String, val icon: Uri?) : SharedIntentData() {
        override fun toSharedData(context: Context, i18n: I18n): SharedData? {
            val icon = icon?.let { icon ->
                val isImage = context.contentResolver.getType(icon)?.let {
                    it.split("/").firstOrNull() == "image"
                }

                UriFileDescriptor(context, icon, i18n)
            }
            return SharedData.Url(url, icon)
        }
    }

    data class SharedItems(val items: List<ClipData.Item>) : SharedIntentData() {
        override fun toSharedData(context: Context, i18n: I18n): SharedData? {
            val files = items.mapNotNull { it.toFileDescriptor(context, i18n) }

            return when (files.size) {
                0 -> return null
                1 -> SharedData.SingleFile(files.first())
                else -> SharedData.MultipleFiles(files)
            }
        }
    }
}

private fun ClipData.Item.toFileDescriptor(context: Context, i18n: I18n): FileDescriptor? {
    return uri?.let { it.toFileDescriptor(context, i18n) } ?: text?.let { it.toFileDescriptor(context, i18n) }
}

private fun CharSequence.toFileDescriptor(context: Context, i18n: I18n): FileDescriptor? {
    val data = toString().encodeToByteArray()
    return ManualFileDescriptor(
        "${i18n.commonUnknown()}.txt",
        data.size.toLong(),
        ContentType.Text.Plain,
        flowOf(data)
    )
}

private fun Uri.toFileDescriptor(context: Context, i18n: I18n): FileDescriptor? {
    return UriFileDescriptor(context, this, i18n)
}

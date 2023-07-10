package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import android.net.Uri
import android.provider.MediaStore
import de.connect2x.trixnity.messenger.getContext
import de.connect2x.trixnity.messenger.viewmodel.util.MimeTypes
import okio.source
import java.io.IOException

actual fun getFileInfo(file: String): FileInfo {
    val uri = Uri.parse(file)
    with(getContext().contentResolver) {
        query(uri, null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val fileName =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME))
                    val fileSize =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE))
                    val source = openInputStream(uri)?.source() ?: throw IOException("Cannot read file '$uri'.")
                    return FileInfo(
                        fileName,
                        fileSize,
                        MimeTypes.guessByFileName(fileName),
                        source
                    )
                } else {
                    throw IOException("Cannot query media.")
                }
            } ?: throw IOException("Cannot query media.")
    }
}
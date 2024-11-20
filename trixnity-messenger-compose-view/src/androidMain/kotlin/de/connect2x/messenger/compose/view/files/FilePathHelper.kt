package de.connect2x.messenger.compose.view.files

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File

internal class FilePathHelper(private val context: Context) {
    fun getPath(uri: Uri?): String? {
        var uriToProcess = uri
        var selectionArgs: Array<String>? = null

        if (uriToProcess == null)
            return null


        if (DocumentsContract.isDocumentUri(context, uriToProcess)) {
            when {
                isExternalStorageDocument(uriToProcess) -> {
                    val docId = DocumentsContract.getDocumentId(uriToProcess)
                    val split = docId.split(":").toTypedArray()
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }

                isDownloadsDocument(uriToProcess) -> {
                    val id = DocumentsContract.getDocumentId(uriToProcess)
                    uriToProcess = ContentUris.withAppendedId(
                        Uri.parse("content://com.android.providers.downloads.documents/document"),
                        id.toLongOrNull() ?: 0
                    )
                }

                isMediaDocument(uriToProcess) -> {
                    val docId = DocumentsContract.getDocumentId(uriToProcess)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]

                    uriToProcess = when (type) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> MediaStore.Files.getContentUri("external")
                    }

                    if (uriToProcess != null) {
                        selectionArgs = arrayOf(split[1])
                    }
                }
            }
        }
        if ("content".equals(uriToProcess?.scheme, ignoreCase = true) && uriToProcess != null) {
            return getDataColumn(uri = uriToProcess, selectionArgs)

        } else if ("file".equals(uriToProcess?.scheme, ignoreCase = true)) {
            return uriToProcess?.path
        }
        return null
    }

    fun getAbsolutePath(uri: Uri): String {
        val directory = getDirectory(uri.path)
        return "${Environment.getExternalStorageDirectory()}/${directory}/${getFileName(uri) ?: ""}"
    }

    private fun getDataColumn(uri: Uri, selectionArgs: Array<String>?): String? {
        try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.DATA),
                FilePathIdentifiers.SELECTION,
                selectionArgs,
                null
            )
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        return cursor.getString(columnIndex)
                    }
                }
        } catch (e: SecurityException) {
            return null
        }

        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return FilePathIdentifiers.EXTERNAL_STORAGE_DOCUMENT_AUTHORITY == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return FilePathIdentifiers.DOWNLOADS_DOCUMENT_AUTHORITY == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return FilePathIdentifiers.MEDIA_DOCUMENT_AUTHORITY == uri.authority
    }

    private fun getFileName(uri: Uri): String? = when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> getContentFileName(uri)
        else -> uri.path?.let(::File)?.name
    }

    private fun getContentFileName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
        }
    }.getOrNull()

    private val directoryMap by lazy {
        mapOf(
            "music" to Environment.DIRECTORY_MUSIC,
            "podcasts" to Environment.DIRECTORY_PODCASTS,
            "ringtones" to Environment.DIRECTORY_RINGTONES,
            "alarms" to Environment.DIRECTORY_ALARMS,
            "notifications" to Environment.DIRECTORY_NOTIFICATIONS,
            "pictures" to Environment.DIRECTORY_PICTURES,
            "movies" to Environment.DIRECTORY_MOVIES,
            "downloads" to Environment.DIRECTORY_DOWNLOADS,
            "dcim" to Environment.DIRECTORY_DCIM,
            "documents" to Environment.DIRECTORY_DOCUMENTS,
        )
    }

    private fun getDirectory(uriPath: String?): String {
        val directoryKey = uriPath?.split("/")?.getOrNull(1) ?: ""
        return directoryMap.entries.firstOrNull { entry ->
            directoryKey.startsWith(entry.key)
        }?.value ?: Environment.DIRECTORY_DOWNLOADS
    }
}

object FilePathIdentifiers {
    const val EXTERNAL_STORAGE_DOCUMENT_AUTHORITY = "com.android.externalstorage.documents"
    const val DOWNLOADS_DOCUMENT_AUTHORITY = "com.android.providers.downloads.documents"
    const val MEDIA_DOCUMENT_AUTHORITY = "com.android.providers.media.documents"
    const val SELECTION = "_id=?"
}

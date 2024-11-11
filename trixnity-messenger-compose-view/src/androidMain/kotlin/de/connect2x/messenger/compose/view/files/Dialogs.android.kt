package de.connect2x.messenger.compose.view.files

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.UriFileDescriptor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.folivo.trixnity.utils.ByteArrayFlow
import java.io.IOException

private val log = KotlinLogging.logger { }

@Composable
actual fun SaveFileDialog(
    fileName: String,
    mimeType: String?,
    error: String?,
    downloadFile: (suspend (ByteArrayFlow) -> Unit) -> Unit,
    onCloseSaveFileDialog: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    var disposed by remember { mutableStateOf(false) } // only download file once
    if (error != null) AlertDialog(
        modifier = Modifier.defaultMinSize(minWidth = 400.dp),
        onDismissRequest = onCloseSaveFileDialog,
        title = { Text(i18n.fileDialogDownloadErrorSave()) },
        dismissButton = {
            Button(
                onCloseSaveFileDialog,
                Modifier.buttonPointerModifier()
            ) { Text(i18n.commonOk()) }
        },
        confirmButton = {},
        shape = RoundedCornerShape(8.dp),
        text = { Text(error) },
    )

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (disposed) return@LaunchedEffect
        disposed = true
        downloadFile { byteArrayFlow ->
            withContext(Dispatchers.IO) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(
                        MediaStore.MediaColumns.MIME_TYPE,
                        mimeType ?: ContentType.Application.OctetStream.toString()
                    )
                    if (Build.VERSION.SDK_INT >= 29) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                }

                var uri: Uri? = null

                runCatching {
                    with(context.contentResolver) {
                        if (Build.VERSION.SDK_INT < 29) {
                            val permission =
                                context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            if (permission == PackageManager.PERMISSION_DENIED) {
                                throw IOException("Insufficient permissions to save files.")
                            }
                            Uri.fromFile(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                    .resolve(fileName)
                            )
                        } else {
                            insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        }?.also {
                            uri = it // Keep uri reference so it can be removed on failure
                            openOutputStream(it)?.use { stream ->
                                byteArrayFlow.collect {
                                    stream.write(it)
                                }
                            }?.also {
                                uri?.let { uri1 ->
                                    val intent = Intent().apply {
                                        action = Intent.ACTION_VIEW
                                        type = mimeType.toString()
                                        uri = uri1
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (exc: ActivityNotFoundException) {
                                        println(exc)
                                    }
                                }
                            } ?: throw IOException("Failed to open output stream.")
                        } ?: throw IOException("Failed to create new MediaStore record.")
                    }
                }.getOrElse {
                    // Don't leave an orphan entry in the MediaStore
                    uri?.let {
                        context.contentResolver.delete(it, null, null)
                    }

                    throw it
                }

                onCloseSaveFileDialog()
            }
        }
    }
}

@Composable
actual fun LoadFileDialog(
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
    mode: LoadFileMode,
) {
    val context = LocalContext.current
    val i18n = DI.get<I18n>()
    val i18nView = DI.get<I18nView>()
    val visualMediaResult = remember { mutableStateOf<Uri?>(null) }
    val fileAttachmentResult = remember { mutableStateOf<Uri?>(null) }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        visualMediaResult.value = it
    }

    val fileAttachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        fileAttachmentResult.value = it
    }

    if (mode == LoadFileMode.Picture) {
        LaunchedEffect(mediaLauncher) {
            mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    } else {
        val offsetY = with(LocalDensity.current) { -(98.dp).roundToPx() }
        Popup(
            alignment = Alignment.CenterEnd,
            offset = IntOffset(0, offsetY),
            onDismissRequest = onCloseLoadFileDialog,
        ) {
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    UploadButton(
                        imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = i18nView.fileDialogLoadFileButton(),
                        iconButtonClick = {
                            fileAttachmentLauncher.launch((arrayOf("*/*")))
                        }
                    )
                    UploadButton(
                        Icons.Default.Image,
                        i18nView.fileDialogLoadImageButton(),
                        iconButtonClick = {
                            mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        }

                    )
                }
            }
        }
    }
    fileAttachmentResult.value?.let { fileAttachmentUri ->
        onFileSelect(UriFileDescriptor(context = context, fileUri = fileAttachmentUri, i18n = i18n))
        onCloseLoadFileDialog()
    }
    visualMediaResult.value?.let { uri ->
        onFileSelect(UriFileDescriptor(context, fileUri = uri, i18n = i18n))
        onCloseLoadFileDialog()
    }
}

@Composable
private fun UploadButton(
    imageVector: ImageVector,
    contentDescription: String,
    iconButtonClick: () -> Unit,
) {
    IconButton(
        onClick = iconButtonClick,
        Modifier
            .size(60.dp)
            .clip(CircleShape)
    ) {
        Box(Modifier.fillMaxSize()) {
            Icon(
                imageVector,
                contentDescription,
                Modifier
                    .align(Alignment.Center)
                    .size(24.dp),
            )
        }
    }
}

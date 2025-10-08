package de.connect2x.trixnity.messenger.compose.view.files

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.FilePickerType
import de.connect2x.trixnity.messenger.compose.view.common.FilePickerType.ATTACHMENT_FILE
import de.connect2x.trixnity.messenger.compose.view.common.FilePickerType.IMAGE_AND_VIDEO_FILE
import de.connect2x.trixnity.messenger.compose.view.common.FilePickerType.IMAGE_FILE
import de.connect2x.trixnity.messenger.compose.view.common.FilePickerType.PHOTO_CAPTURE
import de.connect2x.trixnity.messenger.compose.view.common.FilePickerType.VIDEO_CAPTURE
import de.connect2x.trixnity.messenger.compose.view.common.FilePickerTypeSelection
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.UriFileDescriptor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media.PlatformMedia
import java.io.File
import java.io.IOException


private val log = KotlinLogging.logger {}

@Composable
actual fun SaveFileDialog(
    fileName: String,
    mimeType: String?,
    error: String?,
    downloadFile: (suspend (PlatformMedia) -> Unit) -> Unit,
    onCloseSaveFileDialog: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val context = LocalContext.current
    val hasError = error?.isNotBlank() == true
    if (hasError) {
        ThemedModalDialog(onCloseSaveFileDialog) {
            ModalDialogHeader {
                Text(i18n.fileDialogDownloadErrorSave())
            }
            ModalDialogContent {
                Text(error)
            }
            ModalDialogFooter {
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    onClick = onCloseSaveFileDialog,
                ) {
                    Text(i18n.actionOk())
                }
            }
        }
    }
    LaunchedEffect(hasError) {
        if (!hasError) downloadFile { byteArrayFlow ->
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
                                        log.error(exc) { "intent could not be called" }
                                    }
                                }
                            } ?: throw IOException("Failed to open output stream.")
                        } ?: throw IOException("Failed to create new MediaStore record.")
                    }
                }.getOrElse {
                    // Don't leave an orphan entry in the MediaStore
                    uri?.let { uri ->
                        context.contentResolver.delete(uri, null, null)
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
    availableTypes: List<FilePickerType>,
    onFileSelect: (FileDescriptor) -> Unit,
    onCloseLoadFileDialog: () -> Unit,
) {
    val context = LocalContext.current
    val i18n = DI.get<I18n>()
    val visualMediaResult = remember { mutableStateOf<Uri?>(null) }
    val fileAttachmentResult = remember { mutableStateOf<Uri?>(null) }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        visualMediaResult.value = it
    }
    val fileAttachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        fileAttachmentResult.value = it
    }
    var showCameraDialog by remember { mutableStateOf<CameraDialogCapturingMode?>(null) }

    fun onPickerSelected(pickerType: FilePickerType) {
        when (pickerType) {
            ATTACHMENT_FILE -> fileAttachmentLauncher.launch((arrayOf("*/*")))

            IMAGE_FILE -> mediaLauncher
                .launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

            IMAGE_AND_VIDEO_FILE -> mediaLauncher
                .launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))

            PHOTO_CAPTURE -> showCameraDialog = CameraDialogCapturingMode.PHOTO

            VIDEO_CAPTURE -> showCameraDialog = CameraDialogCapturingMode.VIDEO
        }
    }
    if (showCameraDialog != null) {
        showCameraDialog?.let { mode ->
            CameraDialog(
                mode,
                { visualMediaResult.value = it },
                onCloseLoadFileDialog,
            )
        }
    } else if (availableTypes.size == 1) {
        LaunchedEffect(mediaLauncher) {
            onPickerSelected(availableTypes.first())
        }
    } else {
        FilePickerTypeSelection(
            availableTypes,
            { onPickerSelected(it) },
            onCloseLoadFileDialog,
        )
    }
    fileAttachmentResult.value?.let { uri ->
        onFileSelect(UriFileDescriptor(context, uri, i18n))
        onCloseLoadFileDialog()
    }
    visualMediaResult.value?.let { uri ->
        onFileSelect(UriFileDescriptor(context, uri, i18n))
        onCloseLoadFileDialog()
    }
}

actual fun filterFilePickerOptionsByAvailability(
    vararg availablePickerTypes: FilePickerType,
): List<FilePickerType> = availablePickerTypes.toList() // Allow all picker types.

// https://medium.com/@dheerubhadoria/capturing-images-from-camera-in-android-with-jetpack-compose-a-step-by-step-guide-64cd7f52e5de
@Composable
fun CameraDialog(
    mode: CameraDialogCapturingMode,
    onCapture: (Uri) -> Unit,
    onCloseCameraDialog: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val context = LocalContext.current
    var showPermissionAlert by remember { mutableStateOf(false) }
    var isPermissionGranted by remember { mutableStateOf(false) }
    val tempFileName = "camera_capture" + when (mode) {
        CameraDialogCapturingMode.PHOTO -> ".jpg"
        CameraDialogCapturingMode.VIDEO -> ".mp4"
    }
    val tempFile = File(context.cacheDir, tempFileName)
    val tempUri = FileProvider.getUriForFile(
        context, "${context.packageName}.provider", tempFile,
    )

    val cameraLauncher = when (mode) {
        CameraDialogCapturingMode.PHOTO -> rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isImageSaved ->
            if (isImageSaved) {
                onCapture(tempUri)
                // TODO consider removing the temp file
            } else {
                onCloseCameraDialog()
            }
        }

        CameraDialogCapturingMode.VIDEO -> rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { isImageSaved ->
            if (isImageSaved) {
                onCapture(tempUri)
                // TODO consider removing the temp file
            } else {
                onCloseCameraDialog()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) isPermissionGranted = true
        else showPermissionAlert = true
    }

    if (showPermissionAlert) {
        ThemedModalDialog(onCloseCameraDialog) {
            ModalDialogContent {
                Text(i18n.cameraDialogAlertNoPermission())
            }
            ModalDialogFooter {
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    onClick = onCloseCameraDialog,
                ) {
                    Text(i18n.actionOk())
                }
            }
        }
    }

    LaunchedEffect(tempUri, isPermissionGranted) {
        if (showPermissionAlert.not()) tempUri?.let {
            val permissionCheckResult =
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(it)
            } else { // Request a permission.
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

enum class CameraDialogCapturingMode {
    PHOTO, VIDEO,
}

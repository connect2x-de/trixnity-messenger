package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector


interface MessengerIcons {
    val typeAudio: ImageVector
    val typeVideo: ImageVector
    val typeImage: ImageVector
    val typeFile: ImageVector
    val attachFile: ImageVector
    val attachImage: ImageVector
    val recordPhoto: ImageVector
    val recordVideo: ImageVector
}

object DefaultMessengerIcons : MessengerIcons {
    override val typeAudio: ImageVector = Icons.Default.MusicNote
    override val typeVideo: ImageVector = Icons.Default.Movie
    override val typeImage: ImageVector = Icons.Default.Image
    override val typeFile: ImageVector = Icons.Default.AttachFile
    override val attachFile: ImageVector = Icons.AutoMirrored.Filled.InsertDriveFile
    override val attachImage: ImageVector = Icons.Default.Image
    override val recordPhoto: ImageVector = Icons.Default.PhotoCamera
    override val recordVideo: ImageVector = Icons.Default.Videocam
}

internal val MessengerIconsProvider =
    staticCompositionLocalOf<MessengerIcons> { error("compositionLocal not defined") }

val MaterialTheme.messengerIcons: MessengerIcons
    @Composable
    @ReadOnlyComposable
    get() = MessengerIconsProvider.current

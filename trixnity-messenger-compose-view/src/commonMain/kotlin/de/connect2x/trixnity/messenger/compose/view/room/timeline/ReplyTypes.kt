package de.connect2x.trixnity.messenger.compose.view.room.timeline

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.ClickableText
import de.connect2x.trixnity.messenger.compose.view.common.FileName
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.dp
import de.connect2x.trixnity.messenger.compose.view.theme.messengerIcons

interface TextReplyView {
    @Composable
    fun create(text: String, maxLines: Int)
}

@Composable
fun TextReply(text: String, maxLines: Int = 4) {
    with(DI.get<TextReplyView>()) { create(text, maxLines) }
}

class TextReplyViewImpl : TextReplyView {
    @Composable
    override fun create(text: String, maxLines: Int) {
        Text(
            text = text,
            fontStyle = FontStyle.Italic,
            style = MaterialTheme.typography.bodySmall,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

interface ImageReplyView {
    @Composable
    fun create(imageBitmap: ImageBitmap)
}

@Composable
fun ImageReply(imageBitmap: ImageBitmap) {
    with(DI.get<ImageReplyView>()) { create(imageBitmap) }
}

class ImageReplyViewImpl : ImageReplyView {
    @Composable
    override fun create(imageBitmap: ImageBitmap) {
        Image(
            imageBitmap,
            "",
            Modifier.heightIn(max = 100.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

interface ImageReplyDefaultView {
    @Composable
    fun create(fileName: String)
}

@Composable
fun ImageReplyDefault(fileName: String) {
    with(DI.get<ImageReplyDefaultView>()) { create(fileName) }
}

class ImageReplyDefaultViewImpl : ImageReplyDefaultView {
    @Composable
    override fun create(fileName: String) {
        val i18n = DI.get<I18nView>()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                MaterialTheme.messengerIcons.typeImage,
                i18n.commonImage(),
                modifier = Modifier.size(MaterialTheme.typography.bodySmall.dp)
            )
            FileName(fileName)
        }
    }
}

interface VideoReplyView {
    @Composable
    fun create(imageBitmap: ImageBitmap)
}

@Composable
fun VideoReply(imageBitmap: ImageBitmap) {
    with(DI.get<VideoReplyView>()) { create(imageBitmap) }
}

class VideoReplyViewImpl : VideoReplyView {
    @Composable
    override fun create(imageBitmap: ImageBitmap) {
        val i18n = DI.get<I18nView>()
        Box {
            Image(
                imageBitmap,
                "",
                Modifier.heightIn(max = 100.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit,
            )
            Icon(
                MaterialTheme.messengerIcons.typeVideo,
                i18n.commonVideo(),
                Modifier.size(25.dp).align(Alignment.Center),
                tint = Color.DarkGray,
            )
        }
    }
}

interface VideoReplyDefaultView {
    @Composable
    fun create(fileName: String)
}

@Composable
fun VideoReplyDefault(fileName: String) {
    with(DI.get<VideoReplyDefaultView>()) { create(fileName) }
}

class VideoReplyDefaultViewImpl : VideoReplyDefaultView {
    @Composable
    override fun create(fileName: String) {

        val i18n = DI.get<I18nView>()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                MaterialTheme.messengerIcons.typeVideo,
                i18n.commonVideo(),
                modifier = Modifier.size(MaterialTheme.typography.bodySmall.dp),
            )
            FileName(fileName)
        }
    }
}

interface AudioReplyView {
    @Composable
    fun create(fileName: String)
}

@Composable
fun AudioReply(fileName: String) {
    with(DI.get<AudioReplyView>()) { create(fileName) }
}

class AudioReplyViewImpl : AudioReplyView {
    @Composable
    override fun create(fileName: String) {

        val i18n = DI.get<I18nView>()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                MaterialTheme.messengerIcons.typeAudio,
                i18n.commonAudio(),
                Modifier.size(30.dp),
                tint = Color.DarkGray,
            )
            FileName(fileName)
        }
    }
}

interface FileReplyView {
    @Composable
    fun create(fileName: String)
}

@Composable
fun FileReply(fileName: String) {
    with(DI.get<FileReplyView>()) { create(fileName) }
}

class FileReplyViewImpl : FileReplyView {
    @Composable
    override fun create(fileName: String) {

        val i18n = DI.get<I18nView>()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Attachment,
                i18n.commonAttachment(),
                Modifier.size(30.dp),
                tint = Color.DarkGray,
            )
            FileName(fileName)
        }
    }
}

interface LocationReplyView {
    @Composable
    fun create(name: String, location: String)
}

@Composable
fun LocationReply(name: String, location: String) {
    with(DI.get<LocationReplyView>()) { create(name, location) }
}

class LocationReplyViewImpl : LocationReplyView {
    @Composable
    override fun create(name: String, location: String) {
        val i18n = DI.get<I18nView>()
        val (geoUrl, pos) = location
            .removePrefix("geo:").substringBefore(";").split(",")
            .let { (lat, lon) ->
                "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon" to Pair(lat, lon)
            }

        val uriHandler = LocalUriHandler.current
        ClickableText(
            text = AnnotatedString(i18n.locationClickText(pos)),
            onClick = {
                uriHandler.openUri(geoUrl)
            },
            onLongPress = {},
            style = MaterialTheme.typography.bodySmall
        )
    }
}

interface UnknownReplyView {
    @Composable
    fun create()
}

@Composable
fun UnknownReply() {
    with(DI.get<UnknownReplyView>()) { create() }
}

class UnknownReplyViewImpl : UnknownReplyView {
    @Composable
    override fun create() {
        val i18n = DI.get<I18nView>()
        Icon(
            Icons.Default.QuestionMark,
            i18n.commonUnknown(),
            Modifier.size(30.dp),
            tint = Color.DarkGray,
        )
    }
}

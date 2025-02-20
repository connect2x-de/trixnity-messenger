package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementHolder
import de.connect2x.messenger.compose.view.theme.DefaultSizes
import de.connect2x.messenger.compose.view.theme.SystemDensity
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewRepliedTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RepliedTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AppearanceSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.EventReactions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import kotlin.math.round

interface AppearanceSettingsSizeView {
    @Composable
    fun ColumnScope.create(appearanceSettingsViewModel: AppearanceSettingsViewModel)
}

@Composable
fun ColumnScope.AppearanceSettingsSize(appearanceSettingsViewModel: AppearanceSettingsViewModel) {
    with(DI.get<AppearanceSettingsSizeView>()) { create(appearanceSettingsViewModel) }
}

class AppearanceSettingsSizeViewImpl : AppearanceSettingsSizeView {
    @Composable
    override fun ColumnScope.create(appearanceSettingsViewModel: AppearanceSettingsViewModel) {
        val i18n = DI.get<I18nView>()
        val defaultSizes = DI.get<DefaultSizes>()
        val applySystemSizes by appearanceSettingsViewModel.applySystemSizes.collectAsState()

        // Font size
        val fontSize = appearanceSettingsViewModel.fontSize.collectAsState().value ?: defaultSizes.fontSize
        var newFontSize by remember { mutableStateOf(-1F) }
        fun getNewFontSize(): Float = if (newFontSize != -1F && newFontSize != fontSize) newFontSize else fontSize

        // Display size
        val displaySize = appearanceSettingsViewModel.displaySize.collectAsState().value ?: defaultSizes.displaySize
        var newDisplaySize by remember { mutableStateOf(-1F) }
        fun getNewDisplaySize(): Float = if (newDisplaySize != -1F && newDisplaySize != displaySize)
            newDisplaySize else displaySize

        // Preview
        val systemDensity = SystemDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(
                systemDensity.density * getNewDisplaySize(),
                systemDensity.fontScale * getNewFontSize()
            )
        ) {
            Column(Modifier.padding(end = 10.dp).fillMaxWidth(1.0f).aspectRatio(1.0f)) {
                TimelineElementHolder(PreviewTimelineElementViewModel1())
                TimelineElementHolder(PreviewTimelineElementViewModel2())
            }
        }
        Spacer(Modifier.height(30.dp))
        HorizontalDivider()

        // Settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = applySystemSizes,
                onCheckedChange = {
                    appearanceSettingsViewModel.toggleApplySystemSizes()
                    newFontSize = -1F
                    newDisplaySize = -1F
                    appearanceSettingsViewModel.setDisplaySize(defaultSizes.displaySize)
                    appearanceSettingsViewModel.setFontSize(defaultSizes.fontSize)
                }
            )
            Text(i18n.appearanceSizesApplySystemHeading())
        }

        Column(Modifier.padding(16.dp).fillMaxSize()) {
            Row(Modifier.padding(2.dp)) {
                Text(
                    text = "${i18n.appearanceFontSizeHeading()}:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.weight(1.0f))
                Text(
                    text = "${round(getNewFontSize() * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Slider(
                value = getNewFontSize(),
                onValueChange = { newFontSize = it },
                steps = 0,
                valueRange = defaultSizes.minFontSize..defaultSizes.maxFontSize,
                enabled = !applySystemSizes
            )
            Spacer(Modifier.height(5.dp))

            Row(Modifier.padding(2.dp)) {
                Text(
                    text = "${i18n.appearanceDisplaySizeHeading()}:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.weight(1.0f))
                Text(
                    text = "${round(getNewDisplaySize() * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Slider(
                value = getNewDisplaySize(),
                onValueChange = { newDisplaySize = it },
                valueRange = defaultSizes.minDisplaySize..defaultSizes.maxDisplaySize,
                steps = 5,
                enabled = !applySystemSizes
            )

            Spacer(Modifier.height(10.dp))

            Button(
                enabled = !applySystemSizes,
                onClick = {
                    appearanceSettingsViewModel.setDisplaySize(getNewDisplaySize())
                    appearanceSettingsViewModel.setFontSize(getNewFontSize())
                }
            ) {
                Text(i18n.appearanceSizesApply())
            }
        }
    }
}

private class PreviewTimelineElementViewModel1 : TimelineElementHolderViewModel {
    override val roomId: RoomId = RoomId("!room")
    override val eventId: EventId = EventId("\$1:localhost")
    override val key: String = eventId.full
    override val element: MutableStateFlow<TimelineElementViewModel<*>?> =
        MutableStateFlow(object : RoomMessageTimelineElementViewModel.TextBased.Text {
            override val body: String = "Hello everyone!"
            override val formattedBody: String = "Hello <b/>everyone!"
            override val mentionsInBody: Map<IntRange, MutableStateFlow<TimelineElementMention>> = mapOf()
            override val mentionsInFormattedBody: Map<IntRange, MutableStateFlow<TimelineElementMention>> = mapOf()
            override fun openMention(mention: TimelineElementMention) {}
        })
    override val isFirstInUserSequence: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val formattedTime: String = "12:12"
    override val formattedDate: String = "21.11.2024"
    override val isByMe: Boolean = true
    override val sender: MutableStateFlow<UserInfoElement?> = MutableStateFlow(null)
    override val showSender: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val showBigGapBefore: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val isReply: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val repliedElement: MutableStateFlow<RepliedTimelineElementHolderViewModel?> =
        MutableStateFlow(PreviewRepliedTimelineElementViewModel())
    override val showUnreadMarker: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorAfter: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isRead: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReadBy: MutableStateFlow<List<UserInfoElement>> = MutableStateFlow(listOf())
    override val canBeReactedTo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReplaced: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeEdited: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeRedacted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val canBeRepliedTo: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canBeReported: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reactions: MutableStateFlow<EventReactions> = MutableStateFlow(EventReactions(setOf()))
    override val highlight: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun replace() {}
    override fun endReplace() {}
    override fun redact() {}
    override fun reply() {}
    override fun endReply() {}
    override fun report() {}
    override fun addReaction(reaction: String) {}
    override fun removeReaction(reaction: String) {}
}

private class PreviewTimelineElementViewModel2 : TimelineElementHolderViewModel {
    override val roomId: RoomId = RoomId("!room")
    override val eventId: EventId = EventId("\$2:localhost")
    override val key: String = eventId.full
    override val element: MutableStateFlow<TimelineElementViewModel<*>?> =
        MutableStateFlow(object : RoomMessageTimelineElementViewModel.TextBased.Text {
            override val body: String = "Hello!"
            override val formattedBody: String = "Hello!"
            override val mentionsInBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
            override val mentionsInFormattedBody: Map<IntRange, StateFlow<TimelineElementMention>> = mapOf()
            override fun openMention(mention: TimelineElementMention) {}
        })
    override val isFirstInUserSequence: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val formattedTime: String = "12:24"
    override val formattedDate: String = "21.11.2024"
    override val isByMe: Boolean = false
    override val sender: MutableStateFlow<UserInfoElement?> =
        MutableStateFlow(UserInfoElement(UserId("bob", "server"), "Bob", "B"))
    override val showSender: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val showBigGapBefore: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val repliedElement: MutableStateFlow<RepliedTimelineElementHolderViewModel?> = MutableStateFlow(null)
    override val showUnreadMarker: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorBefore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showLoadingIndicatorAfter: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isRead: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReadBy: MutableStateFlow<List<UserInfoElement>> = MutableStateFlow(listOf())
    override val canBeReactedTo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReplaced: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isReply: MutableStateFlow<Boolean?> = MutableStateFlow(false)
    override val canBeEdited: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canBeRedacted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val redactionError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val canBeRepliedTo: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canBeReported: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val reactions: MutableStateFlow<EventReactions> = MutableStateFlow(EventReactions(setOf()))
    override val highlight: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun replace() {}
    override fun endReplace() {}
    override fun redact() {}
    override fun reply() {}
    override fun endReply() {}
    override fun report() {}
    override fun addReaction(reaction: String) {}
    override fun removeReaction(reaction: String) {}
}

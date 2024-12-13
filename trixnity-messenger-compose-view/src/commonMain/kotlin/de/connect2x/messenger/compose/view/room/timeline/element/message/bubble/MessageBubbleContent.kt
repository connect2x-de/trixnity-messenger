package de.connect2x.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.ReadMarker
import de.connect2x.messenger.compose.view.room.timeline.element.util.asOutboxElementHolder
import de.connect2x.messenger.compose.view.room.timeline.element.util.asTimelineElementHolder
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel

@Composable
fun MessageBubbleContent(
    holder: BaseTimelineElementHolderViewModel,
    needsMaxWidth: Boolean,
    showActionMenu: () -> Unit,
    content: @Composable (showActionMenu: () -> Unit) -> Unit,
) {
    val i18n = DI.get<I18nView>()

    val highlight = holder.asTimelineElementHolder()?.highlight?.collectAsState()?.value == true
    val sendError = holder.asOutboxElementHolder()?.sendError?.collectAsState()?.value
    val showSender = holder.showSender.collectAsState().value == true
    val isReplaced = holder.asTimelineElementHolder()?.isReplaced?.collectAsState()?.value
    val hasRepliedElement = holder.isReply.collectAsState().value != null

    val highlighted = if (highlight) Modifier.border(
        width = 3.dp,
        color = MaterialTheme.colorScheme.outline,
        shape = RoundedCornerShape(8.dp),
    ) else Modifier
    Row(Modifier.fillMaxWidth()) {
        if (sendError != null) {
            Icon(
                Icons.Default.Warning, "send error",
                Modifier.padding(5.dp).align(Alignment.CenterVertically)
            )
        }
        Column(
            Modifier
                .padding(0.dp)
                .weight(1.0f, fill = needsMaxWidth.not())
                .then(highlighted)
        ) {
            if (showSender) {
                Box(
                    Modifier
                        .padding(start = 10.dp, end = 10.dp, top = 5.dp)
                ) {
                    val sender = holder.sender.collectAsState().value
                    if (sender != null) {
                        Text(
                            text = sender.name,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.messengerColors.getUserColor(
                                    sender.userId
                                )
                            ),
                        )
                    } else {
                        // TODO needs placeholder AND does not support offline szenario. This is a general problem of
                        //  UserInfoElement, which waits for the image to load.
                        CircularProgressIndicator(Modifier.size(20.dp))
                    }
                }
            }

            RepliedElement(holder)

            // the hasRepliedElement is needed to avoid layouting already layouted elements which leads to this: "Asking for intrinsic measurements of SubcomposeLayout layouts is not supported."
            if (needsMaxWidth || hasRepliedElement) {
                content(showActionMenu)
                Row(
                    Modifier.align(Alignment.End).padding(5.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    holder.formattedTime.let {
                        Box(
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.paddingFromBaseline(0.dp),
                                maxLines = 1,
                            )
                        }
                    }
                    ReadMarker(holder)
                }
            } else {
                Layout(
                    content = {
                        content(showActionMenu)
                        holder.formattedTime.let {
                            Row(
                                modifier = Modifier.padding(
                                    start = 5.dp,
                                    end = 5.dp,
                                    bottom = 5.dp
                                ),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                if (isReplaced == true)
                                    Text(
                                        i18n.messageBubbleEdited(),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.paddingFromBaseline(0.dp)
                                            .padding(end = 2.dp),
                                        maxLines = 1,
                                    )
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.paddingFromBaseline(0.dp),
                                    maxLines = 1,
                                )
                                ReadMarker(holder)
                            }
                        }
                    },
                    measurePolicy = object : MeasurePolicy {
                        val spacing = 10.dp
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints
                        ): MeasureResult {
                            val spacing = spacing.roundToPx()
                            val message = measurables[0].measure(constraints)
                            val date = measurables.getOrNull(1)?.measure(constraints)

                            return date?.let {
                                if (message.width + spacing + date.width < constraints.maxWidth) {
                                    // add extra padding to bottom that is missing otherwise
                                    val height = message.height + 10.dp.roundToPx()
                                    layout(
                                        width = message.width + spacing + date.width,
                                        height = height,
                                    ) {
                                        message.place(0, 0)
                                        date.place(
                                            message.width + spacing,
                                            height - date.height
                                        )
                                    }
                                } else {
                                    layout(
                                        width = constraints.maxWidth,
                                        height = message.height + date.height
                                    ) {
                                        message.place(0, 0)
                                        date.place(
                                            constraints.maxWidth - date.width,
                                            message.height
                                        )
                                    }
                                }
                            } ?: layout(
                                message.width,
                                message.height
                            ) {
                                message.place(0, 0)
                            }
                        }

                        override fun IntrinsicMeasureScope.minIntrinsicWidth(
                            measurables: List<IntrinsicMeasurable>,
                            height: Int
                        ): Int {
                            val spacing = (spacing + 1.dp).roundToPx() // to be _just_ big enough for one line
                            return measurables.sumOf { it.minIntrinsicWidth(height) } + spacing
                        }

                        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                            measurables: List<IntrinsicMeasurable>,
                            height: Int
                        ): Int {
                            val spacing = (spacing + 1.dp).roundToPx() // to be _just_ big enough for one line
                            return measurables.sumOf { it.maxIntrinsicWidth(height) } + spacing
                        }

                        override fun IntrinsicMeasureScope.minIntrinsicHeight(
                            measurables: List<IntrinsicMeasurable>,
                            width: Int
                        ): Int {
                            return measurables.sumOf { it.minIntrinsicHeight(width) }
                        }

                        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                            measurables: List<IntrinsicMeasurable>,
                            width: Int
                        ): Int {
                            return measurables.sumOf { it.maxIntrinsicHeight(width) }
                        }
                    })
            }

            if (sendError != null) {
                Box(Modifier.padding(start = 10.dp, end = 10.dp)) {
                    Text(
                        text = sendError,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

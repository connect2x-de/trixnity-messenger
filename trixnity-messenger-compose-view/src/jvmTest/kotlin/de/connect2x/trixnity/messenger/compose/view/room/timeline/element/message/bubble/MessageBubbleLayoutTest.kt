package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MessageBubbleLayoutTest {
    @Test
    fun longSenderKeepsTimestampAtRightEdgeWithoutIntrinsicMeasurements() = runComposeUiTest {
        setContent {
            Box(Modifier.size(320.dp)) {
                Layout(
                    modifier = Modifier.testTag("bubble"),
                    content = {
                        Box(Modifier.layoutId(MessageBubbleMeasurePolicy.LayoutId.SENDER).size(180.dp, 20.dp))
                        // Rich text uses BoxWithConstraints, which rejects intrinsic size queries.
                        BoxWithConstraints(Modifier.layoutId(MessageBubbleMeasurePolicy.LayoutId.MESSAGE)) {
                            Box(Modifier.size(40.dp, 20.dp).testTag("message"))
                        }
                        Box(
                            Modifier.layoutId(MessageBubbleMeasurePolicy.LayoutId.TIMESTAMP)
                                .size(30.dp, 10.dp)
                                .testTag("timestamp")
                        )
                    },
                    measurePolicy = MessageBubbleMeasurePolicy,
                )
            }
        }
        val bubble = onNodeWithTag("bubble").fetchSemanticsNode().boundsInRoot
        val message = onNodeWithTag("message").fetchSemanticsNode().boundsInRoot
        val timestamp = onNodeWithTag("timestamp").fetchSemanticsNode().boundsInRoot
        assertEquals(bubble.right, timestamp.right, 0.5f)
        assertEquals(bubble.left, message.left, 0.5f)
        assertEquals(message.height, message.top - bubble.top, 0.5f)
        assertEquals(message.width * 4.5f, bubble.width, 0.5f)
    }
}

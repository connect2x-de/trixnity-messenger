package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.modifier.RovingFocusDirection
import de.connect2x.trixnity.messenger.compose.view.common.modifier.customClickable
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.trixnity.messenger.compose.view.theme.messengerFocusIndicator


@Composable
fun EmojiSelector(
    modifier: Modifier = Modifier,
    onTextAdded: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var focusedItem by remember { mutableStateOf(emojis.firstOrNull()) }

    val singletonFocusRequester: FocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier) {
        Row(modifier = Modifier.verticalScroll(scrollState).align(Alignment.Center)) {
            BoxWithConstraints(Modifier.padding(12.dp)) {
                EmojiPicker(
                    Modifier.onKeyEvent { event ->
                        when (event.key) {
                            Key.Escape -> {
                                if (event.type == KeyEventType.KeyDown) {
                                    onDismiss()
                                }
                                true
                            }

                            else -> false
                        }
                    }.rovingFocusContainer(
                        direction = RovingFocusDirection.Grid,
                        coroutineScope = coroutineScope,
                        singletonFocusRequester = singletonFocusRequester,
                        isFocusedItemVisible = { false },
                        scrollToFocusedItem = { scrollState.scrollTo(0) }
                    )
                ) {
                    emojis.forEachIndexed { index, emoji ->
                        EmojiButton(
                            label = emoji,
                            modifier = Modifier.rovingFocusItem(
                                isFocused = focusedItem == emoji,
                                onFocus = { focusedItem = emoji },
                                singletonFocusRequester = singletonFocusRequester,
                                hasRequester = { index == 0 }
                            ),
                            onClick = { onTextAdded(emoji) },
                        )
                    }
                }
            }
        }
        VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState)
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EmojiButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .customClickable(
                indication = ripple(bounded = false, radius = 24.dp),
                onClick = onClick,
                onFocus = Modifier.border(
                    BorderStroke(
                        width = MaterialTheme.messengerFocusIndicator.borderWidth,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    shape = CircleShape,
                ),
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = LocalTextStyle.current.copy(fontSize = 18.sp, textAlign = TextAlign.Center)
        )
    }
}

/**
 * Custom emoji picker which arranges items similar to [androidx.compose.foundation.layout.Arrangement.SpaceBetween] with the exception of the last line, which is left-aligned
 * and positioned with the same spacing as previous lines
 * @param modifier The modifier to use on the layout
 * @param content The emojis to arrange within the picker. For correct arrangement they are required to all have the same size
 */
@Composable
private fun EmojiPicker(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        if (measurables.isNotEmpty()) {
            val placeables = measurables.map {
                it.measure(constraints)
            }
            val itemSize = IntSize(placeables.first().width, placeables.first().height)
            val emojisPerLine = constraints.maxWidth / itemSize.width

            val consumedSize = emojisPerLine * itemSize.width
            val noOfGaps = emojisPerLine - 1
            val gapSize = (constraints.maxWidth - consumedSize).toFloat() / noOfGaps
            val numOfRows =
                (placeables.size / emojisPerLine).run { if (placeables.size % emojisPerLine != 0) this.inc() else this }
            val height = numOfRows * itemSize.height
            layout(constraints.maxWidth, height) {
                var yPos = 0f
                var xPos = 0f
                var emojiIndex = 0
                placeables.forEach {
                    it.placeRelative(xPos.fastRoundToInt(), yPos.fastRoundToInt())

                    xPos += it.width + gapSize
                    emojiIndex++
                    if (emojiIndex == emojisPerLine) {
                        emojiIndex = 0
                        xPos = 0F
                        yPos += it.height
                    }
                }
            }
        } else layout(0, 0) {}
    }
}

private val emojis = listOf(
    "\ud83d\ude00", // Grinning Face
    "\ud83d\ude01", // Grinning Face With Smiling Eyes
    "\ud83d\ude02", // Face With Tears of Joy
    "\ud83d\ude03", // Smiling Face With Open Mouth
    "\ud83d\ude04", // Smiling Face With Open Mouth and Smiling Eyes
    "\ud83d\ude05", // Smiling Face With Open Mouth and Cold Sweat
    "\ud83d\ude06", // Smiling Face With Open Mouth and Tightly-Closed Eyes
    "\uD83D\uDC4D", // Thumbs up
    "\uD83D\uDC4E", // Thumbs down
    "\ud83d\ude09", // Winking Face
    "\ud83d\ude0a", // Smiling Face With Smiling Eyes
    "\ud83d\ude0b", // Face Savouring Delicious Food
    "\ud83d\ude0e", // Smiling Face With Sunglasses
    "\ud83d\ude0d", // Smiling Face With Heart-Shaped Eyes
    "\ud83d\ude18", // Face Throwing a Kiss
    "\ud83d\ude17", // Kissing Face
    "\ud83d\ude19", // Kissing Face With Smiling Eyes
    "\ud83d\ude1a", // Kissing Face With Closed Eyes
    "\ud83d\ude42", // Slightly Smiling Face
    "\ud83e\udd17", // Hugging Face
    "\ud83d\ude07", // Smiling Face With Halo
    "\ud83e\udd13", // Nerd Face
    "\ud83e\udd14", // Thinking Face
    "\ud83d\ude10", // Neutral Face
    "\ud83d\ude11", // Expressionless Face
    "\ud83d\ude36", // Face Without Mouth
    "\ud83d\ude44", // Face With Rolling Eyes
    "\ud83d\ude0f", // Smirking Face
    "\ud83d\ude23", // Persevering Face
    "\ud83d\ude25", // Disappointed but Relieved Face
    "\ud83d\ude2e", // Face With Open Mouth
    "\ud83e\udd10", // Zipper-Mouth Face
    "\ud83d\ude2f", // Hushed Face
    "\ud83d\ude2a", // Sleepy Face
    "\ud83d\ude2b", // Tired Face
    "\ud83d\ude34", // Sleeping Face
    "\ud83d\ude0c", // Relieved Face
    "\ud83d\ude1b", // Face With Stuck-Out Tongue
    "\ud83d\ude1c", // Face With Stuck-Out Tongue and Winking Eye
    "\ud83d\ude1d", // Face With Stuck-Out Tongue and Tightly-Closed Eyes
    "\ud83d\ude12", // Unamused Face
    "\ud83d\ude13", // Face With Cold Sweat
    "\ud83d\ude14", // Pensive Face
    "\ud83d\ude15", // Confused Face
    "\ud83d\ude43", // Upside-Down Face
    "\ud83e\udd11", // Money-Mouth Face
    "\ud83d\ude32", // Astonished Face
    "\ud83d\ude37", // Face With Medical Mask
    "\ud83e\udd12", // Face With Thermometer
    "\ud83e\udd15", // Face With Head-Bandage
    "\ud83d\ude41", // Slightly Frowning Face
    "\ud83d\ude16", // Confounded Face
    "\ud83d\ude1e", // Disappointed Face
    "\ud83d\ude1f", // Worried Face
    "\ud83d\ude24", // Face With Look of Triumph
    "\ud83d\ude22", // Crying Face
    "\ud83d\ude2d", // Loudly Crying Face
    "\ud83d\ude26", // Frowning Face With Open Mouth
    "\ud83d\ude27", // Anguished Face
    "\ud83d\ude28", // Fearful Face
    "\ud83d\ude29", // Weary Face
    "\ud83d\ude2c", // Grimacing Face
    "\ud83d\ude30", // Face With Open Mouth and Cold Sweat
    "\ud83d\ude31", // Face Screaming in Fear
    "\ud83d\ude33", // Flushed Face
    "\ud83d\ude35", // Dizzy Face
    "\ud83d\ude21", // Pouting Face
    "\ud83d\ude20", // Angry Face
    "\ud83d\ude08", // Smiling Face With Horns
    "\ud83d\udc7f", // Imp
    "\ud83d\udc79", // Japanese Ogre
    "\ud83d\udc7a", // Japanese Goblin
    "\ud83d\udc80", // Skull
    "\ud83d\udc7b", // Ghost
    "\ud83d\udc7d", // Extraterrestrial Alien
    "\ud83e\udd16", // Robot Face
    "\ud83d\udca9", // Pile of Poo
    "\ud83d\ude3a", // Smiling Cat Face With Open Mouth
    "\ud83d\ude38", // Grinning Cat Face With Smiling Eyes
    "\ud83d\ude39", // Cat Face With Tears of Joy
    "\ud83d\ude3b", // Smiling Cat Face With Heart-Shaped Eyes
    "\ud83d\ude3c", // Cat Face With Wry Smile
    "\ud83d\ude3d", // Kissing Cat Face With Closed Eyes
    "\ud83d\ude40", // Weary Cat Face
    "\ud83d\ude3f", // Crying Cat Face
    "\ud83d\ude3e", // Pouting Cat Face
    "\ud83d\ude80", // Rocket
    "\ud83d\udd25", // Fire
    "\uD83E\uDD2F", // Exploding Head
    "❤\uFE0F", // Red Heart
    "\uD83D\uDC9C", // Purple Heart
    "\uD83D\uDC9B", // Yellow Heart
    "\uD83D\uDC9A", // Green Heart
    "\uD83E\uDD0D", // White Heart
    "\uD83E\uDE77", // Pink Heart
    "\uD83E\uDD40", // Wilted Flower
    "✅", // Check
    "\uD83D\uDE4F", // Folded Hands
    "\uD83D\uDEAC", // Cigarette
    "\uD83C\uDF75", // Teacup without handle
    "❌", // Cross Mark
    "\uD83C\uDF89", // Party Popper
    "\uD83E\uDD20", // Cowboy Hat Face
    "\uD83E\uDD7A", // Pleading Face
    "\uD83E\uDD79", // Face holding back Tears
)

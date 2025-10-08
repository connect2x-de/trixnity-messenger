package de.connect2x.trixnity.messenger.compose.view.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.common.deriveFromHue
import de.connect2x.trixnity.messenger.compose.view.common.hue
import de.connect2x.trixnity.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.MessageAddReactionButton
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.MessageReactionButton
import de.connect2x.trixnity.messenger.compose.view.theme.DefaultAccentColorImpl
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_background
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_error
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_errorContainer
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_inverseOnSurface
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_inversePrimary
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_inverseSurface
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_onBackground
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_onError
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_onErrorContainer
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_onPrimary
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_onPrimaryContainer
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_onSecondary
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_onSecondaryContainer
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_onSurface
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_onSurfaceVariant
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_onTertiary
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_onTertiaryContainer
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_outline
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_outlineVariant
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_primaryContainer
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_scrim
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_secondary
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_secondaryContainer
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_surface
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_surfaceTint
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_surfaceVariant
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_tertiary
import de.connect2x.trixnity.messenger.compose.view.theme.md_theme_light_tertiaryContainer
import de.connect2x.trixnity.messenger.util.PlatformGraphemeIterableProvider
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.EventReactions.ByReactionInfo
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.InitialsImpl
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.UserId


private fun previewReactionEvent(sender: String, initials: Initials, isMe: Boolean = false) =
    ByReactionInfo(
        eventId = EventId(""),
        sender = UserInfoElement(
            name = sender,
            userId = UserId("@kirill:local"),
            initials = initials.compute(sender),
            image = null,
        ),
        isMe = isMe,
    )

@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    val accentColor = DefaultAccentColorImpl().value
    val accentHue = accentColor.hue
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = accentColor,
            onPrimary = md_theme_light_onPrimary,
            primaryContainer = md_theme_light_primaryContainer.deriveFromHue(accentHue),
            onPrimaryContainer = md_theme_light_onPrimaryContainer,
            secondary = md_theme_light_secondary.deriveFromHue(accentHue),
            onSecondary = md_theme_light_onSecondary,
            secondaryContainer = md_theme_light_secondaryContainer,
            onSecondaryContainer = md_theme_light_onSecondaryContainer,
            tertiary = md_theme_light_tertiary,
            onTertiary = md_theme_light_onTertiary,
            tertiaryContainer = md_theme_light_tertiaryContainer,
            onTertiaryContainer = md_theme_light_onTertiaryContainer,
            error = md_theme_light_error,
            errorContainer = md_theme_light_errorContainer,
            onError = md_theme_light_onError,
            onErrorContainer = md_theme_light_onErrorContainer,
            background = md_theme_light_background,
            onBackground = md_theme_light_onBackground,
            surface = md_theme_light_surface,
            onSurface = md_theme_light_onSurface,
            surfaceVariant = md_theme_light_surfaceVariant.deriveFromHue(accentHue),
            onSurfaceVariant = md_theme_light_onSurfaceVariant,
            outline = md_theme_light_outline.deriveFromHue(accentHue),
            inverseOnSurface = md_theme_light_inverseOnSurface.deriveFromHue(accentHue),
            inverseSurface = md_theme_light_inverseSurface.deriveFromHue(accentHue),
            inversePrimary = md_theme_light_inversePrimary.deriveFromHue(accentHue),
            surfaceTint = md_theme_light_surfaceTint,
            outlineVariant = md_theme_light_outlineVariant,
            scrim = md_theme_light_scrim,
        ), shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        content = content
    )
}


@OptIn(ExperimentalLayoutApi::class)
@Preview
@Composable
fun MessageReactionPreview() {
    InitMessengerPreview {
        val initials = InitialsImpl(PlatformGraphemeIterableProvider)
        PreviewTheme {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            ) {
                MessageReactionButton(
                    reaction = "\uD83D\uDC4D",
                    reactionEvents = setOf(
                        ByReactionInfo(
                            eventId = EventId(""),
                            sender = UserInfoElement(
                                name = "Martin",
                                userId = UserId("@martin:local"),
                                initials = "M",
                            ),
                            isMe = false,
                        )
                    ),
                    count = 3,
                    myReaction = false,
                    onAddReaction = { },
                    onRemoveReaction = { },
                )
                MessageReactionButton(
                    reaction = "\uD83D\uDC4D",
                    reactionEvents = setOf(
                        ByReactionInfo(
                            eventId = EventId(""),
                            sender = UserInfoElement(
                                name = "Jan",
                                userId = UserId("@jan:local"),
                                initials = "M",
                            ),
                            isMe = false,
                        ),
                        previewReactionEvent("username", initials, isMe = true)
                    ),
                    count = 2,
                    myReaction = true,
                    onAddReaction = { },
                    onRemoveReaction = { },
                )
                MessageAddReactionButton(
                    onClick = {},
                    label = "React"
                )
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Preview
@Composable
fun MessageReactionWrappingPreview() {
    InitMessengerPreview {
        val initials = InitialsImpl(PlatformGraphemeIterableProvider)
        PreviewTheme {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            ) {
                for (i in 0..10) {
                    MessageReactionButton(
                        reaction = "\uD83D\uDC4D",
                        reactionEvents = setOf(),
                        count = 3,
                        myReaction = false,
                        onAddReaction = { },
                        onRemoveReaction = { },
                    )
                }
                MessageReactionButton(
                    reaction = "\uD83D\uDC4D",
                    reactionEvents = setOf(previewReactionEvent("username", initials, isMe = false)),
                    count = 2,
                    myReaction = true,
                    onAddReaction = { },
                    onRemoveReaction = { },
                )
                MessageReactionButton(
                    reaction = "Bee Movie By Jerry Seinfeld NARRATOR: (Black screen with text; The sound of buzzing bees can be heard) According to all known laws of aviation, : there is no way a bee should be able to fly. : Its wings are too small to get its fat little body off the ground. : The bee, of course, flies anyway : because bees don't care what humans think is impossible. BARRY BENSON: (Barry is picking out a shirt) Yellow, black. Yellow, black. Yellow, black. Yellow, black. : Ooh, black and yellow! Let's shake it up a little. JANET BENSON: Barry! Breakfast is ready! BARRY: Coming! : Hang on a second. (Barry uses his antenna like a phone) : Hello? ADAM FLAYMAN: (Through phone) - Barry? BARRY: - Adam? ADAM: - Can you believe this is happening? BARRY: - I can't. I'll pick you up. (Barry flies down the stairs) ",
                    reactionEvents = setOf(),
                    count = 2,
                    myReaction = false,
                    onAddReaction = { },
                    onRemoveReaction = { },
                )
                MessageAddReactionButton(
                    onClick = {},
                    label = "React"
                )
            }
        }
    }
}

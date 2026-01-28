package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.core.model.events.EmptyEventContent
import de.connect2x.trixnity.core.model.events.MessageEventContent
import de.connect2x.trixnity.core.model.events.RedactedEventContent
import de.connect2x.trixnity.core.model.events.StateEventContent
import de.connect2x.trixnity.core.model.events.UnknownEventContent
import de.connect2x.trixnity.core.model.events.m.ReactionEventContent
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationDoneEventContent
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationReadyEventContent
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationStartEventContent
import de.connect2x.trixnity.core.model.events.m.room.AvatarEventContent
import de.connect2x.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.EncryptionEventContent
import de.connect2x.trixnity.core.model.events.m.room.GuestAccessEventContent
import de.connect2x.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.NameEventContent
import de.connect2x.trixnity.core.model.events.m.room.PowerLevelsEventContent
import de.connect2x.trixnity.core.model.events.m.room.RedactionEventContent
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.core.model.events.m.room.ThirdPartyInviteEventContent
import de.connect2x.trixnity.core.model.events.m.room.TombstoneEventContent
import de.connect2x.trixnity.core.model.events.m.room.TopicEventContent
import de.connect2x.trixnity.core.model.events.stateKeyOrNull
import de.connect2x.trixnity.messenger.i18n.I18n

fun interface TimelineEventContentToString {
    operator fun invoke(timelineEvent: TimelineEvent, fileName: String?): String?
}

class TimelineEventContentToStringImpl(private val i18n: I18n) : TimelineEventContentToString {
    override fun invoke(timelineEvent: TimelineEvent, fileName: String?): String? {
        val contentResult = timelineEvent.content
        return when {
            contentResult != null -> when {
                contentResult.isSuccess -> {
                    when (val content = contentResult.getOrThrow()) {
                        is MessageEventContent -> when (content) {
                            is RoomMessageEventContent -> {
                                val body = content.body
                                when (content) {
                                    is RoomMessageEventContent.TextBased.Text -> body
                                    is RoomMessageEventContent.TextBased.Emote -> i18n.exportRoomEmote(body)
                                    is RoomMessageEventContent.TextBased.Notice -> i18n.exportRoomNotice(body)
                                    is RoomMessageEventContent.FileBased -> {
                                        val name = fileName ?: content.fileName ?: content.body
                                        when (content) {
                                            is RoomMessageEventContent.FileBased.Image -> i18n.exportRoomImage(name)
                                            is RoomMessageEventContent.FileBased.Audio -> i18n.exportRoomAudio(name)
                                            is RoomMessageEventContent.FileBased.Video -> i18n.exportRoomVideo(name)
                                            is RoomMessageEventContent.FileBased.File -> i18n.exportRoomFile(name)
                                        }
                                    }

                                    is RoomMessageEventContent.Location -> i18n.exportRoomLocation(body, content.geoUri)
                                    is RoomMessageEventContent.Unknown,
                                    is RoomMessageEventContent.VerificationRequest -> null
                                }
                            }

                            is ReactionEventContent,
                            is VerificationStartEventContent,
                            is VerificationReadyEventContent,
                            is VerificationDoneEventContent,
                            is VerificationCancelEventContent -> null

                            else -> null
                        }

                        is StateEventContent -> when (content) {
                            is AvatarEventContent -> i18n.exportRoomAvatar(content.url)
                            is CanonicalAliasEventContent -> i18n.exportRoomCanonicalAlias(listOfNotNull(content.alias) + content.aliases.orEmpty())
                            is CreateEventContent -> i18n.exportRoomCreate(content.federate, content.type?.name)
                            is JoinRulesEventContent -> i18n.exportRoomJoinRule(content.joinRule.name)
                            is MemberEventContent -> i18n.exportRoomMember(
                                userId = timelineEvent.event.stateKeyOrNull,
                                membership = content.membership.name,
                                displayName = content.displayName,
                                avatarUrl = content.avatarUrl,
                                reason = content.reason
                            )

                            is NameEventContent -> i18n.exportRoomName(content.name)
                            is TopicEventContent -> i18n.exportRoomTopic(
                                content.topic?.text?.plain ?: content.legacy.topic
                            )

                            is EncryptionEventContent -> i18n.exportRoomEncryption()
                            is HistoryVisibilityEventContent -> i18n.exportRoomHistoryVisibility(content.historyVisibility.name)
                            is GuestAccessEventContent -> i18n.exportRoomGuestAccess(content.guestAccess.name)
                            is TombstoneEventContent -> i18n.exportRoomTombstone(content.body, content.replacementRoom)

                            is PowerLevelsEventContent, // <- TODO
                            is ThirdPartyInviteEventContent -> null

                            else -> null
                        }?.let { i18n.exportRoomState(it) }

                        is RedactedEventContent ->
                            i18n.exportRoomRedacted(
                                timelineEvent.event.unsigned?.redactedBecause?.sender,
                                (timelineEvent.event.unsigned?.redactedBecause?.content as? RedactionEventContent)?.reason
                            )

                        EmptyEventContent,
                        is UnknownEventContent -> null
                    }
                }

                contentResult.isFailure -> {
                    val error = contentResult.exceptionOrNull()
                        ?: throw IllegalStateException("result in unexpected state")
                    if (error is TimelineEvent.TimelineEventContentError) {
                        when (error) {
                            TimelineEvent.TimelineEventContentError.DecryptionAlgorithmNotSupported -> i18n.exportRoomDecryptionError()
                            is TimelineEvent.TimelineEventContentError.DecryptionError -> i18n.exportRoomDecryptionError()
                            TimelineEvent.TimelineEventContentError.DecryptionTimeout -> i18n.exportRoomDecryptionError()
                            TimelineEvent.TimelineEventContentError.NoContent -> null
                        }
                    } else null
                }

                else -> throw IllegalStateException("result in unexpected state")
            }

            else -> null
        }
    }
}

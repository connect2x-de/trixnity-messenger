package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogFooter
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogScrollContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.messenger.compose.view.theme.components.ThemedSelect
import de.connect2x.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.viewmodel.room.settings.CurrentMax
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PowerlevelViewModel
import net.folivo.trixnity.core.model.events.EventType
import net.folivo.trixnity.core.serialization.events.DefaultEventContentSerializerMappings
import net.folivo.trixnity.core.serialization.events.EventContentSerializerMapping

@Composable
fun ChangePowerLevelContainer(model: PowerlevelViewModel) {
    ChangePowerLevel(model)
}

interface ChangePowerLevelView {
    @Composable
    fun create(model: PowerlevelViewModel)
}

@Composable
fun ChangePowerLevel(model: PowerlevelViewModel) {
    DI.get<ChangePowerLevelView>().create(model)
}

class ChangePowerLevelViewImpl : ChangePowerLevelView {
    @Composable
    override fun create(model: PowerlevelViewModel) {
        val i18n = DI.get<I18nView>()
        val contentMaybeNull by model.powerLevels.collectAsState()
        val content = contentMaybeNull ?: return

        val canChangePowerLevels by model.canChangePowerLevels.collectAsState()

        var newContent by remember { mutableStateOf(content.copy()) }
        val eventStrings: Set<String> = newContent.events.keys.mapTo(mutableSetOf()) { it.toString() }

        ErrorModal(model)

        Column {
            Header(onBack = model::back, title = i18n.changePowerLevelHeader())

            if (!canChangePowerLevels) {
                ThemedSurface(style = MaterialTheme.components.sidebar) {
                    Text(i18n.cannotChangePowerLevels())
                }
                Spacer(Modifier.height(12.dp))
            }

            AdaptiveDialogScrollContent {
                PowerLevelInput(
                    label = i18n.mRoomUserDefaultHeading(),
                    value = newContent.usersDefault,
                    enabled = canChangePowerLevels,
                    update = {
                        newContent = newContent.copy(usersDefault = newContent.usersDefault.copy(current = it))
                    })
                PowerLevelInput(
                    label = i18n.mRoomEventDefaultHeading(),
                    value = newContent.eventsDefault,
                    enabled = canChangePowerLevels,
                    update = {
                        newContent = newContent.copy(eventsDefault = newContent.eventsDefault.copy(current = it))
                    })
                PowerLevelInput(
                    label = i18n.mRoomStateDefaultHeading(),
                    value = newContent.stateDefault,
                    enabled = canChangePowerLevels,
                    update = {
                        newContent = newContent.copy(stateDefault = newContent.stateDefault.copy(current = it))
                    })

                PowerLevelInput(
                    label = i18n.mRoomBanHeading(),
                    value = newContent.ban,
                    enabled = canChangePowerLevels,
                    update = { newContent = newContent.copy(ban = newContent.ban.copy(current = it)) })
                PowerLevelInput(
                    label = i18n.mRoomInviteHeading(),
                    value = newContent.invite,
                    enabled = canChangePowerLevels,
                    update = { newContent = newContent.copy(invite = newContent.invite.copy(current = it)) })
                PowerLevelInput(
                    label = i18n.mRoomKickHeading(),
                    value = newContent.kick,
                    enabled = canChangePowerLevels,
                    update = { newContent = newContent.copy(kick = newContent.kick.copy(current = it)) })
                PowerLevelInput(
                    label = i18n.mRoomRedactHeading(),
                    value = newContent.redact,
                    enabled = canChangePowerLevels,
                    update = { newContent = newContent.copy(redact = newContent.redact.copy(current = it)) })

                newContent.events.forEach { (eventType, value) ->
                    PowerLevelInput(
                        label = translateEventHeading(eventType.toString()),
                        enabled = canChangePowerLevels,
                        value = value,
                        update = {
                            newContent = newContent.copy(
                                events = newContent.events + (eventType to newContent.events[eventType]!!.copy(
                                    current = it
                                ))
                            )
                        })
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                KnownUnsetEvents(
                    enabled = canChangePowerLevels,
                    mappings = DefaultEventContentSerializerMappings.message.filter { !eventStrings.contains(it.type) }
                        .sortedBy { it.type }.toList(),
                    onCreate = {
                        newContent = newContent.copy(
                            events = newContent.events + (EventType(it.kClass, it.type) to newContent.eventsDefault)
                        )
                    },
                )

                Spacer(Modifier.height(16.dp))

                NewPowerLevel(
                    enabled = canChangePowerLevels, existingEvents = eventStrings, newEvent = {
                        newContent = newContent.copy(
                            events = newContent.events + (EventType(null, it) to newContent.eventsDefault)
                        )
                    })
            }

            if (canChangePowerLevels) {
                AdaptiveDialogFooter(style = MaterialTheme.components.modalDialog) {
                    ThemedButton(
                        onClick = { newContent = content.copy() },
                        enabled = newContent != content,
                        style = MaterialTheme.components.secondaryButton,
                        content = { Text(i18n.actionCancel()) })
                    ThemedButton(
                        onClick = { model.setPowerLevels(newContent) },
                        enabled = newContent != content,
                        style = MaterialTheme.components.primaryButton,
                        content = { Text(i18n.actionConfirm()) })
                }
            }
        }
    }
}


@Composable
private fun KnownUnsetEvents(
    enabled: Boolean,
    mappings: List<EventContentSerializerMapping<*>>,
    onCreate: (EventContentSerializerMapping<*>) -> Unit
) {
    val i18n = DI.get<I18nView>()
    var selected by remember { mutableStateOf(mappings[0]) }

    Column(Modifier.fillMaxWidth()) {
        Text("Set powerlevel for known event")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ThemedSelect(
                value = selected,
                enabled = enabled,
                onValueChange = { selected = it },
                options = mappings,
                render = { translateEventHeading(it.type) })
            Spacer(Modifier.weight(1f))
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                enabled = enabled,
                onClick = { onCreate(selected) },
                content = { Text(i18n.actionCreate()) })
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun NewPowerLevel(enabled: Boolean, existingEvents: Set<String>, newEvent: (String) -> Unit) {
    val i18n = DI.get<I18nView>()
    var customValue by remember { mutableStateOf("") }
    fun isErr(): Boolean = existingEvents.contains(customValue)

    Column(Modifier.fillMaxWidth()) {
        Text("Set powerlevel for unknown event")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                modifier = Modifier.widthIn(min = OutlinedTextFieldDefaults.MinWidth),
                value = customValue,
                onValueChange = { customValue = it },
                enabled = enabled,
                label = { Text(i18n.newEventIdLabel()) },
                isError = isErr(),
                supportingText = { Text(if (isErr()) i18n.newEventAlreadyExistsErr() else "") })
            Spacer(Modifier.weight(1f))
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                enabled = enabled && !isErr(),
                onClick = { if (!isErr()) newEvent(customValue) },
                content = { Text(i18n.actionCreate()) })
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ErrorModal(model: PowerlevelViewModel) {
    val i18n = DI.get<I18nView>()
    model.error.collectAsState().value?.let { error ->
        ThemedModalDialog(onDismissRequest = { model.errorDismiss() }) {
            ModalDialogHeader { Text(i18n.anErrorHasOccurred()) }
            ModalDialogContent { Text(error) }
            ModalDialogFooter {
                ThemedButton(onClick = { model.errorDismiss() }) {
                    Text(i18n.actionClose())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PowerLevelInput(label: String, enabled: Boolean, value: CurrentMax, update: (Long) -> Unit) {
    val i18n = DI.get<I18nView>()
    val options = listOf("User", "Moderator", "Admin", "Custom")
    val defaultVals = listOf(0L, 50L, 100L, value.current)
    val initialIndex = defaultVals.indexOf(value.current)

    var customValue by remember { mutableStateOf(TextFieldValue(value.current.toString())) }
    var isCustomSelected by remember { mutableStateOf(initialIndex == 3) }
    var isValidLong by remember { mutableStateOf(true) }
    var isUnderMaxPowerLevel by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }

    fun newText(newText: TextFieldValue) {
        customValue = newText
        when (val l = customValue.text.toLongOrNull()) {
            null -> isValidLong = false
            else -> {
                isValidLong = true
                if (value.max != null && value.max!! < l) {
                    isUnderMaxPowerLevel = false
                } else {
                    isUnderMaxPowerLevel = true
                    update(l)
                }
            }
        }
    }

    fun errorMsg(): String = if (!isValidLong) {
        i18n.powerLevelInputErrNotANumber()
    } else if (!isUnderMaxPowerLevel) {
        i18n.powerLevelInputErrAboveAllowedPowerLevel(value.max!!)
    } else {
        ""
    }

    Column(Modifier.fillMaxWidth()) {
        Text(label)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ThemedSelect(
                label = { Text(i18n.roleLabel()) },
                options = options,
                value = options[initialIndex],
                enabled = enabled,
                render = {
                    when (it) {
                        "User" -> i18n.userProfileRoleUser()
                        "Moderator" -> i18n.userProfileRoleModerator()
                        "Admin" -> i18n.userProfileRoleAdministrator()
                        "Custom" -> i18n.userProfileRoleCustom()
                        else -> ""
                    }
                },
                onValueChange = {
                    val v = defaultVals[options.indexOf(it)].toString()
                    if (it == "Custom") {
                        isCustomSelected = true
                        newText(TextFieldValue(v, TextRange(0, v.length)))
                        focusRequester.requestFocus()
                    } else {
                        isCustomSelected = false
                        newText(TextFieldValue(v))
                    }
                })
            OutlinedTextField(
                modifier = Modifier.weight(2f).focusRequester(focusRequester).pointerHoverIcon(PointerIcon.Default),
                value = customValue,
                enabled = enabled && isCustomSelected,
                readOnly = !isCustomSelected,
                onValueChange = { newText(it) },
                isError = !isValidLong || !isUnderMaxPowerLevel,
                label = { Text(i18n.powerLevelLabel()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    if (!isValidLong || !isUnderMaxPowerLevel) {
                        Tooltip(tooltip = { Text(errorMsg()) }, content = {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = i18n.commonError(),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        })
                    }
                },
                supportingText = { Text(errorMsg()) })
        }
    }
}

@Composable
private fun translateEventHeading(event: String): String {
    val i18n = DI.get<I18nView>()
    return when (event) {
        "m.room.avatar" -> i18n.mRoomAvatarPowerLevelHeading()
        "m.room.name" -> i18n.mRoomNameHeading()
        "m.room.topic" -> i18n.mRoomTopicHeading()
        "m.room.member" -> i18n.mRoomMemberHeading()
        "m.room.power_levels" -> i18n.mRoomPowerLevelsHeading()
        "m.room.join_rules" -> i18n.mRoomJoinRulesHeading()
        "m.room.history_visibility" -> i18n.mRoomHistoryVisibilityHeading()
        "m.room.encryption" -> i18n.mRoomEncryptionHeading()
        "m.room.pinned_events" -> i18n.mRoomPinnedEventsHeading()
        "m.room.canonical_alias" -> i18n.mRoomCanonicalAliasHeading()
        "m.room.server_acl" -> i18n.mRoomServerAclHeading()
        "m.room.tombstone" -> i18n.mRoomTombstoneHeading()

        "m.room.message" -> i18n.mRoomMessageHeading()
        "m.reaction" -> i18n.mReactionHeading()
        "m.room.redaction" -> i18n.mRoomRedactionHeading()
        "m.room.encrypted" -> i18n.mRoomEncryptedHeading()

        "m.key.verification.start" -> i18n.mKeyVerificationStartHeading()
        "m.key.verification.ready" -> i18n.mKeyVerificationReadyHeading()
        "m.key.verification.accept" -> i18n.mKeyVerificationAcceptHeading()
        "m.key.verification.key" -> i18n.mKeyVerificationKeyHeading()
        "m.key.verification.mac" -> i18n.mKeyVerificationMacHeading()
        "m.key.verification.done" -> i18n.mKeyVerificationDoneHeading()
        "m.key.verification.cancel" -> i18n.mKeyVerificationCancelHeading()

        "m.call.invite" -> i18n.mCallInviteHeading()
        "m.call.candidates" -> i18n.mCallCandidatesHeading()
        "m.call.answer" -> i18n.mCallAnswerHeading()
        "m.call.hangup" -> i18n.mCallHangupHeading()
        "m.call.negotiate" -> i18n.mCallNegotiateHeading()
        "m.call.reject" -> i18n.mCallRejectHeading()
        "m.call.select_answer" -> i18n.mCallSelectAnswerHeading()
        "m.call.sdp_stream_metadata_changed" -> i18n.mCallSdpStreamMetadataChangedHeading()

        else -> event
    }
}

package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
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
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PowerlevelViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.Value
import net.folivo.trixnity.core.model.events.EventType
import net.folivo.trixnity.core.serialization.events.EventContentSerializerMapping

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

        val canChangePowerLevels by model.canChangePowerLevels.collectAsState()
        val isAnyInputModified by model.isAnyInputModified.collectAsState()
        val inputError by model.inputError.collectAsState()

        val events by model.events.collectAsState()
        val knownEvents by model.knownEvents.collectAsState()

        val eventStrings by remember(events) {
            mutableStateOf(events.keys.mapTo(mutableSetOf()) { it.toString() })
        }

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
                PowerLevelInput(i18n.mRoomUserDefaultHeading(), model.usersDefault, canChangePowerLevels)
                PowerLevelInput(i18n.mRoomEventDefaultHeading(), model.eventsDefault, canChangePowerLevels)
                PowerLevelInput(i18n.mRoomStateDefaultHeading(), model.stateDefault, canChangePowerLevels)

                PowerLevelInput(i18n.mRoomBanHeading(), model.ban, canChangePowerLevels)
                PowerLevelInput(i18n.mRoomInviteHeading(), model.invite, canChangePowerLevels)
                PowerLevelInput(i18n.mRoomKickHeading(), model.kick, canChangePowerLevels)
                PowerLevelInput(i18n.mRoomRedactHeading(), model.redact, canChangePowerLevels)

                events.forEach { (eventType, value) ->
                    PowerLevelInput(translateEventHeading(eventType.toString()), value, canChangePowerLevels)
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                KnownUnsetEvents(
                    enabled = canChangePowerLevels,
                    mappings = knownEvents.filter { !eventStrings.contains(it.type) }.sortedBy { it.type }.toList(),
                    onCreate = { model.newEvent(EventType(it.kClass, it.type)) },
                )

                Spacer(Modifier.height(16.dp))

                PowerLevelUnknownEvent(
                    enabled = canChangePowerLevels,
                    existingEvents = eventStrings,
                    newEvent = { model.newEvent(EventType(null, it)) })
            }

            if (canChangePowerLevels) {
                AdaptiveDialogFooter(style = MaterialTheme.components.modalDialog) {
                    ThemedButton(
                        onClick = { model.resetPowerLevels() },
                        enabled = isAnyInputModified,
                        style = MaterialTheme.components.secondaryButton,
                        content = { Text(i18n.actionCancel()) })
                    ThemedButton(
                        onClick = { model.setPowerLevels() },
                        enabled = !inputError && isAnyInputModified,
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
    if (mappings.isEmpty()) return

    val i18n = DI.get<I18nView>()
    var selected by remember(mappings) { mutableStateOf(mappings[0]) }

    Column(Modifier.fillMaxWidth()) {
        Text(i18n.powerLevelChangeUnsetKnownEventHeading())
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            ThemedSelect(
                value = selected,
                enabled = enabled,
                onValueChange = { selected = it },
                options = mappings,
                render = { translateEventHeading(it.type) })
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                enabled = enabled,
                onClick = { onCreate(selected) },
                content = { Text(i18n.actionCreate()) })
        }
    }
}

@Composable
private fun PowerLevelUnknownEvent(enabled: Boolean, existingEvents: Set<String>, newEvent: (String) -> Unit) {
    val i18n = DI.get<I18nView>()
    var customValue by remember { mutableStateOf("") }
    fun isErr(): Boolean = existingEvents.contains(customValue)

    Column(Modifier.fillMaxWidth()) {
        Text(i18n.powerLevelChangeNewEventHeading())
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = customValue,
                onValueChange = { customValue = it },
                enabled = enabled,
                label = { Text(i18n.newEventIdLabel()) },
                isError = isErr(),
                supportingText = { Text(if (isErr()) i18n.newEventAlreadyExistsErr() else "") })
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                enabled = enabled && !isErr(),
                onClick = { if (!isErr()) newEvent(customValue) },
                content = { Text(i18n.actionCreate()) })
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
private fun PowerLevelInput(label: String, value: Value, enabled: Boolean) {
    val i18n = DI.get<I18nView>()

    var textFieldValue by value.input.collectAsTextFieldValueState()
    val isModified by value.isModified.collectAsState()
    val isValidLong by value.isValidLong.collectAsState()
    val isUnderMaxPowerLevel by value.isUnderMaxPowerLevel.collectAsState()
    val isError by value.error.collectAsState()
    val old by value.old.collectAsState()
    val max by value.max.collectAsState()

    val options = listOf("User", "Moderator", "Admin", "Custom")
    val defaultVals = listOf(0L, 50L, 100L, textFieldValue.text.toLongOrNull() ?: old)
    val initialIndex = defaultVals.indexOf(textFieldValue.text.toLongOrNull() ?: old)

    var isCustomSelected by remember(initialIndex) { mutableStateOf(initialIndex == 3) }
    val focusRequester = remember { FocusRequester() }

    fun errorMsg(): String = when {
        !isModified -> "" // don't show error messages on unmodified entries
        !isValidLong -> i18n.powerLevelInputErrNotANumber()
        !isUnderMaxPowerLevel -> i18n.powerLevelInputErrAboveAllowedPowerLevel(max ?: old)
        else -> ""
    }

    Column(Modifier.fillMaxWidth()) {
        Text(label)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ThemedSelect(
                modifier = Modifier.widthIn(max = 200.dp),
                label = { Text(i18n.roleLabel()) },
                options = options,
                value = options[initialIndex],
                enabled = enabled && max != null && old < (max ?: old),
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
                        textFieldValue = TextFieldValue(v, TextRange(0, v.length))
                        focusRequester.requestFocus()
                    } else {
                        isCustomSelected = false
                        textFieldValue = TextFieldValue(v)
                    }
                })

            OutlinedTextField(
                modifier = Modifier.weight(2f).focusRequester(focusRequester).pointerHoverIcon(PointerIcon.Default),
                value = textFieldValue,
                enabled = enabled && isCustomSelected,
                readOnly = !isCustomSelected,
                onValueChange = { textFieldValue = it },
                isError = isError && isModified,
                label = { Text(i18n.powerLevelLabel()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    if (isError && isModified) {
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

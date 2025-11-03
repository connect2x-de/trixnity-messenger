package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.messenger.compose.view.theme.components.ThemedSelect
import de.connect2x.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PowerlevelViewModel

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

        ErrorModal(model)

        Column {
            Header(onBack = model::back, title = i18n.changePowerLevelHeader())

            AdaptiveDialogScrollContent {
                if (!canChangePowerLevels) {
                    ThemedSurface(style = MaterialTheme.components.sidebar) {
                        Text(i18n.cannotChangePowerLevels())
                    }
                    Spacer(Modifier.height(12.dp))
                }

                PowerLevelInput(i18n.mRoomUserDefaultHeading(), model.usersDefault, canChangePowerLevels)
                PowerLevelInput(i18n.mRoomEventDefaultHeading(), model.eventsDefault, canChangePowerLevels)
                PowerLevelInput(i18n.mRoomStateDefaultHeading(), model.stateDefault, canChangePowerLevels)

                PowerLevelInput(i18n.mRoomBanHeading(), model.ban, canChangePowerLevels)
                PowerLevelInput(i18n.mRoomInviteHeading(), model.invite, canChangePowerLevels)
                PowerLevelInput(i18n.mRoomKickHeading(), model.kick, canChangePowerLevels)
                PowerLevelInput(i18n.mRoomRedactHeading(), model.redact, canChangePowerLevels)

                events.forEach { (eventType, value) ->
                    PowerLevelInput(translateEventHeading(eventType), value, canChangePowerLevels)
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                NewEvent(model)
            }

            if (canChangePowerLevels) {
                AdaptiveDialogFooter(style = MaterialTheme.components.modalDialog) {
                    ThemedButton(
                        onClick = { model.resetAll() },
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
private fun NewEvent(model: PowerlevelViewModel) {
    val i18n = DI.get<I18nView>()

    val enabled by model.canChangePowerLevels.collectAsState()

    val knownEvents = model.availableUnsetEvents.collectAsState().value.toList()

    var input by model.newEventInput.collectAsTextFieldValueState()
    val errMsg by model.newEventError.collectAsState()
    val isError = errMsg != null

    Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
        Text(i18n.powerLevelChangeNewEventHeading())

        if (knownEvents.isNotEmpty()) {
            var selected = knownEvents[0]
            ThemedSelect(
                value = selected,
                enabled = enabled,
                onValueChange = { selected = it; input = TextFieldValue(it) },
                options = knownEvents,
                render = { translateEventHeading(it) })
        }

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = input,
                onValueChange = { input = it },
                enabled = enabled,
                label = { Text(i18n.newEventIdLabel()) },
                isError = isError,
                supportingText = { Text(errMsg ?: "") })
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                enabled = enabled && !isError && input.text != "",
                onClick = { model.newEventCreate() },
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
private fun PowerLevelInput(label: String, value: PowerlevelViewModel.Value, canChangeAnyPowerlevel: Boolean) {
    val i18n = DI.get<I18nView>()

    var textFieldValue by value.input.collectAsTextFieldValueState()
    val isModified by value.isModified.collectAsState()

    val options = listOf("User", "Moderator", "Admin", "Custom")
    val defaultVals = listOf(0L, 50L, 100L, textFieldValue.text.toLongOrNull())
    val initialIndex = defaultVals.indexOf(textFieldValue.text.toLongOrNull())

    var isCustomSelected by remember(initialIndex) { mutableStateOf(initialIndex == 3) }
    val focusRequester = remember { FocusRequester() }

    val isError = value.error.collectAsState().value != null
    val errorMsg = value.error.collectAsState().value ?: ""
    val canChange = value.canChange.collectAsState().value

    Column(Modifier.fillMaxWidth()) {
        Text(label)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (value.canBeRemoved && canChangeAnyPowerlevel) ThemedIconButton(
                onClick = { value.remove() },
                Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(Icons.Default.Close, i18n.actionRemove())
            }

            ThemedSelect(
                modifier = Modifier.widthIn(max = 170.dp),
                label = { Text(i18n.roleLabel()) },
                options = options,
                value = options[initialIndex],
                enabled = canChangeAnyPowerlevel && canChange,
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
                enabled = canChangeAnyPowerlevel && isCustomSelected && canChange,
                readOnly = !isCustomSelected,
                onValueChange = { textFieldValue = it },
                isError = isError && isModified,
                maxLines = 1,
                label = { Text(i18n.powerLevelLabel()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    if (isError && isModified) {
                        Tooltip(tooltip = { Text(errorMsg) }, content = {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = i18n.commonError(),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        })
                    }
                },
                supportingText = { Text(errorMsg) })
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

        "m.room.create" -> i18n.mRoomCreateHeading()
        "m.room.third_party_invite" -> i18n.mRoomThirdPartyInviteHeading()
        "m.room.guest_access" -> i18n.mRoomGuestAccessHeading()
        "m.policy.rule.user" -> i18n.mPolicyRuleUserHeading()
        "m.policy.rule.room" -> i18n.mPolicyRuleRoomHeading()
        "m.policy.rule.server" -> i18n.mPolicyRuleServerHeading()
        "m.space.parent" -> i18n.mSpaceParentHeading()
        "m.space.child" -> i18n.mSpaceChildHeading()

        else -> event
    }
}

package de.connect2x.trixnity.messenger.i18n

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages.DE
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages.EN
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.mb
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import kotlin.time.Instant

private val log = KotlinLogging.logger { }

// TODO this is not lazy -> use property delegation or one class for one language instead
@Suppress("UNUSED")
open class I18n(
    languages: Languages,
    settings: MatrixMessengerSettingsHolder,
    getSystemLang: GetSystemLang,
    timeZone: TimeZone
) : I18nBase(languages, settings, getSystemLang, timeZone) {

    // ---- translations -----
    open fun commonUnknown() = translate {
        EN - "unknown"
        DE - "unbekannt"
    }

    open fun commonAnd(first: String, second: String) = translate {
        EN - "$first and $second"
        DE - "$first und $second"
    }

    open fun commonUs() = translate {
        EN - "us"
        DE - "uns"
    }

    open fun commonCancelled() = translate {
        EN - "cancelled"
        DE - "abgebrochen"
    }

    open fun uiaCancelledByUser() = translate {
        EN - "The authorization has been cancelled by you."
        DE - "Die Autorisierung wurde von Ihnen abgebrochen."
    }

    open fun uiaGenericError(message: String? = commonUnknown()) = translate {
        EN - "The authorization has failed: ${message ?: commonUnknown()}"
        DE - "Die Autorisierung ist fehlgeschlagen: ${message ?: commonUnknown()}"
    }

    open fun uiaFallbackNotSupported(authenticationType: AuthenticationType) = translate {
        EN - "The authentication type ${authenticationType.name} is not supported."
        DE - "Die Authorisierungsmethode ${authenticationType.name} wird nicht unterstützt."
    }

    open fun uiaInvalidRegistrationToken() = translate {
        EN - "Invalid registration token."
        DE - "Ungültiger Registrierungstoken."
    }

    open fun uiaInvalidUsernameOrPassword() = translate {
        EN - "Invalid username or password."
        DE - "Ungültiger Benutzername oder Passwort."
    }

    open fun roomNameInvitation() = translate {
        EN - "Invitation"
        DE - "Einladung"
    }

    open fun roomNameInvitationFrom(groupOrChat: String, roomName: String) = translate {
        EN - "Invitation into $groupOrChat '$roomName'"
        DE - "Einladung in $groupOrChat '$roomName'"
    }

    open fun roomNameKnockFor(roomId: String) = translate {
        EN - "Mebership request for $roomId"
        DE - "Beitrittsanfrage für $roomId"
    }

    open fun roomNamePeople(count: Int) = translate {
        EN - "$count persons"
        DE - "$count Personen"
    }

    open fun roomNameEmptyChat() = translate {
        EN - "Empty chat"
        DE - "Leerer Chat"
    }

    open fun roomNameEmptyChatWas(oldName: String) = translate {
        EN - "Empty chat (was $oldName)"
        DE - "Leerer Chat (war $oldName)"
    }

    open fun roomNameAnd() = translate {
        EN - "and"
        DE - "und"
    }

    open fun roomNameChat() = translate {
        EN - "chat"
        DE - "Chat"
    }

    open fun roomNameGroup() = translate {
        EN - "group"
        DE - "Gruppe"
    }

    open fun roomNameOther(othersCount: Int) = translate {
        EN - if (othersCount == 1) "one other" else "$othersCount others"
        DE - if (othersCount == 1) "ein anderer" else "$othersCount andere"
    }

    open fun eventChangeAvatar(username: String) = translate {
        EN - "$username has changed the avatar image"
        DE - "$username hat das Profilbild geändert"
    }

    open fun eventChangeDisplayName(oldDisplayName: String?, newDisplayName: String?) = translate {
        EN - "'$oldDisplayName' has changed their name to '$newDisplayName'"
        DE - "'$oldDisplayName' hat den Namen zu '$newDisplayName' geändert"
    }

    open fun eventRemoveDisplayName(oldDisplayName: String?) = translate {
        EN - "'$oldDisplayName' has removed their name"
        DE - "'$oldDisplayName' hat den Namen entfernt"
    }

    open fun eventChangeChatGenitive() = translate {
        EN - "the chat"
        DE - "des Chats"
    }

    open fun eventChangeChatDative() = translate {
        EN - "the chat"
        DE - "dem Chat"
    }

    open fun eventChangeChatAccusative() = translate {
        EN - "the chat"
        DE - "den Chat"
    }

    open fun eventChangeGroupGenitive() = translate {
        EN - "the group"
        DE - "der Gruppe"
    }

    open fun eventChangeGroupDative() = translate {
        EN - "the group"
        DE - "der Gruppe"
    }

    open fun eventChangeGroupAccusative() = translate {
        EN - "the group"
        DE - "die Gruppe"
    }

    open fun eventChangeInvite(invitee: String, inviter: String, reason: String? = null) = translate {
        EN - "$invitee was invited by $inviter${if (reason == null) "" else " because \"$reason\""}"
        DE - "$invitee wurde von $inviter eingeladen${if (reason == null) "" else ", da \"$reason\""}"
    }

    open fun eventChangeJoin(username: String, groupOrChat: String) = translate {
        EN - "$username has joined $groupOrChat"
        DE - "$username ist $groupOrChat beigetreten"
    }

    open fun eventChangeLeave(username: String, groupOrChat: String) = translate {
        EN - "$username has left $groupOrChat"
        DE - "$username hat $groupOrChat verlassen"
    }

    open fun eventChangeBan(target: String, moderator: String, groupOrChat: String, reason: String? = null) =
        translate {
            EN - "$target was banned from $groupOrChat by $moderator${if (reason == null) "" else " because \"$reason\""}"
            DE - "$target wurde von $moderator aus $groupOrChat verbannt${if (reason == null) "" else ", da \"$reason\""}"
        }

    open fun eventChangeKick(target: String, moderator: String, groupOrChat: String, reason: String? = null) =
        translate {
            EN - "$target was removed from $groupOrChat by $moderator${if (reason == null) "" else " because \"$reason\""}"
            DE - "$target wurde von $moderator aus $groupOrChat entfernt${if (reason == null) "" else ", da \"$reason\""}"
        }

    open fun eventChangeKnock(username: String, groupOrChat: String, reason: String? = null) = translate {
        EN - "$username requested to join $groupOrChat${if (reason == null) "" else " because \"$reason\""}. Check the room settings to manage the Request"
        DE - "$username möchte $groupOrChat beitreten${if (reason == null) "" else ", da \"$reason\""}. Du kannst die Anfrage in den Raumeinstellungen verwalten"
    }

    open fun eventChangeRejected(invitee: String, reason: String? = null) = translate {
        EN - "$invitee has rejected the invitation${if (reason == null) "" else " because \"$reason\""}"
        DE - "$invitee hat die Einladung abgelehnt${if (reason == null) "" else ", da \"$reason\""}"
    }

    open fun eventChangeRevoked(invitee: String, inviter: String, reason: String? = null) = translate {
        EN - "$inviter has revoked the invitation to $invitee${if (reason == null) "" else " because \"$reason\""}"
        DE - "$inviter hat die Einladung an $invitee zurückgezogen${if (reason == null) "" else ", da \"$reason\""}"
    }

    open fun eventChangeUnban(target: String, moderator: String, reason: String? = null) = translate {
        EN - "$target was unbanned by $moderator${if (reason == null) "" else " because \"$reason\""}"
        DE - "$target wurde von $moderator entbannt${if (reason == null) "" else ", da \"$reason\""}"
    }

    open fun eventMessageRedacted(username: String) = translate {
        EN - "message has been deleted by $username"
        DE - "Nachricht wurde von $username gelöscht"
    }

    open fun eventMessageRedactedByMe() = translate {
        EN - "You deleted this message"
        DE - "Sie haben diese Nachricht gelöscht"
    }

    open fun eventMessageRedactedByUnknown() = translate {
        EN - "This message has been deleted"
        DE - "Diese Nachricht ist gelöscht worden"
    }

    open fun eventRoomCreated(username: String, groupOrChat: String) = translate {
        EN - "$username has created $groupOrChat"
        DE - "$username hat $groupOrChat erstellt"
    }

    open fun eventChangeFrom(oldName: String) = translate {
        EN - "from '$oldName' "
        DE - "von '$oldName' "
    }

    open fun eventRoomNameChange(username: String, groupOrChat: String, from: String, roomName: String) = translate {
        EN - "$username has changed the name of $groupOrChat ${from}to '$roomName'"
        DE - "$username hat den Namen $groupOrChat ${from}zu '$roomName' geändert"
    }

    open fun eventRoomTopicChange(username: String, groupOrChat: String, from: String, roomName: String) = translate {
        EN - "$username has changed the topic of $groupOrChat ${from}to '$roomName'"
        DE - "$username hat die Beschreibung $groupOrChat ${from}zu '$roomName' geändert"
    }

    open fun eventPowerLevelChange(user: String, newPowerLevel: Long) = translate {
        EN - when (newPowerLevel) {
            0L -> "$user is now a user"
            50L -> "$user is now a moderator"
            100L -> "$user is now an administrator"
            else -> "power level $newPowerLevel"
        }
        DE - when (newPowerLevel) {
            0L -> "$user ist nun ein Nutzer"
            50L -> "$user ist nun ein Moderator"
            100L -> "$user ist nun ein Administrator"
            else -> "$user hat nun Berechtigungslevel $newPowerLevel"
        }
    }

    open fun setAsMainAlias(username: String, alias: String) = translate {
        EN - "$username set main alias to $alias"
        DE - "$username hat $alias als Hauptalias festgelegt"
    }

    open fun removeAsMainAlias(username: String, alias: String) = translate {
        EN - "$username remove $alias as main alias"
        DE - "$username hat $alias als Hauptalias entfernt"
    }

    open fun addedAlias(username: String, alias: String) = translate {
        EN - "$username added alias $alias"
        DE - "$username hat den Alias $alias hinzugefügt"
    }

    open fun removedAlias(username: String, alias: String) = translate {
        EN - "$username removed alias $alias"
        DE - "$username hat den Alias $alias entfernt"
    }

    open fun aliasesChanged(username: String) = translate {
        EN - "$username changed the aliases"
        DE - "$username hat die Aliase verändert"
    }

    open fun historyVisibilityChange(username: String, groupOrChat: String, from: String, historyVisibility: String) =
        translate {
            EN - "$username has changed the history visibility of $groupOrChat ${from}to '$historyVisibility'"
            DE - "$username hat die Sichtbarkeit bestehender Nachrichten $groupOrChat ${from}zu '$historyVisibility' geändert"
        }

    open fun historyVisibilityShared() = translate {
        EN - "shared"
        DE - "geteilt"
    }

    open fun historyVisibilityJoined() = translate {
        EN - "joined"
        DE - "ab Beitritt"
    }

    open fun historyVisibilityWorldReadable() = translate {
        EN - "world_readable"
        DE - "allgemein lesbar"
    }

    open fun historyVisibiltyInvite() = translate {
        EN - "invited"
        DE - "ab Einladung"
    }

    open fun eventRoomAvatarChange(username: String, groupOrChat: String) = translate {
        EN - "$username has changed the avatar of $groupOrChat"
        DE - "$username hat den Avatar $groupOrChat geändert"
    }

    open fun invitationFrom(inviter: String) = translate {
        EN - "(Invitation from $inviter)"
        DE - "(Einladung von $inviter)"
    }

    open fun bootstrapErrorAccount(message: String? = commonUnknown()) = translate {
        EN - "Account creation failed: ${message ?: commonUnknown()}"
        DE - "Einrichtung des Kontos fehlgeschlagen: ${message ?: commonUnknown()}"
    }

    open fun verificationMethodSasDevice() = translate {
        EN - """        Please compare a set of emojis on this device and another device where this account is active.
        In case the emojis are different, please contact your administrator.""".trimIndent()
        DE - """        Vergleichen Sie eine Reihe von Emojis an diesem Gerät und einem anderen Gerät, auf dem Ihr Konto aktiviert ist.
        Sollten die Emojis nicht übereinstimmen, kontaktieren Sie bitte Ihren Administrator.""".trimIndent()
    }

    open fun verificationMethodSasUser() = translate {
        EN - "Compare a set of emojis with this user. Please use another trustworthy channel like a telefone call or direct conversation to compare the emojis."
        DE - "Vergleichen Sie eine Reihe von Emojis mit diesem Nutzer. Verwenden Sie dazu bitte einen anderen vertrauenswürdigen Kanal wie z.B. Telefon oder das direkte Gespräch um die Emojis zu vergleichen."
    }

    open fun verificationMethodSasUnknown() = translate {
        EN - "Unknown verification method that is currently not supported."
        DE - "Unbekannte Verifizierungsmethode, welche momentan nicht unterstützt wird."
    }

    open fun selfVerificationErrorMasterKey() = translate {
        EN - "Cannot verify with master key."
        DE - "Fehler beim Freischalten mit dem Generalschlüssel."
    }

    open fun selfVerificationErrorMasterPassphrase() = translate {
        EN - "Cannot verify with master passphrase."
        DE - "Fehler beim Freischalten mit dem Generalpasswort."
    }

    open fun userVerificationSuccess() = translate {
        EN - "Successful"
        DE - "Erfolgreich"
    }

    open fun userVerificationTimeout() = translate {
        EN - "Timeout"
        DE - "Zeitüberschreitung"
    }

    open fun userVerificationNoMatch() = translate {
        EN - "no match"
        DE - "keine Übereinstimmung"
    }

    open fun createNewRoomError(isChat: Boolean) = translate {
        EN - "Cannot create new ${if (isChat) "chat" else "group"}."
        DE - "${if (isChat) "Neuer Chat" else "Neue Gruppe"} kann nicht angelegt werden."
    }

    open fun createNewRoomBadJson(isChat: Boolean) = translate {
        EN - "Cannot create new ${if (isChat) "chat" else "group"}: bad JSON."
        DE - "${if (isChat) "Neuer Chat" else "Neue Gruppe"} kann nicht angelegt werden: JSON Fehler."
    }

    open fun createNewRoomRoomInUse(isChat: Boolean) = translate {
        EN - "Cannot create new ${if (isChat) "chat" else "group"}: alias already in use."
        DE - "${if (isChat) "Neuer Chat" else "Neue Gruppe"} kann nicht angelegt werden: Alias wird bereits verwendet."
    }

    open fun createNewRoomInvalidState(isChat: Boolean) = translate {
        EN - "Cannot create new ${if (isChat) "chat" else "group"}: invalid state (may be caused by bad power levels)."
        DE - "${if (isChat) "Neuer Chat" else "Neue Gruppe"} kann nicht angelegt werden: ungültiger Zustand (evtl. sind die Berechtigungslevel falsch)."
    }

    open fun createNewRoomInvalidRoomVersion(isChat: Boolean) = translate {
        EN - "Cannot create new ${if (isChat) "chat" else "group"}: the room version is invalid."
        DE - "${if (isChat) "Neuer Chat" else "Neue Gruppe"} kann nicht angelegt werden: die Raumversion ist veraltet."
    }

    open fun roomListYou() = translate {
        EN - "you"
        DE - "ich"
    }

    open fun roomListInvitationFrom(username: String) = translate {
        EN - "from $username"
        DE - "von $username"
    }

    open fun roomListInvitationOffline() = translate {
        EN - "You cannot accept invitations while you are offline."
        DE - "Sie können offline keine Einladungen annehmen."
    }

    open fun roomListInvitationError() = translate {
        EN - "There has been an error. Please try again later."
        DE - "Es gab einen Fehler. Bitte versuchen Sie es später."
    }

    open fun roomListKnockOffline() = translate {
        EN - "You cannot take your membership request back while you are offline."
        DE - "Sie können offline die Beitrittsanfrage nicht zurücknehmen."
    }

    open fun roomListKnockError() = translate {
        EN - "There has been an error. Please try again later."
        DE - "Es gab einen Fehler. Bitte versuchen Sie es später."
    }

    open fun roomListContentImage() = translate {
        EN - "Image"
        DE - "Bild"
    }

    open fun roomListContentVideo() = translate {
        EN - "Video"
        DE - "Video"
    }

    open fun roomListContentAudio() = translate {
        EN - "Audio"
        DE - "Audio"
    }

    open fun roomListContentFile() = translate {
        EN - "File"
        DE - "Datei"
    }

    open fun roomListContentVerificationRequest(username: String) = translate {
        EN - "User verification request for user $username"
        DE - "Anfrage für Nutzerverifikation von $username"
    }

    open fun roomListContentVerificationCancelled() = translate {
        EN - "User verification cancelled"
        DE - "Nutzerverifikation abgebrochen"
    }

    open fun roomListContentVerificationCompleted() = translate {
        EN - "User verification completed"
        DE - "Nutzerverifikation abgeschlossen"
    }

    open fun roomListContentVerificationInProgress() = translate {
        EN - "User verification in progress"
        DE - "Nutzerverifikation wird durchgeführt"
    }

    open fun roomHeaderTypingSingle(username: String) = translate {
        EN - "$username is typing..."
        DE - "$username schreibt..."
    }

    open fun roomHeaderTypingSingleDirect() = translate {
        EN - "is typing..."
        DE - "schreibt..."
    }

    open fun roomHeaderTypingMultiple(usernames: String) = translate {
        EN - "$usernames are typing..."
        DE - "$usernames schreiben..."
    }

    open fun roomHeaderTypingMultipleMore(usernames: String) = translate {
        EN - "$usernames and others are typing..."
        DE - "$usernames und andere schreiben..."
    }

    open fun connectingErrorStandard(message: String) = translate {
        EN - "Cannot connect to the Matrix server: $message"
        DE - "Matrix-Server kann nicht erreicht werden: $message"
    }

    open fun connectingErrorForbidden() = translate {
        EN - "Your credentials are not correct."
        DE - "Die Zugangsdaten sind nicht korrekt."
    }

    open fun connectingErrorNotFound() = translate {
        EN - "Cannot find a Matrix server with the given address."
        DE - "Kann keinen Matrix-Server unter der angegebenen Adresse finden."
    }

    open fun connectingErrorWrongAddress() = translate {
        EN - "The address of the Matrix server cannot be determined, or the address might be corrupt."
        DE - "Die Adresse des Matrix-Servers kann nicht bestimmt werden bzw. ist fehlerhaft."
    }

    open fun connectingErrorHttps() = translate {
        EN - "Only secure connections (https) are allowed."
        DE - "Es muss eine sichere Verbindung (https) genutzt werden."
    }

    open fun connectingErrorNoMatrixClient() = translate {
        EN - "No Matrix client could be initialized"
        DE - "Ein Matrix Client kann nicht erstellt werden."
    }

    open fun connectingAccountAlreadyExists(userId: UserId) = translate {
        EN - "There already is a local account for the user $userId."
        DE - "Es gibt bereits ein lokales Konto für den Nutzer $userId."
    }

    open fun connectingErrorDbLocked() = translate {
        EN - "This app seems to be running already. You cannot start more than one instance."
        DE - "Diese App läuft bereits. Sie können die App nur einmal starten."
    }

    open fun connectingErrorDbAccess() = translate {
        EN - "The local database cannot be accessed."
        DE - "Auf die lokale Datenbank kann nicht zugegriffen werden."
    }

    open fun logoutFailure() = translate {
        EN - "An error occurred during logout. Please try again later."
        DE - "Beim Ausloggen aus Ihrem Account gab es einen Fehler. Bitte versuchen Sie es später noch einmal."
    }

    open fun registrationTokenNotValid() = translate {
        EN - "the given registration is not valid"
        DE - "das Registrierungs-Token ist nicht gültig"
    }

    open fun registrationErrorNotSupported() = translate {
        EN - "You can only register a new user with a registration token."
        DE - "Sie können einen neuen Nutzer nur mit einem Registrierungs-Token anlegen."
    }

    open fun registrationErrorCannotDetermine() = translate {
        EN - "The methods for registration cannot be determined."
        DE - "Es ist nicht möglich die Registrierungsoptionen zu bestimmen."
    }

    open fun registrationErrorNotSuccessful() = translate {
        EN - "The registration has not been successful."
        DE - "Die Registrierung konnte nicht erfolgreich abgeschlossen werden."
    }

    open fun registrationErrorUserInUse() = translate {
        EN - "This username is already taken."
        DE - "Dieser Nutzername ist bereits vergeben."
    }

    open fun registrationErrorInvalidUsername() = translate {
        EN - "The username is invalid. Please use another one."
        DE - "Der gewählte Nutzername ist ungültig. Bitte wählen Sie einen anderen."
    }

    open fun settingsNotificationsSound() = translate {
        EN - "sound"
        DE - "Töne"
    }

    open fun settingsNotificationsSilent() = translate {
        EN - "silent"
        DE - "Stumm"
    }

    open fun settingsNotificationsVibration() = translate {
        EN - "vibration"
        DE - "Vibration"
    }

    open fun settingsNotificationsVibrationNot() = translate {
        EN - "no vibration"
        DE - "keine Vibration"
    }

    open fun settingsNotificationsLights() = translate {
        EN - "lights"
        DE - "Licht"
    }

    open fun settingsNotificationsLightsNot() = translate {
        EN - "no lights"
        DE - "kein Licht"
    }

    open fun settingsNotificationsPopup() = translate {
        EN - "show popups"
        DE - "zeige Popups"
    }

    open fun settingsNotificationsPopupNot() = translate {
        EN - "no popups"
        DE - "keine Popups"
    }

    open fun settingsNotificationsText() = translate {
        EN - "show text preview"
        DE - "Textvorschau"
    }

    open fun settingsNotificationsTextNot() = translate {
        EN - "no text preview"
        DE - "keine Textvorschau"
    }

    open fun settingsDevicesLoadError() = translate {
        EN - "Cannot load devices."
        DE - "Geräte können nicht geladen werden."
    }

    open fun settingsDevicesDisplayNameLastSeen(instant: Instant) = translate {
        val date = instant.toLocalDateTime(currentTimezone).date
        EN - "last seen: ${date.month.number}/${date.day}/${date.year}" // AE
        DE - "zuletzt gesehen: ${date.day}.${date.month.number}.${date.year}"
    }

    open fun settingsDevicesDisplayNameError() = translate {
        EN - "Cannot change the name of the device."
        DE - "Der Name des Geräts kann nicht geändert werden."
    }

    open fun settingsDevicesVerificationError() = translate {
        EN - "Cannot verify this device."
        DE - "Das Gerät kann Ihnen nicht zugeordnet werden."
    }

    open fun settingsDevicesRemoveConfirmationMessage(deviceName: String?, deviceId: String) = translate {
        EN - if (deviceName != null) "Are you sure you wish to remove the device \"$deviceName\" ($deviceId)?"
        else "Are you sure you wish to remove the device $deviceId?"
        DE - if (deviceName != null) "Sind sie sicher Sie wollen Gerät \"$deviceName\" ($deviceId) entfernen?"
        else "Sind sie sicher Sie wollen Gerät $deviceId entfernen?"
    }

    open fun settingsDevicesRemoveError(message: String? = commonUnknown()) = translate {
        EN - "The device cannot be removed: ${message ?: commonUnknown()}"
        DE - "Das Gerät kann nicht gelöscht werden: ${message ?: commonUnknown()}"
    }

    open fun settingsRoomAddMembersAnd() = translate {
        EN - "and"
        DE - "und"
    }

    open fun settingsRoomAddMembersErrorSingular(username: String) = translate {
        EN - "$username could not be invited."
        DE - "$username konnte nicht eingeladen werden."
    }

    open fun settingsRoomAddMembersErrorPlural(usernames: String) = translate {
        EN - "$usernames could not be invited."
        DE - "$usernames konnten nicht eingeladen werden."
    }

    open fun settingsRoomAddMembersErrorOffline() = translate {
        EN - "You cannot invite users when you are offline."
        DE - "Sie können niemanden einladen, wenn Sie offline sind."
    }

    open fun forgetRoomError(groupOrChat: String) = translate {
        EN - "There has been an error forgetting $groupOrChat."
        DE - "Fehler beim Vergessen $groupOrChat."
    }

    open fun forgetRoomErrorOffline() = translate {
        EN - "You cannot leave a chat or group when you are offline."
        DE - "Sie können offline keine Chats oder Gruppen verlassen."
    }

    open fun settingsRoomLeaveRoomErrorOffline() = translate {
        EN - "You cannot forget a chat or group when you are offline."
        DE - "Sie können offline keine Chats oder Gruppen vergessen."
    }

    open fun settingsRoomForgetRoomMessageChat() = translate {
        EN - "Forget chat"
        DE - "Chat vergessen"
    }

    open fun settingsRoomForgetRoomMessageGroup() = translate {
        EN - "Forget group"
        DE - "Gruppe vergessen"
    }

    open fun settingsRoomForgetRoomWarningConfirmButtonChat() = translate {
        EN - "Yes, forget chat"
        DE - "Ja, Chat vergessen"
    }

    open fun settingsRoomForgetRoomWarningConfirmButtonGroup() = translate {
        EN - "Yes, forget group"
        DE - "Ja, Gruppe vergessen"
    }

    open fun settingsRoomForgetRoomWarningTitleChat() = translate {
        EN - "Forget chat?"
        DE - "Den Chat vergessen?"
    }

    open fun settingsRoomForgetRoomWarningTitleGroup() = translate {
        EN - "Forget room?"
        DE - "Die Gruppe vergessen?"
    }

    open fun settingsRoomLeaveRoomError(groupOrChat: String) = translate {
        EN - "There has been an error leaving $groupOrChat."
        DE - "Fehler beim Verlassen $groupOrChat."
    }

    open fun settingsRoomLeaveRoomMessageChat() = translate {
        EN - "Leave chat"
        DE - "Chat verlassen"
    }

    open fun settingsRoomLeaveRoomMessageGroup() = translate {
        EN - "Leave group"
        DE - "Gruppe verlassen"
    }

    open fun settingsRoomLeaveRoomWarningConfirmButtonChat() = translate {
        EN - "Yes, leave chat"
        DE - "Ja, Chat verlassen"
    }

    open fun settingsRoomLeaveRoomWarningConfirmButtonGroup() = translate {
        EN - "Yes, leave group"
        DE - "Ja, Gruppe verlassen"
    }

    open fun settingsRoomLeaveRoomWarningMessageChat() = translate {
        EN - "You will not be able to access the contents of the chat afterwards."
        DE - "Sie können danach nicht mehr auf die Inhalte des Chats zugreifen."
    }

    open fun settingsRoomLeaveRoomWarningMessageGroup() = translate {
        EN - "You will not be able to access the contents of the group afterwards."
        DE - "Sie können danach nicht mehr auf die Inhalte der Gruppe zugreifen."
    }

    open fun settingsRoomLeaveRoomWarningTitleChat() = translate {
        EN - "Leave chat?"
        DE - "Den Chat verlassen?"
    }

    open fun settingsRoomLeaveRoomWarningTitleGroup() = translate {
        EN - "Leave room?"
        DE - "Die Gruppe verlassen?"
    }

    open fun settingsRoomChangeNameError() = translate {
        EN - "Failed to change the room name."
        DE - "Fehler beim Ändern des Raumnamens."
    }

    open fun settingsRoomChangeTopicError() = translate {
        EN - "Failed to change the room topic."
        DE - "Fehler beim Ändern der Raumbeschreibung."
    }

    open fun settingsRoomMemberListChangePowerLevelError(username: String) = translate {
        EN - "Failed to change the power level of user $username."
        DE - "Das Berechtigungslevel des Nutzers $username konnte nicht geändert werden."
    }

    open fun settingsRoomMemberListChangePowerLevelErrorOffline() = translate {
        EN - "You cannot change power levels of users when you are offline."
        DE - "Sie können offline das Berechtigungslevel eines Nutzers nicht ändern."
    }

    open fun settingsRoomMemberListChangePowerLevelInputValidationNotEntitled() = translate {
        EN - "You are not allowed to change the power level of a user."
        DE - "Sie dürfen das Berechtigungslevel eines Nutzers nicht verändern."
    }

    open fun settingsRoomMemberListChangePowerLevelInputValidationPowerLevelTooLow(maximum: Long) = translate {
        EN - "You can only set the user's power level to a maximum of $maximum."
        DE - "Sie können das Berechtigungslevel des Nutzers nur maximal auf $maximum setzen."
    }

    open fun settingsRoomMemberListChangePowerLevelInputValidationShouldBeNumber(maximum: Long) = translate {
        EN - "Please enter a valid number, up to a maximum of $maximum."
        DE - "Bitte geben Sie eine gültige Zahl ein, maximal jedoch $maximum."
    }

    open fun settingsRoomMemberListKickUserError() = translate {
        EN - "Failed to remove user."
        DE - "Der Nutzer konnte nicht entfernt werden."
    }

    open fun settingsRoomMemberListKickUserErrorOffline() = translate {
        EN - "You cannot remove users when you are offline."
        DE - "Sie können offline keine Nutzer entfernen."
    }

    open fun settingsRoomMemberListRoleAdmin() = translate {
        EN - "Administrator"
        DE - "Administrator"
    }

    open fun settingsRoomMemberListRoleModerator() = translate {
        EN - "Moderator"
        DE - "Moderator"
    }

    open fun settingsRoomMemberListRoleUser() = translate {
        EN - "User"
        DE - "Nutzer"
    }

    open fun settingsRoomNotificationsError() = translate {
        EN - "Cannot set room notifications."
        DE - "Fehler beim Setzen der Benachrichtigungseinstellungen."
    }

    open fun settingsRoomNotificationsAll() = translate {
        EN - "All messages"
        DE - "Alle Nachrichten"
    }

    open fun settingsRoomNotificationsMentions() = translate {
        EN - "Mentions"
        DE - "Erwähnungen"
    }

    open fun settingsRoomNotificationsSilent() = translate {
        EN - "Silent"
        DE - "Stumm"
    }

    open fun settingsRoomNotificationsDefault() = translate {
        EN - "Default"
        DE - "Standard"
    }

    open fun settingsRoomNotificationsAllExplanation() = translate {
        EN - "you are notified on every new message"
        DE - "Sie werden über jede neue Nachricht informiert"
    }

    open fun settingsRoomNotificationsMentionsExplanation() = translate {
        EN - "you are notified on new messages that are directed to you"
        DE - "Sie werden über Nachrichten informiert, die direkt an Sie gerichtet sind"
    }

    open fun settingsRoomNotificationsSilentExplanation() = translate {
        EN - "you are not notified on any new message"
        DE - "Sie erhalten keinerlei Benachrichtigungen über neue Nachrichten"
    }

    open fun settingsRoomNotificationsDefaultExplanation() = translate {
        EN - "you are notified as specified in the global settings"
        DE - "Sie werden so benachrichtigt, wie dies in den globalen Einstellungen festgelegt ist."
    }

    open fun settingsRoomHistoryVisibilityChangeError() = translate {
        EN - "Failed to change room history visibility."
        DE - "Fehler beim Ändern der Sichtbarkeit der Raumhistorie."
    }

    open fun settingsRoomHistoryVisibilityInsufficientPowerLevel() = translate {
        EN - "Insufficient power level to change room history visibility"
        DE - "Unzureichendes Berechtigungslevel um die Sichtbarkeit der Raumhistorie zu ändern"
    }

    open fun settingsRoomJoinRulesChangeError() = translate {
        EN - "Failed to change room join rules"
        DE - "Fehler beim Ändern der Raum-Beitrittsregeln"
    }

    open fun settingsRoomJoinRulesInsufficientPowerLevel() = translate {
        EN - "Insufficient power level to change room join rules"
        DE - "Unzureichendes Berechtigungslevel um die Raum-Beitrittsregeln zu ändern"
    }

    open fun settingsRoomAliasRemoveInsufficientPowerLevel() = translate {
        EN - "Insufficient power level to remove room alias"
        DE - "Unzureichendes Berechtigungslevel um diesen Alias zu entfernen"
    }

    open fun settingsRoomAliasChangeInvalidSyntax() = translate {
        EN - "Invalid room alias"
        DE - "Ungültiger Raumalias"
    }

    open fun settingsRoomAliasAddExists() = translate {
        EN - "Alias already exists"
        DE - "Raumalias existiert bereits"
    }

    open fun settingsRoomAliasBadAlias() = translate {
        EN - "This room alias is associated with another room"
        DE - "Dieser Raumalias ist mit einem anderen Raum assoziiert"
    }

    open fun settingsRoomAliasGeneric() = translate {
        EN - "Something went wrong"
        DE - "Etwas ist schiefgelaufen"
    }

    open fun settingsRoomAliasRemoveNotFound() = translate {
        EN - "Alias was already removed"
        DE - "Alias wurde bereits entfernt"
    }

    open fun settingsRoomAliasChangeMainInsufficientPowerLevel() = translate {
        EN - "Insufficient power level to change main room alias"
        DE - "Unzureichendes Berechtigungslevel um den Hauptalias zu ändern"
    }

    open fun settingsRoomAliasChangeMainUnrelatedAlias() = translate {
        EN - "Room alias not related to this room"
        DE - "Alias nicht mit diesem Raum assoziiert"
    }

    open fun settingsRoomAliasChangeMainNotFound() = translate {
        EN - "Couldn't find that main room alias"
        DE - "Konnte diesen Hauptalias nicht finden"
    }

    open fun settingsRoomAliasAddAliasInsufficientPowerLevel() = translate {
        EN - "Insufficient power level to add a room alias"
        DE - "Unzureichendes Berechtigungslevel um einen Alias hinzuzufügen"
    }

    open fun settingsRoomAliasAddAliasInvalid() = translate {
        EN - "Invalid Alias"
        DE - "Invalider Alias"
    }

    open fun settingsRoomAliasAddAliasExisting() = translate {
        EN - "Invalid Alias"
        DE - "Invalider Alias"
    }

    open fun settingsUnblockUserError(userId: String) = translate {
        EN - "Cannot unblock user '$userId'. Please try again later."
        DE - "Nutzer '$userId' kann nicht entblockt werden. Bitte versuchen Sie es später erneut."
    }

    open fun blockUserError(userId: String) = translate {
        EN - "Cannot block user '$userId'."
        DE - "Nutzer '$userId' kann nicht geblockt werden."
    }

    open fun profileLoadError() = translate {
        EN - "Profile could not be loaded"
        DE - "Beim Laden des Profils ist ein Fehler aufgetreten."
    }

    open fun profileAvatarError() = translate {
        EN - "The avatar image could not be changed. Please try again later."
        DE - "Das Nutzerbild konnte nicht geändert werden. Bitte versuchen Sie es später erneut."
    }

    open fun profileNameError() = translate {
        EN - "The username could not be changed."
        DE - "Der Nutzername konnte nicht geändert werden."
    }

    open fun profileNameForbidden() = translate {
        EN - "You are not allowed to change the username."
        DE - "Der Nutzername darf von Ihnen nicht geändert werden."
    }

    open fun matrixClientInitLoading() = translate {
        EN - "Loading..."
        DE - "Lade Daten..."
    }

    open fun matrixClientInitSuccess() = translate {
        EN - "Successfully loaded data."
        DE - "Daten erfolgreich geladen."
    }

    open fun timelineLeaveRoomErrorOffline() = settingsRoomLeaveRoomErrorOffline()
    open fun timelineLeaveRoomError(groupOrChat: String) = settingsRoomLeaveRoomError(groupOrChat)

    open fun timelineElementReadBy() = translate {
        EN - "Read by"
        DE - "Gelesen von"
    }

    open fun timelineElementRedactError() = translate {
        EN - "Cannot delete message."
        DE - "Fehler beim Löschen der Nachricht."
    }

    open fun serverDiscoveryFailed() = translate {
        EN - "Server could not be determined or is not valid."
        DE - "Server konnte nicht ermittelt werden oder ist nicht gültig."
    }

    open fun sendErrorEventPermission() = translate {
        EN - "You do not have permission to send a message in this room."
        DE - "Sie haben keine Rechte, um Nachrichten in diesem Raum zu versenden."
    }

    open fun sendErrorMediaPermission() = translate {
        EN - "You do not have permission to upload this file. The file type may not be supported or you reached an upload quota."
        DE - "Sie haben keine Rechte, um diese Datei hochzuladen. Möglicherweise wurde der Dateityp abgelehnt oder Sie haben Ihr maxinmales Kontingent erreicht."
    }

    open fun sendErrorMediaTooLarge() = translate {
        EN - "The file you want to upload is too large."
        DE - "Die Datei, die sich versuchen hochzuladen ist zu groß."
    }

    open fun sendErrorUnknown(errorMessage: String?) = translate {
        EN - "There was an unexpected error sending the message${if (errorMessage == null) "." else ": $errorMessage"}"
        DE - "Es gab einen unbekannten Fehler beim Absenden Ihrer Nachricht${if (errorMessage == null) "." else ": $errorMessage"}"
    }

    open fun attachmentSizeMaxSizeError(attachmentMaxSize: Long) = translate {
        val sizeInMB = attachmentMaxSize / 1.mb()
        EN - "The attachment exceeds the maximum allowed attachment size of $sizeInMB MB."
        DE - "Der Anhang überschreitet die maximal zulässige Größe für Anhänge von $sizeInMB MB."
    }

    open fun avatarSizeMaxSizeError(avatarMaxSize: Long) = translate {
        val sizeInMB = avatarMaxSize / 1.mb()
        EN - "The avatar exceeds the maximum allowed avatar size of $sizeInMB MB."
        DE - "Der Avatar überschreitet die maximal zulässige Größe für Avatare von $sizeInMB MB."
    }

    open fun profileCreationDuplicate() = translate {
        EN - "The profile name is already in use."
        DE - "Dieser Profilname wird bereits benutzt."
    }

    open fun exportRoomStateInit(total: Long) = translate {
        EN - "The export is being prepared. $total room events have already been found."
        DE - "Der Export wird vorbereitet. Es wurden bereits $total Raum-Ereignisse gefunden."
    }

    open fun exportRoomStateProcessed(processed: Long, total: Long) = translate {
        EN - "The export is being executed. $processed out of $total room events have already been processed."
        DE - "Der Export wird durchgeführt. Es wurden bereits $processed von $total Raum-Ereignisse verarbeitet."
    }

    open fun exportRoomStateFinished(total: Long) = translate {
        EN - "The export was successful. $total room events were processed."
        DE - "Der Export war erfolgreich. Es wurden $total Raum-Ereignisse verarbeitet."
    }

    open fun exportRoomErrorRoomNotFound() = translate {
        EN - "The room does not exist."
        DE - "Der Raum existiert nicht."
    }

    open fun exportRoomErrorPropertiesNotSupported() = translate {
        EN - "The export properties are not supported."
        DE - "Die Export-Eigenschaften werden nicht unterstützt."
    }

    open fun exportRoomErrorSink(message: String) = translate {
        EN - "There was an error during export: $message"
        DE - "Es gab einen Fehler beim Export: $message"
    }

    open fun exportRoomSuccessWithErrors(missingMedia: Int, deryptionFailures: Int) = translate {
        EN - "The export was successful, but some media could not be exported ($missingMedia) and some messages could not be decrypted ($deryptionFailures)."
        DE - "Der Export war erfolgreich, dennoch konnten einige Medien nicht exportiert ($missingMedia) und einige Nachrichten nicht entschlüsselt werden ($deryptionFailures)."
    }

    open fun exportRoomEmote(message: String) = translate {
        EN - "* $message"
        DE - "* $message"
    }

    open fun exportRoomNotice(message: String) = translate {
        EN - "// $message"
        DE - "// $message"
    }

    open fun exportRoomImage(fileName: String) = translate {
        EN - "image: $fileName"
        DE - "Bild: $fileName"
    }

    open fun exportRoomAudio(fileName: String) = translate {
        EN - "audio: $fileName"
        DE - "Audio: $fileName"
    }

    open fun exportRoomVideo(fileName: String) = translate {
        EN - "video: $fileName"
        DE - "Video: $fileName"
    }

    open fun exportRoomFile(fileName: String) = translate {
        EN - "file: $fileName"
        DE - "Datei: $fileName"
    }

    open fun exportRoomLocation(name: String, uri: String) = translate {
        EN - "location: $name $uri"
        DE - "Ort: $name $uri"
    }

    open fun exportRoomState(message: String) = translate {
        EN - "state change: $message"
        DE - "Zustandsänderung: $message"
    }

    open fun exportRoomAvatar(url: String?) = translate {
        EN - "room avatar has been changed to $url"
        DE - "Raumbild wurde zu $url geändert"
    }

    open fun exportRoomCanonicalAlias(aliases: List<RoomAliasId>) = translate {
        EN - "room aliases has been changed to $aliases"
        DE - "Raumaliase wurden zu $aliases geändert"
    }

    open fun exportRoomCreate(federate: Boolean?, roomType: String?) = translate {
        EN - "room has been created (federation=$federate, type=$roomType)"
        DE - "Raum wurde erstellt (Föderation=$federate, Typ=$roomType)"
    }

    open fun exportRoomJoinRule(joinRule: String) = translate {
        EN - "join rule has been changed to $joinRule"
        DE - "Beitrittsregel wurde zu $joinRule geändert"
    }

    open fun exportRoomMember(
        userId: String?,
        membership: String,
        displayName: String?,
        avatarUrl: String?,
        reason: String?
    ) = translate {
        EN - "changes of the member properties of $userId (membership=$membership, displayname=$displayName, avatar=$avatarUrl, reason=$reason)"
        DE - "Änderungen der Mitgliedseigenschaften von $userId (Mitgliedschaft=$membership, Anzeigename=$displayName, Anzeigebild=$avatarUrl, Grund=$reason)"
    }

    open fun exportRoomName(name: String) = translate {
        EN - "room name has been changed to \"$name\""
        DE - "Raumname wurde zu \"$name\" geändert"
    }

    open fun exportRoomTopic(name: String) = translate {
        EN - "room name has been changed to \"$name\""
        DE - "Raumname wurde zu \"$name\" geändert"
    }

    open fun exportRoomEncryption() = translate {
        EN - "room encryption has been enabled"
        DE - "Raumverschlüsselung wurde aktiviert"
    }

    open fun exportRoomHistoryVisibility(historyVisibility: String) = translate {
        EN - "history visibility has been changed to $historyVisibility"
        DE - "Sichtbarkeit wurde zu $historyVisibility geändert"
    }

    open fun exportRoomGuestAccess(access: String) = translate {
        EN - "guest access has been changed to $access"
        DE - "Gastbeitritt wurde zu $access geändert"
    }

    open fun exportRoomTombstone(body: String, roomId: RoomId) = translate {
        EN - "room has been replaced by new room $roomId: $body"
        DE - "Raum wurde durch den neuen Raum $roomId ersetzt: $body"
    }

    open fun exportRoomRedacted(by: UserId?, reason: String?) = translate {
        EN - "* message has been deleted by $by, reason: $reason"
        DE - "* Nachricht wurde von $by gelöscht, Grund: $reason"
    }

    open fun exportRoomDecryptionError() = translate {
        EN - "* message cannot be decrypted"
        DE - "* Nachricht konnte nicht entschlüsselt werden"
    }

    open fun mediaCouldNotBeRead() = translate {
        EN - "File could not be read"
        DE - "Datei konnte nicht gelesen werden"
    }

    open fun mediaCanNotBePreviewed() = translate {
        EN - "File can not be previewed."
        DE - "Datei kann nicht angezeigt werden."
    }

    open fun mediaTooLargeForPreview() = translate {
        EN - "File is too large for previewing. Try downloading it instead."
        DE - "Datei ist zu groß für die Vorschau. Versuchen Sie stattdessen, die Datei herunterzuladen."
    }

    open fun updateNotificationSettingsError(error: String) = translate {
        EN - "There was an error updating the notification settings: $error"
        DE - "Es gab einen Fehler beim Aktualisieren der Benachrichtigungseinstellungen: $error"
    }

    open fun updateNotificationSettingsTimeoutError() = translate {
        EN - "There was an error updating the notification settings: timeout"
        DE - "Es gab einen Fehler beim Aktualisieren der Benachrichtigungseinstellungen: Zeitüberschreitung"
    }

    open fun yourNewProfileAvatar() = translate {
        EN - "Your new profile avatar"
        DE - "Ihr neues Profilbild"
    }

    open fun yourNewRoomAvatar() = translate {
        EN - "Your new room image"
        DE - "Ihr neues Raumbild"
    }

    open fun roomEncryptionEnableError() = translate {
        EN - "There was an error enabling the end-to-end encryption"
        DE - "Es gab einen Fehler beim Aktivieren der Ende-zu-Ende Verschlüsselung"
    }

    open fun roomEncryptionAlreadyEnabledError() = translate {
        EN - "There was an error enabling the end-to-end encryption: the encryption was already enabled"
        DE - "Es gab einen Fehler beim Aktivieren der Ende-zu-Ende Verschlüsselung: Die Verschlüsselung ist bereits aktiviert"
    }

    open fun roomEncryptionEnabled(user: String) = translate {
        EN - "$user enabled end-to-end encryption"
        DE - "$user hat die Ende-zu-Ende Verschlüsselung aktiviert"
    }

    open fun settingsRoomMemberBanUserError() = translate {
        EN - "There was an error banning this user"
        DE - "Es gab einen Fehler beim Bannen dieses Teilnehmers"
    }

    open fun settingsRoomMemberBanUserErrorNotPossible() = translate {
        EN - "You are unable to ban this user"
        DE - "Sie können diesen Teilnehmer nicht bannen"
    }

    open fun settingsRoomMemberBanUserErrorOffline() = translate {
        EN - "You cannot ban users when you are offline"
        DE - "Sie können offline keine Teilnehmer bannen"
    }

    open fun userProfileMembershipChanging() = translate {
        EN - "Membership is still changing at the momenet"
        DE - "Mitgliedschaft wird aktuell noch verändert"
    }

    open fun settingsRoomMemberUnbanUserError() = translate {
        EN - "There was an error unbanning the user"
        DE - "Es gab einen Fehler beim Entbannen des Teilnehmers"
    }

    open fun settingsRoomMemberUnbanUserErrorNotPossible() = translate {
        EN - "You are unable to unban the user"
        DE - "Sie können den Teilnehmer nicht entbannen"
    }

    open fun settingsRoomMemberUnbanUserErrorOffline() = translate {
        EN - "You cannot unban users when you are offline"
        DE - "Sie können offline keine Teilnehmer entbannen"
    }

    open fun timelineElementDecryptionErrorAlgorithmNotSupported() = translate {
        EN - "Decryption algorithm not supported."
        DE - "Verschlüsselungsalgorithmus wird nicht unterstützt."
    }

    open fun timelineElementDecryptionErrorTimeout() = translate {
        EN - "Decryption took too much time."
        DE - "Entschlüsselung hat zu lange gedauert."
    }

    open fun timelineElementDecryptionErrorNoContent() = translate {
        EN - "This message has been edited, but the new content could not be found."
        DE - "Diese Nachricht wurde editiert, aber der neue Inhalt konnte nicht gefunden werden."
    }

    open fun timelineElementDecryptionErrorGeneric(error: String?) = translate {
        EN - "There was an error decrypting this message: ${error ?: commonUnknown()}"
        DE - "Es gab einen Fehler beim Entschlüsseln der Nachricht: ${error ?: commonUnknown()}"
    }

    open fun downloadFailed(error: String?) = translate {
        EN - "Download failed: ${error ?: commonUnknown()}"
        DE - "Herunterladen fehlgeschlagen: ${error ?: commonUnknown()}"
    }

    open fun alreadyRunningError(appName: String) = translate {
        EN - "$appName is already running in another tab or window. Please close it first."
        DE - "$appName läuft bereits in einem anderen Tab oder Fenster. Bitte schließen Sie es zuerst."
    }

    open fun searchGroupFailedSearch() = translate {
        EN - "Searching rooms failed"
        DE - "Raumsuche ist fehlgeschlagen"
    }

    open fun searchGroupJoinFailedGeneric() = translate {
        EN - "Joining the room failed"
        DE - "Raumbeitritt ist fehlgeschlagen"
    }

    open fun enterRoomFailedGenericJoin() = translate {
        EN - "Joining the room failed"
        DE - "Raumbeitritt ist fehlgeschlagen"
    }

    open fun enterRoomFailedRestricted() = translate {
        EN - "Unqualified to join this room"
        DE - "Unqualifiziert diesen Raum beizutreten"
    }

    open fun enterRoomFailedInvite() = translate {
        EN - "Joining room is invite-only"
        DE - "Kann Raum nur auf Einladung betreten"
    }

    open fun enterRoomFailedGenericKnock() = translate {
        EN - "Membership request failed"
        DE - "Raummitgliedschaftssanfrage ist fehlgeschlagen"
    }

    open fun enterRoomFailedNoPermission() = translate {
        EN - "No Permission to join this room"
        DE - "Fehlende Berechtigung diesem Raum beizutreten"
    }

    open fun enterRoomFailedRoomDoesNotExist() = translate {
        EN - "Room does not exist"
        DE - "Raum existiert nicht"
    }

    open fun acceptKnockFailed() = translate {
        EN - "Failed to accept the membership request"
        DE - "Beitrittanfrage annehmen ist fehlgeschlagen"
    }

    open fun rejectKnockFailed() = translate {
        EN - "Failed to reject the membership request"
        DE - "Beitrittanfrage ablehnen ist fehlgeschlagen"
    }

    open fun banningFailed() = translate {
        EN - "Failed to ban user"
        DE - "Bannen dieses Teilnehmers ist fehlgeschlagen"
    }

    open fun deactivateAccountConfirmationMessage(userId: String) = translate {
        EN - "Attention, if you continue, your entire account ($userId) will be deleted. You will lose access to all data (chats, message content, recovery key, etc.) and will no longer be able to log in with this account. There is no way to restore this data afterwards."
        DE - "Achtung, wenn sie Fortfahren wird ihr gesamter Account ($userId) gelöscht. Sie verlieren somit den Zugriff auf alle Daten (Chats, Nachrichteninhalte, Widerherstellungsschlüssel, etc.) und können sich nicht mehr mit diesem Account anmelden. Es gibt keine Möglichkeit, diese Daten im Nachhinein wieder herzustellen."
    }

    open fun deactivateAccountError(message: String) = translate {
        EN - "Account could not be deactivated: $message"
        DE - "Der Account konnte nicht deaktiviert werden: $message"
    }

    open fun powerLevelUpdateBan(powerLevel: Long) = translate {
        EN - when (powerLevel) {
            0L -> "Users can now ban users"
            50L -> "Moderators can now ban users"
            100L -> "Administrators can now ban users"
            else -> "Power level of $powerLevel can now ban users"
        }
        DE - when (powerLevel) {
            0L -> "Nutzer können nun Nutzer bannen"
            50L -> "Moderatoren können nun Nutzer bannen"
            100L -> "Administratoren können nun Nutzer bannen"
            else -> "Berechtigungslevel $powerLevel kann nun Nutzer bannen"
        }
    }

    open fun powerLevelUpdateInvite(powerLevel: Long) = translate {
        EN - when (powerLevel) {
            0L -> "Users can now invite users"
            50L -> "Moderators can now invite users"
            100L -> "Administrators can now invite users"
            else -> "Power level of $powerLevel can now invite users"
        }
        DE - when (powerLevel) {
            0L -> "Nutzer können nun Nutzer einladen"
            50L -> "Moderatoren können nun Nutzer einladen"
            100L -> "Administratoren können nun Nutzer einladen"
            else -> "Berechtigungslevel $powerLevel kann nun Nutzer einladen"
        }
    }

    open fun powerLevelUpdateKick(powerLevel: Long) = translate {
        EN - when (powerLevel) {
            0L -> "Users can now kick users"
            50L -> "Moderators can now kick users"
            100L -> "Administrators can now kick users"
            else -> "Power level of $powerLevel can now kick users"
        }
        DE - when (powerLevel) {
            0L -> "Nutzer können nun Nutzer entfernen"
            50L -> "Moderatoren können nun Nutzer entfernen"
            100L -> "Administratoren können nun Nutzer entfernen"
            else -> "Berechtigungslevel $powerLevel kann nun Nutzer entfernen"
        }
    }

    open fun powerLevelUpdateRedact(powerLevel: Long) = translate {
        EN - when (powerLevel) {
            0L -> "Users can now delete messages"
            50L -> "Moderators can now delete messages"
            100L -> "Administrators can now delete messages"
            else -> "Power level of $powerLevel can now delete messages"
        }
        DE - when (powerLevel) {
            0L -> "Nutzer können nun Nachrichten löschen"
            50L -> "Moderatoren können nun Nachrichten löschen"
            100L -> "Administratoren können nun Nachrichten löschen"
            else -> "Berechtigungslevel $powerLevel kann nun Nachrichten löschen"
        }
    }

    open fun powerLevelUpdateStateDefault(powerLevel: Long) = translate {
        EN - when (powerLevel) {
            0L -> "Users can now change room settings"
            50L -> "Moderators can now change room settings"
            100L -> "Administrators can now change room settings"
            else -> "Powerlevel of $powerLevel can now change room settings"
        }
        DE - when (powerLevel) {
            0L -> "Nutzer können nun Raumeinstellungen ändern"
            50L -> "Moderatoren können nun Raumeinstellungen ändern"
            100L -> "Administratoren können nun Raumeinstellungen ändern"
            else -> "Berechtigungslevel $powerLevel kann nun Raumeinstellungen ändern"
        }
    }

    open fun powerLevelUpdateEventsDefault(powerLevel: Long) = translate {
        EN - when (powerLevel) {
            0L -> "Users can now send all event types"
            50L -> "Moderators can now send all event types"
            100L -> "Administrators can now send all event types"
            else -> "Powerlevel of $powerLevel can now send all event types"
        }
        DE - when (powerLevel) {
            0L -> "Nutzer können nun alle Ereignistypen senden"
            50L -> "Moderatoren können nun alle Ereignistypen senden"
            100L -> "Administratoren können nun alle Ereignistypen senden"
            else -> "Berechtigungslevel $powerLevel kann nun alle Ereignistypen senden"
        }
    }


    open fun powerLevelUpdateUsersDefault(powerLevel: Long) = translate {
        EN - when (powerLevel) {
            0L -> "Default power level is set to User"
            50L -> "Default power level is set to Moderator"
            100L -> "Default power level is set to Administrator"
            else -> "Default power level is set to $powerLevel"
        }
        DE - when (powerLevel) {
            0L -> "Standard-Berechtigungslevel ist nun Nutzer"
            50L -> "Standard-Berechtigungslevel ist nun Moderatore"
            100L -> "Standard-Berechtigungslevel ist nun Administratoren"
            else -> "Standard-Berechtigungslevel ist nun $powerLevel"
        }
    }

    open fun powerLevelUpdateNoChanges() = translate {
        EN - "No changes to power levels"
        DE - "Keine Änderungen der Berechtigungslevel"
    }

    open fun powerLevelUpdateNChanges(n: Int) = translate {
        EN - "$n power level changes occurred"
        DE - "$n Berechtigungslevel wurden geändert"
    }

    open fun powerLevelUpdateEvent(event: String, powerLevel: Long) = translate {
        var rendered = when (powerLevel) {
            0L -> "user"
            50L -> "moderator"
            100L -> "administrator"
            else -> "power level $powerLevel"
        }

        EN - when (event) {
            "m.room.avatar" -> "Power level for changing the room avatar set to $rendered"
            "m.room.name" -> "Power level for changing the room name set to $rendered"
            "m.room.topic" -> "Power level for changing the room topic set to $rendered"
            "m.room.member" -> "Power level for membership changes set to $rendered"
            "m.room.power_levels" -> "Power level for changing power levels set to $rendered"
            "m.room.join_rules" -> "Power level for changing join rules set to $rendered"
            "m.room.history_visibility" -> "Power level for changing history visibility set to $rendered"
            "m.room.encryption" -> "Power level for enabling encryption set to $rendered"
            "m.room.pinned_events" -> "Power level for pinning events set to $rendered"
            "m.room.canonical_alias" -> "Power level for changing the main room alias set to $rendered"
            "m.room.server_acl" -> "Power level for changing server ACL set to $rendered"
            "m.room.tombstone" -> "Power level for upgrading the room set to $rendered"

            "m.room.message" -> "Power level for sending messages set to $rendered"
            "m.reaction" -> "Power level for sending reactions set to $rendered"
            "m.room.redaction" -> "Power level for message redactions set to $rendered"
            "m.room.encrypted" -> "Power level for sending encrypted messages set to $rendered"

            "m.key.verification.start" -> "Power level for starting key verification set to $rendered"
            "m.key.verification.ready" -> "Power level for ready key verification set to $rendered"
            "m.key.verification.accept" -> "Power level for accepting key verification set to $rendered"
            "m.key.verification.key" -> "Power level for exchanging verification keys set to $rendered"
            "m.key.verification.mac" -> "Power level for MAC verification set to $rendered"
            "m.key.verification.done" -> "Power level for completing key verification set to $rendered"
            "m.key.verification.cancel" -> "Power level for cancelling key verification set to $rendered"

            "m.call.invite" -> "Power level for call invites set to $rendered"
            "m.call.candidates" -> "Power level for call candidates set to $rendered"
            "m.call.answer" -> "Power level for answering calls set to $rendered"
            "m.call.hangup" -> "Power level for hanging up calls set to $rendered"
            "m.call.negotiate" -> "Power level for call negotiation set to $rendered"
            "m.call.reject" -> "Power level for rejecting calls set to $rendered"
            "m.call.select_answer" -> "Power level for selecting call answers set to $rendered"
            "m.call.sdp_stream_metadata_changed" -> "Power level for SDP stream metadata changes set to $rendered"

            else -> "Power level for $event set to $rendered"
        }

        rendered = when (powerLevel) {
            0L -> "Nutzer"
            50L -> "Moderator"
            100L -> "Administrator"
            else -> "Berechtigungslevel $powerLevel"
        }

        DE - when (event) {
            "m.room.avatar" -> "Berechtigungslevel für das Ändern des Raumavatars auf $rendered gesetzt"
            "m.room.name" -> "Berechtigungslevel für das Ändern des Raumnamens auf $rendered gesetzt"
            "m.room.topic" -> "Berechtigungslevel für das Ändern des Raumthemas auf $rendered gesetzt"
            "m.room.member" -> "Berechtigungslevel für Mitgliederänderungen auf $rendered gesetzt"
            "m.room.power_levels" -> "Berechtigungslevel für das Ändern der Berechtigungen auf $rendered gesetzt"
            "m.room.join_rules" -> "Berechtigungslevel für Beitrittsregeln auf $rendered gesetzt"
            "m.room.history_visibility" -> "Berechtigungslevel für Verlaufssichtbarkeit auf $rendered gesetzt"
            "m.room.encryption" -> "Berechtigungslevel für das Aktivieren der Verschlüsselung auf $rendered gesetzt"
            "m.room.pinned_events" -> "Berechtigungslevel für das Anheften von Ereignissen auf $rendered gesetzt"
            "m.room.canonical_alias" -> "Berechtigungslevel für den Hauptalias des Raums auf $rendered gesetzt"
            "m.room.server_acl" -> "Berechtigungslevel für Server-ACL auf $rendered gesetzt"
            "m.room.tombstone" -> "Berechtigungslevel für Raumaktualisierungen auf $rendered gesetzt"

            "m.room.message" -> "Berechtigungslevel für das Senden von Nachrichten auf $rendered gesetzt"
            "m.reaction" -> "Berechtigungslevel für das Senden von Reaktionen auf $rendered gesetzt"
            "m.room.redaction" -> "Berechtigungslevel für das Entfernen von Nachrichten auf $rendered gesetzt"
            "m.room.encrypted" -> "Berechtigungslevel für das Senden von verschlüsselten Nachrichten auf $rendered gesetzt"

            "m.key.verification.start" -> "Berechtigungslevel für das Starten der Verifizierung auf $rendered gesetzt"
            "m.key.verification.ready" -> "Berechtigungslevel für die Bereitschaft zur Verifizierung auf $rendered gesetzt"
            "m.key.verification.accept" -> "Berechtigungslevel für das Akzeptieren der Verifizierung auf $rendered gesetzt"
            "m.key.verification.key" -> "Berechtigungslevel für das Senden des Verifizierungsschlüssels auf $rendered gesetzt"
            "m.key.verification.mac" -> "Berechtigungslevel für das Senden des Verifizierungs-MAC auf $rendered gesetzt"
            "m.key.verification.done" -> "Berechtigungslevel für das Abschließen der Verifizierung auf $rendered gesetzt"
            "m.key.verification.cancel" -> "Berechtigungslevel für das Abbrechen der Verifizierung auf $rendered gesetzt"

            "m.call.invite" -> "Berechtigungslevel für das Starten von Anrufen auf $rendered gesetzt"
            "m.call.candidates" -> "Berechtigungslevel für das Austauschen von Anrufkandidaten auf $rendered gesetzt"
            "m.call.answer" -> "Berechtigungslevel für das Annehmen von Anrufen auf $rendered gesetzt"
            "m.call.hangup" -> "Berechtigungslevel für das Beenden von Anrufen auf $rendered gesetzt"
            "m.call.negotiate" -> "Berechtigungslevel für das Aushandeln von Anrufparametern auf $rendered gesetzt"
            "m.call.reject" -> "Berechtigungslevel für das Ablehnen von Anrufen auf $rendered gesetzt"
            "m.call.select_answer" -> "Berechtigungslevel für das Auswählen von Anrufantworten auf $rendered gesetzt"
            "m.call.sdp_stream_metadata_changed" -> "Berechtigungslevel für das Aktualisieren von Anruf-Stream-Metadaten auf $rendered gesetzt"

            else -> "Berechtigungslevel für $event auf $rendered gesetzt"
        }
    }

    open fun powerLevelWronglyConfiguredError() = translate {
        EN - "The power levels were wrongly configured"
        DE - "Die Berechtigungslevel sind falsch konfiguriert"
    }

    open fun powerLevelInputErrNotANumber() = translate {
        EN - "not a number"
        DE - "keine Zahl"
    }

    open fun powerLevelInputErrAboveAllowedPowerLevel(maxPowerLevel: Long) = translate {
        EN - "power level has to be below $maxPowerLevel"
        DE - "Berechtigungslevel muss unter $maxPowerLevel sein"
    }

    open fun roomUpgraded(message: String) = translate {
        EN - "This room has been upgraded. Message from the Admins: \"$message\""
        DE - "Dieser Raum wurde aktualisiert. Nachricht von den Admins: \"$message\""
    }

    open fun newEventAlreadyExistsErr() = translate {
        EN - "This event id exists already"
        DE - "Diese Event Id existiert bereits"
    }
}

internal fun getLang(
    languages: Languages,
    settings: MatrixMessengerSettingsHolder,
    getSystemLang: GetSystemLang
): Language {
    val preferredLang = settings.value.base.preferredLang
    log.trace { "preferred language: $preferredLang" }
    return preferredLang?.let { languages.langOf(it) }
        ?: languages.langOf(getSystemLang())
        ?: EN// fallback is english
}

internal suspend fun setLang(language: Language, settings: MatrixMessengerSettingsHolder) {
    settings.update<MatrixMessengerSettingsBase> { it.copy(preferredLang = language.code) }
}

package de.connect2x.trixnity.messenger.i18n

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages.DE
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages.EN
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId

private val log = KotlinLogging.logger { }

// TODO this is not lazy -> use property delegation or one class for one language instead
abstract class I18n(languages: Languages, settings: MatrixMessengerSettingsHolder, getSystemLang: GetSystemLang) :
    I18nBase(languages, settings, getSystemLang) {

    // ---- translations -----
    fun commonUnknown() = translate {
        EN - "unknown"
        DE - "unbekannt"
    }

    fun commonAnd(first: String, second: String) = translate {
        EN - "$first and $second"
        DE - "$first und $second"
    }

    fun commonUs() = translate {
        EN - "us"
        DE - "uns"
    }

    fun commonCancelled() = translate {
        EN - "cancelled"
        DE - "abgebrochen"
    }

    fun roomNameInvitation() = translate {
        EN - "Invitation"
        DE - "Einladung"
    }

    fun roomNameInvitationFrom(groupOrChat: String, roomName: String) = translate {
        EN - "Invitation into $groupOrChat '$roomName'"
        DE - "Einladung in $groupOrChat '$roomName'"
    }

    fun roomNameEmptyChat() = translate {
        EN - "Empty chat"
        DE - "Leerer Chat"
    }

    fun roomNameEmptyChatWas(oldName: String) = translate {
        EN - "Empty chat (was $oldName)"
        DE - "Leerer Chat (war $oldName)"
    }

    fun roomNameAnd() = translate {
        EN - "and"
        DE - "und"
    }

    fun roomNameChat() = translate {
        EN - "chat"
        DE - "Chat"
    }

    fun roomNameGroup() = translate {
        EN - "group"
        DE - "Gruppe"
    }

    fun roomNameOther(othersCount: Long) = translate {
        EN - if (othersCount == 1L) "one other" else "$othersCount others"
        DE - if (othersCount == 1L) "ein anderer" else "$othersCount andere"
    }

    fun eventChangeAvatar(username: String) = translate {
        EN - "$username has changed the avatar image"
        DE - "$username hat das Profilbild geändert"
    }

    fun eventChangeDisplayName(oldDisplayName: String?, newDisplayName: String?) = translate {
        EN - "'$oldDisplayName' has changed their name to '$newDisplayName'"
        DE - "'$oldDisplayName' hat den Namen zu '$newDisplayName' geändert"
    }

    fun eventChangeDirectRoom(isDirect: Boolean) = translate {
        EN - if (isDirect) "Group has been converted to Chat" else "Chat has been converted to Group"
        DE - if (isDirect) "Gruppe wurde in einen Chat umgewandelt" else "Chat wurde in eine Gruppe umgewandelt"
    }

    fun eventChangeChatGenitive() = translate {
        EN - "the chat"
        DE - "des Chats"
    }

    fun eventChangeChatDative() = translate {
        EN - "the chat"
        DE - "dem Chat"
    }

    fun eventChangeChatAccusative() = translate {
        EN - "the chat"
        DE - "den Chat"
    }

    fun eventChangeGroupGenitive() = translate {
        EN - "the group"
        DE - "der Gruppe"
    }

    fun eventChangeGroupDative() = translate {
        EN - "the group"
        DE - "der Gruppe"
    }

    fun eventChangeGroupAccusative() = translate {
        EN - "the group"
        DE - "die Gruppe"
    }

    fun eventChangeInvite(invitee: String, inviter: String) = translate {
        EN - "$invitee has been invited by $inviter"
        DE - "$invitee wurde von $inviter eingeladen"
    }

    fun eventChangeJoin(username: String, groupOrChat: String) = translate {
        EN - "$username has joined $groupOrChat"
        DE - "$username ist $groupOrChat beigetreten"
    }

    fun eventChangeLeave(username: String, groupOrChat: String) = translate {
        EN - "$username has left $groupOrChat"
        DE - "$username hat $groupOrChat verlassen"
    }

    fun eventChangeBan(username: String, banner: String, groupOrChat: String) = translate {
        EN - "$username has been removed by $banner from $groupOrChat"
        DE - "$username wurde von $banner aus $groupOrChat ausgeschlossen"
    }

    fun eventChangeKnock(username: String, groupOrChat: String) = translate {
        EN - "$username wants to join $groupOrChat"
        DE - "$username möchte $groupOrChat beitreten"
    }

    fun eventMessageRedacted(username: String) = translate {
        EN - "message has been deleted by $username"
        DE - "Nachricht wurde von $username gelöscht"
    }

    fun eventMessageRedactedByMe() = translate {
        EN - "You deleted this message"
        DE - "Sie haben diese Nachricht gelöscht"
    }

    fun eventMessageRedactedByUnknown() = translate {
        EN - "This message has been deleted"
        DE - "Diese Nachricht ist gelöscht worden"
    }

    fun eventRoomCreated(username: String, groupOrChat: String) = translate {
        EN - "$username has created $groupOrChat"
        DE - "$username hat $groupOrChat erstellt"
    }

    fun eventChangeFrom(oldName: String) = translate {
        EN - "from '$oldName' "
        DE - "von '$oldName' "
    }

    fun eventRoomNameChange(username: String, groupOrChat: String, from: String, roomName: String) = translate {
        EN - "$username has changed the name of $groupOrChat ${from}to '$roomName'"
        DE - "$username hat den Namen $groupOrChat ${from}zu '$roomName' geändert"
    }

    fun eventRoomTopicChange(username: String, groupOrChat: String, from: String, roomName: String) = translate {
        EN - "$username has changed the topic of $groupOrChat ${from}to '$roomName'"
        DE - "$username hat die Beschreibung $groupOrChat ${from}zu '$roomName' geändert"
    }

    fun historyVisibilityChange(username: String, groupOrChat: String, from: String, historyVisibility: String) = translate {
        EN - "$username has changed the history visibility of $groupOrChat ${from}to '$historyVisibility'"
        DE - "$username hat die Sichtbarkeit bestehender Nachrichten $groupOrChat ${from}zu '$historyVisibility' geändert"
    }

    fun historyVisibilityShared() = translate {
        EN - "shared"
        DE - "geteilt"
    }

    fun historyVisibilityJoined() = translate {
        EN - "joined"
        DE - "ab Beitritt"
    }

    fun historyVisibilityWorldReadable() = translate {
        EN - "world_readable"
        DE - "allgemein lesbar"
    }

    fun historyVisibiltyInvite() = translate {
        EN - "invited"
        DE - "ab Einladung"
    }

    fun eventRoomAvatarChange(username: String, groupOrChat: String) = translate {
        EN - "$username has changed the avatar of $groupOrChat"
        DE - "$username hat den Avatar $groupOrChat geändert"
    }

    fun invitationFrom(inviter: String) = translate {
        EN - "(Invitation from $inviter)"
        DE - "(Einladung von $inviter)"
    }

    fun bootstrapErrorAccount() = translate {
        EN - "Account creation failed"
        DE - "Einrichtung des Kontos fehlgeschlagen"
    }

    fun bootstrapErrorLogin() = translate {
        EN - "There has been an error trying to log into your account."
        DE - "Es gab einen Fehler beim Einloggen mit Ihren Kontodaten."
    }

    fun verificationMethodSasDevice() = translate {
        EN - """Please compare a set of emojis on this device and another device where this account is active.
        In case the emojis are different, please contact your administrator.""".trimIndent()
        DE - """Vergleichen Sie eine Reihe von Emojis an diesem Gerät und einem anderen Gerät, auf dem Ihr Konto aktiviert ist.
        Sollten die Emojis nicht übereinstimmen, kontaktieren Sie bitte Ihren Administrator.""".trimIndent()
    }

    fun verificationMethodSasUser() = translate {
        EN - "Compare a set of emojis with this user. Please use another trustworthy channel like a telefone call or direct conversation to compare the emojis."
        DE - "Vergleichen Sie eine Reihe von Emojis mit diesem Nutzer. Verwenden Sie dazu bitte einen anderen vertrauenswürdigen Kanal wie z.B. Telefon oder das direkte Gespräch um die Emojis zu vergleichen."
    }

    fun verificationMethodSasUnknown() = translate {
        EN - "Unknown verification method that is currently not supported."
        DE - "Unbekannte Verifizierungsmethode, welche momentan nicht unterstützt wird."
    }

    fun selfVerificationErrorMasterKey() = translate {
        EN - "Cannot verify with master key."
        DE - "Fehler beim Freischalten mit dem Generalschlüssel."
    }

    fun selfVerificationErrorMasterPassphrase() = translate {
        EN - "Cannot verify with master passphrase."
        DE - "Fehler beim Freischalten mit dem Generalpasswort."
    }

    fun userVerificationSuccess() = translate {
        EN - "Successful"
        DE - "Erfolgreich"
    }

    fun userVerificationTimeout() = translate {
        EN - "Timeout"
        DE - "Zeitüberschreitung"
    }

    fun userVerificationNoMatch() = translate {
        EN - "no match"
        DE - "keine Übereinstimmung"
    }

    fun createNewChatError() = translate {
        EN - "Cannot create new chat."
        DE - "Neuer Chat kann nicht angelegt werden."
    }

    fun createNewGroupError() = translate {
        EN - "Cannot create new group"
        DE - "Neue Gruppe kann nicht angelegt werden."
    }

    fun roomListYou() = translate {
        EN - "you"
        DE - "ich"
    }

    fun roomListInvitationFrom(username: String) = translate {
        EN - "from $username"
        DE - "von $username"
    }

    fun roomListInvitationOffline() = translate {
        EN - "You cannot accept invitations while you are offline."
        DE - "Sie können offline keine Einladungen annehmen."
    }

    fun roomListInvitationError() = translate {
        EN - "There has been an error. Please try again later."
        DE - "Es gab einen Fehler. Bitte versuchen Sie es später."
    }

    fun roomListContentImage() = translate {
        EN - "Image"
        DE - "Bild"
    }

    fun roomListContentVideo() = translate {
        EN - "Video"
        DE - "Video"
    }

    fun roomListContentAudio() = translate {
        EN - "Audio"
        DE - "Audio"
    }

    fun roomListContentFile() = translate {
        EN - "File"
        DE - "Datei"
    }

    fun roomHeaderTypingSingle(username: String) = translate {
        EN - "$username is typing..."
        DE - "$username schreibt..."
    }

    fun roomHeaderTypingSingleDirect() = translate {
        EN - "is typing..."
        DE - "schreibt..."
    }

    fun roomHeaderTypingMultiple(usernames: String) = translate {
        EN - "$usernames are typing..."
        DE - "$usernames schreiben..."
    }

    fun roomHeaderTypingMultipleMore(usernames: String) = translate {
        EN - "$usernames and others are typing..."
        DE - "$usernames und andere schreiben..."
    }

    fun connectingErrorStandard() = translate {
        EN - "Cannot connect to the Matrix server."
        DE - "Matrix-Server kann nicht erreicht werden."
    }

    fun connectingErrorForbidden() = translate {
        EN - "Your username or password are not correct."
        DE - "Der Nutzername oder das Passwort sind nicht korrekt."
    }

    fun connectingErrorNotFound() = translate {
        EN - "Cannot find a Matrix server with the given address."
        DE - "Kann keinen Matrix-Server unter der angegebenen Adresse finden."
    }

    fun connectingErrorWrongAddress() = translate {
        EN - "The address of the Matrix server cannot be determined, or the address might be corrupt."
        DE - "Die Adresse des Matrix-Servers kann nicht bestimmt werden bzw. ist fehlerhaft."
    }

    fun connectingErrorHttps() = translate {
        EN - "Only secure connections (https) are allowed."
        DE - "Es muss eine sichere Verbindung (https) genutzt werden."
    }

    fun connectingAccountAlreadyExists(userId: UserId) = translate {
        EN - "There already is a local account for the user $userId."
        DE - "Es gibt bereits ein lokales Konto für den Nutzer $userId."
    }

    fun connectingErrorDbLocked() = translate {
        EN - "This app seems to be running already. You cannot start more than one instance."
        DE - "Diese App läuft bereits. Sie können die App nur einmal starten."
    }

    fun connectingErrorDbAccess() = translate {
        EN - "The local database cannot be accessed."
        DE - "Auf die lokale Datenbank kann nicht zugegriffen werden."
    }

    fun logoutFailure() = translate {
        EN - "An error occurred during logout. Please try again later."
        DE - "Beim Ausloggen aus Ihrem Account gab es einen Fehler. Bitte versuchen Sie es später noch einmal."
    }

    fun registrationTokenNotValid() = translate {
        EN - "the given registration is not valid"
        DE - "das Registrierungs-Token ist nicht gültig"
    }

    fun registrationErrorNotSupported() = translate {
        EN - "You can only register a new user with a registration token."
        DE - "Sie können einen neuen Nutzer nur mit einem Registrierungs-Token anlegen."
    }

    fun registrationErrorCannotDetermine() = translate {
        EN - "The methods for registration cannot be determined."
        DE - "Es ist nicht möglich die Registrierungsoptionen zu bestimmen."
    }

    fun registrationErrorNotSuccessful() = translate {
        EN - "The registration has not been successful."
        DE - "Die Registrierung konnte nicht erfolgreich abgeschlossen werden."
    }

    fun registrationErrorUserInUse() = translate {
        EN - "This username is already taken."
        DE - "Dieser Nutzername ist bereits vergeben."
    }

    fun registrationErrorInvalidUsername() = translate {
        EN - "The username is invalid. Please use another one."
        DE - "Der gewählte Nutzername ist ungültig. Bitte wählen Sie einen anderen."
    }

    fun settingsNotificationsSound() = translate {
        EN - "sound"
        DE - "Töne"
    }

    fun settingsNotificationsSilent() = translate {
        EN - "silent"
        DE - "Stumm"
    }

    fun settingsNotificationsVibration() = translate {
        EN - "vibration"
        DE - "Vibration"
    }

    fun settingsNotificationsVibrationNot() = translate {
        EN - "no vibration"
        DE - "keine Vibration"
    }

    fun settingsNotificationsLights() = translate {
        EN - "lights"
        DE - "Licht"
    }

    fun settingsNotificationsLightsNot() = translate {
        EN - "no lights"
        DE - "kein Licht"
    }

    fun settingsNotificationsPopup() = translate {
        EN - "show popups"
        DE - "zeige Popups"
    }

    fun settingsNotificationsPopupNot() = translate {
        EN - "no popups"
        DE - "keine Popups"
    }

    fun settingsNotificationsText() = translate {
        EN - "show text preview"
        DE - "Textvorschau"
    }

    fun settingsNotificationsTextNot() = translate {
        EN - "no text preview"
        DE - "keine Textvorschau"
    }

    fun settingsDevicesLoadError() = translate {
        EN - "Cannot load devices."
        DE - "Geräte können nicht geladen werden."
    }

    fun settingsDevicesDisplayNameLastSeen(instant: Instant) = translate {
        val date = instant.toLocalDateTime(currentTimezone).date
        EN - "last seen: ${date.monthNumber}/${date.dayOfMonth}/${date.year}" // AE
        DE - "zuletzt gesehen: ${date.dayOfMonth}.${date.monthNumber}.${date.year}"
    }

    fun settingsDevicesDisplayNameError() = translate {
        EN - "Cannot change the name of the device."
        DE - "Der Name des Geräts kann nicht geändert werden."
    }

    fun settingsDevicesVerificationError() = translate {
        EN - "Cannot verify this device."
        DE - "Das Gerät kann Ihnen nicht zugeordnet werden."
    }

    fun settingsDevicesRemoveError() = translate {
        EN - "The device cannot be removed"
        DE - "Das Gerät kann nicht gelöscht werden."
    }

    fun settingsDevicesRemoveLoginError(error: String) = translate {
        EN - "Cannot login: $error"
        DE - "Login kann nicht durchgeführt werden: $error"
    }

    fun settingsRoomAddMembersAnd() = translate {
        EN - "and"
        DE - "und"
    }

    fun settingsRoomAddMembersErrorSingular(username: String) = translate {
        EN - "$username could not be invited."
        DE - "$username konnte nicht eingeladen werden."
    }

    fun settingsRoomAddMembersErrorPlural(usernames: String) = translate {
        EN - "$usernames could not be invited."
        DE - "$usernames konnten nicht eingeladen werden."
    }

    fun settingsRoomAddMembersErrorOffline() = translate {
        EN - "You cannot invite users when you are offline."
        DE - "Sie können niemanden einladen, wenn Sie offline sind."
    }

    fun settingsRoomLeaveRoomError(groupOrChat: String) = translate {
        EN - "There has been an error leaving $groupOrChat."
        DE - "Fehler beim Verlassen $groupOrChat."
    }

    fun settingsRoomLeaveRoomErrorOffline() = translate {
        EN - "You cannot leave a chat or group when you are offline."
        DE - "Sie können offline keine Chats oder Gruppen verlassen."
    }

    fun settingsRoomLeaveRoomMessageChat() = translate {
        EN - "Leave chat"
        DE - "Chat verlassen"
    }

    fun settingsRoomLeaveRoomMessageGroup() = translate {
        EN - "Leave group"
        DE - "Gruppe verlassen"
    }

    fun settingsRoomLeaveRoomWarningConfirmButtonChat() = translate {
        EN - "Yes, leave chat"
        DE - "Ja, Chat verlassen"
    }

    fun settingsRoomLeaveRoomWarningConfirmButtonGroup() = translate {
        EN - "Yes, leave group"
        DE - "Ja, Gruppe verlassen"
    }

    fun settingsRoomLeaveRoomWarningMessageChat() = translate {
        EN - "You will not be able to access the contents of the chat afterwards."
        DE - "Sie können danach nicht mehr auf die Inhalte des Chats zugreifen."
    }

    fun settingsRoomLeaveRoomWarningMessageGroup() = translate {
        EN - "You will not be able to access the contents of the group afterwards."
        DE - "Sie können danach nicht mehr auf die Inhalte der Gruppe zugreifen."
    }

    fun settingsRoomLeaveRoomWarningTitleChat() = translate {
        EN - "Leave chat?"
        DE - "Den Chat verlassen?"
    }

    fun settingsRoomLeaveRoomWarningTitleGroup() = translate {
        EN - "Leave room?"
        DE - "Die Gruppe verlassen?"
    }

    fun settingsRoomChangeNameError() = translate {
        EN - "Failed to change the room name."
        DE - "Fehler beim Ändern des Raumnamens."
    }

    fun settingsRoomChangeTopicError() = translate {
        EN - "Failed to change the room topic."
        DE - "Fehler beim Ändern der Raumbeschreibung."
    }

    fun settingsRoomMemberListChangePowerLevelError(username: String) = translate {
        EN - "Failed to change the power level of user $username."
        DE - "Das Berechtigungslevel des Nutzers $username konnte nicht geändert werden."
    }

    fun settingsRoomMemberListChangePowerLevelErrorOffline() = translate {
        EN - "You cannot change power levels of users when you are offline."
        DE - "Sie können offline das Berechtigungslevel eines Nutzers nicht ändern."
    }

    fun settingsRoomMemberListChangePowerLevelInputValidationNotEntitled() = translate {
        EN - "You are not allowed to change the power level of a user."
        DE - "Sie dürfen das Berechtigungslevel eines Nutzers nicht verändern."
    }

    fun settingsRoomMemberListChangePowerLevelInputValidationPowerLevelTooLow(maximum: Long) = translate {
        EN - "You can only set the user's power level to a maximum of $maximum."
        DE - "Sie können das Berechtigungslevel des Nutzers nur maximal auf $maximum setzen."
    }

    fun settingsRoomMemberListChangePowerLevelInputValidationShouldBeNumber(maximum: Long) = translate {
        EN - "Please enter a valid number between 0 and $maximum."
        DE - "Bitte geben Sie eine gültige Zahl zwischen 0 und $maximum ein."
    }

    fun settingsRoomMemberListKickUserError() = translate {
        EN - "Failed to remove user."
        DE - "Der Nutzer konnte nicht entfernt werden."
    }

    fun settingsRoomMemberListKickUserErrorOffline() = translate {
        EN - "You cannot remove users when you are offline."
        DE - "Sie können offline keine Nutzer entfernen."
    }

    fun settingsRoomMemberListKickUserWarningMessageChat() = translate {
        EN - "The user will not be able to access the contents of the chat afterwards."
        DE - "Der Nutzer kann danach nicht mehr auf die Inhalte des Chats zugreifen."
    }

    fun settingsRoomMemberListKickUserWarningMessageGroup() = translate {
        EN - "The user will not be able to access the contents of the group afterwards."
        DE - "Der Nutzer kann danach nicht mehr auf die Inhalte der Gruppe zugreifen."
    }

    fun settingsRoomMemberListKickUserWarningTitleChat(username: String) = translate {
        EN - "Remove user $username from chat?"
        DE - "Nutzer $username aus dem Chat entfernen?"
    }

    fun settingsRoomMemberListKickUserWarningTitleGroup(username: String) = translate {
        EN - "Remove user $username from group?"
        DE - "Nutzer $username aus der Gruppe entfernen?"
    }

    fun settingsRoomMemberListRoleAdmin() = translate {
        EN - "Administrator"
        DE - "Administrator"
    }

    fun settingsRoomMemberListRoleModerator() = translate {
        EN - "Moderator"
        DE - "Moderator"
    }

    fun settingsRoomMemberListRoleUser() = translate {
        EN - "User"
        DE - "Nutzer"
    }

    fun settingsRoomNotificationsError() = translate {
        EN - "Cannot set room notifications."
        DE - "Fehler beim Setzen der Benachrichtigungseinstellungen."
    }

    fun settingsRoomNotificationsAll() = translate {
        EN - "All messages"
        DE - "Alle Nachrichten"
    }

    fun settingsRoomNotificationsMentions() = translate {
        EN - "Mentions"
        DE - "Erwähnungen"
    }

    fun settingsRoomNotificationsSilent() = translate {
        EN - "Silent"
        DE - "Stumm"
    }

    fun settingsRoomNotificationsDefault() = translate {
        EN - "Default"
        DE - "Standard"
    }

    fun settingsRoomNotificationsAllExplanation() = translate {
        EN - "you are notified on every new message"
        DE - "Sie werden über jede neue Nachricht informiert"
    }

    fun settingsRoomNotificationsMentionsExplanation() = translate {
        EN - "you are notified on new messages that are directed to you"
        DE - "Sie werden über Nachrichten informiert, die direkt an Sie gerichtet sind"
    }

    fun settingsRoomNotificationsSilentExplanation() = translate {
        EN - "you are not notified on any new message"
        DE - "Sie erhalten keinerlei Benachrichtigungen über neue Nachrichten"
    }

    fun settingsRoomNotificationsDefaultExplanation() = translate {
        EN - "you are notified as specified in the global settings"
        DE - "Sie werden so benachrichtigt, wie dies in den globalen Einstellungen festgelegt ist."
    }

    fun settingsRoomHistoryVisibilityChangeError() = translate {
        EN - "Failed to change room history visibility."
        DE - "Fehler beim Ändern der Sichtbarkeit der Raumhistorie."
    }

    fun settingsRoomHistoryVisibilityInsufficientPowerLevel() = translate {
        EN - "Insufficient power level to change room history visibility"
        DE - "Unzureichendes Berechtigungslevel um die Sichtbarkeit der Raumhistorie zu ändern"
    }

    fun settingsRoomJoinRulesChangeError() = translate {
        EN - "Failed to change room join rules"
        DE - "Fehler beim Ändern der Raum-Beitrittsregeln"
    }

    fun settingsRoomJoinRulesInsufficientPowerLevel() = translate {
        EN - "Insufficient power level to change room join rules"
        DE - "Unzureichendes Berechtigungslevel um die Sichtbarkeit der Raum-Beitrittsregeln zu ändern"
    }

    fun settingsUnblockUserError(userId: String) = translate {
        EN - "Cannot unblock user '$userId'. Please try again later."
        DE - "Nutzer '$userId' kann nicht entblockt werden. Bitte versuchen Sie es später erneut."
    }

    fun blockUserError(userId: String) = translate {
        EN - "Cannot block user '$userId'."
        DE - "Nutzer '$userId' kann nicht geblockt werden."
    }

    fun profileLoadError() = translate {
        EN - "Profile could not be loaded"
        DE - "Beim Laden des Profils ist ein Fehler aufgetreten."
    }

    fun profileAvatarError() = translate {
        EN - "The avatar image could not be changed."
        DE - "Das Nutzerbild konnte nicht geändert werden."
    }

    fun profileNameError() = translate {
        EN - "The username could not be changed."
        DE - "Der Nutzername konnte nicht geändert werden."
    }

    fun profileNameForbidden() = translate {
        EN - "You are not allowed to change the username."
        DE - "Der Nutzername darf von Ihnen nicht geändert werden."
    }

    fun matrixClientInitLoading() = translate {
        EN - "Loading..."
        DE - "Lade Daten..."
    }

    fun matrixClientInitSuccess() = translate {
        EN - "Successfully loaded data."
        DE - "Daten erfolgreich geladen."
    }

    fun timelineLeaveRoomErrorOffline() = settingsRoomLeaveRoomErrorOffline()
    fun timelineLeaveRoomError(groupOrChat: String) = settingsRoomLeaveRoomError(groupOrChat)
    fun timelineElementReadBy(usernames: String) = translate {
        EN - "read by $usernames"
        DE - "gelesen von $usernames"
    }

    fun timelineElementRedactError() = translate {
        EN - "Cannot delete message."
        DE - "Fehler beim Löschen der Nachricht."
    }

    fun serverDiscoveryFailed() = translate {
        EN - "Server could not be determined or is not valid."
        DE - "Server konnte nicht ermittelt werden oder ist nicht gültig."
    }

    fun sendErrorEventPermission() = translate {
        EN - "You do not have permission to send a message in this room."
        DE - "Sie haben keine Rechte, um Nachrichten in diesem Raum zu versenden."
    }

    fun sendErrorMediaPermission() = translate {
        EN - "You do not have permission to upload this file. The file type may not be supported or you reached an upload quota."
        DE - "Sie haben keine Rechte, um diese Datei hochzuladen. Möglicherweise wurde der Dateityp abgelehnt oder Sie haben Ihr maxinmales Kontingent erreicht."
    }

    fun sendErrorMediaTooLarge() = translate {
        EN - "The file you want to upload is too large."
        DE - "Die Datei, die sich versuchen hochzuladen ist zu groß."
    }

    fun sendErrorUnknown(errorMessage: String?) = translate {
        EN - "There was an unexpected error sending the message${if (errorMessage == null) "." else ": $errorMessage"}"
        DE - "Es gab einen unbekannten Fehler beim Absenden Ihrer Nachricht${if (errorMessage == null) "." else ": $errorMessage"}\""
    }

    fun attachmentSizeMaxSizeError(attachmentMaxSizeInMegaByte: Int) = translate {
        EN - "The attachment exceeds the maximum allowed attachment size of $attachmentMaxSizeInMegaByte MB."
        DE - "Der Anhang überschreitet die maximal zulässige Größe für Anhänge von $attachmentMaxSizeInMegaByte MB."
    }

    fun profileCreationDuplicate() = translate {
        EN - "The profile name is already in use."
        DE - "Dieser Profilname wird bereits benutzt."
    }

    fun exportRoomStateInit(total: Long) = translate {
        EN - "The export is being prepared. $total room events have already been found."
        DE - "Der Export wird vorbereitet. Es wurden bereits $total Raum-Ereignisse gefunden."
    }

    fun exportRoomStateProcessed(processed: Long, total: Long) = translate {
        EN - "The export is being executed. $processed out of $total room events have already been processed."
        DE - "Der Export wird durchgeführt. Es wurden bereits $processed von $total Raum-Ereignisse verarbeitet."
    }

    fun exportRoomStateFinished(total: Long) = translate {
        EN - "The export was successful. $total room events were processed."
        DE - "Der Export war erfolgreich. Es wurden $total Raum-Ereignisse verarbeitet."
    }

    fun exportRoomErrorRoomNotFound() = translate {
        EN - "The room does not exist."
        DE - "Der Raum existiert nicht."
    }

    fun exportRoomErrorPropertiesNotSupported() = translate {
        EN - "The export properties are not supported."
        DE - "Die Export-Eigenschaften werden nicht unterstützt."
    }

    fun exportRoomErrorSink(message: String) = translate {
        EN - "There was an error during export: $message"
        DE - "Es gab einen Fehler beim Export: $message"
    }

    fun exportRoomSuccessWithMissingMedia() = translate {
        EN - "The export was successful, but not all media could be downloaded"
        DE - "Der export war erfolgreich, dennoch konnten nicht alle Medien nicht heruntergeladen werden."
    }

    fun exportRoomEmote(message: String) = translate {
        EN - "* $message"
        DE - "* $message"
    }

    fun exportRoomNotice(message: String) = translate {
        EN - "// $message"
        DE - "// $message"
    }

    fun exportRoomImage(fileName: String) = translate {
        EN - "image: $fileName"
        DE - "Bild: $fileName"
    }

    fun exportRoomAudio(fileName: String) = translate {
        EN - "audio: $fileName"
        DE - "Audio: $fileName"
    }

    fun exportRoomVideo(fileName: String) = translate {
        EN - "video: $fileName"
        DE - "Video: $fileName"
    }

    fun exportRoomFile(fileName: String) = translate {
        EN - "file: $fileName"
        DE - "Datei: $fileName"
    }

    fun exportRoomLocation(name: String, uri: String) = translate {
        EN - "location: $name $uri"
        DE - "Ort: $name $uri"
    }

    fun exportRoomState(message: String) = translate {
        EN - "state change: $message"
        DE - "Zustandsänderung: $message"
    }

    fun exportRoomAvatar(url: String?) = translate {
        EN - "room avatar has been changed to $url"
        DE - "Raumbild wurde zu $url geändert"
    }

    fun exportRoomCanonicalAlias(aliases: List<RoomAliasId>) = translate {
        EN - "room aliases has been changed to $aliases"
        DE - "Raumaliase wurden zu $aliases geändert"
    }

    fun exportRoomCreate(federate: Boolean, roomType: String?) = translate {
        EN - "room has been created (federation=$federate, type=$roomType)"
        DE - "Raum wurde erstellt (Föderation=$federate, Typ=$roomType)"
    }

    fun exportRoomJoinRule(joinRule: String) = translate {
        EN - "join rule has been changed to $joinRule"
        DE - "Beitrittsregel wurde zu $joinRule geändert"
    }

    fun exportRoomMember(
        userId: String?,
        membership: String,
        displayName: String?,
        avatarUrl: String?,
        reason: String?
    ) = translate {
        EN - "changes of the member properties of $userId (membership=$membership, displayname=$displayName, avatar=$avatarUrl, reason=$reason)"
        DE - "Änderungen der Mitgliedseigenschaften von $userId (Mitgliedschaft=$membership, Anzeigename=$displayName, Anzeigebild=$avatarUrl, Grund=$reason)"
    }

    fun exportRoomName(name: String) = translate {
        EN - "room name has been changed to \"$name\""
        DE - "Raumname wurde zu \"$name\" geändert"
    }

    fun exportRoomTopic(name: String) = translate {
        EN - "room name has been changed to \"$name\""
        DE - "Raumname wurde zu \"$name\" geändert"
    }

    fun exportRoomEncryption() = translate {
        EN - "room encryption has been enabled"
        DE - "Raumverschlüsselung wurde aktiviert"
    }

    fun exportRoomHistoryVisibility(historyVisibility: String) = translate {
        EN - "history visibility has been changed to $historyVisibility"
        DE - "Sichtbarkeit wurde zu $historyVisibility geändert"
    }

    fun exportRoomGuestAccess(access: String) = translate {
        EN - "guest access has been changed to $access"
        DE - "Gastbeitritt wurde zu $access geändert"
    }

    fun exportRoomTombstone(body: String, roomId: RoomId) = translate {
        EN - "room has been replaced by new room $roomId: $body"
        DE - "Raum wurde durch den neuen Raum $roomId ersetzt: $body"
    }

    fun exportRoomRedacted(by: UserId?, reason: String?) = translate {
        EN - "* message has been deleted by $by, reason: $reason"
        DE - "* Nachricht wurde von $by gelöscht, Grund: $reason"
    }

    fun exportRoomDecryptionError() = translate {
        EN - "* message cannot be decrypted"
        DE - "* Nachricht konnte nicht entschlüsselt werden"
    }
}

internal fun getLang(
    languages: Languages,
    settings: MatrixMessengerSettingsHolder,
    getSystemLang: GetSystemLang
): Language {
    val preferredLang = settings.value.preferredLang
    log.trace { "preferred language: $preferredLang" }
    return preferredLang?.let { languages.langOf(it) }
        ?: languages.langOf(getSystemLang())
        ?: EN// fallback is english
}

internal suspend fun setLang(language: Language, settings: MatrixMessengerSettingsHolder) {
    settings.update { it.copy(preferredLang = language.code) }
}

package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.util.Lang.DE
import de.connect2x.trixnity.messenger.util.Lang.EN
import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val log = KotlinLogging.logger { }

interface Lang {
    object DE : Lang
    object EN : Lang

    fun langOf(lang: String?): Lang? {
        return when (lang) {
            "de" -> DE
            "en" -> EN
            else -> null
        }
    }

    fun stringRepresentation(): String {
        return when (this) {
            DE -> "de"
            EN -> "en"
            else -> ""
        }
    }
}

abstract class I18nBase(private val lang: Lang, messengerSettings: MessengerSettings) {

    private var currentLang = getLang(lang, messengerSettings)
    val currentTimezone = TimeZone.of(timezone())

    /**
     * Used to explicitly set the language, e.g., for testing.
     */
    fun setCurrentLang(newLang: String) {
        this.currentLang = lang.langOf(newLang) ?: EN
    }

    fun getCurrentLang(): Lang = currentLang

    fun translate(block: TranslateBuilder.() -> Unit): String {
        return TranslateBuilder().apply(block).map.translate()
    }

    class TranslateBuilder {

        val map: MutableMap<Lang, String> = mutableMapOf()
        operator fun Lang.minus(translation: String) {
            map[this] = translation
        }
    }

    private fun Map<Lang, String>.translate(): String {
        val translated = this[currentLang]
        return if (translated == null) {
            log.warn { "cannot find translation for language $currentLang: $this" }
            this[EN] ?: "<missing translation>"
        } else {
            translated
        }
    }
}

abstract class I18n(lang: Lang, messengerSettings: MessengerSettings) : I18nBase(lang, messengerSettings) {

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

    fun roomNameOthersCount(heroes: String, others: Long) = translate {
        EN - "$heroes$others others"
        DE - "$heroes$others andere"
    }

    fun roomNameOneOther() = translate {
        EN - "one other"
        DE - "ein anderer"
    }

    fun eventChangeAvatar(username: String) = translate {
        EN - "$username has changed the avatar image"
        DE - "$username hat das Profilbild geändert"
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

    fun eventRoomCreated(username: String, groupOrChat: String) = translate {
        EN - "$username has created $groupOrChat"
        DE - "$username hat $groupOrChat erstellt"
    }

    fun eventRoomChangeFrom(oldName: String) = translate {
        EN - "from '$oldName' "
        DE - "von '$oldName' "
    }

    fun eventRoomChange(username: String, groupOrChat: String, from: String, roomName: String) = translate {
        EN - "$username has changed the name of $groupOrChat ${from}to '$roomName'"
        DE - "$username hat den Namen $groupOrChat ${from}zu '$roomName' geändert"
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

    fun settingsRoomMemberListChangePowerLevelInputValidationPowerLevelTooLow(maximum: Int) = translate {
        EN - "You can only set the user's power level to a maximum of $maximum."
        DE - "Sie können das Berechtigungslevel des Nutzers nur maximal auf $maximum setzen."
    }

    fun settingsRoomMemberListChangePowerLevelInputValidationShouldBeNumber(maximum: Int) = translate {
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

    fun settingsUnblockUserError(userId: String) =  translate {
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

    fun accountAlreadyExistsLocally(accountName: String) = translate {
        EN - "There already is a local account for the name $accountName."
        DE - "Es gibt bereits ein lokales Konto für den Namen $accountName."
    }

    fun accountNameMustNotBeEmpty() = translate {
        EN - "The account name must not be empty."
        DE - "Der Kontoname darf nicht leer sein."
    }

    fun userIdShouldStartWithAt() = translate {
        EN - "An address has to start with an '@'."
        DE - "Eine Adresse muss mit einem '@' beginnen."
    }

    fun serverDiscoveryFailed() = translate {
        EN - "Server could not be determined (after the ':'). Please check or enter the server manually."
        DE - "Server konnte nicht ermittelt werden (nach dem ':'). Bitte prüfen oder tragen Sie den Server manuell ein."
    }

    fun userIdDomainMissing() = translate {
        EN - "Your address is missing a ':'."
        DE - "Ihre Adresse benötigt ein ':'."
    }

    fun userIdServerUrlProblems() = translate {
        EN - "Your server (after the ':') is not valid."
        DE - "Der Server (nach dem ':') ist nicht korrekt."
    }
}

private fun getLang(lang: Lang, messengerSettings: MessengerSettings): Lang {
    val preferredLang = getPreferredLang(messengerSettings)
    val systemLang = getSystemLang()
    log.debug { "preferred language: $preferredLang, system language: $systemLang" }
    return lang.langOf(preferredLang) ?: lang.langOf(systemLang) ?: EN // fallback is english
}

fun setLang(lang: Lang, preferredLang: String) {
    lang.langOf(preferredLang)
}

fun setPreferredLang(lang: String, messengerSettings: MessengerSettings) {
    messengerSettings.preferredLang = lang
}

fun getPreferredLang(messengerSettings: MessengerSettings): String? = messengerSettings.preferredLang

expect fun getSystemLang(): String?

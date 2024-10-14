package de.connect2x.messenger.compose.view.i18n

import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages.DE
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages.EN
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18nBase
import de.connect2x.trixnity.messenger.i18n.Languages
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.module

abstract class I18nView(
    lang: Languages,
    messengerSettings: MatrixMessengerSettingsHolder,
    getSystemLang: GetSystemLang
) : I18nBase(lang, messengerSettings, getSystemLang) {

    fun commonInactive() = translate {
        EN - "inactive"
        DE - "inaktiv"
    }

    fun commonUnknown() = translate {
        EN - "unknown"
        DE - "unbekannt"
    }

    fun commonAnd(first: String, second: String) = translate {
        EN - "$first and $second"
        DE - "$first und $second"
    }

    fun commonOr() = translate {
        EN - "or"
        DE - "oder"
    }

    fun commonCancel() = translate {
        EN - "cancel"
        DE - "abbrechen"
    }

    fun commonCancelled() = translate {
        EN - "cancelled"
        DE - "abgebrochen"
    }

    fun commonMore() = translate {
        EN - "more"
        DE - "mehr"
    }

    fun commonNone() = translate {
        EN - "none"
        DE - "keine"
    }

    fun commonClose() = translate {
        EN - "close"
        DE - "schließen"
    }

    fun commonOk() = translate {
        EN - "OK"
        DE - "OK"
    }

    fun commonBack() = translate {
        EN - "back"
        DE - "zurück"
    }

    fun commonNext() = translate {
        EN - "next"
        DE - "weiter"
    }

    fun commonRemove() = translate {
        EN - "remove"
        DE - "entfernen"
    }

    fun commonDelete() = translate {
        EN - "delete"
        DE - "löschen"
    }

    fun commonDeleted() = translate {
        EN - "deleted"
        DE - "gelöscht"
    }

    fun commonCreate() = translate {
        EN - "create"
        DE - "anlegen"
    }

    fun commonSelect() = translate {
        EN - "select"
        DE - "auswählen"
    }

    fun commonCopy() = translate {
        EN - "copy"
        DE - "kopieren"
    }

    fun commonCopied() = translate {
        EN - "copied"
        DE - "kopiert"
    }

    fun commonWaiting() = translate {
        EN - "waiting"
        DE - "Warten"
    }

    fun commonDone() = translate {
        EN - "done"
        DE - "abgeschlossen"
    }

    fun commonRename() = translate {
        EN - "rename"
        DE - "umbenennen"
    }

    fun commonRefresh() = translate {
        EN - "refresh"
        DE - "Aktualisieren"
    }

    fun commonEdit() = translate {
        EN - "edit"
        DE - "ändern"
    }

    fun commonAcceptEdit() = translate {
        EN - "accept changes"
        DE - "Änderungen übernehmen"
    }

    fun commonVerify() = translate {
        EN - "verify"
        DE - "freischalten"
    }

    fun commonFailure() = translate {
        EN - "failure"
        DE - "Fehlschlag"
    }

    fun commonSuccess() = translate {
        EN - "success"
        DE - "Erfolg"
    }

    fun commonAvatar() = translate {
        EN - "Avatar"
        DE - "Profilbild"
    }

    fun commonHelp() = translate {
        EN - "help"
        DE - "Hilfe"
    }

    fun commonInformation() = translate {
        EN - "information"
        DE - "Information"
    }

    fun commonMoreInformation() = translate {
        EN - "more information"
        DE - "mehr Informationen"
    }

    fun commonWarning() = translate {
        EN - "warning"
        DE - "Warnung"
    }

    fun commonImage() = translate {
        EN - "image"
        DE - "Bild"
    }

    fun commonVideo() = translate {
        EN - "video"
        DE - "Video"
    }

    fun commonAudio() = translate {
        EN - "audio"
        DE - "Audio"
    }

    fun commonFile() = translate {
        EN - "file"
        DE - "Datei"
    }

    fun commonAttachment() = translate {
        EN - "attachment"
        DE - "Anhang"
    }

    fun commonNotifications() = translate {
        EN - "notifications"
        DE - "Benachrichtigungen"
    }

    fun commonDefault() = translate {
        EN - "default"
        DE - "Standard"
    }

    fun commonOn() = translate {
        EN - "on"
        DE - "an"
    }

    fun commonOff() = translate {
        EN - "off"
        DE - "aus"
    }

    fun commonStandard() = translate {
        EN - "standard"
        DE - "Standard"
    }

    fun commonChat() = translate {
        EN - "chat"
        DE - "Chat"
    }

    fun commonGroup() = translate {
        EN - "group"
        DE - "Gruppe"
    }

    fun commonSettings() = translate {
        EN - "settings"
        DE - "Einstellungen"
    }

    fun commonRecoveryKey() = translate {
        EN - "recovery key"
        DE - "Generalschlüssel"
    }

    fun commonRecoveryPassphrase() = translate {
        EN - "recovery passphrase"
        DE - "Generalpasswort"
    }

    fun commonShowPassword() = translate {
        EN - "show password"
        DE - "zeige Passwort"
    }

    fun commonNotSupported() = translate {
        EN - "not supported"
        DE - "nicht unterstützt"
    }

    fun commonDays() = translate {
        EN - "days"
        DE - "Tage"
    }

    fun commonLogo() = translate {
        EN - "logo"
        DE - "Logo"
    }

    fun commonOptionalReason() = translate {
        EN - "Reason (optional)"
        DE - "Grund (optional)"
    }

    fun commonExpand() = translate {
        EN - "expand"
        DE - "ausklappen"
    }

    fun commonCollapse() = translate {
        EN - "collapse"
        DE - "einklappen"
    }

    fun newNotification() = translate {
        EN - "new notification"
        DE - "neue Nachricht"
    }

    fun addAlias() = translate {
        EN - "add alias"
        DE - "Alias hinzufügen"
    }

    fun deleteAlias() = translate {
        EN - "delete alias"
        DE - "Alias löschen"
    }

    fun makeMainAlias() = translate {
        EN - "set as main alias"
        DE - "als Hauptalias setzen"
    }

    fun unmakeMainAlias() = translate {
        EN - "remove as main alias"
        DE - "als Hauptalias entfernen"
    }

    fun mainAlias() = translate {
        EN - "mainalias"
        DE - "Hauptalias"
    }

    fun alias() = translate {
        EN - "alias"
        DE - "Alias"
    }

    fun manageAliases() = translate {
        EN - "manage aliases"
        DE - "Aliase verwalten"
    }

    fun newAlias() = translate {
        EN - "new alias"
        DE - "neuer alias"
    }

    fun aliases() = translate {
        EN - "aliases"
        DE - "Aliase"
    }

    fun showAliases() = translate {
        EN - "show aliases"
        DE - "Aliase anzeigen"
    }

    fun automated() = translate {
        EN - "Automated"
        DE - "Automatisiert"
    }

    fun passwordVisibility() = translate {
        EN - "show password"
        DE - "Passwort anzeigen"
    }

    fun passwordVisibilityOff() = translate {
        EN - "hide password"
        DE - "Passwort verstecken"
    }

    fun presenceOnline() = translate {
        EN - "online"
        DE - "online"
    }

    fun presenceOffline() = translate {
        EN - "offline"
        DE - "offline"
    }

    fun presenceUnavailable() = translate {
        EN - "unavailable"
        DE - "nicht erreichbar"
    }

    fun registrationHeader() = translate {
        EN - " a new user"
        DE - "Registrierung eines neuen Nutzers"
    }

    fun registrationMethodDependsOnServer() = translate {
        EN - "The available registration methods depend on the chosen server."
        DE - "Die möglichen Registrierungsmechanismen sind abhängig vom gewählten Server."
    }

    fun registrationOptionsEmpty() = translate {
        EN - "you cannot  an account on this server"
        DE - "Sie können auf diesem Server keine Nutzer anlegen"
    }

    fun registrationUsername() = translate {
        EN - "Choose your username"
        DE - "Wählen Sie einen Nutzernamen"
    }

    fun registrationDisplayname() = translate {
        EN - "Choose your displayname"
        DE - "Wählen Sie Ihren Anzeigenamen"
    }

    fun registrationChangeDisplayname() = translate {
        EN - "Change displayname"
        DE - "Anzeigenamen ändern"
    }

    fun registrationPassword() = translate {
        EN - "Choose your password"
        DE - "Wählen Sie ein Passwort"
    }

    fun registrationToken() = translate {
        EN - "registration token"
        DE - "Registrierungs-Token"
    }

    fun registrationDummy() = translate {
        EN - ""
        DE - ""
    }

    fun registrationUsernamePassword() = translate {
        EN - " with username and password"
        DE - "Registrierung mit Nutzername und Passwort"
    }

    fun registrationEmail() = translate {
        EN - " with Email"
        DE - "Registrierung über E-Mail"
    }

    fun registrationRecaptcha() = translate {
        EN - "solve a CAPTCHA"
        DE - "ein CAPTCHA lösen"
    }

    fun registrationSso() = translate {
        EN - " with Single Sign On"
        DE - "Registrierung über Single Sign On"
    }

    fun registrationMsisdn() = translate {
        EN - " with MSISDN"
        DE - "Registrierung über MSISDN"
    }

    fun verificationVerifiedDevice() = translate {
        EN - "This device has been verified. Other users that trust you will trust messages sent from this device."
        DE - "Dieses Gerät ist Ihnen zugeordnet. Andere Teilnehmer, die Ihnen vertrauen, vertrauen Nachrichten von diesem Gerät."
    }

    fun verificationVerifiedUser() = translate {
        EN - "You trust this user and all of her/his devices."
        DE - "Sie vertrauen diesem Nutzer und all seinen Geräten."
    }

    fun verificationNotVerifiedDevice() = translate {
        EN - "This device has not been verified by you. Other users might see a warning to not trust messages sent from this device."
        DE - "Dieses Gerät ist Ihnen noch nicht zugeordnet. Andere Teilnehmer sehen evtl. eine Warnung, Ihren Nachrichten von diesem Gerät nicht zu vertrauen."
    }

    fun verificationNotVerifiedUser() = translate {
        EN - "This user has at least one unverified device activated. It cannot be verified that messages you receive are actually from this user."
        DE - "Dieser Nutzer verwendet Geräte, die ihm nicht zugeordnet sind. Dass Nachrichten wirklich von ihm stammen, kann nicht überprüft werden."
    }

    fun verificationNeutralUser() = translate {
        EN - "This user has only verified devices. You have not verified yet that the user is the person you expect her/him to be."
        DE - "Dieser Nutzer verwendet ausschließlich Geräte, die ihm zugeordnet sind. Sie haben noch nicht überprüft, ob es sich tatsächlich um die ausgegebene Person handelt."
    }

    fun verificationTrusted() = translate {
        EN - "trusted"
        DE - "vertraut"
    }

    fun verificationNotTrusted() = translate {
        EN - "not trusted"
        DE - "nicht vertraut"
    }

    fun verificationNotVerifiedYet() = translate {
        EN - "not verified yet"
        DE - "noch nicht verifiziert"
    }

    fun anErrorHasOccurred() = translate {
        EN - "An error has occurred."
        DE - "Ein Fehler ist aufgetreten."
    }

    fun closeApp(appName: String) = translate {
        EN - "Close $appName"
        DE - "$appName schließen"
    }

    fun login() = translate {
        EN - "Login"
        DE - "Anmelden"
    }

    fun authenticate() = translate {
        EN - "authenticate"
        DE - "Autorisieren"
    }

    fun externalLogin(providerName: String) = translate {
        EN - "Login externally with $providerName"
        DE - "Extern anmelden mit $providerName"
    }

    fun loginAt(serverUrl: String) = translate {
        EN - "Login at $serverUrl"
        DE - "Anmelden bei $serverUrl"
    }

    fun loginWithPassword() = translate {
        EN - "Login with password"
        DE - "Anmelden mit Passwort"
    }

    fun loginWithSSO(name: String) = translate {
        EN - "Login with $name"
        DE - "Anmelden mit $name"
    }

    fun register() = translate {
        EN - "Create new account"
        DE - "Neues Konto anlegen"
    }

    fun uiaPasswordTitle() = translate {
        EN - "Password Authorization"
        DE - "Passwort Autorisierung"
    }

    fun uiaPasswordButtonSubmit() = translate {
        EN - "submit"
        DE - "absenden"
    }

    fun uiaFallbackTitle() = translate {
        EN - "3rd Party Authorization"
        DE - "Drittanbieter Autorisierung"
    }

    fun uiaFallbackButtonRedirect() = translate {
        EN - "Redirect"
        DE - "Umleiten"
    }

    fun uiaRegistrationTokenTitle() = translate {
        EN - "Registration Token"
        DE - "Registrierungs-Token"
    }

    fun uiaRegistrationTokenButtonSubmit() = translate {
        EN - "submit"
        DE - "absenden"
    }

    fun uiaRegistrationTokenAddToken() = translate {
        EN - "your registration token"
        DE - "Ihr Registrierungs-Token"
    }

    fun uiaDummyTitle() = translate {
        EN - "Dummy Authorization"
        DE - "Dummy Autorisierung"
    }

    fun uiaDummyButtonNext() = translate {
        EN - "next"
        DE - "weiter"
    }

    fun addMatrixClientCreateMatrixAccount() = translate {
        EN - "Creation of your Matrix account"
        DE - "Einrichtung Ihres Matrix Kontos"
    }

    fun addMatrixClientAnotherMatrixClient() = translate {
        EN - "Creation of another Matrix account"
        DE - "Einrichtung eines weiteren Matrix Kontos"
    }

    fun addMatrixClientServerMatrix() = translate {
        EN - "Your Matrix server"
        DE - "Ihr Matrix Server"
    }

    fun addMatrixClientAccountName() = translate {
        EN - "Account name"
        DE - "Name für dieses Konto"
    }

    fun addMatrixClientAccountNameHelp() = translate {
        EN - "You can add more accounts later. Therefore, try to use an account name that is recognizable."
        DE - "Sie können später weitere Konten hinzufügen. Vergeben Sie deshalb einen wiedererkennbaren Namen."
    }

    fun addMatrixClientServerDiscoverySuccess() = translate {
        EN - "server discovery has been successful"
        DE - "Server konnte bestimmt werden"
    }

    fun addMatrixClientMatrixAddress() = translate {
        EN - "your Matrix address"
        DE - "Ihre Matrix Adresse"
    }

    fun addMatrixClientMatrixUsername() = translate {
        EN - "your Matrix username"
        DE - "Ihr Matrix Nutzername"
    }

    fun addMatrixClientPassword() = translate {
        EN - "your password"
        DE - "Ihr Passwort"
    }

    fun matrixClientLogout(accountName: String) = translate {
        EN - "Logout of account $accountName."
        DE - "Ausloggen aus Account $accountName."
    }

    fun storeFailureAlreadyOpen(appName: String) = translate {
        EN - "$appName is already opened. This window will therefore be closed."
        DE - "$appName ist bereits geöffnet. Dieses Fenster wird nun geschlossen."
    }

    fun storeFailureLocalDbNotLoaded() = translate {
        EN - "The local database could not be loaded."
        DE - "Die lokale Datenbank konnte nicht geladen werden."
    }

    fun storeFailureLocalDbSelect() = translate {
        EN - "Unfortunately, this cannot be repaired automatically. You have 2 options:"
        DE - "Dieser Zustand kann leider nicht repariert werden. Sie haben nun folgende 2 Möglichkeiten: "
    }

    fun storeFailureLocalDbRestart(appName: String) = translate {
        EN - "A restart of $appName might solve the problem. If this problem persists, please file a bug report."
        DE - "Sie können versuchen $appName neu zu starten. Falls das Problem bestehen bleibt, erstellen Sie bitte einen Fehlerbericht."
    }

    fun storeFailureDeleteLocalDb() = translate {
        EN - "Delete the local database."
        DE - "Die lokale Datenbank löschen."
    }

    fun storeFailureDeleteLocalDbSelect() = translate {
        EN - "This option should only be selected if:"
        DE - "Diese Option sollte nur gewählt werden, wenn:"
    }

    fun storeFailureDeleteLocalDbRecoveryKey() = translate {
        EN - "• you have the account's recovery key with you OR"
        DE - "• Sie den Generalschlüssel für Ihr Konto parat haben ODER"
    }

    fun storeFailureDeleteLocalDbOtherDevice() = translate {
        EN - "• you have another device with you that has been activated with this account"
        DE - "• Sie ein freigeschaltetes Gerät zur Hand haben um eine Freischaltung durchzuführen"
    }

    fun storeFailureDeleteLocalDbWarning() = translate {
        EN - "You can only restore your old messages on this device if you have the recovery key or another device with the account with you. Otherwise, those messages are lost!"
        DE - "Nur mithilfe des Generalschlüssels oder der Freischaltung über ein anderes Gerät können Sie ihre alten Nachrichten lesen. Andernfalls sind diese Nachrichten verloren!"
    }

    fun storeFailureDeleteDb() = translate {
        EN - "Delete database"
        DE - "Datenbank löschen"
    }

    fun imageCouldNotBeLoaded() = translate {
        EN - "Image could not be loaded."
        DE - "Bild konnte nicht geladen werden."
    }

    fun videoCouldNotBeLoaded() = translate {
        EN - "Video could not be loaded."
        DE - "Video konnte nicht geladen werden."
    }

    fun addMembers() = translate {
        EN - "add members"
        DE - "Teilnehmer hinzufügen"
    }

    fun memberListChangeRole(username: String, oldRole: String, newRole: String) = translate {
        EN - "Change the role of $username from \"$oldRole\" to \"$newRole\"?"
        DE - "Rolle von $username von \"$oldRole\" zu \"$newRole\" ändern?"
    }

    fun memberListChangeRoleWarning() = translate {
        EN - "This action might not be reversible."
        DE - "Diese Aktion kann eventuell nicht mehr rückgängig gemacht werden."
    }

    fun memberListRoleAdministrator() = translate {
        EN - "administrator"
        DE - "Administrator"
    }

    fun memberListRoleModerator() = translate {
        EN - "moderator"
        DE - "Moderator"
    }

    fun memberListRoleUser() = translate {
        EN - "user"
        DE - "Nutzer"
    }

    fun memberListChangeTo(role: String) = translate {
        EN - "change status to $role"
        DE - "zum $role machen"
    }

    fun memberListChangePowerLevel() = translate {
        EN - "change power level"
        DE - "Berechtigungslevel festlegen"
    }

    fun memberListNote() = translate {
        EN - "Note:"
        DE - "Hinweis:"
    }

    fun memberListNoteText() = translate {
        EN - """Every user in a chat/group is given a value between 0 and 100.
            |This value determines the rights a user has in this chat or group.
            |
            |The higher the value, the more rights a user has.
            |The correlation between the given value and certain rights is determined by an admin of the room.
            |
            |Standard values are: 0 (user), 50 (moderator) and 100 (administrator).
        """.trimMargin()
        DE - """Jedem Nutzer in einem Chat/Gruppe wird ein Wert zwischen 0 und 100 zugewiesen.
            |Dieser Wert bestimmt welche Rechte ein Nutzer in diesem Chat oder dieser Gruppe besitzt.
            |
            |Je höher dieser Wert ist, desto mehr Rechte besitzt der Nutzer.
            |Ab welchen Werten der Nutzer welche Rechte besitzt, kann durch berechtigte Personen festgelegt werden.
            |
            |Standardwerte sind 0 (Einfacher Nutzer), 50 (Moderator) und 100 (Administrator).
        """.trimMargin()
    }

    fun memberListRemoveUser() = translate {
        EN - "remove user"
        DE - "Teilnehmer entfernen"
    }

    fun memberListBanUser() = translate {
        EN - "ban user"
        DE - "Teilnehmer bannen"
    }

    fun memberListRemoveUserConfirmation() = translate {
        EN - "Yes, remove user"
        DE - "Ja, Teilnehmer entfernen"
    }

    fun memberListBanUserConfirmation() = translate {
        EN - "Yes, ban user"
        DE - "Ja, Teilnehmer bannen"
    }

    fun memberListBanTitle() = translate {
        EN - "Are you sure to ban this user?"
        DE - "Möchten Sie den Teilnehmer wirklich bannen?"
    }

    fun unbannable() = translate {
        EN - "unbanable"
        DE - "entbannbar"
    }

    fun notUnbannable() = translate {
        EN - "not unbannable"
        DE - "nicht entbannbar"
    }

    fun memberListUnbanUser() = translate {
        EN - "unban user"
        DE - "Teilnehmer entbannen"
    }

    fun unbanTitle() = translate {
        EN - "Unban user"
        DE - "Teilnehmer entbannen"
    }

    fun unbanUserConfirmation() = translate {
        EN - "Yes, unban user"
        DE - "Ja, Teilnehmer entbannen"
    }

    fun roomHeaderUserIsBlocked() = translate {
        EN - "This user is blocked by you."
        DE - "Dieser Nutzer wird von Ihnen geblockt."
    }

    fun roomSettings() = translate {
        EN - "room settings"
        DE - "Raumeinstellungen"
    }

    fun roomSettingsRoomName() = translate {
        EN - "Name"
        DE - "Name"
    }

    fun roomSettingsRoomNamePlaceholder() = translate {
        EN - "give this room a name"
        DE - "Geben Sie diesem Raum einen Namen"
    }

    fun roomSettingsRoomNameCannotChange() = translate {
        EN - "You do not have sufficient rights to change the room's name."
        DE - "Sie haben nicht die nötigen Rechte um den Raumnamen zu ändern."
    }

    fun roomSettingsRoomTopic() = translate {
        EN - "Topic"
        DE - "Beschreibung"
    }

    fun roomSettingsRoomTopicPlaceholder() = translate {
        EN - "give this room a topic"
        DE - "Geben Sie diesem Raum eine Beschreibung"
    }

    fun roomSettingsRoomTopicCannotChange() = translate {
        EN - "You do not have sufficient rights to change the room's topic."
        DE - "Sie haben nicht die nötigen Rechte um die Raumbeschreibung zu ändern."
    }

    fun roomSettingsMembers() = translate {
        EN - "members"
        DE - "Mitglieder"
    }

    fun roomSettingsAliases() = translate {
        EN - "Room Aliases"
        DE - "Raumaliase"
    }

    fun roomSettingsBannedMembers() = translate {
        EN - "Banned members"
        DE - "Gebannte Mitglieder"
    }

    fun roomSettingsMentions() = translate {
        EN - "Mentions"
        DE - "Erwähnungen"
    }

    fun userSearchSearchPeople() = translate {
        EN - "search people"
        DE - "suche Personen"
    }

    fun userSearchNameOrMatrixId() = translate {
        EN - "display name or Matrix id"
        DE - "Name oder Matrix Id"
    }

    fun userSearchOffline() = translate {
        EN - "You are offline and thus cannot search for people."
        DE - "Sie sind offline. Personen können daher nicht gesucht werden."
    }

    fun userSearchNotFound() = translate {
        EN - "no people found"
        DE - "keine Personen gefunden"
    }

    fun indicatorUnreadMessages() = translate {
        EN - "unread messages"
        DE - "ungelesene Nachrichten"
    }

    fun indicatorLeave(groupOrChat: String) = translate {
        EN - "leave $groupOrChat"
        DE - "$groupOrChat verlassen"
    }

    fun inputAreaCannotSendMessages() = translate {
        EN - "You cannot send messages here."
        DE - "Sie können hier keine Nachrichten senden."
    }

    fun inputAreaPrompt() = translate {
        EN - "Your messsage..."
        DE - "Ihre Nachricht..."
    }

    fun inputAreaCancelEdit() = translate {
        EN - "cancel edit"
        DE - "Bearbeiten abbrechen"
    }

    fun inputAreaSend() = translate {
        EN - "send"
        DE - "Senden"
    }

    fun inputAreaEmojis() = translate {
        EN - "emojis"
        DE - "Emojis"
    }

    fun inputAreaSelectAttachment() = translate {
        EN - "select attachment"
        DE - "Anhang auswählen"
    }

    fun messageBubbleBeingDeleted() = translate {
        EN - "being deleted"
        DE - "wird gelöscht"
    }

    fun messageBubbleEdited() = translate {
        EN - "(edited)"
        DE - "(editiert)"
    }

    fun messageBubbleRead() = translate {
        EN - "read"
        DE - "gelesen"
    }

    fun messageContentWaitForKeys() = translate {
        EN - "waiting for decryption keys"
        DE - "Warten auf Schlüssel zur Entschlüsselung"
    }

    fun messageContentNoDecryption() = translate {
        EN - "message could not be decrypted"
        DE - "Nachricht konnte nicht entschlüsselt werden"
    }

    fun messageContentDownloadCompleted() = translate {
        EN - "download completed"
        DE - "Download abgeschlossen"
    }

    fun replyTo() = translate {
        EN - "reply to"
        DE - "Antwort auf"
    }

    fun replyToCancel() = translate {
        EN - "cancel reply"
        DE - "Antworten abbrechen"
    }

    fun roomHeaderSettings() = translate {
        EN - "room settings"
        DE - "Raumeinstellungen"
    }

    fun roomHeaderMore() = translate {
        EN - "more"
        DE - "mehr"
    }

    fun roomHeaderStartUserVerification() = translate {
        EN - "start user verification"
        DE - "Vertrauensprüfung starten"
    }

    fun exportRoom(groupOrChat: String) = translate {
        EN - "Export $groupOrChat"
        DE - "$groupOrChat exportieren"
    }

    fun exportRoomAbort() = translate {
        EN - "Cancel"
        DE - "Abbrechen"
    }

    fun exportRoomButton() = translate {
        EN - "Export"
        DE - "Exportieren"
    }

    fun exportRoomTargetDirectory() = translate {
        EN - "target directory"
        DE - "Zielpfad"
    }

    fun exportRoomTargetDirectoryAndroid() = translate {
        EN - "Your export is placed inside the Downloads folder"
        DE - "Ihr Export wird im Downloads Ordner abgelegt"
    }

    fun exportRoomBodyLabel(roomName: String) = translate {
        EN - "Select from the options below to export '$roomName'"
        DE - "Wählen Sie eine der folgenden Optionen, um '$roomName' zu exportieren"
    }

    fun exportRoomTargetPlainText() = translate {
        EN - "file: plain text (txt)"
        DE - "Datei: einfacher text (txt)"
    }

    fun exportRoomTargetCsv() = translate {
        EN - "file: table (csv)"
        DE - "Datei: Tabelle (csv)"
    }

    fun archiveThresholdFromBeginning() = translate {
        EN - "From the beginning"
        DE - "Von Beginn an"
    }

    fun archiveThresholdSpecifyNumber() = translate {
        EN - "Specify a number of messages"
        DE - "Geben Sie eine Anzahl von Nachrichten an"
    }

    fun labelMessages() = translate {
        EN - "Messages"
        DE - "Nachrichten"
    }

    fun roomHeaderBlockUser() = translate {
        EN - "block user"
        DE - "Nutzer blocken"
    }

    fun roomHeaderUnblockUser() = translate {
        EN - "unblock user"
        DE - "Nutzer entblocken"
    }

    fun sendAttachmentTitle() = translate {
        EN - "Send attachment"
        DE - "Anhang versenden"
    }

    fun timelineSendFile() = translate {
        EN - "send file"
        DE - "Datei senden"
    }

    fun userVerificationStarted(sender: String) = translate {
        EN - "user verification (started by $sender)"
        DE - "Vertrauensprüfung (gestartet von $sender)"
    }

    fun userVerificationDone() = translate {
        EN - "done"
        DE - "beendet"
    }

    fun userVerificationSuccess() = translate {
        EN - "success"
        DE - "erfolgreich"
    }

    fun userVerificationNotSuccessful() = translate {
        EN - "not successful"
        DE - "nicht erfolgreich"
    }

    fun userVerificationRequest(from: String) = translate {
        EN - "In the following steps it is made sure that messages of '$from' are actually of this user."
        DE - "Im Folgenden wird sichergestellt, dass Nachrichten von '$from' auch wirklich von diesem Nutzer stammen."
    }

    fun userVerificationSuccessMessage() = translate {
        EN - "You now trust this user"
        DE - "Sie vertrauen nun diesem Nutzer."
    }

    fun userVerificationOtherDevice() = translate {
        EN - "This request is already handled by another device."
        DE - "Diese Anfrage wird bereits durch ein anderes Gerät durchgeführt."
    }

    fun accountChangeAccount() = translate {
        EN - "Change account"
        DE - "Konto wechseln"
    }

    fun accountAllAccounts() = translate {
        EN - "All accounts"
        DE - "Alle Konten"
    }

    fun accountDeactivateFilter() = translate {
        EN - "deactivate filter"
        DE - "Filter deaktivieren"
    }

    fun accountSelectFilter() = translate {
        EN - "select filter"
        DE - "Filter auswählen"
    }

    fun accountDeactivateSearch() = translate {
        EN - "deactivate search"
        DE - "Suche ausschalten"
    }

    fun accountActivateSearch() = translate {
        EN - "activate search for people and groups"
        DE - "Suche nach Personen oder Gruppen einschalten"
    }

    fun accountCloseProfile() = translate {
        EN - "close the currently selected profile and return to profile selection"
        DE - "schließe das aktuell gewählte Profil und kehre zur Profilauswahl zurück"
    }

    fun accountCreateNewRoom() = translate {
        EN - "create new chat or group"
        DE - "neuen Chat oder Gruppe erstellen"
    }

    fun accountSelectAccount() = translate {
        EN - "Select an account"
        DE - "Wählen Sie ein Konto aus"
    }

    fun accountMoreSettings() = translate {
        EN - "more settings"
        DE - "weitere Einstellungen"
    }

    fun accountYourAccounts() = translate {
        EN - "Your accounts"
        DE - "Ihre Konten"
    }

    fun accountAboutTheApp(appName: String) = translate {
        EN - "About $appName"
        DE - "Über $appName"
    }

    fun accountSendErrorLogs() = translate {
        EN - "Send error logs"
        DE - "Fehlerbericht senden"
    }

    fun createNewGroupNewGroup() = translate {
        EN - "New group"
        DE - "Neue Gruppe"
    }

    fun createNewGroupAddUser() = translate {
        EN - "Add user"
        DE - "Teilnehmer hinzufügen"
    }

    fun createNewGroupCreate() = translate {
        EN - "Create group"
        DE - "Gruppe anlegen"
    }

    fun createNewGroupSearch() = translate {
        EN - "Search group"
        DE - "Gruppe suchen"
    }

    fun searchGroupTitle() = translate {
        EN - "Search public groups"
        DE - "Öffentliche Gruppen suchen"
    }

    fun searchGroupSearch() = translate {
        EN - "Search term"
        DE - "Suchbegriff"
    }

    fun searchGroupNotFound() = translate {
        EN - "No group found"
        DE - "Keine passende Gruppe gefunden"
    }

    fun createNewChatTitle() = translate {
        EN - "New chat"
        DE - "Neuer Chat"
    }

    fun roomListRemoveRoom() = translate {
        EN - "Remove chat or group"
        DE - "Chat oder Gruppe entfernen"
    }

    fun roomListNoRoom() = translate {
        EN - "You have no chats or groups, yet."
        DE - "Sie haben noch keine Chats oder Gruppen."
    }

    fun roomListCreateRoom() = translate {
        EN - "Create a new chat or group"
        DE - "Neuen Chat oder Gruppe anlegen"
    }

    fun roomListNoFilter() = translate {
        EN - "no filter"
        DE - "kein Filter"
    }

    fun roomListSearch() = translate {
        EN - "Search for people or groups"
        DE - "Suche Personen oder Gruppen"
    }

    fun roomListSyncErrorNoConnection() = translate {
        EN - "No connection"
        DE - "Keine Verbindung"
    }

    fun roomListSyncErrorNoInternet() = translate {
        EN - "Connection to the internet is lost"
        DE - "Verbindung zum Internet unterbrochen"
    }

    fun roomListSyncErrorAccounts(accountList: String) = translate {
        EN - "These accounts are not connected: $accountList"
        DE - "Folgende Accounts sind nicht verbunden: $accountList"
    }

    fun roomListSyncErrorSendMessages() = translate {
        EN - "Your messages are sent as soon as a connection can be established."
        DE - "Ihre Nachrichten werden gesendet sobald eine Verbindung hergestellt werden kann."
    }

    fun roomListJoin() = translate {
        EN - "Join"
        DE - "Beitreten"
    }

    fun roomListAccountNotVerifiedIcon() = translate {
        EN - "Account not verified"
        DE - "Nutzerkonto nicht verifiziert"
    }

    fun roomListAccountNotVerifiedMessage(userId: UserId) = translate {
        EN - "Click here to verify this device for your account $userId"
        DE - "Drücken Sie hier um dieses Gerät für ihr Konto $userId zu verifizieren."
    }

    fun accountsOverviewCreateNewAccount() = translate {
        EN - "Create new account"
        DE - "Neues Konto anlegen"
    }

    fun accountsOverviewLogoutWarning(userId: String) = translate {
        EN - "Logout of account '$userId'"
        DE - "Ausloggen aus Account '$userId'"
    }

    fun accountsOverviewLogoutWarningExplanation() = translate {
        EN - """You are about to log out of your account. In order to have further access to this account, please make sure you either have:
            | * your recovery key
            | * another device logged into this account
            | 
            |If you have neither, all your DATA WILL BE LOST!
        """.trimMargin()
        DE - """Sie sind dabei sich aus diesem Konto auszuloggen. Um weiterhin Zugriff zu diesem Konto zu haben, vergewissern Sie sich eines der folgenden Dinge zu haben:
            | * Ihren Generalschlüssel
            | * ein weiteres Gerät, das in diesem Konto eingeloggt ist
            | 
            |Falls Sie nichts davon besitzen, sind ALLE IHRE DATEN VERLOREN!
        """.trimMargin()
    }

    fun accountsOverviewLogout() = translate {
        EN - "Logout"
        DE - "Ausloggen"
    }

    fun appInfoVersion(versionNumber: String) = translate {
        EN - "Version $versionNumber"
        DE - "Version $versionNumber"
    }

    fun appInfoPrivacy() = translate {
        EN - "Privacy"
        DE - "Datenschutz"
    }

    fun appInfoPrivacyLink() = translate {
        EN - "Show Privacy Info"
        DE - "Zur Datenschutzerklärung"
    }

    fun appInfoImprint() = translate {
        EN - "Imprint"
        DE - "Impressum"
    }

    fun appInfoImprintLink() = translate {
        EN - "Show Imprint"
        DE - "Zum Impressum"
    }

    fun appInfoLicenses() = translate {
        EN - "Licenses"
        DE - "Lizenzen"
    }

    fun configureNotificationsSettings() = translate {
        EN - "Notification settings"
        DE - "Benachrichtigungseinstellungen"
    }

    fun devicesTitle() = translate {
        EN - "Devices"
        DE - "Geräte"
    }

    fun devicesThisDevice() = translate {
        EN - "This device"
        DE - "Dieses Gerät"
    }

    fun devicesOtherDevices() = translate {
        EN - "Other devices"
        DE - "Andere Geräte"
    }

    fun devicesRemoveDevice() = translate {
        EN - "Remove device"
        DE - "Gerät entfernen"
    }

    fun devicesRemoveDevice(deviceName: String) = translate {
        EN - "Remove device '$deviceName'"
        DE - "Gerät '$deviceName' entfernen"
    }

    fun devicesRemoveDeviceInformationHeader() = translate {
        EN - "You are about to remove one of your devices. This should only be necessary if:"
        DE - "Sie sind im Begriff eines Ihrer Geräte zu entfernen. Dies sollte nur notwendig sein, wenn:"
    }

    fun devicesRemoveDeviceInformationLostDevice() = translate {
        EN - "• Your device has been lost"
        DE - "• Ihr Gerät verloren gegangen ist"
    }

    fun devicesRemoveDeviceInformationAttacker() = translate {
        EN - "• Your device has been compromised by an attacker"
        DE - "• Ihr Gerät durch einen Angreifer kompromittiert wurde"
    }

    fun devicesRemoveDeviceEnterPassword() = translate {
        EN - "Please confirm removing the device with your account's password:"
        DE - "Bitte bestätigen Sie das Entfernen mit der Eingabe Ihres Konto-Passworts:"
    }

    fun devicesRemovePasswordNotCorrect() = translate {
        EN - "Password is not correct"
        DE - "Passwort ist nicht korrekt"
    }

    fun devicesRemoveDeviceConfirm(deviceName: String) = translate {
        EN - "I want to remove the device '$deviceName'."
        DE - "Ich möchte das Gerät '$deviceName' entfernen."
    }

    fun notificationsSettingsEnabledForThisDevice() = translate {
        EN - "Enable Notifications on this device"
        DE - "Benachrichtigungen auf diesem Gerät aktivieren"
    }

    fun notificationsSettingsPlatform() = translate {
        EN - "Device Settings"
        DE - "Geräteeinstellungen"
    }

    fun notificationSettingsPlatformEnablePermissionsWarning() = translate {
        EN - "Please enable notifications via the device settings"
        DE - "Bitte erlauben Sie das Senden von Benachrichtigungen in den Geräteeinstellungen"
    }

    fun notificationsSettingsPlatformPushMode(mode: String) = translate {
        EN - "Mode: $mode"
        DE - "Modus: $mode"
    }

    fun notificationsSettingsPlatformPushModePush() = translate {
        EN - "Energy-Saving"
        DE - "Energiesparend"
    }

    fun notificationsSettingsPlatformPushModePushExplantation() = translate {
        EN - "You will receive a notification on your phone instantly when a new message is posted via Google Services. Google cannot access any contents of the messages, but notice that you have received a message."
        DE - "Über Google Services wird im Falle einer neuen Nachricht an Sie sofort eine Meldung auf Ihr Mobilgerät übertragen. Google kann hierbei auf keine Nachrichteninhalte zugreifen, aber sehen, dass Sie eine Nachricht empfangen haben."
    }

    fun notificationsSettingsPlatformPushModePolling() = translate {
        EN - "Privacy-Friendly"
        DE - "Datenschutzfreundlich"
    }

    fun notificationsSettingsPlatformPushModePollingExplanation() = translate {
        EN - "Notifications are received with a delay. You might notice a permanent element in the notification bar that new messages are received."
        DE - "Benachrichtigungen werden mit einer Verzögerung empfangen. Sie sehen evtl. dauerhaft ein Element in der Benachrichtigungsleiste, dass neue Nachrichten empfangen werden."
    }

    fun notificationsSettingsPlatformPlaySound() = translate {
        EN - "Sound"
        DE - "Töne"
    }

    fun notificationsSettingsPlatformShowPopup() = translate {
        EN - "Show popup"
        DE - "Zeige Popup"
    }

    fun notificationsSettingsPlatformShowText() = translate {
        EN - "Show text preview"
        DE - "Textvorschau zeigen"
    }


    fun notificationsSettingsAccountDefaultLevel(level: String) = translate {
        EN - "Default Level: $level"
        DE - "Standardlevel: $level"
    }

    fun notificationsSettingsAccountDefaultLevelRoom() = translate {
        EN - "All Rooms"
        DE - "Alle Räume"
    }

    fun notificationsSettingsAccountDefaultLevelDM() = translate {
        EN - "Direct Messages"
        DE - "Direktnachrichten"
    }

    fun notificationsSettingsAccountDefaultLevelMention() = translate {
        EN - "Mentions"
        DE - "Erwähnungen"
    }

    fun notificationsSettingsAccountDefaultLevelNone() = translate {
        EN - "Nothing"
        DE - "Keine"
    }

    fun notificationsSettingsAccountSound() = translate {
        EN - "Notification Sounds"
        DE - "Benachrichtigungstöne"
    }

    fun notificationsSettingsAccountSoundRoom() = translate {
        EN - "Rooms"
        DE - "Räume"
    }

    fun notificationsSettingsAccountSoundDM() = translate {
        EN - "Direct Messages"
        DE - "Direktnachrichten"
    }

    fun notificationsSettingsAccountSoundMention() = translate {
        EN - "Mentions"
        DE - "Erwähnungen"
    }

    fun notificationsSettingsAccountSoundCall() = translate {
        EN - "Calls"
        DE - "Anrufe"
    }

    fun notificationsSettingsAccountOthers() = translate {
        EN - "Other Notifications"
        DE - "Weitere Benachrichtigungen"
    }

    fun notificationsSettingsAccountActivityInvite() = translate {
        EN - "Invitations"
        DE - "Einladungen"
    }

    fun notificationsSettingsAccountActivityStatus() = translate {
        EN - "Room Changes"
        DE - "Raumänderungen"
    }

    fun notificationsSettingsAccountActivityNotice() = translate {
        EN - "Bot Messages"
        DE - "Bot Nachrichten"
    }

    fun notificationsSettingsAccountMentionUser(userId: UserId) = translate {
        EN - "$userId Mentions"
        DE - "$userId Erwähnungen"
    }

    fun notificationsSettingsAccountMentionRoom() = translate {
        EN - "@room Mentions"
        DE - "@room Erwähnungen"
    }

    fun notificationsSettingsAccountMentionKeyword() = translate {
        EN - "Keywords"
        DE - "Schlüsselwörter"
    }

    fun privacyTitle() = translate {
        EN - "Privacy"
        DE - "Datenschutz"
    }

    fun privacyPresenceIsPublic() = translate {
        EN - "Online Status"
        DE - "Online Status"
    }

    fun privacyPresenceIsPublicExplanation(appName: String) = translate {
        EN - "Others can see whether you are online with $appName"
        DE - "Andere Nutzer können sehen ob Sie mit $appName online sind"
    }

    fun privacyReadMarkerIsPublic() = translate {
        EN - "Read Marker"
        DE - "Lesebestätigungen"
    }

    fun privacyReadMarkerIsPublicExplanation() = translate {
        EN - "Others can see which messages you have read"
        DE - "Andere Nutzer können sehen, welche Nachrichten sie bereits gelesen haben"
    }

    fun typingIsPublic() = translate {
        EN - "Typing Indicators"
        DE - "Tippindikatoren"
    }

    fun typingIsPublicExplanation() = translate {
        EN - "Others can see when you type a message"
        DE - "Andere Nutzer können sehen, wenn Sie eine neue Nachricht schreiben"
    }

    fun profileTitle() = translate {
        EN - "Profile"
        DE - "Profil"
    }

    fun profileAvatarChange() = translate {
        EN - "change avatar"
        DE - "Profilbild ändern"
    }

    fun profileYourName() = translate {
        EN - "Your name"
        DE - "Ihr Name"
    }

    fun profileYourNameInfo() = translate {
        EN - "This is your display name. It is public and is seen by other users in your chats and groups."
        DE - "Die ist Ihr Anzeigename. Dieser ist öffentlich und wird von allen Teilnehmern in Ihren Chats und Gruppen gesehen."
    }

    fun profileUserName() = translate {
        EN - "Your username"
        DE - "Ihre Benutzerkennung"
    }

    fun profileUserNameInfo() = translate {
        EN - "Your username is required to login. It is public and is used to identify your account in case of of a display name duplication in a group."
        DE - "Ihre Benutzerkennung wird zum Einloggen benötigt. Sie ist öffentlich und wird verwendet um Sie - im Falle einer Namensdopplung - in einer Gruppe eindeutig zu identifizieren."
    }

    fun bootstrapRecoveryKeyExplanationTitle() = translate {
        EN - "Message vault and recovery key"
        DE - "Nachrichtentresor und Generalschlüssel"
    }

    fun bootstrapRecoveryKeyExplanation1() = translate {
        EN - "During the setup of your account, a vault for your messages is created, in which your messages are safely stored. Only the device with which your are performing the setup now is able to access the vault."
        DE - "Im Rahmen der Kontoeinrichtung wird nun ein Tresor für Ihre Nachrichten angelegt. In diesem werden Ihre Nachrichten sicher verwahrt. Nur das Gerät, mit dem Sie die Einrichtung gerade durchführen, hat Zugriff auf diesen Tresor."
    }

    fun bootstrapRecoveryKeyExplanation2() = translate {
        EN - "The message vault additionally can be opened with an individual recovery key. In case your device is lost, you can open the message vault on another device with the recovery key which is issued to you in the next step."
        DE - "Dieser Tresor kann zudem mit einem individuellen Generalschlüssel (im Bild: \"RecoveryKey\") geöffnet werden. Um im Falle eines Geräteverlusts diesen Tresor auf einem neuen Gerät öffnen zu können, wird Ihnen der Generalschlüssel im nächsten Schritt ausgehändigt."
    }

    fun bootstrapRecoveryKeyVaultCreation() = translate {
        EN - "Message vault is being created"
        DE - "Nachrichtentresor wird angelegt"
    }

    fun bootstrapRecoveryKeyCreateVault() = translate {
        EN - "Create vault"
        DE - "Tresor anlegen"
    }

    fun bootstrapRecoveryKeyTitle() = translate {
        EN - "Recovery key"
        DE - "Schlüsselübergabe"
    }

    fun bootstrapRecoveryKeyHandling() = translate {
        EN - "Write down your recovery key and put it in a safe space. A password manager is recommended (e.g., KeePassXC, 1Password, etc.)."
        DE - "Notieren Sie sich den Generalschlüssel und sichern Sie ihn an einem geeigneten Ort. Ein Passwort-Manager wird empfohlen (bspw. KeePassXC, 1Password, etc.)."
    }

    fun bootstrapRecoveryKeyWarning() = translate {
        EN - "In case your recovery key is stolen, the attacker can get access to your message vault!"
        DE - "Falls Ihr Generalschlüssel in falsche Hände gerät, kann auf Ihren Tresor und damit alle Ihre Gespräche zugegriffen werden!"
    }

    fun bootstrapRecoveryKeyAttention() = translate {
        EN - "Attention!"
        DE - "Achtung!"
    }

    fun bootstrapRecoveryKeyOnlyOnce() = translate {
        EN - "Your recovery key is only displayed once now and is not saved by us."
        DE - "Ihr Generalschlüssel wird nur dieses eine Mal angezeigt und wird von uns nicht gespeichert."
    }

    fun bootstrapRecoveryKeyCopyToClipboard() = translate {
        EN - "Copy recovery key to clipboard"
        DE - "Generalschlüssel in die Zwischenablage kopieren"
    }

    fun bootstrapRecoveryKeySafe() = translate {
        EN - "I have copied the recovery key to a safe location."
        DE - "Ich habe den Generalschlüssel notiert und sicher verwahrt."
    }

    fun bootstrapFinished() = translate {
        EN - "Your account is now set up."
        DE - "Ihr Konto ist nun eingerichtet."
    }

    fun deviceVerificationTitle() = translate {
        EN - "Device verification"
        DE - "Freischaltung für dieses Gerät"
    }

    fun deviceVerificationInitiatedBy(username: String) = translate {
        EN - "initiated by $username"
        DE - "ausgelöst durch $username"
    }

    fun deviceVerificationToAccount(deviceName: String) = translate {
        EN - "Verify that the device '$deviceName' belongs to your account."
        DE - "Ordnen Sie das Gerät '$deviceName' Ihrem Konto zu."
    }

    fun redoSelfVerificationTitle(userId: UserId) = translate {
        EN - "Cancel device verification for account '$userId'?"
        DE - "Gerätefreischaltung für Konto '$userId' abbrechen?"
    }

    fun redoSelfVerificationWarning1() = translate {
        EN - "If you do not verify this device, the following restrictions apply:"
        DE - "Wenn Sie Ihr Gerät nicht freischalten, dann gelten folgende Einschränkungen:"
    }

    fun redoSelfVerificationWarning2() = translate {
        EN - "• you cannot read older messages"
        DE - "• Sie können alte Nachrichten nicht lesen"
    }

    fun redoSelfVerificationWarning3() = translate {
        EN - "• your contacts might get a warning that your account cannot be trusted anymore"
        DE - "• Ihren Gesprächspartnern wird evtl. angezeigt, dass Ihrem Konto nicht mehr vertraut werden kann"
    }

    fun redoSelfVerificationDoIt() = translate {
        EN - "Restart the verification process again or continue with the aforementioned restrictions."
        DE - "Starten Sie die Freischaltung des Gerätes erneut oder fahren Sie mit den zuvor genannten Einschränkungen fort."
    }

    fun redoSelfVerificationDoItLater() = translate {
        EN - "Without verification, it is recommended to redo the verification process at a later time. You can find the option in 'settings' -> 'devices'."
        DE - "Ohne Freischaltung wird empfohlen diese zu einem späteren Zeitpunkt nachzuholen. Sie finden die Option in 'Einstellungen' -> 'Geräte'."
    }

    fun redoSelfVerificationContinueWithoutVerification() = translate {
        EN - "Continue without verification"
        DE - "Ohne Freischaltung fortfahren"
    }

    fun redoSelfVerificationRedo() = translate {
        EN - "Verify device"
        DE - "Gerät freischalten"
    }

    fun selfVerificationTitle(userId: UserId) = translate {
        EN - "Verify this device (account: $userId)"
        DE - "Dieses Gerät freischalten (Konto: $userId)"
    }

    fun selfVerificationResetRecoveryWarningTitle(userId: UserId) = translate {
        EN - "Reset recovery keys (account: $userId)"
        DE - "Generalschlüssel zurücksetzen (Konto: $userId)"
    }

    fun selfVerificationHelpOtherDevice() = translate {
        EN - "Your account has already been setup with another device."
        DE - "Ihr Konto wurde bereits über ein anderes Gerät eingerichtet."
    }

    fun selfVerificationHelpVerifyThis() = translate {
        EN - "You have to verify this new device (connect it to your account) in order to use it."
        DE - "Sie müssen das neue Gerät freischalten (mit Ihrem Konto verknüpfen) um es nutzen zu können."
    }

    fun selfVerificationHelpReasonTitle() = translate {
        EN - "Why do I have to verify this device?"
        DE - "Warum muss ich dieses Gerät freischalten?"
    }

    fun selfVerificationHelpReason1() = translate {
        EN - "This step is necessary to increase security and trust of all users among each other. You and your contacts want to make sure that messages are really delivered to the correct recipient."
        DE - "Dieser Schritt ist notwendig um die Sicherheit und das Vertrauen aller Teilnehmer untereinander zu erhöhen. Sie und Ihre Gesprächspartner möchten sichergehen, dass Ihre Nachrichten auch wirklich beim korrekten Empfänger ankommen."
    }

    fun selfVerificationHelpReason2() = translate {
        EN - "Despite all security measures an attacker can gain access to your account. The attacker can impersonate you and trick your contacts to disclose sensitive information."
        DE - "Trotz aller Sicherheitsmaßnahmen kann es einem Angreifer gelingen, Zugriff auf Ihr Konto zu erlangen. Damit kann er sich als Sie ausgeben und andere Gesprächsteilnehmer täuschen ihm sensible Informationen anzuvertrauen."
    }

    fun selfVerificationHelpReason3() = translate {
        EN - "With the device verification, an attacker would not only need access to your account, but also to your recovery key or a already verified device. This second factor increases the security and trustworthiness."
        DE - "Durch die Gerätefreischaltung benötigt ein Angreifer nun nicht nur Zugriff auf Ihr Konto, sondern muss zudem im Besitz des Generalschlüssels oder eine freigegebenen Gerätes sein. Dieser zweite Faktor erhöht die Sicherheit und Vertrauenswürdigkeit deutlich."
    }

    fun selfVerificationMethodsTitle() = translate {
        EN - "Please choose one method to verify this device:"
        DE - "Sie können aus folgenden Möglichkeiten zur Freischaltung wählen:"
    }

    fun selfVerificationMethodsOtherDevice() = translate {
        EN - "with another already verified device you have access to"
        DE - "mit Hilfe eines bereits freigeschalteten Gerätes, zu dem Sie jetzt Zugang haben"
    }

    fun selfVerificationMethodsOtherDeviceInfo() = translate {
        EN - "You are asked to compare some emojis. If they match on both devices, the verification is complete and you can fully use this device."
        DE - "Hierbei werden Sie aufgefordert, eine Reihe von Emojis zu vergleichen. Beim Übereinstimmen auf beiden Geräten wird die Freischaltung aktiviert und Sie können dieses Gerät vollumfänglich nutzen."
    }

    fun selfVerificationMethodsRecoveryKey() = translate {
        EN - "with the recovery key"
        DE - "mit Hilfe des Generalschlüssels"
    }

    fun selfVerificationMethodsRecoveryKeyInfo() = translate {
        EN - "You have to provide the recovery key that you have received during the setup of this account."
        DE - "Geben Sie den Generalschlüssel ein, den Sie beim Erstellen dieses Kontos erhalten haben."
    }

    fun selfVerificationMethodsRecoveryPassphrase() = translate {
        EN - "with the recovery passphrase"
        DE - "mit Hilfe des Generalpassworts"
    }

    fun selfVerificationMethodsRecoveryPassphraseInfo() = translate {
        EN - "You have to provide the recovery passphrase you have chosen during the setup of this account."
        DE - "Geben Sie das Generalpasswort ein, das Sie beim Erstellen dieses Kontos gewählt haben."
    }

    fun selfVerificationMethodsRecoveryPassphraseTitle() = translate {
        EN - "Please provide the recovery passphrase."
        DE - "Bitte geben Sie das Generalpasswort ein."
    }

    fun selfVerificationMethodsRecoveryPassphraseWarning() = translate {
        EN - " this recovery passphrase is not the password of your account!"
        DE - " dieses Passwort is nicht Ihr Konto-Passwort!."
    }

    fun selfVerificationMethodsRecoveryPassphraseWrong() = translate {
        EN - "recovery passphrase wrong"
        DE - "Generalpasswort ist nicht korrekt"
    }

    fun selfVerificationMethodsRecoveryKeyTitle() = translate {
        EN - "Please provide the recovery key for the account."
        DE - "Bitte geben Sie den Generalschlüssel für das Konto ein."
    }

    fun selfVerificationMethodsRecoveryKeyWrong() = translate {
        EN - "recovery key wrong"
        DE - "Generalschlüssel ist nicht korrekt"
    }

    fun selfVerificationResetRecoveryKey() = translate {
        EN - "Reset recovery key"
        DE - "Generalschlüssel zurücksetzen"
    }

    fun verificationWait() = translate {
        EN - "Wait for input on other device."
        DE - "Warte auf Eingabe an anderem Gerät."
    }

    fun verificationStartEmoji() = translate {
        EN - "Start of the emoji comparison"
        DE - "Start des Emoji-Vergleichs"
    }

    fun verificationEmojiComparison() = translate {
        EN - "Please compare the emojis on both devices."
        DE - "Vergleichen Sie bitte die Emojis auf beiden Geräten."
    }

    fun verificationNumberComparison() = translate {
        EN - "Please compare the numbers on both devices."
        DE - "Vergleichen Sie bitte die Zahlen auf beiden Geräten."
    }

    fun verificationMatch() = translate {
        EN - "They match"
        DE - "Sie passen zueinander"
    }

    fun verificationNotMatch() = translate {
        EN - "They do not match"
        DE - "Sie passen nicht zueinander"
    }

    fun verificationSuccess(deviceName: String) = translate {
        EN - "Device '$deviceName' was verified successfully."
        DE - "Das Gerät '$deviceName' konnte erfolgreich freigeschalten werden."
    }

    fun verificationRejected(type: String) = translate {
        EN - "${type.capitalize(Locale.current)} was not successful. The emojis/numbers did not match."
        DE - "${type.capitalize(Locale.current)} war nicht erfolgreich. Die übermittelten Emojis/Zahlen stimmen nicht überein."
    }

    fun verificationTimeout(type: String) = translate {
        EN - "${type.capitalize(Locale.current)} was not successful. The timeout has been reached."
        DE - "${type.capitalize(Locale.current)} war nicht erfolgreich. Das Zeitfenster wurde überschritten."
    }

    fun verificationCancelled(type: String) = translate {
        EN - "${type.capitalize(Locale.current)} has been cancelled."
        DE - "${type.capitalize(Locale.current)} wurde abgebrochen."
    }

    fun deviceVerification() = translate {
        EN - "device verification"
        DE - "Gerätefreischaltung"
    }

    fun userVerification() = translate {
        EN - "user verification"
        DE - "Vertrauensprüfung"
    }

    fun resetWarningIsPermanent() = translate {
        EN - "Resetting the recovery keys is permanent and cannot be undone."
        DE - "Das Zurücksetzen der Generalschlüssel ist dauerhaft und kann nicht rückgängig gemacht werden."
    }

    fun resetWarningLostAccessAndReVerify() = translate {
        EN - "You will be unable to access old encrypted messages and need to re-verify with your contacts."
        DE - "Sie können nicht auf alte verschlüsselte Nachrichten zugreifen und müssen sich bei Ihren Kontakten erneut verifizieren."
    }

    fun resetWarningAcknowledge() = translate {
        EN - "I am aware of the consequences this action will have."
        DE - "Ich bin mir der Konsequenzen dieser Handlung bewusst."
    }

    fun resetProceed() = translate {
        EN - "Proceed with reset"
        DE - "Mit dem Zurücksetzen fortfahren"
    }

    fun resetWarningLastResort() = translate {
        EN - "The reset should be your last resort, please double check and make sure that there is no other option."
        DE - "Das Zurücksetzen sollte Ihre letzte Option sein. Bitte überprüfen Sie es sorgfältig und stellen Sie sicher, dass es keine andere Möglichkeit gibt."
    }

    fun syncOverlayTitle() = translate {
        EN - "Sync..."
        DE - "Lade Daten..."
    }

    fun syncOverlayAccount(userId: UserId) = translate {
        EN - "Account: $userId"
        DE - "Konto: $userId"
    }

    fun syncOverlayInitialSync() = translate {
        EN - "initial Sync"
        DE - "initiales Laden"
    }

    fun syncOverlayInitialSyncInfo(appName: String) = translate {
        EN - "This might take a while. It is only necessary the first time $appName is started on this device."
        DE - "Dieser Vorgang kann einige Zeit dauern und ist nur beim ersten Start von $appName auf diesem Gerät notwendig."
    }

    fun fileOverlayPreviewNotSupported() = translate {
        EN - "File preview not supported"
        DE - "Datei-Vorschau nicht verfügbar"
    }

    fun fileOverlayPdfPageDescriptor(pageId: Int) = translate {
        EN - "PDF page number: $pageId"
        DE - "PDF-Seiten-Nummer $pageId"
    }

    fun invitationAccept() = translate {
        EN - "accept the invitation"
        DE - "Einladung annehmen"
    }

    fun invitationRejectHeader() = translate {
        EN - "Reject invitation"
        DE - "Einladung ablehnen"
    }

    fun invitationReject() = translate {
        EN - "Reject"
        DE - "Ablehnen"
    }

    fun invitationBlock() = translate {
        EN - "Reject and block user"
        DE - "Ablehnen und Nutzer blocken"
    }

    fun blockedContactsHeader() = translate {
        EN - "Blocked contacts"
        DE - "Blockierte Kontakte"
    }

    fun unblockContactDescription() = translate {
        EN - "Unblock"
        DE - "Entblocken"
    }

    fun blockedContactDescription() = translate {
        EN - "Blocked Contact"
        DE - "Blockierter Kontakt"
    }

    fun blockedContactsButtonCaption(count: Int) = translate {
        EN - "$count contacts blocked"
        DE - "$count Kontakte blockiert"
    }

    fun blockedContactsAccountLabel(account: String) = translate {
        EN - "For account $account:"
        DE - "Für's Konto $account:"
    }

    fun blockedContactsEmptyListLabel() = translate {
        EN - "There are no contacts blocked for this account."
        DE - "Es gibt keine blockierten Kontakte für dieses Konto"
    }

    fun roomType() = translate {
        EN - "Setting: "
        DE - "Einstellung: "
    }

    fun roomTypePrivate() = translate {
        EN - "Private"
        DE - "Privat"
    }

    fun roomTypePublic() = translate {
        EN - "Public"
        DE - "Öffentlich"
    }

    fun roomTypeForbidden() = translate {
        EN - "Forbidden"
        DE - "Nicht erlaubt"
    }

    fun roomTypeEncrypted() = translate {
        EN - "Encrypted"
        DE - "Verschlüsselt"
    }

    fun roomTypeUnencrypted() = translate {
        EN - "Unencrypted"
        DE - "Unverschlüsselt"
    }

    fun roomTypeEncryptedInfo() = translate {
        EN - "Encrypted rooms are end-to-end encrypted. Only the participants of the room can read the messages."
        DE - "Verschlüsselte Räume sind Ende-zu-Ende verschlüsselt. Nur die Teilnehmer des Raumes können die Nachrichten lesen."
    }

    fun roomTypeUnencryptedInfo() = translate {
        EN - "Messages in unencrypted rooms can potentially be read by anyone. Use only for non-sensitive information. Otherwise, encrypted rooms are recommended."
        DE - "Nachrichten in unverschlüsselten Räumen können potentiell von jedem gelesen werden. Verwenden Sie diese nur für nicht-sensible Informationen. Ansonsten werden verschlüsselte Räume empfohlen."
    }

    fun roomTypePublicInfo() = translate {
        EN - "Public rooms are visible to all users. You can join them without an invitation."
        DE - "Öffentliche Räume sind für alle Nutzer sichtbar. Sie können ihnen ohne Einladung beitreten."
    }

    fun roomTypePrivateInfo() = translate {
        EN - "Private rooms are only visible to invited users. You can only join them with an invitation."
        DE - "Private Räume sind nur für eingeladene Nutzer sichtbar. Sie können ihnen nur mit einer Einladung beitreten."
    }

    fun roomVisibility() = translate {
        EN - "Visibility: "
        DE - "Sichtbarkeit: "
    }

    fun roomEncryption() = translate {
        EN - "Encryption: "
        DE - "Verschlüsselung: "
    }

    fun optionalGroupNamePlaceholder() = translate {
        EN - "Optional group name"
        DE - "Optionaler Gruppenname"
    }

    fun optionalGroupTopicPlaceholder() = translate {
        EN - "Optional group topic"
        DE - "Optionales Gruppenthema"
    }

    fun downloadMessage() = translate {
        EN - "Download"
        DE - "Herunterladen"
    }

    fun editMessage() = translate {
        EN - "Edit"
        DE - "Bearbeiten"
    }

    fun redactMessage() = translate {
        EN - "Delete"
        DE - "Löschen"
    }

    fun replyMessage() = translate {
        EN - "Answer"
        DE - "Antworten"
    }

    fun reactMessage() = translate {
        EN - "React"
        DE - "Reagieren"
    }

    fun reportMessage() = translate {
        EN - "Report"
        DE - "Bericht"
    }

    fun reportMessageHeader() = translate {
        EN - "Report Message"
        DE - "Nachricht melden"
    }

    fun reportMessageLabel() = translate {
        EN - "Please enter report reason"
        DE - "Bitte geben Sie den Grund für die Meldung ein"
    }

    fun retrySendMessage() = translate {
        EN - "Retry send"
        DE - "Senden erneut versuchen"
    }

    fun abortSendMessage() = translate {
        EN - "Abort send"
        DE - "Senden abbrechen"
    }

    fun debugMessage() = translate {
        EN - "Debug"
        DE - "Debug"
    }

    fun eventMentionPile(roomName: String) = translate {
        EN - "Message in #$roomName"
        DE - "Nachricht in #$roomName"
    }

    fun chatHistoryVisibility() = translate {
        EN - "Chat history visibility"
        DE - "Sichtbarkeit des Chat Verlaufs"
    }

    fun historyVisibilityWorldReadable() = translate {
        EN - "Global"
        DE - "Global"
    }

    fun historyVisibilityWorldReadableExplanation() = translate {
        EN - "All Messages are visible for everyone, even participants that didn't join the room. Use with caution!"
        DE - "Alle Nachrichten sind für jeden Nutzbar sichtbar, auch wenn Sie nicht Teilnehmer des Raumes sind. Verwenden Sie diese Einstellung mit Vorsicht!"
    }

    fun historyVisibilityWorldReadableEncryptedExplanation() = translate {
        EN - "An encrypted group can't be assigned the global chat history visibility "
        DE - "Eine verschlüsselte Gruppe kann keinen global sichtbaren Chat-Verlauf besitzen"
    }

    fun historyVisibilityShared() = translate {
        EN - "Complete history"
        DE - "Gesamter Verlauf"
    }

    fun historyVisibilitySharedExplanation() = translate {
        EN - "All messages are visible to new participants."
        DE - "Alle Nachrichten sind für neue Teilnehmer sichtbar."
    }

    fun historyVisibilityInvited() = translate {
        EN - "Since Invitation"
        DE - "Ab Einladung"
    }

    fun historyVisibilityInvitedExplanation() = translate {
        EN - "All messages since the invitation are visible to new participants."
        DE - "Alle Nachrichten seit der Einladung sind für neue Teilnehmer sichtbar."
    }

    fun historyVisibilityJoined() = translate {
        EN - "After joining"
        DE - "Ab Beitritt"
    }

    fun historyVisibilityJoinedExplanation() = translate {
        EN - "Messages after joining are visible to new participants."
        DE - "Nachrichten sind ab dem Moment des Beitritts sichtbar."
    }

    fun createProfileHeader() = translate {
        EN - "Create a new profile"
        DE - "Anlegen eines neuen Profils"
    }

    fun createProfileSelectName() = translate {
        EN - "Please select a name for your profile"
        DE - "Bitte wählen Sie einen Namen für Ihr Profil"
    }

    fun createProfileAction() = translate {
        EN - "Create profile"
        DE - "Profil anlegen"
    }

    fun selectProfileHeader() = translate {
        EN - "Please select a profile"
        DE - "Wählen Sie ein Profil aus"
    }

    fun selectProfileCreateInstead() = translate {
        EN - "Create a new profile"
        DE - "Neues Profil anlegen"
    }

    fun fileDialogTitleLoad() = translate {
        EN - "Pick attachment"
        DE - "Anhang wählen"
    }

    fun fileDialogLoadFileButton() = translate {
        EN - "Upload file"
        DE - "Datei hochladen"
    }

    fun fileDialogLoadImageButton() = translate {
        EN - "Upload image"
        DE - "Bild hochladen"
    }

    fun fileDialogDownloadErrorSave() = translate {
        EN - "Download failed"
        DE - "Download fehlgeschlagen"
    }

    fun commonAccept() = translate {
        EN - "Accept"
        DE - "Akzeptieren"
    }

    fun locationClickText(pos: Pair<String, String>) = translate {
        EN - "Click to show ${pos.first},${pos.second}"
        DE - "Klicken um ${pos.first},${pos.second} anzuzeigen"
    }

    fun unknownFileInfo() = translate {
        EN - "Unknown file"
        DE - "Unbekannte Datei"
    }

    fun appearanceTitle() = translate {
        EN - "Appearance"
        DE - "Erscheinungsbild"
    }

    fun appearanceColorsTitle() = translate {
        EN - "Colors"
        DE - "Farben"
    }

    fun appearanceThemeHeading(name: String) = translate {
        EN - "Theme: $name"
        DE - "Thema: $name"
    }

    fun appearanceThemeDefaultHeading() = translate {
        EN - "Default"
        DE - "Standard"
    }

    fun appearanceThemeDefaultExplanation() = translate {
        EN - "Let the app decide which theme to use based on the system preference"
        DE - "Lassen Sie die App entscheiden welches Thema verwendet wird auf Basis der Systempreferenz"
    }

    fun appearanceThemeLightHeading() = translate {
        EN - "Light"
        DE - "Hell"
    }

    fun chatJoinRule() = translate {
        EN - "Chat join rule"
        DE - "Beitrittseinstellung der Gruppe"
    }

    fun joinRuleInvitedExplanation() = translate {
        EN - "Only users with an invitation from a group member are able to join"
        DE - "Nur von einem Gruppenmitglied eingeladene Nutzer können beitreten"
    }

    fun joinRulePublicExplanation() = translate {
        EN - "Everybody is able to join"
        DE - "Jeder kann beitreten"
    }

    fun joinRuleKnockExplanation() = translate {
        EN - "Only users who request an invite and are accepted or get one from a group member are able to join"
        DE - "Nur Nutzer, die eine Beitrittsanfrage stellen und akzeptiert werden oder von einem Gruppenmitglied eingeladen werden, können beitreten"
    }

    fun joinRuleRestrictedExplanation() = translate {
        EN - "Only users who satisfy a specified condition are able to join"
        DE - "Nur Nutzer, die eine Bedingung erfüllen, können beitreten"
    }

    fun joinRuleKnockRestrictedExplanation() = translate {
        EN - "Only users who satisfy a specified condition, whose requested invitation gets accepted or are invited by a group member are able to join"
        DE - "Nur Nutzer, die eine Bedingung erfüllen, deren Beitrittsanfrage akzeptiert wird oder die von einem Gruppenmitglied eingeladen werden, können beitreten"
    }

    fun joinRulePrivateExplanation() = translate {
        EN - "Nobody is able to join"
        DE - "Niemand kann beitreten"
    }

    fun joinRulePublic() = translate {
        EN - "Public"
        DE - "Öffentlich"
    }

    fun joinRuleInvited() = translate {
        EN - "Invite"
        DE - "Auf Einladung"
    }

    fun joinRuleKnock() = translate {
        EN - "Knock"
        DE - "Auf Beitrittsanfrage"
    }

    fun joinRuleRestricted() = translate {
        EN - "Restricted"
        DE - "Beschränkt"
    }

    fun joinRuleKnockRestricted() = translate {
        EN - "Knock or restricted"
        DE - "Beschränkt oder auf Beitrittsanfrage"
    }

    fun joinRulePrivate() = translate {
        EN - "Private"
        DE - "Privat"
    }

    fun appearanceThemeLightExplanation() = translate {
        EN - "Force the app to use the light theme"
        DE - "Zwingen Sie die App das helle Thema zu verwenden"
    }

    fun appearanceThemeDarkHeading() = translate {
        EN - "Dark"
        DE - "Dunkel"
    }

    fun appearanceThemeDarkExplanation() = translate {
        EN - "Force the app the use the dark theme"
        DE - "Zwingen Sie die App das dunkle Thema zu verwenden"
    }

    fun appearanceHighContrastHeading() = translate {
        EN - "High Contrast"
        DE - "Hoher Kontrast"
    }

    fun appearanceHighContrastExplanation() = translate {
        EN - "Increases the overall contrast between background and foreground elements to make them easier to discern"
        DE - "Erhöht den Gesamtkontrast zwischen Vorder- und Hintergrund-Elemente um diese besser erkennbar zu machen"
    }

    fun appearanceAccentColorHeading() = translate {
        EN - "Accent Color"
        DE - "Akzentfarbe"
    }

    fun formattedInvitationBody(inviterName: String, roomName: String?) = translate {
        EN - "Invitation from $inviterName${if (roomName != null) " to '$roomName'" else ""}"
        DE - "Einladung von $inviterName${if (roomName != null) " zu '$roomName'" else ""}"
    }

    fun security() = translate {
        EN - "Security"
        DE - "Sicherheit"
    }

    fun roomEndToEndEncryption() = translate {
        EN - "End-to-End encryption"
        DE - "Ende-zu-Ende Verschlüsselung"
    }

    fun roomEndToEndEncryptionDescription() = translate {
        EN - "Enable the end-to-end encryption for this room"
        DE - "Ende-zu-Ende Verschlüsselung für diesen Raum aktivieren"
    }

    fun roomEnableEncryptionWarningConfirmation() = translate {
        EN - "Yes, continue"
        DE - "Ja, fortfahren"
    }

    fun roomSettingsEnableEncryptionWarningTitleGroup() = translate {
        DE - "Verschlüsselung für diese Gruppe aktivieren?"
        EN - "Enable encryption for this group?"
    }

    fun roomSettingsEnableEncryptionWarningMessageGroup() = translate {
        DE - "Die Aktivierung der Verschlüsselung der Gruppe kann nicht rückgängig gemacht werden."
        EN - "The activation of the encryption of the group cannot be revoked."
    }

    fun roomSettingsEnableEncryptionWarningTitleChat() = translate {
        DE - "Verschlüsselung für diesen Chat aktivieren?"
        EN - "Enable encryption for this chat?"
    }

    fun roomSettingsEnableEncryptionWarningMessageChat() = translate {
        DE - "Die Aktivierung der Verschlüsselung des Chats kann nicht rückgängig gemacht werden."
        EN - "The activation of the encryption of the chat cannot be revoked."
    }
}

fun i18nViewModule() = module {
    single<I18nView> { object : I18nView(get(), get(), get()) {} }
}

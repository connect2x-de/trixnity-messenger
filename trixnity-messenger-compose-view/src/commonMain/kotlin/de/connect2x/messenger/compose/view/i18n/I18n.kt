package de.connect2x.messenger.compose.view.i18n

import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages.DE
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages.EN
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18nBase
import de.connect2x.trixnity.messenger.i18n.Languages
import de.connect2x.trixnity.messenger.util.SharedData
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.module

@Suppress("UNUSED")
open class I18nView(
    lang: Languages,
    messengerSettings: MatrixMessengerSettingsHolder,
    getSystemLang: GetSystemLang,
    timeZone: TimeZone,
) : I18nBase(lang, messengerSettings, getSystemLang, timeZone) {
    open fun commonArchived() = translate {
        EN - "Archived"
        DE - "Archiviert"
    }

    open fun commonError() = translate {
        EN - "Error"
        DE - "Fehler"
    }

    open fun commonContextMenu() = translate {
        EN - "context menu"
        DE - "Kontextmenü"
    }

    open fun commonButtonDisabled() = translate {
        EN - "This option is deactivated"
        DE - "Diese Option ist deaktiviert"
    }

    open fun commonInactive() = translate {
        EN - "inactive"
        DE - "inaktiv"
    }

    open fun commonUnknown() = translate {
        EN - "unknown"
        DE - "unbekannt"
    }

    open fun commonAnd(first: String, second: String) = translate {
        EN - "$first and $second"
        DE - "$first und $second"
    }

    open fun commonOr() = translate {
        EN - "or"
        DE - "oder"
    }

    open fun commonCancel() = translate {
        EN - "cancel"
        DE - "abbrechen"
    }

    open fun commonCancelled() = translate {
        EN - "cancelled"
        DE - "abgebrochen"
    }

    open fun commonMore() = translate {
        EN - "more"
        DE - "mehr"
    }

    open fun commonNone() = translate {
        EN - "none"
        DE - "keine"
    }

    open fun commonClose() = translate {
        EN - "close"
        DE - "schließen"
    }

    open fun commonOk() = translate {
        EN - "OK"
        DE - "OK"
    }

    open fun commonBack() = translate {
        EN - "back"
        DE - "zurück"
    }

    open fun commonNext() = translate {
        EN - "next"
        DE - "weiter"
    }

    open fun commonRemove() = translate {
        EN - "remove"
        DE - "entfernen"
    }

    open fun commonDelete() = translate {
        EN - "delete"
        DE - "löschen"
    }

    open fun commonDeleted() = translate {
        EN - "deleted"
        DE - "gelöscht"
    }

    open fun commonCreate() = translate {
        EN - "create"
        DE - "anlegen"
    }

    open fun commonSelect() = translate {
        EN - "select"
        DE - "auswählen"
    }

    open fun commonCopy() = translate {
        EN - "copy"
        DE - "kopieren"
    }

    open fun commonCopied() = translate {
        EN - "copied"
        DE - "kopiert"
    }

    open fun commonWaiting() = translate {
        EN - "waiting"
        DE - "Warten"
    }

    open fun commonDone() = translate {
        EN - "done"
        DE - "abgeschlossen"
    }

    open fun commonRename() = translate {
        EN - "rename"
        DE - "umbenennen"
    }

    open fun commonRefresh() = translate {
        EN - "refresh"
        DE - "Aktualisieren"
    }

    open fun commonEdit() = translate {
        EN - "edit"
        DE - "ändern"
    }

    open fun commonAcceptEdit() = translate {
        EN - "accept changes"
        DE - "Änderungen übernehmen"
    }

    open fun commonVerify() = translate {
        EN - "verify"
        DE - "freischalten"
    }

    open fun commonFailure() = translate {
        EN - "failure"
        DE - "Fehlschlag"
    }

    open fun commonSuccess() = translate {
        EN - "success"
        DE - "Erfolg"
    }

    open fun commonAvatar() = translate {
        EN - "Avatar"
        DE - "Profilbild"
    }

    open fun commonHelp() = translate {
        EN - "help"
        DE - "Hilfe"
    }

    open fun commonInformation() = translate {
        EN - "information"
        DE - "Information"
    }

    open fun commonMoreInformation() = translate {
        EN - "more information"
        DE - "mehr Informationen"
    }

    open fun commonWarning() = translate {
        EN - "warning"
        DE - "Warnung"
    }

    open fun commonImage() = translate {
        EN - "image"
        DE - "Bild"
    }

    open fun commonVideo() = translate {
        EN - "video"
        DE - "Video"
    }

    open fun commonAudio() = translate {
        EN - "audio"
        DE - "Audio"
    }

    open fun commonFile() = translate {
        EN - "file"
        DE - "Datei"
    }

    open fun commonAttachment() = translate {
        EN - "attachment"
        DE - "Anhang"
    }

    open fun commonNotifications() = translate {
        EN - "notifications"
        DE - "Benachrichtigungen"
    }

    open fun commonAccessibility() = translate {
        EN - "Appearance"
        DE - "Aussehen"
    }

    open fun commonDefault() = translate {
        EN - "default"
        DE - "Standard"
    }

    open fun commonOn() = translate {
        EN - "on"
        DE - "an"
    }

    open fun commonOff() = translate {
        EN - "off"
        DE - "aus"
    }

    open fun commonStandard() = translate {
        EN - "standard"
        DE - "Standard"
    }

    open fun commonChat() = translate {
        EN - "chat"
        DE - "Chat"
    }

    open fun commonGroup() = translate {
        EN - "group"
        DE - "Gruppe"
    }

    open fun commonSettings() = translate {
        EN - "settings"
        DE - "Einstellungen"
    }

    open fun commonRecoveryKey() = translate {
        EN - "recovery key"
        DE - "Generalschlüssel"
    }

    open fun commonRecoveryPassphrase() = translate {
        EN - "recovery passphrase"
        DE - "Generalpasswort"
    }

    open fun commonShowPassword() = translate {
        EN - "show password"
        DE - "zeige Passwort"
    }

    open fun commonNotSupported() = translate {
        EN - "not supported"
        DE - "nicht unterstützt"
    }

    open fun commonDays() = translate {
        EN - "days"
        DE - "Tage"
    }

    open fun commonLogo() = translate {
        EN - "logo"
        DE - "Logo"
    }

    open fun commonWelcome() = translate {
        EN - "Welcome"
        DE - "Willkommen"
    }

    open fun commonConfirm() = translate {
        EN - "confirm"
        DE - "bestätigen"
    }

    open fun commonOptionalReason() = translate {
        EN - "Reason (optional)"
        DE - "Grund (optional)"
    }

    open fun commonExpand() = translate {
        EN - "expand"
        DE - "ausklappen"
    }

    open fun commonAll() = translate {
        EN - "All"
        DE - "Alle"
    }

    open fun commonCollapse() = translate {
        EN - "collapse"
        DE - "einklappen"
    }

    open fun commonZoomIn() = translate {
        EN - "zoom in"
        DE - "Vergrößern"
    }

    open fun commonZoomOut() = translate {
        EN - "zoom out"
        DE - "Verkleinern"
    }

    open fun commonSubmit() = translate {
        EN - "submit"
        DE - "absenden"
    }

    open fun commonShowTooltip() = translate {
        EN - "show tooltip"
        DE - "tooltip anzeigen"
    }

    open fun ban() = translate {
        EN - "ban"
        DE - "Bannen"
    }

    open fun banned() = translate {
        EN - "Banned"
        DE - "Gebannt"
    }

    open fun unban() = translate {
        EN - "unban"
        DE - "Entbannen"
    }

    open fun block() = translate {
        EN - "block"
        DE - "Blockieren"
    }

    open fun unblock() = translate {
        EN - "unblock"
        DE - "Entblockieren"
    }

    open fun secure() = translate {
        EN - "secure"
        DE - "Sicher"
    }

    open fun insecure() = translate {
        EN - "insecure"
        DE - "Unsicher"
    }

    open fun contact() = translate {
        EN - "contact"
        DE - "Kontakt"
    }

    open fun newMessage() = translate {
        EN - "new message"
        DE - "neue Nachricht"
    }

    open fun addAlias() = translate {
        EN - "add alias"
        DE - "Alias hinzufügen"
    }

    open fun deleteAlias() = translate {
        EN - "delete alias"
        DE - "Alias löschen"
    }

    open fun makeMainAlias() = translate {
        EN - "set as main alias"
        DE - "als Hauptalias setzen"
    }

    open fun unmakeMainAlias() = translate {
        EN - "remove as main alias"
        DE - "als Hauptalias entfernen"
    }

    open fun mainAlias() = translate {
        EN - "mainalias"
        DE - "Hauptalias"
    }

    open fun alias() = translate {
        EN - "alias"
        DE - "Alias"
    }

    open fun manageAliases() = translate {
        EN - "manage aliases"
        DE - "Aliase verwalten"
    }

    open fun newAlias() = translate {
        EN - "new alias"
        DE - "neuer alias"
    }

    open fun aliases() = translate {
        EN - "aliases"
        DE - "Aliase"
    }

    open fun showAliases() = translate {
        EN - "show aliases"
        DE - "Aliase anzeigen"
    }

    open fun automated() = translate {
        EN - "Automated"
        DE - "Automatisiert"
    }

    open fun passwordVisibility() = translate {
        EN - "show password"
        DE - "Passwort anzeigen"
    }

    open fun passwordVisibilityOff() = translate {
        EN - "hide password"
        DE - "Passwort verstecken"
    }

    open fun presenceOnline() = translate {
        EN - "online"
        DE - "online"
    }

    open fun presenceOffline() = translate {
        EN - "offline"
        DE - "offline"
    }

    open fun presenceUnavailable() = translate {
        EN - "unavailable"
        DE - "nicht erreichbar"
    }

    open fun registrationHeader() = translate {
        EN - " a new user"
        DE - "Registrierung eines neuen Nutzers"
    }

    open fun registrationMethodDependsOnServer() = translate {
        EN - "The available registration methods depend on the chosen server."
        DE - "Die möglichen Registrierungsmechanismen sind abhängig vom gewählten Server."
    }

    open fun registrationOptionsEmpty() = translate {
        EN - "you cannot  an account on this server"
        DE - "Sie können auf diesem Server keine Nutzer anlegen"
    }

    open fun registrationUsername() = translate {
        EN - "Choose your username"
        DE - "Wählen Sie einen Nutzernamen"
    }

    open fun registrationDisplayname() = translate {
        EN - "Choose your displayname"
        DE - "Wählen Sie Ihren Anzeigenamen"
    }

    open fun registrationChangeDisplayname() = translate {
        EN - "Change displayname"
        DE - "Anzeigenamen ändern"
    }

    open fun registrationPassword() = translate {
        EN - "Choose your password"
        DE - "Wählen Sie ein Passwort"
    }

    open fun registrationToken() = translate {
        EN - "registration token"
        DE - "Registrierungs-Token"
    }

    open fun registrationDummy() = translate {
        EN - ""
        DE - ""
    }

    open fun registrationUsernamePassword() = translate {
        EN - " with username and password"
        DE - "Registrierung mit Nutzername und Passwort"
    }

    open fun registrationEmail() = translate {
        EN - " with Email"
        DE - "Registrierung über E-Mail"
    }

    open fun registrationRecaptcha() = translate {
        EN - "solve a CAPTCHA"
        DE - "ein CAPTCHA lösen"
    }

    open fun registrationSso() = translate {
        EN - " with Single Sign On"
        DE - "Registrierung über Single Sign On"
    }

    open fun registrationMsisdn() = translate {
        EN - " with MSISDN"
        DE - "Registrierung über MSISDN"
    }

    open fun verificationVerifiedDevice() = translate {
        EN - "This device has been verified. Other users that trust you will trust messages sent from this device."
        DE - "Dieses Gerät ist Ihnen zugeordnet. Andere Teilnehmer, die Ihnen vertrauen, vertrauen Nachrichten von diesem Gerät."
    }

    open fun verificationVerifiedUser() = translate {
        EN - "You trust this user and all of her/his devices."
        DE - "Sie vertrauen diesem Nutzer und all seinen Geräten."
    }

    open fun verificationNotVerifiedDevice() = translate {
        EN - "This device has not been verified by you. Other users might see a warning to not trust messages sent from this device."
        DE - "Dieses Gerät ist Ihnen noch nicht zugeordnet. Andere Teilnehmer sehen evtl. eine Warnung, Ihren Nachrichten von diesem Gerät nicht zu vertrauen."
    }

    open fun verificationNotVerifiedUser() = translate {
        EN - "This user has at least one unverified device activated. It cannot be verified that messages you receive are actually from this user."
        DE - "Dieser Nutzer verwendet Geräte, die ihm nicht zugeordnet sind. Dass Nachrichten wirklich von ihm stammen, kann nicht überprüft werden."
    }

    open fun verificationNeutralUser() = translate {
        EN - "This user has only verified devices. You have not verified yet that the user is the person you expect her/him to be."
        DE - "Dieser Nutzer verwendet ausschließlich Geräte, die ihm zugeordnet sind. Sie haben noch nicht überprüft, ob es sich tatsächlich um die ausgegebene Person handelt."
    }

    open fun verificationTrusted() = translate {
        EN - "trusted"
        DE - "vertraut"
    }

    open fun verificationNotTrusted() = translate {
        EN - "not trusted"
        DE - "nicht vertraut"
    }

    open fun verificationNotVerifiedYet() = translate {
        EN - "not verified yet"
        DE - "noch nicht verifiziert"
    }

    open fun verificationAlreadyRunning() = translate {
        EN - "There is already a verification process with this user, please finish/cancel it before starting a new verification"
        DE - "Es läuft bereits ein Verifikationsprozess mit diesem Nutzer, bitte beenden oder brechen Sie diesen ab, bevor Sie eine neue Verifikation starten"
    }

    open fun verificationAlreadyRunningInAnotherRoom() = translate {
        EN - "There is already a verification process with this user in another room"
        DE - "Es läuft bereits ein Verifikationsprozess mit diesem Nutzer in einem anderen Raum"
    }

    open fun dehydratedDevice() = translate {
        EN - "This virtual (so called dehydrated) device is used to decrypt messages when no other device is online."
        DE - "Dieses virtuelle (sogenannte dehydrierte) Gerät wird verwendet um Nachrichten zu entschlüsseln, wenn kein anderes Gerät online ist."
    }

    open fun anErrorHasOccurred() = translate {
        EN - "An error has occurred."
        DE - "Ein Fehler ist aufgetreten."
    }

    open fun errorDetails() = translate {
        EN - "Error Details"
        DE - "Fehlerdetails"
    }

    open fun closeApp(appName: String) = translate {
        EN - "Close $appName"
        DE - "$appName schließen"
    }

    open fun login() = translate {
        EN - "Login"
        DE - "Anmelden"
    }

    open fun authenticate() = translate {
        EN - "authenticate"
        DE - "Autorisieren"
    }

    open fun externalLogin(providerName: String) = translate {
        EN - "Login externally with $providerName"
        DE - "Extern anmelden mit $providerName"
    }

    open fun loginAt(serverUrl: String) = translate {
        EN - "Login at $serverUrl"
        DE - "Anmelden bei $serverUrl"
    }

    open fun loginWithPassword() = translate {
        EN - "Login with password"
        DE - "Anmelden mit Passwort"
    }

    open fun loginWithSSO(name: String) = translate {
        EN - "Login with $name"
        DE - "Anmelden mit $name"
    }

    open fun register() = translate {
        EN - "Create new account"
        DE - "Neues Konto anlegen"
    }

    open fun uiaPasswordTitle() = translate {
        EN - "Password Authorization"
        DE - "Passwort Autorisierung"
    }

    open fun uiaPasswordButtonSubmit() = translate {
        EN - "submit"
        DE - "absenden"
    }

    open fun uiaFallbackTitle() = translate {
        EN - "3rd Party Authorization"
        DE - "Drittanbieter Autorisierung"
    }

    open fun uiaFallbackButtonRedirect() = translate {
        EN - "Redirect"
        DE - "Umleiten"
    }

    open fun uiaRegistrationTokenTitle() = translate {
        EN - "Registration Token"
        DE - "Registrierungs-Token"
    }

    open fun uiaRegistrationTokenButtonSubmit() = translate {
        EN - "submit"
        DE - "absenden"
    }

    open fun uiaRegistrationTokenAddToken() = translate {
        EN - "your registration token"
        DE - "Ihr Registrierungs-Token"
    }

    open fun uiaDummyTitle() = translate {
        EN - "Dummy Authorization"
        DE - "Dummy Autorisierung"
    }

    open fun uiaDummyButtonNext() = translate {
        EN - "next"
        DE - "weiter"
    }

    open fun uiaMsisdnTitle() = translate {
        EN - "Phone number authentication"
        DE - "Autorisierung mit Telefonnummer"
    }

    open fun uiaEmailTitle() = translate {
        EN - "Email authentication"
        DE - "E-Mail Autorisierung"
    }

    open fun addMatrixClientCreateMatrixAccount() = translate {
        EN - "Creation of your Matrix account"
        DE - "Einrichtung Ihres Matrix Kontos"
    }

    open fun addMatrixClientAnotherMatrixClient() = translate {
        EN - "Adding another Matrix account"
        DE - "Hinzufügen eines weiteren Matrix Kontos"
    }

    open fun addMatrixClientServerMatrix() = translate {
        EN - "Your Matrix server"
        DE - "Ihr Matrix Server"
    }

    open fun addMatrixClientAccountName() = translate {
        EN - "Account name"
        DE - "Name für dieses Konto"
    }

    open fun addMatrixClientAccountNameHelp() = translate {
        EN - "You can add more accounts later. Therefore, try to use an account name that is recognizable."
        DE - "Sie können später weitere Konten hinzufügen. Vergeben Sie deshalb einen wiedererkennbaren Namen."
    }

    open fun addMatrixClientServerDiscoverySuccess() = translate {
        EN - "server discovery has been successful"
        DE - "Server konnte bestimmt werden"
    }

    open fun addMatrixClientMatrixAddress() = translate {
        EN - "your Matrix address"
        DE - "Ihre Matrix Adresse"
    }

    open fun addMatrixClientMatrixUsername() = translate {
        EN - "your Matrix username"
        DE - "Ihr Matrix Nutzername"
    }

    open fun addMatrixClientPassword() = translate {
        EN - "your password"
        DE - "Ihr Passwort"
    }

    open fun matrixClientLogout(accountName: String) = translate {
        EN - "Logout of account $accountName."
        DE - "Ausloggen aus Account $accountName."
    }

    open fun storeFailureAlreadyOpen(appName: String) = translate {
        EN - "$appName is already opened. This window will therefore be closed."
        DE - "$appName ist bereits geöffnet. Dieses Fenster wird nun geschlossen."
    }

    open fun storeFailureLocalDbNotLoaded() = translate {
        EN - "The local database could not be loaded."
        DE - "Die lokale Datenbank konnte nicht geladen werden."
    }

    open fun storeFailureLocalDbSelect() = translate {
        EN - "Unfortunately, this cannot be repaired automatically. You have 2 options:"
        DE - "Dieser Zustand kann leider nicht repariert werden. Sie haben nun folgende 2 Möglichkeiten: "
    }

    open fun storeFailureLocalDbRestart(appName: String) = translate {
        EN - "A restart of $appName might solve the problem. If this problem persists, please file a bug report."
        DE - "Sie können versuchen $appName neu zu starten. Falls das Problem bestehen bleibt, erstellen Sie bitte einen Fehlerbericht."
    }

    open fun storeFailureDeleteLocalDb() = translate {
        EN - "Delete the local database."
        DE - "Die lokale Datenbank löschen."
    }

    open fun storeFailureDeleteLocalDbSelect() = translate {
        EN - "This option should only be selected if:"
        DE - "Diese Option sollte nur gewählt werden, wenn:"
    }

    open fun storeFailureDeleteLocalDbRecoveryKey() = translate {
        EN - "• you have the account's recovery key with you OR"
        DE - "• Sie den Generalschlüssel für Ihr Konto parat haben ODER"
    }

    open fun storeFailureDeleteLocalDbOtherDevice() = translate {
        EN - "• you have another device with you that has been activated with this account"
        DE - "• Sie ein freigeschaltetes Gerät zur Hand haben um eine Freischaltung durchzuführen"
    }

    open fun storeFailureDeleteLocalDbWarning() = translate {
        EN - "You can only restore your old messages on this device if you have the recovery key or another device with the account with you. Otherwise, those messages are lost!"
        DE - "Nur mithilfe des Generalschlüssels oder der Freischaltung über ein anderes Gerät können Sie ihre alten Nachrichten lesen. Andernfalls sind diese Nachrichten verloren!"
    }

    open fun storeFailureDeleteDb() = translate {
        EN - "Delete database"
        DE - "Datenbank löschen"
    }

    open fun imageCouldNotBeLoaded() = translate {
        EN - "Cannot load image file."
        DE - "Bild konnte nicht geladen werden."
    }

    open fun videoCouldNotBeLoaded() = translate {
        EN - "Cannot load video file."
        DE - "Video konnte nicht geladen werden."
    }

    open fun fileCouldNotBeLoaded() = translate {
        EN - "Cannot load file."
        DE - "Datei konnte nicht geladen werden."
    }

    open fun addMembers() = translate {
        EN - "add members"
        DE - "Teilnehmer hinzufügen"
    }

    open fun memberListChangeRole(username: String, oldRole: String, newRole: String) = translate {
        EN - "Change the role of $username from \"$oldRole\" to \"$newRole\"?"
        DE - "Rolle von $username von \"$oldRole\" zu \"$newRole\" ändern?"
    }

    open fun memberListChangeRoleWarning() = translate {
        EN - "This action might not be reversible."
        DE - "Diese Aktion kann eventuell nicht mehr rückgängig gemacht werden."
    }

    open fun userProfileRoleAdministrator() = translate {
        EN - "administrator"
        DE - "Administrator"
    }

    open fun userProfileRoleModerator() = translate {
        EN - "moderator"
        DE - "Moderator"
    }

    open fun userProfileRoleUser() = translate {
        EN - "default"
        DE - "Standard"
    }

    open fun memberListChangeTo(role: String) = translate {
        EN - "change status to $role"
        DE - "zum $role machen"
    }

    open fun userProfileChangePowerLevel() = translate {
        EN - "change power level"
        DE - "Berechtigungslevel festlegen"
    }

    open fun userProfileNote() = translate {
        EN - "Note:"
        DE - "Hinweis:"
    }

    open fun userProfileNoteText() = translate {
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

    open fun userProfileContact() = translate {
        EN - "contact user"
        DE - "Teilnehmer kontaktieren"
    }

    open fun userProfileVerification() = translate {
        EN - "start user verification"
        DE - "Vertrauensprüfung starten"
    }

    open fun userProfileNavigateToVerification() = translate {
        EN - "to verification process"
        DE - "Zur Vertrauensprüfung"
    }

    open fun userProfileCopyUserId() = translate {
        EN - "copy MXID"
        DE - "Kopiere MXID"
    }

    open fun userProfileRemoveUser() = translate {
        EN - "remove user"
        DE - "Teilnehmer entfernen"
    }

    open fun userProfileBanUser() = translate {
        EN - "ban user"
        DE - "Teilnehmer bannen"
    }

    open fun userProfileAcceptKnock() = translate {
        EN - "Accept membership request"
        DE - "Beitrittsanfrage annehmen"
    }

    open fun userProfileRejectKnock() = translate {
        EN - "Reject membership request"
        DE - "Beitrittsanfrage ablehnen"
    }

    open fun userProfileBlockUser() = translate {
        EN - "block user"
        DE - "Teilnehmer blockieren"
    }

    open fun userProfileRemoveUserConfirmation() = translate {
        EN - "Yes, remove user"
        DE - "Ja, Teilnehmer entfernen"
    }

    open fun userProfileBanUserConfirmation() = translate {
        EN - "Yes, ban user"
        DE - "Ja, Teilnehmer bannen"
    }

    open fun userProfileBanReason() = translate {
        EN - "ban reason"
        DE - "Bangrund"
    }

    open fun userProfileBanUserConfirmationSure() = translate {
        EN - "Are you sure to ban this user?"
        DE - "Möchten Sie den Teilnehmer wirklich bannen?"
    }

    open fun userProfileRoomOptions() = translate {
        EN - "Room Options"
        DE - "Raumoptionen"
    }

    open fun userProfileUserOptions() = translate {
        EN - "User Options"
        DE - "Benutzeroptionen"
    }

    open fun settingsRoomMemberListJoined() = translate {
        EN - "Joined"
        DE - "Beigetreten"
    }

    open fun settingsRoomMemberListKnocking() = translate {
        EN - "Kocking"
        DE - "Klopfend"
    }

    open fun settingsRoomMemberListInvited() = translate {
        EN - "Invited"
        DE - "Eingeladen"
    }

    open fun settingsRoomMemberListBanned() = translate {
        EN - "Banned"
        DE - "Gebannt"
    }

    open fun unbannable() = translate {
        EN - "unbanable"
        DE - "entbannbar"
    }

    open fun notUnbannable() = translate {
        EN - "not unbannable"
        DE - "nicht entbannbar"
    }

    open fun memberListUnbanUser() = translate {
        EN - "unban user"
        DE - "Teilnehmer entbannen"
    }

    open fun unbanTitle() = translate {
        EN - "Unban user"
        DE - "Teilnehmer entbannen"
    }

    open fun unbanUserConfirmation() = translate {
        EN - "Yes, unban user"
        DE - "Ja, Teilnehmer entbannen"
    }

    open fun roomHeaderUserIsBlocked() = translate {
        EN - "This user is blocked by you."
        DE - "Dieser Nutzer wird von Ihnen geblockt."
    }

    open fun roomHeaderKnockingUsersCount(count: Int) = translate {
        EN - "$count user${if (count > 1) "s" else ""} requesting membership"
        DE - "$count Nutzer frag${if (count > 1) "en" else "t"} den Beitritt am"
    }

    open fun timelineElementMetadataTitle() = translate {
        EN - "Message details"
        DE - "Nachrichtendetails"
    }

    open fun timelineElementMetadataSender() = translate {
        EN - "Sender"
        DE - "Absender"
    }

    open fun timelineElementMetadataUserInfoTooltipReactions(reactions: String) = translate {
        EN - "Reactions: $reactions"
        DE - "Reaktionen: $reactions"
    }

    open fun timelineElementMetadataMessage() = translate {
        EN - "Message"
        DE - "Nachricht"
    }

    open fun timelineElementMetadataHistory() = translate {
        EN - "Show history of message"
        DE - "Zeige Nachrichtenhistorie"
    }

    open fun timelineElementMetadataReadersAndReactions() = translate {
        EN - "Seen and reacted by"
        DE - "Leser und Reaktionen"
    }

    open fun timelineElementMetadataReadersAndReactionsNone() = translate {
        EN - "No message interactions yet."
        DE - "Bisher keine Nachrichtsinteraktionen"
    }

    // Eigenname, daher keine Übersetzung
    open fun timelineElementMetadataBody() = translate {
        EN - "Body"
        DE - "Body"
    }

    // Eigenname, daher keine Übersetzung
    open fun timelineElementMetadataFormattedBody() = translate {
        EN - "Formatted Body"
        DE - "Formatted Body"
    }

    open fun timelineJumpToEnd() = translate {
        EN - "Jump to the end"
        DE - "Ans Ende springen"
    }

    open fun roomSettings() = translate {
        EN - "room settings"
        DE - "Raumeinstellungen"
    }

    open fun roomSettingsRoomName() = translate {
        EN - "Name"
        DE - "Name"
    }

    open fun roomSettingsRoomNamePlaceholder() = translate {
        EN - "give this room a name"
        DE - "Geben Sie diesem Raum einen Namen"
    }

    open fun roomSettingsRoomNameCannotChange() = translate {
        EN - "You do not have sufficient rights to change the room's name."
        DE - "Sie haben nicht die nötigen Rechte um den Raumnamen zu ändern."
    }

    open fun roomSettingsRoomTopic() = translate {
        EN - "Topic"
        DE - "Beschreibung"
    }

    open fun roomSettingsRoomTopicPlaceholder() = translate {
        EN - "give this room a topic"
        DE - "Geben Sie diesem Raum eine Beschreibung"
    }

    open fun roomSettingsRoomTopicCannotChange() = translate {
        EN - "You do not have sufficient rights to change the room's topic."
        DE - "Sie haben nicht die nötigen Rechte um die Raumbeschreibung zu ändern."
    }

    open fun roomSettingsMembers() = translate {
        EN - "Members"
        DE - "Mitglieder"
    }

    open fun roomSettingsAliases() = translate {
        EN - "Room Aliases"
        DE - "Raumaliase"
    }

    open fun roomSettingsBannedMembers() = translate {
        EN - "Banned members"
        DE - "Gebannte Mitglieder"
    }

    open fun roomSettingsMentions() = translate {
        EN - "Mentions"
        DE - "Erwähnungen"
    }

    open fun userSearchSearchPeople() = translate {
        EN - "search people"
        DE - "suche Personen"
    }

    open fun messageInfoReadBy() = translate {
        EN - "Read by"
        DE - "Gelesen von"
    }

    open fun messageInfoReactions() = translate {
        EN - "Reactions"
        DE - "Reaktionen"
    }

    open fun userSearchNameOrMatrixId() = translate {
        EN - "display name or Matrix id"
        DE - "Name oder Matrix Id"
    }

    open fun userSearchOffline() = translate {
        EN - "You are offline and thus cannot search for people."
        DE - "Sie sind offline. Personen können daher nicht gesucht werden."
    }

    open fun userSearchNotFound() = translate {
        EN - "no people found"
        DE - "keine Personen gefunden"
    }

    open fun indicatorUnreadMessages() = translate {
        EN - "unread messages"
        DE - "ungelesene Nachrichten"
    }

    open fun indicatorLeave(groupOrChat: String) = translate {
        EN - "leave $groupOrChat"
        DE - "$groupOrChat verlassen"
    }

    open fun inputAreaCannotSendMessages() = translate {
        EN - "You cannot send messages here."
        DE - "Sie können hier keine Nachrichten senden."
    }

    open fun inputAreaPrompt() = translate {
        EN - "Your message..."
        DE - "Ihre Nachricht..."
    }

    open fun inputAreaCancelEdit() = translate {
        EN - "cancel edit"
        DE - "Bearbeiten abbrechen"
    }

    open fun inputAreaSend() = translate {
        EN - "send"
        DE - "Senden"
    }

    open fun inputAreaEmojis() = translate {
        EN - "emojis"
        DE - "Emojis"
    }

    open fun inputAreaSelectAttachment() = translate {
        EN - "select attachment"
        DE - "Anhang auswählen"
    }

    open fun messageBubbleBeingDeleted() = translate {
        EN - "being deleted"
        DE - "wird gelöscht"
    }

    open fun messageBubbleEdited() = translate {
        EN - "(edited)"
        DE - "(editiert)"
    }

    open fun messageBubbleRead() = translate {
        EN - "read"
        DE - "gelesen"
    }

    open fun messageBubbleSent() = translate {
        EN - "sent"
        DE - "gesendet"
    }

    open fun messageContentWaitForKeys() = translate {
        EN - "waiting for decryption keys"
        DE - "Warten auf Schlüssel zur Entschlüsselung"
    }

    open fun messageContentNoDecryption() = translate {
        EN - "message could not be decrypted"
        DE - "Nachricht konnte nicht entschlüsselt werden"
    }

    open fun messageContentDownloadCompleted() = translate {
        EN - "download completed"
        DE - "Download abgeschlossen"
    }

    open fun replyTo() = translate {
        EN - "reply to"
        DE - "Antwort auf"
    }

    open fun replyToCancel() = translate {
        EN - "cancel reply"
        DE - "Antworten abbrechen"
    }

    open fun roomHeaderSettings() = translate {
        EN - "room settings"
        DE - "Raumeinstellungen"
    }

    open fun roomHeaderMore() = translate {
        EN - "more"
        DE - "mehr"
    }

    open fun roomHeaderStartUserVerification() = translate {
        EN - "start user verification"
        DE - "Vertrauensprüfung starten"
    }

    open fun exportRoom(groupOrChat: String) = translate {
        EN - "Export $groupOrChat"
        DE - "$groupOrChat exportieren"
    }

    open fun exportRoomAbort() = translate {
        EN - "Cancel"
        DE - "Abbrechen"
    }

    open fun exportRoomButton() = translate {
        EN - "Export"
        DE - "Exportieren"
    }

    open fun exportRoomTargetDirectory() = translate {
        EN - "target directory"
        DE - "Zielpfad"
    }

    open fun exportRoomTargetDirectoryAndroid() = translate {
        EN - "Your export is placed inside the Downloads folder"
        DE - "Ihr Export wird im Downloads Ordner abgelegt"
    }

    open fun exportRoomBodyLabel(roomName: String) = translate {
        EN - "Select from the options below to export '$roomName'"
        DE - "Wählen Sie eine der folgenden Optionen, um '$roomName' zu exportieren"
    }

    open fun exportRoomTargetPlainText() = translate {
        EN - "file: plain text (txt)"
        DE - "Datei: einfacher text (txt)"
    }

    open fun exportRoomTargetCsv() = translate {
        EN - "file: table (csv)"
        DE - "Datei: Tabelle (csv)"
    }

    open fun archiveThresholdFromBeginning() = translate {
        EN - "From the beginning"
        DE - "Von Beginn an"
    }

    open fun archiveThresholdSpecifyNumber() = translate {
        EN - "Specify a number of messages"
        DE - "Geben Sie eine Anzahl von Nachrichten an"
    }

    open fun labelMessages() = translate {
        EN - "Messages"
        DE - "Nachrichten"
    }

    open fun roomHeaderBlockUser() = translate {
        EN - "block user"
        DE - "Nutzer blocken"
    }

    open fun roomHeaderUnblockUser() = translate {
        EN - "unblock user"
        DE - "Nutzer entblocken"
    }

    open fun sendAttachmentTitle() = translate {
        EN - "Send attachment"
        DE - "Anhang versenden"
    }

    open fun timelineSendFile() = translate {
        EN - "send file"
        DE - "Datei senden"
    }

    open fun userVerificationStarted(sender: String) = translate {
        EN - "user verification (started by $sender)"
        DE - "Vertrauensprüfung (gestartet von $sender)"
    }

    open fun userVerificationDone() = translate {
        EN - "done"
        DE - "beendet"
    }

    open fun userVerificationSuccess() = translate {
        EN - "success"
        DE - "erfolgreich"
    }

    open fun userVerificationNotSuccessful() = translate {
        EN - "not successful"
        DE - "nicht erfolgreich"
    }

    open fun userVerificationRequest(from: String) = translate {
        EN - "In the following steps it is made sure that messages of '$from' are actually of this user."
        DE - "Im Folgenden wird sichergestellt, dass Nachrichten von '$from' auch wirklich von diesem Nutzer stammen."
    }

    open fun userVerificationSuccessMessage() = translate {
        EN - "You now trust this user"
        DE - "Sie vertrauen nun diesem Nutzer."
    }

    open fun userVerificationOtherDevice() = translate {
        EN - "This request is already handled by another device."
        DE - "Diese Anfrage wird bereits durch ein anderes Gerät durchgeführt."
    }

    open fun accountChangeAccount() = translate {
        EN - "Change account"
        DE - "Konto wechseln"
    }

    open fun accountAllAccounts() = translate {
        EN - "All accounts"
        DE - "Alle Konten"
    }

    open fun accountDeactivateSearch() = translate {
        EN - "deactivate search"
        DE - "Suche ausschalten"
    }

    open fun accountActivateSearch() = translate {
        EN - "activate search for people and groups"
        DE - "Suche nach Personen oder Gruppen einschalten"
    }

    open fun accountCloseProfile() = translate {
        EN - "close the currently selected profile and return to profile selection"
        DE - "schließe das aktuell gewählte Profil und kehre zur Profilauswahl zurück"
    }

    open fun accountCreateNewRoom() = translate {
        EN - "create new chat or group"
        DE - "neuen Chat oder Gruppe erstellen"
    }

    open fun accountSelectAccount() = translate {
        EN - "Select an account"
        DE - "Wählen Sie ein Konto aus"
    }

    open fun accountMoreSettings() = translate {
        EN - "more settings"
        DE - "weitere Einstellungen"
    }

    open fun accountYourAccounts() = translate {
        EN - "Your accounts"
        DE - "Ihre Konten"
    }

    open fun accountAboutTheApp(appName: String) = translate {
        EN - "About $appName"
        DE - "Über $appName"
    }

    open fun accountSendErrorLogs() = translate {
        EN - "Send error logs"
        DE - "Fehlerbericht senden"
    }

    open fun createNewGroupNewGroup() = translate {
        EN - "New group"
        DE - "Neue Gruppe"
    }

    open fun createNewGroupAddUser() = translate {
        EN - "Add user"
        DE - "Teilnehmer hinzufügen"
    }

    open fun createNewGroupCreate() = translate {
        EN - "Create group"
        DE - "Gruppe anlegen"
    }

    open fun createNewGroupSearch() = translate {
        EN - "Search group"
        DE - "Gruppe suchen"
    }

    open fun searchGroupTitle() = translate {
        EN - "Search public groups"
        DE - "Öffentliche Gruppen suchen"
    }

    open fun searchGroupSearch() = translate {
        EN - "Search term"
        DE - "Suchbegriff"
    }

    open fun searchGroupNotFound() = translate {
        EN - "No group found"
        DE - "Keine passende Gruppe gefunden"
    }

    open fun createNewChatTitle() = translate {
        EN - "New chat"
        DE - "Neuer Chat"
    }

    open fun roomListRemoveRoom() = translate {
        EN - "Remove chat or group"
        DE - "Chat oder Gruppe entfernen"
    }

    open fun roomListNoRoom() = translate {
        EN - "You have no chats or groups, yet."
        DE - "Sie haben noch keine Chats oder Gruppen."
    }

    open fun roomListCreateRoom() = translate {
        EN - "Create a new chat or group"
        DE - "Neuen Chat oder Gruppe anlegen"
    }

    open fun roomListSearch() = translate {
        EN - "Search for people or groups"
        DE - "Suche Personen oder Gruppen"
    }

    open fun roomListSyncErrorNoConnection() = translate {
        EN - "No connection"
        DE - "Keine Verbindung"
    }

    open fun roomListSyncErrorNoInternet() = translate {
        EN - "Connection to the internet is lost"
        DE - "Verbindung zum Internet unterbrochen"
    }

    open fun roomListSyncErrorAccounts(accountList: String) = translate {
        EN - "These accounts are not connected: $accountList"
        DE - "Folgende Accounts sind nicht verbunden: $accountList"
    }

    open fun roomListSyncErrorSendMessages() = translate {
        EN - "Your messages are sent as soon as a connection can be established."
        DE - "Ihre Nachrichten werden gesendet sobald eine Verbindung hergestellt werden kann."
    }

    open fun roomListJoin() = translate {
        EN - "Join"
        DE - "Beitreten"
    }

    open fun roomListAccountNotVerifiedIcon() = translate {
        EN - "Account not verified"
        DE - "Nutzerkonto nicht verifiziert"
    }

    open fun roomListAccountNotVerifiedMessage(userId: UserId) = translate {
        EN - "Click here to verify this device for your account $userId"
        DE - "Drücken Sie hier um dieses Gerät für ihr Konto $userId zu verifizieren."
    }

    open fun accountsOverviewCreateNewAccount() = translate {
        EN - "Create new account"
        DE - "Neues Konto anlegen"
    }

    open fun accountsOverviewLogoutWarning(userId: String) = translate {
        EN - "Logout of account '$userId'"
        DE - "Ausloggen aus Account '$userId'"
    }

    open fun accountsOverviewLogoutWarningExplanation() = translate {
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

    open fun accountsOverviewLogout() = translate {
        EN - "Logout"
        DE - "Ausloggen"
    }

    open fun appInfoVersion(versionNumber: String) = translate {
        EN - "Version $versionNumber"
        DE - "Version $versionNumber"
    }

    open fun appInfoPrivacy() = translate {
        EN - "Privacy"
        DE - "Datenschutz"
    }

    open fun appInfoPrivacyLink() = translate {
        EN - "Show Privacy Info"
        DE - "Zur Datenschutzerklärung"
    }

    open fun appInfoImprint() = translate {
        EN - "Imprint"
        DE - "Impressum"
    }

    open fun appInfoImprintLink() = translate {
        EN - "Show Imprint"
        DE - "Zum Impressum"
    }

    open fun appInfoLicenses() = translate {
        EN - "Licenses"
        DE - "Lizenzen"
    }

    open fun configureNotificationsSettings() = translate {
        EN - "Notification settings"
        DE - "Benachrichtigungseinstellungen"
    }

    open fun devicesTitle() = translate {
        EN - "Devices"
        DE - "Geräte"
    }

    open fun devicesThisDevice() = translate {
        EN - "This device"
        DE - "Dieses Gerät"
    }

    open fun devicesOtherDevices() = translate {
        EN - "Other devices"
        DE - "Andere Geräte"
    }

    open fun devicesRemoveDevice() = translate {
        EN - "Remove device"
        DE - "Gerät entfernen"
    }

    open fun devicesRemoveDevice(deviceName: String) = translate {
        EN - "Remove device '$deviceName'"
        DE - "Gerät '$deviceName' entfernen"
    }

    open fun devicesRemoveDeviceInformationHeader() = translate {
        EN - "You are about to remove one of your devices. This should only be necessary if:"
        DE - "Sie sind im Begriff eines Ihrer Geräte zu entfernen. Dies sollte nur notwendig sein, wenn:"
    }

    open fun devicesRemoveDeviceInformationLostDevice() = translate {
        EN - "• Your device has been lost"
        DE - "• Ihr Gerät verloren gegangen ist"
    }

    open fun devicesRemoveDeviceInformationAttacker() = translate {
        EN - "• Your device has been compromised by an attacker"
        DE - "• Ihr Gerät durch einen Angreifer kompromittiert wurde"
    }

    open fun devicesRemoveDeviceEnterPassword() = translate {
        EN - "Please confirm removing the device with your account's password:"
        DE - "Bitte bestätigen Sie das Entfernen mit der Eingabe Ihres Konto-Passworts:"
    }

    open fun devicesRemovePasswordNotCorrect() = translate {
        EN - "Password is not correct"
        DE - "Passwort ist nicht korrekt"
    }

    open fun devicesRemoveDeviceConfirm(deviceName: String) = translate {
        EN - "I want to remove the device '$deviceName'."
        DE - "Ich möchte das Gerät '$deviceName' entfernen."
    }

    open fun notificationsSettingsEnabledForThisDevice() = translate {
        EN - "Enable Notifications on this device"
        DE - "Benachrichtigungen auf diesem Gerät aktivieren"
    }

    open fun notificationsSettingsPlatform() = translate {
        EN - "Device Settings"
        DE - "Geräteeinstellungen"
    }

    open fun notificationSettingsPlatformEnablePermissionsWarning() = translate {
        EN - "Please enable notifications via the device settings"
        DE - "Bitte erlauben Sie das Senden von Benachrichtigungen in den Geräteeinstellungen"
    }

    open fun notificationsSettingsPlatformPushMode(mode: String) = translate {
        EN - "Mode: $mode"
        DE - "Modus: $mode"
    }

    open fun notificationsSettingsPlatformPushModePush() = translate {
        EN - "Energy-Saving"
        DE - "Energiesparend"
    }

    open fun notificationsSettingsPlatformPushModePushExplantation() = translate {
        EN - "You will receive a notification on your phone instantly when a new message is posted via Google Services. Google cannot access any contents of the messages, but notice that you have received a message."
        DE - "Über Google Services wird im Falle einer neuen Nachricht an Sie sofort eine Meldung auf Ihr Mobilgerät übertragen. Google kann hierbei auf keine Nachrichteninhalte zugreifen, aber sehen, dass Sie eine Nachricht empfangen haben."
    }

    open fun notificationsSettingsPlatformPushModePolling() = translate {
        EN - "Privacy-Friendly"
        DE - "Datenschutzfreundlich"
    }

    open fun notificationsSettingsPlatformPushModePollingExplanation() = translate {
        EN - "Notifications are received with a delay. You might notice a permanent element in the notification bar that new messages are received."
        DE - "Benachrichtigungen werden mit einer Verzögerung empfangen. Sie sehen evtl. dauerhaft ein Element in der Benachrichtigungsleiste, dass neue Nachrichten empfangen werden."
    }

    open fun notificationsSettingsPlatformPlaySound() = translate {
        EN - "Sound"
        DE - "Töne"
    }

    open fun notificationsSettingsPlatformShowPopup() = translate {
        EN - "Show popup"
        DE - "Zeige Popup"
    }

    open fun notificationsSettingsPlatformShowText() = translate {
        EN - "Show text preview"
        DE - "Textvorschau zeigen"
    }


    open fun notificationsSettingsAccountDefaultLevel(level: String) = translate {
        EN - "Default Level: $level"
        DE - "Standardlevel: $level"
    }

    open fun notificationsSettingsAccountDefaultLevelRoom() = translate {
        EN - "All Rooms"
        DE - "Alle Räume"
    }

    open fun notificationsSettingsAccountDefaultLevelDM() = translate {
        EN - "Direct Messages"
        DE - "Direktnachrichten"
    }

    open fun notificationsSettingsAccountDefaultLevelMention() = translate {
        EN - "Mentions"
        DE - "Erwähnungen"
    }

    open fun notificationsSettingsAccountDefaultLevelNone() = translate {
        EN - "Nothing"
        DE - "Keine"
    }

    open fun notificationsSettingsAccountSound() = translate {
        EN - "Notification Sounds"
        DE - "Benachrichtigungstöne"
    }

    open fun notificationsSettingsAccountSoundRoom() = translate {
        EN - "Rooms"
        DE - "Räume"
    }

    open fun notificationsSettingsAccountSoundDM() = translate {
        EN - "Direct Messages"
        DE - "Direktnachrichten"
    }

    open fun notificationsSettingsAccountSoundMention() = translate {
        EN - "Mentions"
        DE - "Erwähnungen"
    }

    open fun notificationsSettingsAccountSoundCall() = translate {
        EN - "Calls"
        DE - "Anrufe"
    }

    open fun notificationsSettingsAccountOthers() = translate {
        EN - "Other Notifications"
        DE - "Weitere Benachrichtigungen"
    }

    open fun notificationsSettingsAccountActivityInvite() = translate {
        EN - "Invitations"
        DE - "Einladungen"
    }

    open fun notificationsSettingsAccountActivityStatus() = translate {
        EN - "Room Changes"
        DE - "Raumänderungen"
    }

    open fun notificationsSettingsAccountActivityNotice() = translate {
        EN - "Bot Messages"
        DE - "Bot Nachrichten"
    }

    open fun notificationsSettingsAccountMentionUser(userId: UserId) = translate {
        EN - "$userId Mentions"
        DE - "$userId Erwähnungen"
    }

    open fun notificationsSettingsAccountMentionRoom() = translate {
        EN - "@room Mentions"
        DE - "@room Erwähnungen"
    }

    open fun notificationsSettingsAccountMentionKeyword() = translate {
        EN - "Keywords"
        DE - "Schlüsselwörter"
    }

    open fun privacyTitle() = translate {
        EN - "Privacy and Security"
        DE - "Datenschutz und Sicherheit"
    }

    open fun privacyPresenceIsPublic() = translate {
        EN - "Online Status"
        DE - "Online Status"
    }

    open fun privacyPresenceIsPublicExplanation(appName: String) = translate {
        EN - "Others can see whether you are online with $appName"
        DE - "Andere Nutzer können sehen ob Sie mit $appName online sind"
    }

    open fun privacyReadMarkerIsPublic() = translate {
        EN - "Read Marker"
        DE - "Lesebestätigungen"
    }

    open fun privacyReadMarkerIsPublicExplanation() = translate {
        EN - "Others can see which messages you have read"
        DE - "Andere Nutzer können sehen, welche Nachrichten Sie bereits gelesen haben"
    }

    open fun privacyTypingIsPublic() = translate {
        EN - "Typing Indicators"
        DE - "Tipp-Indikatoren"
    }

    open fun privacyTypingIsPublicExplanation() = translate {
        EN - "Others can see when you type a message"
        DE - "Andere Nutzer können sehen, wenn Sie eine neue Nachricht schreiben"
    }

    open fun profileTitle() = translate {
        EN - "Profile"
        DE - "Profil"
    }

    open fun profileAvatarChange() = translate {
        EN - "change avatar"
        DE - "Profilbild ändern"
    }

    open fun profileYourName() = translate {
        EN - "Your name"
        DE - "Ihr Name"
    }

    open fun profileYourNameInfo() = translate {
        EN - "This is your display name. It is public and is seen by other users in your chats and groups."
        DE - "Die ist Ihr Anzeigename. Dieser ist öffentlich und wird von allen Teilnehmern in Ihren Chats und Gruppen gesehen."
    }

    open fun profileUserName() = translate {
        EN - "Your username"
        DE - "Ihre Benutzerkennung"
    }

    open fun profileUserNameInfo() = translate {
        EN - "Your username is required to login. It is public and is used to identify your account in case of of a display name duplication in a group."
        DE - "Ihre Benutzerkennung wird zum Einloggen benötigt. Sie ist öffentlich und wird verwendet um Sie - im Falle einer Namensdopplung - in einer Gruppe eindeutig zu identifizieren."
    }

    open fun bootstrapRecoveryKeyExplanationTitle() = translate {
        EN - "Message vault and recovery key"
        DE - "Nachrichtentresor und Generalschlüssel"
    }

    open fun bootstrapRecoveryKeyExplanation1() = translate {
        EN - "During the setup of your account, a vault for your messages is created, in which your messages are safely stored. Only the device with which your are performing the setup now is able to access the vault."
        DE - "Im Rahmen der Kontoeinrichtung wird nun ein Tresor für Ihre Nachrichten angelegt. In diesem werden Ihre Nachrichten sicher verwahrt. Nur das Gerät, mit dem Sie die Einrichtung gerade durchführen, hat Zugriff auf diesen Tresor."
    }

    open fun bootstrapRecoveryKeyExplanation2() = translate {
        EN - "The message vault additionally can be opened with an individual recovery key. In case your device is lost, you can open the message vault on another device with the recovery key which is issued to you in the next step."
        DE - "Dieser Tresor kann zudem mit einem individuellen Generalschlüssel (im Bild: \"RecoveryKey\") geöffnet werden. Um im Falle eines Geräteverlusts diesen Tresor auf einem neuen Gerät öffnen zu können, wird Ihnen der Generalschlüssel im nächsten Schritt ausgehändigt."
    }

    open fun bootstrapVault() = translate {
        EN - "vault"
        DE - "Tresor"
    }

    open fun bootstrapRecoveryKeyVaultCreation() = translate {
        EN - "Message vault is being created"
        DE - "Nachrichtentresor wird angelegt"
    }

    open fun bootstrapRecoveryKeyCreateVault() = translate {
        EN - "Create vault"
        DE - "Tresor anlegen"
    }

    open fun bootstrapRecoveryKeyTitle() = translate {
        EN - "Recovery key"
        DE - "Schlüsselübergabe"
    }

    open fun bootstrapRecoveryKey() = translate {
        EN - "recovery key"
        DE - "Generalschlüssel"
    }

    open fun bootstrapRecoveryKeyHandling() = translate {
        EN - "Write down your recovery key and put it in a safe space. A password manager is recommended (e.g., KeePassXC, 1Password, etc.)."
        DE - "Notieren Sie sich den Generalschlüssel und sichern Sie ihn an einem geeigneten Ort. Ein Passwort-Manager wird empfohlen (bspw. KeePassXC, 1Password, etc.)."
    }

    open fun bootstrapRecoveryKeyWarning() = translate {
        EN - "In case your recovery key is stolen, the attacker can get access to your message vault!"
        DE - "Falls Ihr Generalschlüssel in falsche Hände gerät, kann auf Ihren Tresor und damit alle Ihre Gespräche zugegriffen werden!"
    }

    open fun bootstrapRecoveryKeyAttention() = translate {
        EN - "Attention!"
        DE - "Achtung!"
    }

    open fun bootstrapRecoveryKeyOnlyOnce() = translate {
        EN - "Your recovery key is only displayed once now and is not saved by us."
        DE - "Ihr Generalschlüssel wird nur dieses eine Mal angezeigt und wird von uns nicht gespeichert."
    }

    open fun bootstrapRecoveryKeyCopyToClipboard() = translate {
        EN - "Copy recovery key to clipboard"
        DE - "Generalschlüssel in die Zwischenablage kopieren"
    }

    open fun bootstrapRecoveryKeySafe() = translate {
        EN - "I have copied the recovery key to a safe location."
        DE - "Ich habe den Generalschlüssel notiert und sicher verwahrt."
    }

    open fun bootstrapFinished() = translate {
        EN - "Your recovery key is now set up."
        DE - "Ihr Generalschlüssel ist nun eingerichtet."
    }

    open fun deviceVerificationTitle(userId: UserId) = translate {
        EN - "Device verification (account: ${userId.full})"
        DE - "Freischaltung für dieses Gerät (Konto: ${userId.full})"
    }

    open fun deviceVerificationInitiatedBy(username: String) = translate {
        EN - "Verification initiated by $username"
        DE - "Verifizierung ausgelöst durch $username"
    }

    open fun deviceVerificationToAccount(deviceName: String) = translate {
        EN - "Verify that the device '$deviceName' belongs to your account."
        DE - "Ordnen Sie das Gerät '$deviceName' Ihrem Konto zu."
    }

    open fun selfVerificationWaitingForMethods() = translate {
        EN - "Waiting for initial sync to complete to gather available verification methods."
        DE - "Warte auf Abschließen des initialen Ladevorgangs, um verfügbare Verifikationsmethoden zu erhalten."
    }

    open fun redoSelfVerificationTitle(userId: UserId) = translate {
        EN - "Cancel device verification for account '$userId'?"
        DE - "Gerätefreischaltung für Konto '$userId' abbrechen?"
    }

    open fun redoSelfVerificationWarning1() = translate {
        EN - "If you do not verify this device, the following restrictions apply:"
        DE - "Wenn Sie Ihr Gerät nicht freischalten, dann gelten folgende Einschränkungen:"
    }

    open fun redoSelfVerificationWarning2() = translate {
        EN - "• you cannot read older messages"
        DE - "• Sie können alte Nachrichten nicht lesen"
    }

    open fun redoSelfVerificationWarning3() = translate {
        EN - "• your contacts might get a warning that your account cannot be trusted anymore"
        DE - "• Ihren Gesprächspartnern wird evtl. angezeigt, dass Ihrem Konto nicht mehr vertraut werden kann"
    }

    open fun redoSelfVerificationDoIt() = translate {
        EN - "Restart the verification process again or continue with the aforementioned restrictions."
        DE - "Starten Sie die Freischaltung des Gerätes erneut oder fahren Sie mit den zuvor genannten Einschränkungen fort."
    }

    open fun redoSelfVerificationDoItLater() = translate {
        EN - "Without verification, it is recommended to redo the verification process at a later time. You can find the option in 'settings' -> 'devices'."
        DE - "Ohne Freischaltung wird empfohlen diese zu einem späteren Zeitpunkt nachzuholen. Sie finden die Option in 'Einstellungen' -> 'Geräte'."
    }

    open fun redoSelfVerificationContinueWithoutVerification() = translate {
        EN - "Continue without verification"
        DE - "Ohne Freischaltung fortfahren"
    }

    open fun redoSelfVerificationRedo() = translate {
        EN - "Verify device"
        DE - "Gerät freischalten"
    }

    open fun selfVerificationTitle(userId: UserId) = translate {
        EN - "Verify this device (account: $userId)"
        DE - "Dieses Gerät freischalten (Konto: $userId)"
    }

    open fun selfVerificationResetRecoveryWarningTitle(userId: UserId) = translate {
        EN - "Reset recovery keys (account: $userId)"
        DE - "Generalschlüssel zurücksetzen (Konto: $userId)"
    }

    open fun selfVerificationHelpOtherDevice() = translate {
        EN - "Your account has already been setup with another device."
        DE - "Ihr Konto wurde bereits über ein anderes Gerät eingerichtet."
    }

    open fun selfVerificationHelpVerifyThis() = translate {
        EN - "You have to verify this new device (connect it to your account) in order to use it."
        DE - "Sie müssen das neue Gerät freischalten (mit Ihrem Konto verknüpfen) um es nutzen zu können."
    }

    open fun selfVerificationHelpReasonTitle() = translate {
        EN - "Why do I have to verify this device?"
        DE - "Warum muss ich dieses Gerät freischalten?"
    }

    open fun selfVerificationHelpReason1() = translate {
        EN - "This step is necessary to increase security and trust of all users among each other. You and your contacts want to make sure that messages are really delivered to the correct recipient."
        DE - "Dieser Schritt ist notwendig um die Sicherheit und das Vertrauen aller Teilnehmer untereinander zu erhöhen. Sie und Ihre Gesprächspartner möchten sichergehen, dass Ihre Nachrichten auch wirklich beim korrekten Empfänger ankommen."
    }

    open fun selfVerificationHelpReason2() = translate {
        EN - "Despite all security measures an attacker can gain access to your account. The attacker can impersonate you and trick your contacts to disclose sensitive information."
        DE - "Trotz aller Sicherheitsmaßnahmen kann es einem Angreifer gelingen, Zugriff auf Ihr Konto zu erlangen. Damit kann er sich als Sie ausgeben und andere Gesprächsteilnehmer täuschen ihm sensible Informationen anzuvertrauen."
    }

    open fun selfVerificationHelpReason3() = translate {
        EN - "With the device verification, an attacker would not only need access to your account, but also to your recovery key or a already verified device. This second factor increases the security and trustworthiness."
        DE - "Durch die Gerätefreischaltung benötigt ein Angreifer nun nicht nur Zugriff auf Ihr Konto, sondern muss zudem im Besitz des Generalschlüssels oder eines freigegebenen Gerätes sein. Dieser zweite Faktor erhöht die Sicherheit und Vertrauenswürdigkeit deutlich."
    }

    open fun selfVerificationMethodsTitle() = translate {
        EN - "Please choose one method to verify this device:"
        DE - "Sie können aus folgenden Möglichkeiten zur Freischaltung wählen:"
    }

    open fun selfVerificationMethodsOtherDevice() = translate {
        EN - "with another already verified device you have access to"
        DE - "mit Hilfe eines bereits freigeschalteten Gerätes, zu dem Sie jetzt Zugang haben"
    }

    open fun selfVerificationMethodsOtherDeviceInfo() = translate {
        EN - "You are asked to compare some emojis. If they match on both devices, the verification is complete and you can fully use this device."
        DE - "Hierbei werden Sie aufgefordert, eine Reihe von Emojis zu vergleichen. Beim Übereinstimmen auf beiden Geräten wird die Freischaltung aktiviert und Sie können dieses Gerät vollumfänglich nutzen."
    }

    open fun selfVerificationMethodsRecoveryKey() = translate {
        EN - "with the recovery key"
        DE - "mit Hilfe des Generalschlüssels"
    }

    open fun selfVerificationMethodsRecoveryKeyInfo() = translate {
        EN - "You have to provide the recovery key that you have received during the setup of this account."
        DE - "Geben Sie den Generalschlüssel ein, den Sie beim Erstellen dieses Kontos erhalten haben."
    }

    open fun selfVerificationMethodsRecoveryPassphrase() = translate {
        EN - "with the recovery passphrase"
        DE - "mit Hilfe des Generalpassworts"
    }

    open fun selfVerificationMethodsRecoveryPassphraseInfo() = translate {
        EN - "You have to provide the recovery passphrase you have chosen during the setup of this account."
        DE - "Geben Sie das Generalpasswort ein, das Sie beim Erstellen dieses Kontos gewählt haben."
    }

    open fun selfVerificationMethodsRecoveryPassphraseTitle() = translate {
        EN - "Please provide the recovery passphrase."
        DE - "Bitte geben Sie das Generalpasswort ein."
    }

    open fun selfVerificationMethodsRecoveryPassphraseWarning() = translate {
        EN - " this recovery passphrase is not the password of your account!"
        DE - " dieses Passwort is nicht Ihr Konto-Passwort!."
    }

    open fun selfVerificationMethodsRecoveryPassphraseWrong() = translate {
        EN - "recovery passphrase wrong"
        DE - "Generalpasswort ist nicht korrekt"
    }

    open fun selfVerificationMethodsRecoveryKeyTitle() = translate {
        EN - "Please provide the recovery key for the account."
        DE - "Bitte geben Sie den Generalschlüssel für das Konto ein."
    }

    open fun selfVerificationMethodsRecoveryKeyWrong() = translate {
        EN - "recovery key wrong"
        DE - "Generalschlüssel ist nicht korrekt"
    }

    open fun selfVerificationResetRecoveryKey() = translate {
        EN - "Reset recovery key"
        DE - "Generalschlüssel zurücksetzen"
    }

    open fun selfVerificationResetRecoveryKeyDescription() = translate {
        EN - "This will lead to you losing access to all past messages."
        DE - "Durch diesen Schritt verlieren Sie Zugriff auf alle ihre vergangenen Nachrichten."
    }

    open fun verificationWait() = translate {
        EN - "Wait for input on other device."
        DE - "Warte auf Eingabe an anderem Gerät."
    }

    open fun verificationStartEmoji() = translate {
        EN - "Start of the emoji comparison"
        DE - "Start des Emoji-Vergleichs"
    }

    open fun verificationEmojiComparison() = translate {
        EN - "Please compare the emojis on both devices."
        DE - "Vergleichen Sie bitte die Emojis auf beiden Geräten."
    }

    open fun verificationNumberComparison() = translate {
        EN - "Please compare the numbers on both devices."
        DE - "Vergleichen Sie bitte die Zahlen auf beiden Geräten."
    }

    open fun verificationMatch() = translate {
        EN - "They match"
        DE - "Sie passen zueinander"
    }

    open fun verificationNotMatch() = translate {
        EN - "They do not match"
        DE - "Sie passen nicht zueinander"
    }

    open fun verificationSuccess() = translate {
        EN - "The verification has been successful."
        DE - "Die Freischaltung war erfolgreich."
    }

    open fun verificationSuccessThisDevice() = translate {
        EN - "This device was verified successfully."
        DE - "Dieses Gerät konnte erfolgreich freigeschaltet werden."
    }

    open fun verificationRejected(type: String) = translate {
        EN - "${type.capitalize(Locale.current)} was not successful. The emojis/numbers did not match. ${verificationTryAgain()}"
        DE - "${type.capitalize(Locale.current)} war nicht erfolgreich. Die übermittelten Emojis/Zahlen stimmen nicht überein. ${verificationTryAgain()}"
    }

    open fun verificationTimeout(type: String) = translate {
        EN - "${type.capitalize(Locale.current)} was not successful. The timeout has been reached. ${verificationTryAgain()}"
        DE - "${type.capitalize(Locale.current)} war nicht erfolgreich. Das Zeitfenster wurde überschritten. ${verificationTryAgain()}"
    }

    open fun verificationCancelled(type: String) = translate {
        EN - "${type.capitalize(Locale.current)} has been cancelled. ${verificationTryAgain()}"
        DE - "${type.capitalize(Locale.current)} wurde abgebrochen. ${verificationTryAgain()}"
    }

    open fun verificationTryAgain() = translate {
        EN - "Please try again or choose a different verification method."
        DE - "Bitte versuchen Sie es erneut oder wählen sie eine andere Verifikationsmethode."
    }

    open fun deviceVerification() = translate {
        EN - "device verification"
        DE - "Gerätefreischaltung"
    }

    open fun userVerification() = translate {
        EN - "user verification"
        DE - "Vertrauensprüfung"
    }

    open fun resetWarningIsPermanent() = translate {
        EN - "Resetting the recovery keys is permanent and cannot be undone."
        DE - "Das Zurücksetzen der Generalschlüssel ist dauerhaft und kann nicht rückgängig gemacht werden."
    }

    open fun resetWarningLostAccessAndReVerify() = translate {
        EN - "You will be unable to access old encrypted messages and need to re-verify with your contacts."
        DE - "Sie können nicht auf alte verschlüsselte Nachrichten zugreifen und müssen sich bei Ihren Kontakten erneut verifizieren."
    }

    open fun resetWarningAcknowledge() = translate {
        EN - "I am aware of the consequences this action will have."
        DE - "Ich bin mir der Konsequenzen dieser Handlung bewusst."
    }

    open fun resetProceed() = translate {
        EN - "Proceed with reset"
        DE - "Mit dem Zurücksetzen fortfahren"
    }

    open fun resetWarningLastResort() = translate {
        EN - "The reset should be your last resort, please double check and make sure that there is no other option."
        DE - "Das Zurücksetzen sollte Ihre letzte Option sein. Bitte überprüfen Sie es sorgfältig und stellen Sie sicher, dass es keine andere Möglichkeit gibt."
    }

    open fun syncOverlayTitle() = translate {
        EN - "Sync..."
        DE - "Lade Daten..."
    }

    open fun syncOverlayAccount(userId: UserId) = translate {
        EN - "Account: $userId"
        DE - "Konto: $userId"
    }

    open fun syncOverlayInitialSync() = translate {
        EN - "initial Sync"
        DE - "initiales Laden"
    }

    open fun syncOverlayInitialSyncInfo(appName: String) = translate {
        EN - "This might take a while. It is only necessary the first time $appName is started on this device."
        DE - "Dieser Vorgang kann einige Zeit dauern und ist nur beim ersten Start von $appName auf diesem Gerät notwendig."
    }

    open fun fileOverlayPreviewNotSupported() = translate {
        EN - "File preview not supported. Please download the file instead."
        DE - "Datei-Vorschau nicht verfügbar. Bitte laden Sie die Datei stattdessen herunter."
    }

    open fun fileOverlayPdfPageDescriptor(pageId: Int) = translate {
        EN - "PDF page number: $pageId"
        DE - "PDF-Seiten-Nummer $pageId"
    }

    open fun invitationAccept() = translate {
        EN - "accept the invitation"
        DE - "Einladung annehmen"
    }

    open fun invitationRejectHeader() = translate {
        EN - "Reject invitation"
        DE - "Einladung ablehnen"
    }

    open fun invitationReject() = translate {
        EN - "Reject"
        DE - "Ablehnen"
    }

    open fun invitationBlock() = translate {
        EN - "Reject and block user"
        DE - "Ablehnen und Nutzer blocken"
    }

    open fun unknock() = translate {
        EN - "Take back membership request"
        DE - "Beitrittsanfrage zurücknehmen"
    }

    open fun blockedContactsHeader() = translate {
        EN - "Blocked contacts"
        DE - "Blockierte Kontakte"
    }

    open fun unblockContactDescription() = translate {
        EN - "Unblock"
        DE - "Entblocken"
    }

    open fun blockedContactDescription() = translate {
        EN - "Blocked Contact"
        DE - "Blockierter Kontakt"
    }

    open fun blockedContactsButtonCaption(count: Int) = translate {
        EN - "$count contacts blocked"
        DE - "$count Kontakte blockiert"
    }

    open fun blockedContactsAccountLabel(account: String) = translate {
        EN - "For account $account:"
        DE - "Für's Konto $account:"
    }

    open fun blockedContactsEmptyListLabel() = translate {
        EN - "There are no contacts blocked for this account."
        DE - "Es gibt keine blockierten Kontakte für dieses Konto"
    }

    open fun roomType() = translate {
        EN - "Setting: "
        DE - "Einstellung: "
    }

    open fun roomTypePrivate() = translate {
        EN - "Private"
        DE - "Privat"
    }

    open fun roomTypePublic() = translate {
        EN - "Public"
        DE - "Öffentlich"
    }

    open fun roomTypeForbidden() = translate {
        EN - "Forbidden"
        DE - "Nicht erlaubt"
    }

    open fun roomTypeEncrypted() = translate {
        EN - "Encrypted"
        DE - "Verschlüsselt"
    }

    open fun roomTypeUnencrypted() = translate {
        EN - "Unencrypted"
        DE - "Unverschlüsselt"
    }

    open fun roomTypeEncryptedInfo() = translate {
        EN - "Encrypted rooms are end-to-end encrypted. Only the participants of the room can read the messages."
        DE - "Verschlüsselte Räume sind Ende-zu-Ende verschlüsselt. Nur die Teilnehmer des Raumes können die Nachrichten lesen."
    }

    open fun roomTypeUnencryptedInfo() = translate {
        EN - "Messages in unencrypted rooms can potentially be read by anyone. Use only for non-sensitive information. Otherwise, encrypted rooms are recommended."
        DE - "Nachrichten in unverschlüsselten Räumen können potentiell von jedem gelesen werden. Verwenden Sie diese nur für nicht-sensible Informationen. Ansonsten werden verschlüsselte Räume empfohlen."
    }

    open fun roomTypePublicInfo() = translate {
        EN - "Public rooms are visible to all users. You can join them without an invitation."
        DE - "Öffentliche Räume sind für alle Nutzer sichtbar. Sie können ihnen ohne Einladung beitreten."
    }

    open fun roomTypePrivateInfo() = translate {
        EN - "Private rooms are only visible to invited users. You can only join them with an invitation."
        DE - "Private Räume sind nur für eingeladene Nutzer sichtbar. Sie können ihnen nur mit einer Einladung beitreten."
    }

    open fun roomVisibility() = translate {
        EN - "Visibility: "
        DE - "Sichtbarkeit: "
    }

    open fun roomEncryption() = translate {
        EN - "Encryption: "
        DE - "Verschlüsselung: "
    }

    open fun optionalGroupNamePlaceholder() = translate {
        EN - "Optional group name"
        DE - "Optionaler Gruppenname"
    }

    open fun optionalGroupTopicPlaceholder() = translate {
        EN - "Optional group topic"
        DE - "Optionales Gruppenthema"
    }

    open fun downloadMessage() = translate {
        EN - "Download"
        DE - "Herunterladen"
    }

    open fun editMessage() = translate {
        EN - "Edit"
        DE - "Bearbeiten"
    }

    open fun redactMessage() = translate {
        EN - "Delete"
        DE - "Löschen"
    }

    open fun replyMessage() = translate {
        EN - "Answer"
        DE - "Antworten"
    }

    open fun reactMessage() = translate {
        EN - "React"
        DE - "Reagieren"
    }

    open fun reactorListMessage() = translate {
        EN - "Reactions"
        DE - "Reaktionen"
    }

    open fun infoMessage() = translate {
        EN - "Info"
        DE - "Info"
    }

    open fun reportMessage() = translate {
        EN - "Report"
        DE - "Melden"
    }

    open fun reportMessageHeader() = translate {
        EN - "Report Message"
        DE - "Nachricht melden"
    }

    open fun reportMessageLabel() = translate {
        EN - "Please enter report reason"
        DE - "Bitte geben Sie den Grund für die Meldung ein"
    }

    open fun retrySendMessage() = translate {
        EN - "Retry send"
        DE - "Senden erneut versuchen"
    }

    open fun abortSendMessage() = translate {
        EN - "Abort send"
        DE - "Senden abbrechen"
    }

    open fun debugMessage() = translate {
        EN - "Debug"
        DE - "Debug"
    }

    open fun eventMentionPile(roomName: String) = translate {
        EN - "Message in #$roomName"
        DE - "Nachricht in #$roomName"
    }

    open fun chatHistoryVisibility() = translate {
        EN - "Chat history visibility"
        DE - "Sichtbarkeit des Chat Verlaufs"
    }

    open fun historyVisibilityWorldReadable() = translate {
        EN - "Global"
        DE - "Global"
    }

    open fun historyVisibilityWorldReadableExplanation() = translate {
        EN - "All Messages are visible for everyone, even participants that didn't join the room. Use with caution!"
        DE - "Alle Nachrichten sind für jeden Nutzbar sichtbar, auch wenn Sie nicht Teilnehmer des Raumes sind. Verwenden Sie diese Einstellung mit Vorsicht!"
    }

    open fun historyVisibilityWorldReadableEncryptedExplanation() = translate {
        EN - "An encrypted group can't be assigned the global chat history visibility "
        DE - "Eine verschlüsselte Gruppe kann keinen global sichtbaren Chat-Verlauf besitzen"
    }

    open fun historyVisibilityShared() = translate {
        EN - "Complete history"
        DE - "Gesamter Verlauf"
    }

    open fun historyVisibilitySharedExplanation() = translate {
        EN - "All messages are visible to new participants."
        DE - "Alle Nachrichten sind für neue Teilnehmer sichtbar."
    }

    open fun historyVisibilityInvited() = translate {
        EN - "Since Invitation"
        DE - "Ab Einladung"
    }

    open fun historyVisibilityInvitedExplanation() = translate {
        EN - "All messages since the invitation are visible to new participants."
        DE - "Alle Nachrichten seit der Einladung sind für neue Teilnehmer sichtbar."
    }

    open fun historyVisibilityJoined() = translate {
        EN - "After joining"
        DE - "Ab Beitritt"
    }

    open fun historyVisibilityJoinedExplanation() = translate {
        EN - "Messages after joining are visible to new participants."
        DE - "Nachrichten sind ab dem Moment des Beitritts sichtbar."
    }

    open fun createProfileHeader() = translate {
        EN - "Create a new profile"
        DE - "Anlegen eines neuen Profils"
    }

    open fun createProfileSelectName() = translate {
        EN - "Please select a name for your profile"
        DE - "Bitte wählen Sie einen Namen für Ihr Profil"
    }

    open fun createProfileAction() = translate {
        EN - "Create profile"
        DE - "Profil anlegen"
    }

    open fun selectProfileHeader() = translate {
        EN - "Please select a profile"
        DE - "Wählen Sie ein Profil aus"
    }

    open fun selectProfileCreateInstead() = translate {
        EN - "Create a new profile"
        DE - "Neues Profil anlegen"
    }

    open fun fileDialogTitleLoad() = translate {
        EN - "Pick attachment"
        DE - "Anhang wählen"
    }

    open fun fileDialogLoadFileButton() = translate {
        EN - "Upload file"
        DE - "Datei hochladen"
    }

    open fun fileDialogLoadImageButton() = translate {
        EN - "Upload image"
        DE - "Bild hochladen"
    }

    open fun fileDialogLoadImageOrVideoButton() = translate {
        EN - "Upload image or video"
        DE - "Bild oder Video hochladen"
    }

    open fun fileDialogTakeImageButton() = translate {
        EN - "Capture image"
        DE - "Bild aufnehmen"
    }

    open fun fileDialogTakeVideoButton() = translate {
        EN - "Capture video"
        DE - "Video aufnehmen"
    }

    open fun fileDialogSaveDescription() = translate {
        EN - "Download File"
        DE - "Datei herunterladen"
    }

    open fun fileDialogDownloadErrorSave() = translate {
        EN - "Download failed"
        DE - "Download fehlgeschlagen"
    }

    open fun cameraDialogAlertNoPermission() = translate {
        EN - "Please check the permissions of the camera"
        DE - "Bitte die Berechtigungen der Kamera prüfen"
    }

    open fun commonAccept() = translate {
        EN - "Accept"
        DE - "Akzeptieren"
    }

    open fun locationClickText(pos: Pair<String, String>) = translate {
        EN - "Click to show ${pos.first},${pos.second}"
        DE - "Klicken um ${pos.first},${pos.second} anzuzeigen"
    }

    open fun unknownFileInfo() = translate {
        EN - "Unknown file"
        DE - "Unbekannte Datei"
    }

    open fun appearanceTitle() = translate {
        EN - "Appearance"
        DE - "Erscheinungsbild"
    }

    open fun appearanceColorsTitle() = translate {
        EN - "Colors"
        DE - "Farben"
    }

    open fun appearanceAccessibilityTitle() = translate {
        EN - "Accessibility"
        DE - "Barrierefreiheit"
    }

    open fun appearanceSizesApplySystemHeading() = translate {
        EN - "use system settings"
        DE - "Systemeinstellungen verwenden"
    }

    open fun appearanceSizesApplySystemExplanation() = translate {
        EN - "Apply the system's size settings to the app"
        DE - "Größeneinstellungen des Systems auf die App anwenden"
    }

    open fun appearanceFontSizeHeading() = translate {
        EN - "Font size"
        DE - "Schriftgröße"
    }

    open fun appearanceDisplaySizeHeading() = translate {
        EN - "Display size"
        DE - "Anzeigegröße"
    }

    open fun appearanceSizesApply() = translate {
        EN - "Apply sizes"
        DE - "Größen anwenden"
    }

    open fun appearanceThemeHeading(name: String) = translate {
        EN - "Theme: $name"
        DE - "Thema: $name"
    }

    open fun appearanceThemeDefaultHeading() = translate {
        EN - "Default"
        DE - "Standard"
    }

    open fun appearanceThemeDefaultExplanation() = translate {
        EN - "Let the app decide which theme to use based on the system preference"
        DE - "Lassen Sie die App entscheiden welches Thema verwendet wird auf Basis der Systempreferenz"
    }

    open fun appearanceThemeLightHeading() = translate {
        EN - "Light"
        DE - "Hell"
    }

    open fun chatJoinRule() = translate {
        EN - "Chat join rule"
        DE - "Beitrittseinstellung der Gruppe"
    }

    open fun joinRuleInvitedExplanation() = translate {
        EN - "Only users with an invitation from a group member are able to join"
        DE - "Nur von einem Gruppenmitglied eingeladene Nutzer können beitreten"
    }

    open fun joinRulePublicExplanation() = translate {
        EN - "Everybody is able to join"
        DE - "Jeder kann beitreten"
    }

    open fun joinRuleKnockExplanation() = translate {
        EN - "Only users who request an invite and are accepted or get one from a group member are able to join"
        DE - "Nur Nutzer, die eine Beitrittsanfrage stellen und akzeptiert werden oder von einem Gruppenmitglied eingeladen werden, können beitreten"
    }

    open fun joinRuleRestrictedExplanation() = translate {
        EN - "Only users who satisfy a specified condition are able to join"
        DE - "Nur Nutzer, die eine Bedingung erfüllen, können beitreten"
    }

    open fun joinRuleKnockRestrictedExplanation() = translate {
        EN - "Only users who satisfy a specified condition, whose requested invitation gets accepted or are invited by a group member are able to join"
        DE - "Nur Nutzer, die eine Bedingung erfüllen, deren Beitrittsanfrage akzeptiert wird oder die von einem Gruppenmitglied eingeladen werden, können beitreten"
    }

    open fun joinRulePrivateExplanation() = translate {
        EN - "Nobody is able to join"
        DE - "Niemand kann beitreten"
    }

    open fun joinRulePublic() = translate {
        EN - "Public"
        DE - "Öffentlich"
    }

    open fun joinRuleInvited() = translate {
        EN - "Invite"
        DE - "Auf Einladung"
    }

    open fun joinRuleKnock() = translate {
        EN - "Knock"
        DE - "Auf Beitrittsanfrage"
    }

    open fun joinRuleRestricted() = translate {
        EN - "Restricted"
        DE - "Beschränkt"
    }

    open fun joinRuleKnockRestricted() = translate {
        EN - "Knock or restricted"
        DE - "Beschränkt oder auf Beitrittsanfrage"
    }

    open fun joinRulePrivate() = translate {
        EN - "Private"
        DE - "Privat"
    }

    open fun appearanceThemeLightExplanation() = translate {
        EN - "Force the app to use the light theme"
        DE - "Zwingen Sie die App das helle Thema zu verwenden"
    }

    open fun appearanceThemeDarkHeading() = translate {
        EN - "Dark"
        DE - "Dunkel"
    }

    open fun appearanceThemeDarkExplanation() = translate {
        EN - "Force the app the use the dark theme"
        DE - "Zwingen Sie die App das dunkle Thema zu verwenden"
    }

    open fun appearanceHighContrastHeading() = translate {
        EN - "High Contrast"
        DE - "Hoher Kontrast"
    }

    open fun appearanceHighContrastExplanation() = translate {
        EN - "Increases the overall contrast between background and foreground elements to make them easier to discern"
        DE - "Erhöht den Gesamtkontrast zwischen Vorder- und Hintergrund-Elemente um diese besser erkennbar zu machen"
    }

    open fun appearanceA11yModeHeading() = translate {
        EN - "A11y Mode"
        DE - "Barrierefreiheit"
    }

    open fun appearanceA11yModeExplanation() = translate {
        EN - "Enables better visibility for focused elements."
        DE - "Fokussierte Elemente werden stärker hervorgehoben."
    }

    open fun appearanceAccentColorHeading() = translate {
        EN - "Accent Color"
        DE - "Akzentfarbe"
    }

    open fun appearanceAccentColorDefault() = translate {
        EN - "Default"
        DE - "Standard"
    }

    open fun forgetRoomWarningHeader() = translate {
        EN - "Forget room?"
        DE - "Raum vergessen?"
    }

    open fun formattedForgetRoomWarningBody(roomName: String?, isDirect: Boolean) = translate {
        EN - "Do you really want to forget ${if (isDirect) "chat" else "group"} ${if (roomName != null) "'$roomName'" else ""}? If you continue, you can't access the room and it's content anymore."
        DE - "Wollen Sie ${if (isDirect) "den Chat" else "die Gruppe"} ${if (roomName != null) "'$roomName'" else ""} wirklich vergessen? Wenn sie fortfahren, können Sie auf den Raum und dessen Inhalte nicht mehr zugreifen."
    }

    open fun formattedInvitationBody(inviterName: String, roomName: String?) = translate {
        EN - "Invitation from $inviterName${if (roomName != null) " to '$roomName'" else ""}"
        DE - "Einladung von $inviterName${if (roomName != null) " zu '$roomName'" else ""}"
    }

    open fun security() = translate {
        EN - "Security"
        DE - "Sicherheit"
    }

    open fun roomEndToEndEncryption() = translate {
        EN - "End-to-End encryption"
        DE - "Ende-zu-Ende Verschlüsselung"
    }

    open fun roomEndToEndEncryptionDescription() = translate {
        EN - "Enable the end-to-end encryption for this room"
        DE - "Ende-zu-Ende Verschlüsselung für diesen Raum aktivieren"
    }

    open fun roomEnableEncryptionWarningConfirmation() = translate {
        EN - "Yes, continue"
        DE - "Ja, fortfahren"
    }

    open fun roomSettingsEnableEncryptionWarningTitleGroup() = translate {
        DE - "Verschlüsselung für diese Gruppe aktivieren?"
        EN - "Enable encryption for this group?"
    }

    open fun roomSettingsEnableEncryptionWarningMessageGroup() = translate {
        DE - "Die Aktivierung der Verschlüsselung der Gruppe kann nicht rückgängig gemacht werden."
        EN - "The activation of the encryption of the group cannot be revoked."
    }

    open fun roomSettingsEnableEncryptionWarningTitleChat() = translate {
        DE - "Verschlüsselung für diesen Chat aktivieren?"
        EN - "Enable encryption for this chat?"
    }

    open fun roomSettingsEnableEncryptionWarningMessageChat() = translate {
        DE - "Die Aktivierung der Verschlüsselung des Chats kann nicht rückgängig gemacht werden."
        EN - "The activation of the encryption of the chat cannot be revoked."
    }

    open fun accountSetupWizardExplanationMessage() = translate {
        DE - "Um Ihren Messenger nach Ihren Vorlieben zu konfigurieren, können Sie im Folgenden einige der wichtigsten Einstellungen konfigurieren. Sämtliche Einstellungen können Sie später verändern."
        EN - "To configure your messenger to your liking, you can configure some of the most important settings now. You can change all settings later."
    }

    open fun accountSetupWizardFinishSetup() = translate {
        DE - "Ist alles nach Ihren Wünschen eingestellt?"
        EN - "Is everything configured to your liking?"
    }

    open fun accountSetupWizardFinishSetupTitle() = translate {
        DE - "Einrichtung abschließen"
        EN - "Finish setup"
    }

    open fun accountSetupWizardReset() = translate {
        DE - "Setup zurücksetzen"
        EN - "Reset setup"
    }

    open fun shareDataTitle(data: SharedData) = when (data) {
        is SharedData.PlainText -> translate {
            EN - "Sharing Text"
            DE - "Teile Text"
        }

        is SharedData.SingleFile -> translate {
            EN - "Sharing 1 file"
            DE - "Teile 1 Datei"
        }

        is SharedData.MultipleFiles -> translate {
            EN - "Sharing ${data.files.size} files"
            DE - "Teile ${data.files.size} Dateien"
        }

        is SharedData.Url -> translate {
            EN - "Sharing URL"
            DE - "Teile URL"
        }
    }

    open fun shareFilesTitle(count: Int) = translate {
        EN - "Sharing $count files"
        DE - "Teile $count Dateien"
    }

    open fun shareFilesCancel() = translate {
        EN - "Cancel"
        DE - "Abbrechen"
    }

    open fun uploadFileErrorTitle() = translate {
        DE - "Beim Hochladen der Datei ist ein Fehler aufgetreten"
        EN - "An error occurred during the upload of the file"
    }

    open fun uploadFileErrorUnknown() = translate {
        DE - "Ein unbekannter Uploadfehler is aufgetreten."
        EN - "An unknown upload error has occurred."
    }

    open fun uploadFileErrorNotPasteable() = translate {
        DE - "Die Inhalte der ausgewählten Datei können nicht hochgeladen werden."
        EN - "The contents of the selected file can't be uploaded."
    }

    open fun uploadFileErrorFileListEmpty() = translate {
        DE - "Die ausgewählte Dateiliste ist leer."
        EN - "The selected file list is empty."
    }

    open fun filePreviewErrorTooBig(maxUploadSize: Long) = translate {
        DE - "Die ausgewählte Datei überschreitet die maximale Vorschaugröße von ${formatSize(maxUploadSize)}."
        EN - "The selected file exceeds the maximum preview size of ${formatSize(maxUploadSize)}."
    }

    open fun roomNoEncryptionFound() = translate {
        EN - "No encryption found"
        DE - "Keine Verschlüsselung gefunden"
    }

    open fun settingsRoomMemberListKickUserWarningMessageChat() = translate {
        EN - "The user will not be able to access the contents of the chat afterwards."
        DE - "Der Nutzer kann danach nicht mehr auf die Inhalte des Chats zugreifen."
    }

    open fun settingsRoomMemberListKickUserWarningMessageGroup() = translate {
        EN - "The user will not be able to access the contents of the group afterwards."
        DE - "Der Nutzer kann danach nicht mehr auf die Inhalte der Gruppe zugreifen."
    }

    open fun settingsRoomMemberListKickUserWarningTitleChat(username: String) = translate {
        EN - "Remove user $username from chat?"
        DE - "Nutzer $username aus dem Chat entfernen?"
    }

    open fun settingsRoomMemberListKickUserWarningTitleGroup(username: String) = translate {
        EN - "Remove user $username from group?"
        DE - "Nutzer $username aus der Gruppe entfernen?"
    }

    open fun knockIcon() = translate {
        EN - "room membership request"
        DE - "Raumbeitrittsanfrage"
    }

    open fun knockRequest() = translate {
        EN - "Request Membership"
        DE - "Beitrittsanfrage"
    }

    open fun knockExplanation() = translate {
        EN - "This room requires you to request to a membership"
        DE - "Dieser Raum erfordert, dass Sie eine Mitgliedschaft anfordern"
    }

    open fun knockLabel() = translate {
        EN - "Why do you wish to join? (Optional)"
        DE - "Warum möchtest du beitreten? (Optional)"
    }

    open fun mentionEventInRoom(roomName: String) = translate {
        EN - "Message in $roomName"
        DE - "Nachricht in $roomName"
    }

    fun actionCancel() = commonCancel().capitalize(Locale.current)
    fun actionMore() = commonMore().capitalize(Locale.current)
    fun actionClose() = commonClose().capitalize(Locale.current)
    fun actionOk() = commonOk().capitalize(Locale.current)
    fun actionBack() = commonBack().capitalize(Locale.current)
    fun actionNext() = commonNext().capitalize(Locale.current)
    fun actionRemove() = commonRemove().capitalize(Locale.current)
    fun actionDelete() = commonDelete().capitalize(Locale.current)
    fun actionCreate() = commonCreate().capitalize(Locale.current)
    fun actionSelect() = commonSelect().capitalize(Locale.current)
    fun actionCopy() = commonCopy().capitalize(Locale.current)
    fun actionConfirm() = commonConfirm().capitalize(Locale.current)
    fun actionExpand() = commonExpand().capitalize(Locale.current)
    fun actionCollapse() = commonCollapse().capitalize(Locale.current)
    fun actionZoomIn() = commonZoomIn().capitalize(Locale.current)
    fun actionZoomOut() = commonZoomOut().capitalize(Locale.current)
    fun actionSubmit() = commonSubmit().capitalize(Locale.current)
}

fun i18nViewModule() = module {
    single<I18nView> { I18nView(get(), get(), get(), get()) }
}

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)

## [Unreleased]

### Added

- Add ability to share plain and formatted text
- Add ability to share url with icon
- Camera capture as a file selection option

### Changed

- Color handling utils
- Changed `RoomSettingsMemberListElement` interfaces to allow more flexibility
- Updated documentation on how to use the framework from SwiftUI
- In single account mode make the profile banner open the user profile

### Deprecated

### Removed

### Fixed

- Format links with escaped ampersand (`&amp;`)
- Format links with semicolon (`https://abc.xyz/a;b;c`)
- Wrong device name displayed in verification success message

### Security

## 2.4.0

### Added

- Show online-status when searching for users
- Ability to see who reacted to a message
- Make Smoketests work on Web
- Make Smoketests work on Android
- Added hook on Android to change the behaviour of the messenger on startup of the Activity
- Add download button for file overlays
- Allow configuration to not use the account setup wizard
- Support private Browser Tabs.
- PDF reader for web

### Changed

- Do not run kmmPublish on push to main
- Automatically proceed bootstrapping without next button
- Open video player overlay when clicking on the preview
- Download text and markdown files when clicking on them in the timeline
- Upgraded dependencies

### Removed

- Removed spaces filter as a prerequisite for grouping rooms by spaces

### Fixed

- Fix thumbnails not being displayed sometimes because of size limit evaluations
- Fix headings not readable in dark mode
- Fix RoomElement using wrong DI context
- Fix crash when notification sound is unavailable
- Fix nightly pipeline missing job dependency
- Fix problem with login when an IOException has been thrown
- Fix crash when opening file picker on linux distribution
- Fix Cannot delete database when corrupted
- Fix server discovery fallback hiding server discovery errors
- Fixed scaling on mobile devices in web.
- Fix SPM: only publish when there are changes

## 2.3.7

### Added

- File save and upload dialog on web.
- use KMMBridge to publish iOS Swift and Objective-C artefacts
  to [https://gitlab.com/connect2x/trixnity-messenger/spm.git]()
- Wizard for account setup after initial login
- Support for "share to" Intent on Android
- support for pasting the first file of a fileList into the clipboard

### Changed

- Upgrade Trixnity to 4.10.0
- Make `MatrixMessenger` and `MatrixMultiMessenger` `Autocloseable` and close `HttpClientEngine`.
- Allow to configure `httpClientEngine` and `httpClientConfig` via `MatrixMessengerConfiguration`/
  `MatrixMultiMessengerConfiguration`
- Button to skip verification is now part of the list of available verification methods
- implement a cutoff for previews of media and thumbnails

### Fixed

- Don't show placeholder, when lastRelevantEventId not known.
- Don't mark state events as relevant.
- Wizard and modal dialog buttons are always shown on smaller screens
- Fix Emojis on Web
- Fix directory picker in room export.
- Fix UIA authentication fallback flows with multiple stages; explicitly show that phone/email verification are not yet
  supported
- Don't show edited message as new message
- Correct lifecycle handling on web
- SSO support in local dev server
- Fix Scrollbars nearly invisible in dark mode
- text messages containing symbols not allowed in file names no longer crash the app when posted into the clipboard

## 2.3.6

### Added

- Cross symbol for sent messages

### Changed

- Reduce flickering by showing creation time and date on outbox messages
- Change symbol for read messages to a double cross
- Remove alpha channel in outbox messages.
- Unify calculation of last relevant event in room list.

### Fixed

- Show Search Results when creating Room on small screen sizes
- Fixed bad contrast due to missing root Surface.
- Fixed editing a message bumps room up.
- Fixed Android notifications.

## 2.3.5

### Removed

- Removed edge-to-edge support until we support it in the UI.

### Fixed

- avoid unnecessary caching of all users for reactions and readBy markers

## 2.3.4

### Added

- Back button minimises App on Android

### Fixed

- Fixed verification modal navigation buttons not being shown on Android
- Do not display empty html tags
- Fixed misleading i18n content
- Fixed double padding in wizard
- Fixed wrong padding on Android 10 keyboard usage

# 2.3.3

### Added

- Introduce appId in configurations

### Changed

- Change keychain handling to include an appId
- Show Read-by information in context menu
- Replace MacOS Keychain handling with new API (a deprecated API was used before).

### Fixed

- Editing a reply now keeps it as reply

## 2.3.2

### Changed

- Open in App Browser for SSO.

## 2.3.1

### Changed

- Upgrade Trixnity

### Fixed

- Wrong indexedDb database name

## 2.3.0

### Added

- Markdown support for sending messages
- Setting to ban members from rooms
- List of banned users in a room
- Setting to unban banned users from rooms
- room alias settings and timeline support
- enable context menu in web version
- edge-to-edge support for Android
- Emoji reactions on messages
- UI: ability to see Aliases without edit permissions
- Show images in Web version
- Allow resetting recovery keys
- Show notifications in Web version
- Respect homeserver media upload limit

### Changed

- Tooltips on the Web version now use the same design as the Android version
- Instead of closing, profile abortion causes it to return to the previous screen
- Sync modal shown only on initial sync
- Run buildConfigGenerator on idea reload
- Renamed ENV variable `BUILD_CONFIG` to `MESSENGER_BUILD_CONFIG` to avoid confusion with white-labelled clients
- Made MediaOverlaySwitch public
- Upgrade Docker build image and dependencies
- Hide multi profile mode in trixnity messenger
- not shown redaction messages now also include not supported events in unencrypted rooms

### Deprecated

### Removed

- `app_name` from compose-view
- reaction functionality for deleted (redacted) messages

### Fixed

- Android-Tooltips on buttons are shown again
- Desktop-Tooltips can no longer move in front of the cursor, blocking button input
- Fix device list loading set to false too early
- Fix licenses by putting them into configuration
- Fix default NotificationHandler using wrong app name and channel ID
- Fix loading of complete timeline, when event mention is wrongly found.
- Fix missing initial sync modal
- Performance improvement: setHtml only once per message
- Fix flickering in the timeline
- mentions for users with same displayname are now displayed with their mxId
- Fix focus loss when typing space while reporting a message
- Fix crash when downloading and uploading files larger than INT_MAX (2.1GB)
- Fix flaky integration test `remove account when logged out and show login`
- Allow pasting images from clipboard (in memory)
- Correctly detect locales with region codes ("de-DE", "de-AT", "en-US")
- Require checkbox to advance when setting up recovery keys
- Create new chats in Web
- Fix message actions on mobile not updating when messages are sent
- Web production code does not crash on unknown timezone
- Do not scroll to bottom on reactions
- Fix crash on android when deleting events that are still in the outbox
- Fix replied message remains highlighted after sending
- Fix answering or editing own messages now correctly highlights it
- Desktop: notifications are shown when app is minimized

## 2.2.2

### Fixed

- Missing configuration in loginWith

## 2.2.0

### Added

- Compose Multiplatform UI 🎉
    * a highly customizable Matrix messenger frontend
    * see [Readme](trixnity-messenger-compose-view/README.md) for more information
- `timmy://localhost/room/<ID>` URI scheme for on-launch navigation

### Changed

- Upgraded Trixnity
- Upgraded dependencies

### Deprecated

### Removed

### Fixed

### Security

## 2.1.2

### Added

- Images uploaded on android and desktop are rotated correctly
- Allow to check login before creating a MatrixClient
- PDF file preview support

### Removed

- View model based login check

### Fixed

- UriCaller not working on Android when MatrixMessenger is created in ApplicationContext
- Show errors in SSO Login

## 2.1.1

### Added

- Editable room avatar
- permission necessary flag to android notification settings

### Changed

- upgrade trixnity version

### Deprecated

### Removed

### Fixed

### Security

## 2.1.0

### Added

- `UserSearchHandler` interface for facilitating user input sanitization of user search terms
- Setting to enable E2E encryption in rooms
- Upload progress to Video-, Audio- and File-Message-ViewModels
- Add warning dialog to 'enable encryption' setting

### Changed

- Upgraded to Kotlin 2.0
- Changed JS media store to OPFS
- Internal: changed gradle lock mode
- Force remove of account data on logout
- Improvements in ExportRoom: parallel decryption, collect decryption errors
- Move Gradle locks to CI
- Restrict ability to change room history to WORLD_READABLE when the group is encrypted
- Fixed typo in RoomEncryptionEnabledViewModel
- Upgrade Gradle to 8.9
- Fixed Java toolchain configuration in emoji subproject
- Only create media directory in export when needed
- Export view model exposes more info in error message
- Export uses better timestamp formats
- Upgrade Trixnity to 4.6.1

### Fixed

- Export is not working, when size of exported events did not match a multiple of buffer size
- Fix wrong filename in export

## 2.0.3

### Added

- mentions in formattedBody

### Fixed

- image dimensions should be null, when not determined

## 2.0.2

### Fixed

- allow to invite user with MXID even if the profile cannot be found

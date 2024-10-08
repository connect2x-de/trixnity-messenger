# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)

## [Unreleased]

### Added

- Setting to ban members from rooms
- List of banned users in a room
- Setting to unban banned users from rooms
- room alias settings and timeline support
- enable context menu in web version
- edge-to-edge support for Android

### Changed

- Tooltips on the Web version now use the same design as the Android version
- Instead of closing, profile abortion causes it to return to the previous screen
- Sync modal shown only on initial sync
- Run buildConfigGenerator on idea reload
- renamed ENV variable `BUILD_CONFIG` to `MESSENGER_BUILD_CONFIG` to avoid confusion with white-labelled clients

### Deprecated

### Removed

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
- Fix crash when downloading and uploading files larger than INT_MAX (2.1GB)
- Allow pasting images from clipboard (in memory)

### Security

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

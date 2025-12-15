# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)

## [Unreleased]

### Added

- Rendering room upgrades (tombstone event)
- Support OAuth2 based authentication
- Support Vodozemac (this is now the default, if this is not wanted, it can be changed it in
  MatrixMessengerConfiguration)

### Changed

- UI: Text selections have better visibility now
- UI: Replace roving focus with a focusGroup based implementation
- UI: Remove download indicator from TimelineEvents
- SDK: Change back handling to use custom handler instead of Decomposes

### Deprecated

### Removed

### Fixed

- ANDROID / UI: Fix upper padding being too big on emoji reaction bottom sheet
- UI: Add vertical divider after room list (for 2- or 3-sided layout)
- WEB: copy/paste/cut shortcuts work now
- UI: Remove unnecessary padding around state events
- WEB: the pdf reader no longer loads indefinitely when loading an invalid pdf file
- WEB,A11Y: CanvasSemanticsOwnerListener: render `SemanticsProperties.ContentDescription` as `aria-description`
- UI: emojis in the picker are no longer cut off at larger font sizes
- UI: Fix setup wizard sometimes not responding to clicks when starting verifications
- IDE: Set correct environment variables for prod/dev version
- SDK: Fix BackHandler missing in MatrixMultiMessenger


### Security

## 3.9.0

### Added

- UI: StoreFailure: show the name of the account with the corrupted database
- A11Y,WEB: add a SemanticsOwnerListener to allow for accessibility in web

### Changed

- DEPENDENCIES: Bump c2x Conventions to 20251114.122133
- Add button to cancel download in file details view
- A11Y,WEB: CanvasSemanticsOwnerListener: dialog now has labelledby and describedby attributes

### Fixed

- WEB: Using latest emojifont version
- A11Y: add proper label and role to LegalFooter
- UI,A11Y: Dropdown menu button is highlighted when hovering or focussing the box instead of the button itself
- API: When calling `ProfileManager.updateProfile`, the closure allows to mutate the configuration
- A11Y: add proper label and role to LegalFooter
- IOS: reporting a message could result in app crash
- UI: Create new group keeps information on expanded elements
- UI: Show download progress indicator when no total file size is received during download
- Allow restart of download after cancellation
- A11Y: add proper label and role to LegalFooter
- UI: headers now have a consistent minimum size
- UI: fix little timeline flickering while loading timeline element sender
- fix getting stuck on error screen after deleting local database
- UI: Emoji picker is correctly aligned to the center and scales as required when using different screen sizes
- WEB: fix incremental compilation
- A11Y,WEB: CanvasSemanticsOwnerListener: fix a bug with nodes not regaining the tabindex property

## 3.8.11

### Added

- Add a page selector to PDF reader

### Changed

- Change zoom behaviour in PDF reader
- Make buttons in FileHeader move into extra row(s) when necessary

### Fixed

- Using latest emojifont version on web
- Dropdown menu button is highlighted when hovering or focussing the box instead of the button itself
- No longer crash when opening a large pdf page on older Android versions
- Only push notifications for enabled accounts

## 3.8.10

### Fixed

- Upgrade sqlitenity to fix iOS issues

## 3.8.9

### Added

- Add a settings panel for configuring power levels
- Add timeline events for all power level changes
- Added a message bubble to the timeline when a user's power level changes
- UI: timeline does not show sticky date header if not necessary
- Readme entry on using the web app inside an `iframe`
- Completely new notification handling

### Changed

- Added Space in the bottom of the RoomList to account for new chat button overlapping UI elements
- Use SQLitenity as Room implementation
- Improve keyboard accessibility of profile selection and blocked user settings
- room settings: improve handling of name and topic display
- Upgrade Trixnity to 4.22.7

### Fixed

- The sticky header now correctly displays the date of the topmost message
- Fix focus handling in chat/group creation user list and add members view
- Show dropdown icon in group creation history visibility menu
- No more Linux segfaults
- Fix SQLitenity on windows
- Fix whitespaces in room list search input leading to unwanted removal of rooms from search results
- Correctly align appearance color preview in appearance settings
- a11y behavior all over the app (TAB behavior, roles, a11y labels, etc)
- mark buttons that create popups as aria-expandable

## 3.8.8

### Added

- Show richtext in room topic

### Changed

- Improve UI of room name & room topic

### Fixed

- The sticky header now correctly displays the date of the topmost message

## 3.8.7

### Added

- Introduce Worker to schedule work when messenger is started
- Add a cancel button to abort the reject invitation dialog
- Add a footer containing legal information to the login wizard
- Introduce new UI component for dropdown select

### Changed

- Explicitly draw system bars for edge to edge UI

### Fixed

- Fix db keys not deleted on account remove
- Correctly send and display strikethrough text via the del-tag
- Fix room settings crashing on opening when using Android
- Whitespace is stripped when searching for mxids
- Turn UiaModalBox into a Dialog so it no longer draws behind the ConnectingWizard
- UiaActionConfirmations without message are no longer being shown
- Make Imprint, Privacy and Licenses depend on MatrixMessengerBaseConfiguration instead of MatrixMessengerConfiguration
- The version number of a library in the licenses has the proper font color now
- Fix room list scrolling unnecessary on selection or focus navigation

## 3.8.6

### Added

- UI: Implement Keyboard navigation

### Fixed

- Technical: Don't override siblings or children in the deep nested settings view
- Fix Emoji Regex in Kotlin/JS
- Correctly show profiles

### Security

## 3.8.5

### Added

- Technical: Support deep nested settings view
- Copy message content
- Captions for Media messages

### Changed

- UI (Desktop, Web): tooltips are WCAG compliant (tooltip itself can be hovered, tooltips can be closed with Esc key,
  tooltips appear on focus)
- iOS: startMessenger() helper function now also returns MatrixMultiMessenger in order to bridge certain values to Swift
  code (like objects that can only be obtained via Koin)
- Don't close DI, because garbage collector can handle it
- Upgrade Trixnity to 4.22.4
- Expose platform media in loadMedia of FileBasedRoomMessageTimelineElementViewModel to allow temporary file generation

### Deprecated

- rename loadMediaResult to loadMediaResultBytes in FileBased RoomMessageEventContent

### Removed

### Fixed

- Focus highlighting for "Create Group" and "Search Group"
- Desktop/Web: show scrollbar in user search when creating a new chat
- Fix crash due to intrinsic size in timeline
- UI spacing for file-timeline-elements
- Mentions List calculated even though mention was already selected
- Crash when selecting mention if cursorPosition=0

## 3.8.4

### Fixed

- SSO Login Redirect to App on iOS targets
- Usernames are not shown in groups which started as a direct chat
- Fix closing profiles not working sometimes and leading to app crash

## 3.8.3

### Added

- Support for edge-to-edge on Android 36 and above
- Button to open app settings on iOS
- Info text when notification permissions are disabled (iOS)
- Network Monitor for network availability check (iOS)
- Highlight mode of focused elements for better a11y

### Changed

- Upgraded Sysnotify to 2.1.0
- Upgraded Gradle to 9.0.0

### Fixed

- Search results on smaller screens can now be scrolled
- Call openURL on main queue to prevent call on background thread (iOS)
- Show more appropriate message when user removes their name
- Copy messages now works on iOS
- Fix copy of recovery key in wizard step on iOS
- Translations on iOS show English when OS has configured German locale
- Set `event_id_only` format when setting pusher on iOS
- Reduce unnecessary timeline rerenders
- Fix layout inspector usage on Android

## 3.8.2

### Changed

- Updated Trixnity to 4.22.2

## 3.8.1

### Changed

- Updated Trixnity to 4.22.1

### Fixed

- Always show the verification as not ready when PreconditionsNotMet (can be triggered by a sync issue later on)

## 3.8.0

### Added

- Copy message content

### Changed

- Updated Trixnity to 4.22.0

## 3.7.2

### Added

- Notifications on iOS
- Added support for displaying MSC2448 Blurhash as image placeholder
- Tooltip for MXID and Username in Avatar Display Section

### Changed

- Updated Sysnotify to 2.0.3
- Improve accessibility of notification settings
- Length Constraint for MXIDs in AccountsOverview and Profilepicker

### Fixed

- Timeline stuttering due to large images
- AccountsOverview glitching out on long MXIDs

## 3.7.1

### Removed

- Removed legacy key chain

### Fixed

- Don't restart sync on presence change
- Crash on startup due to process file lock on some shared libraries
- Crash due to AutoLinkifyVisitor processing Matches out of order

## 3.7.0

### Added

- File load and save dialogs on iOS targets
- PDF reader support on iOS
- MatrixMessengerConfiguration for toggling refresh tokens

### Changed

- Bump Compose to 1.8.2
- Bump FileKit to 0.10.0-beta04
- Move startMessenger logic for the iOS client in separate function
- Power Levels now use net.folivo.trixnity.client.user.PowerLevel instead of Long
- Added Role.CREATOR
- Change markdown to GFM
- Don't use refresh tokens by default
- Bump c2x Conventions to 50375801
- Improved Reference/Link parsing

### Fixed

- Fixed crash due to scrollbar measuring policy on Android
- Fixed several translation issues
- Show error messages when replacing events

### Security

- Support room version 12

## 3.6.17

### Changed

- New rich text renderer
- Show correct feedback when getting no search results in the room list
- Labels for text fields are always on
- Adapt to trixnity References-API changes

### Fixed

- Web client: sync is not stopped when tab or browser loses focus
- Fix OOM gradle dependencies tasks

## 3.6.16

### Changed

- More explicit error messages on room creation
- Upgraded Gradle to 8.14.3
- Downgraded to Java 17
- Upgraded Sysnotify from 1.9.0 to 2.0.0

### Fixed

- Hardcoded log level in Web is not adjustable

## 3.6.15

### Added

- Network availability check for iOS
- Get dimension of image on iOS

### Changed

- Updated SysNotify to 1.9.0 and commonized desktop and web notification handling
- Room list elements now show multiple lines when the font size is large
- Presence icons for users are now distinguishable by color and shape
- Adapted to new mention regex
- Update Trixnity from 4.17.1 to 4.18.0

### Fixed

- Verification getting stuck after app lost focus
- Failing ITs with user search race condition that led to flaky tests
- Various issues in regard to PDF reader page loading and zooming

## 3.6.14

### Changed

- Room list elements now show multiple lines when the font size is large
- Presence icons for users are now distinguishable by color and shape
- Update Trixnity from 4.16.9 to 4.17.1

### Fixed

- Correctly indicate overflows in room invite name or matrix ids
- Fix Android back button closing the app instead of returning to previous view

## 3.6.13

### Changed

- Timeline element and details views are now based on interfaces and thus can be replaced

## 3.6.12

### Added

- Support for building for iOS x64

### Changed

- Send correct image size when uploading images on web

### Fixed

- Boostrap wizard graphics are not inverted and have visible background
- Remove timeline flickering due to switching between image fallback and thumbnail in reply elements
- Fix root lifecycle not starting
- Back Button doesn't work in BlockedContacts view, when focussing a pdf and in the Wizards
- Add lifecycle stop/start when moving iOS app to background/foreground
- Export not working in web
- Don't export media when downloads are disabled
- Escape CSV export with additional '

## 3.6.11

### Added

- More features in the iOS UI
- MatrixMultiMessenger.closeSuspending() to wait for every operation in Trixnity Messenger to be finished (in comparison
  to close() which will fire and forget and thus could give the impression that all operations are finished which might
  not be the case)

### Changed

- Rename observeAsSate to observeAsState
- Update Trixnity from 4.16.8 to 4.16.9

### Fixed

- Show error to user when room list element action failed
- Forgetting room when declining invite to deleted room
- Image detail view now displays image the first time it is opened
- PDF details view displays valid PDFs again
- Reopening PDF details view does not trigger download again

## 3.6.10

### Changed

- Updated Trixnity from 4.16.7 to 4.16.8

### Fixed

- Update room database to support iOS shared storage

## 3.6.9

### Fixed

- Don't block opening room database

## 3.6.8

### Added

- Disable downloads by build environment variable

### Changed

- Updated Trixnity from 4.16.5 to 4.16.7
- Updated various dependencies (important: kotlin-wrappers updated to 2025.6.9)

### Fixed

- Unsupported image mime types are treated as images
- link colour in privacy and imprint page

## 3.6.7

### Changed

- Move to room with active verification, when verification with user is already running
- Disable start user verification option when one is already running

### Fixed

- Show correct error message on corrupt files
- Fix database encryption problem when app was in background mode

## 3.6.6

### Changed

- Text input fields have a max length now
- Rename Sqlite3mc driver package name
- Allow to enable database encryption via config (all new accounts use encrypted databases by default, except web where
  no decryption can be activated; existing unencrypted databases stay unencrypted)

## 3.6.5

### Added

- Added loading spinner to user profile view when blocking user to show in-progress action

### Changed

- Don't show option for user verification when user is already verified
- Updated Trixnity from 4.16.4 to 4.16.5

### Fixed

- Show correct user verification status in UserInfo when user verification hasn't been completed
- Fixed tooltips and accessible labels for Read/Sent markers
- Fixed switch in user profile view for blocking not reflecting correct server-side state
- Fixed error display not going away when renaming device

## 3.6.4

### Changed

- Updated Trixnity from 4.16.3 to 4.16.4 (hotfix: dehydrated devices)

## 3.6.3

### Changed

- Updated Trixnity from 4.16.2 to 4.16.3 (hotfix: dehydrated devices)

## 3.6.2

### Added

- Accessibility Settings Step for Setup Wizard (`AccountSetupWizardStep.AccessibilityStep`)

### Changed

- Internal: add remote caching
- Updated Trixnity from 4.16.1 to 4.16.2 (includes dehydrated devices fix)

### Fixed

- When creating a user verification from another room, jump to the direct room where the user verification is taking
  place

## 3.6.1

### Changed

- Overlay file transfer progress over uploaded image thumbnail
- Updated Trixnity from 4.16.0 to 4.16.1

### Fixed

- Remove timeline flickering due to switching between image fallback and thumbnail
- Resolved crash due to "Image too large" on Android
- Reuse an existing direct chat only if it is a direct chat with only that user when creating a new chat

### Security

## 3.6.0

### Added

- Add setAvatarImage function to AvatarCutterViewModel
- Add tests for various logic

### Changed

- Name "privacy" settings to "privacy and security" settings
- Show readers and reactions of first instead of last revision of a message in `TimelineElementMetadataView`
- UserSearchHandler's foundUsers is now eagerly computed
- Make tooltip show/hide delays configurable
- Styling: Use customizable components for labels
- Persist presence
- Styling: Use theme components for dialogs
- Clearly distinguish between a room member leaving, being kicked, banned or unbanned.
- Adapt to Trixnity media cache changes

### Fixed

- Update file size for images after processing
- Display avatar correctly cropped in message metadata view
- End URLs on whitespace
- Fixed tooltip appears too fast in `TimelineElementMetadataView`
- Fixed "edited" is shown in the wrong place in `TimelineElementMetadataViewModel`
- Send button is only active after the second char in the input field
- Canceled or completed user verifications are shown correctly in the timeline (**Breaking change**: user verification
  end states (canceled or done) have separate timeline view models now
  (MessageTimelineElementViewModel.VerificationCancel, MessageTimelineElementViewModel.VerificationDone))
- Fixed AvatarCutter ignoring alignment/rotation metadata

## 3.5.8

### Added

- PDF readers for Desktop, Android and Web (can be disabled via configuration: @see
  MatrixMessengerConfiguration.features)
- Added indicator for dehydrated devices in device settings.
- Allow changing locale in Android app settings

### Changed

- Updated Ktor from 3.1.3 to 3.1.4
- Updated Koin from 4.0.3 to 4.0.4
- Updated C2X conventions plugin from 41503362 to 43109500
- Updated Trixnity from 4.15.2 to 4.15.3
- Updated js-joda from 2.21.2 to 2.22.0
- Updated zip.js from 2.7.58 to 2.7.61
- Updated Kotlin Wrappers from 2025.3.20 to 2025.5.6
- Updated KotlinX Coroutines from 1.10.1 to 1.10.2
- Updated KotlinX Serialization from 1.8.0 to 1.8.1
- Updated SKIE from 0.10.1 to 0.10.2-preview.2.1.20
- Updated KIM from 0.23 to 0.24
- Updated pdfbox from 3.0.4 to 3.0.5
- Updated okio from 3.10.2 to 3.11.0
- Updated AndroidX Crypto from 1.1.0-alpha06 to 1.1.0-alpha07
- Updated AndroidX LiveData from 2.8.7 to 2.9.0
- Updated AndroidX SQLite from 2.5.0-rc03 to 2.5.1
- Updated AndroidX SQLite MC from 2.5.0-rc03 to 2.5.1
- Updated Kotlin Logging 7.0.5 to 7.0.7
- Separate license processing tasks for android product flavors and build type permutations
- Switch to room view when starting a user verification in single pane mode
- Show state of user verification in room list element
- Show error popups when problems occur during room avatar update
- Align confirm avatar update button to lower right in AvatarCutter
- When creating a new group, added users are shown in a scrollable list and not a grid (UsersInGroup)

### Fixed

- Fixed cancellation exceptions and a small memory leak caused by message replies
- Correct button placement in user verification
- Fix wrong calculations in ChangePowerLevelViewModel
- Allow to change power levels for own user
- Fixed various bugs in user search
- Fixed user search results not being cleared when search term is empty
- Fix crash when selecting user search results
- Export Button was disabled on web
- Fix: Emoji Selector keyboard navigation

## 3.5.7

### Changed

- Give immediate feedback when a message has been sent (even before sync)
- Update Firebase Cloud Messaging from 33.11.0 to 33.13.0
- Rename member filters and add invited filter
- Expose current theme settings for library users
- Styling: Allow customizing slider via theme
- Styling: Use styleable components for avatars
- Styling: Use styleable components for chips
- Styling: Make details pane style customizable
- Styling: Allow customizing banners via theme
- Styling: Allow customizing switches via theme
- Styling: Make emoji reactions themable

### Fixed

- Add delay for appearance of tooltips when hovering element
- Remove double parenthesis for file size of upload element
- Unintuitive icon used for logout button
- No padding at the end of the room header description (View: RoomHeader)
- User Profile Avatars not loading
- Styling: use tonalElevation for surfaces
- Styling: Fix alignment & color for room list time & encryption icon
- Direct rooms sometimes were interpreted as a group

## 3.5.6

### Changed

- Styling: Use stylable components for roomlist
- Styling: Define default colors for different surface levels
- Styling: support local content color for most components
- Styling: Allow overriding MessengerColors

### Fixed

- Duplicated libraries in the About > Licenses are now differentiable
- Wait for database being closed before trying to delete user folder
- Update selected account when deleting account
- Improved web build performance

## 3.5.5

### Added

- Scroll to replied element in timeline when clicking
- Add "deactivate account" functionality

### Fixed

- Show correct indicator message when an invitation is rejected or revoked

## 3.5.4

### Added

- Copy button for MXID in UserProfile
- Theming for ProgressIndicators
- Leave and forget upgraded rooms too

### Changed

- Clicking on files without preview opens a "Not supported" message instead of starting a download
- Close di on close
- Destroy lifecycle on profile change
- Fix crash on startup because of mismatched Compose UI Test version

### Fixed

- Overflows in RoomHeader
- Reset read and send marker when editing messages while being offline
- Preview in accessibility settings
- Copy recovery key not available on small screens
- Wrong tooltip text of 'cancel edit' button
- Open room settings when clicking on header in group room

## 3.5.3

### Added

- See and accept knocks via Roomsettings and Timeline
- Show reason for ban and invite in Timeline

### Changed

- Setup wizard can no longer be skipped

### Fixed

- Minor styling consistency fixes
- Fix Recreating Profile in Web
- iOS: problems with keychain access and missing key
- Avoid re-render when unrelated settings change
- Fix low contrast in dark mode
- Fix `forgetting rooms while not leaved` error
- Fix possible duplicate room creation for groups and chats

## 3.5.2

### Changed

- Upgrade Trixnity to 4.14.3
- `PlatformSecretByteArrayKeyProvider` uses key derivation when not the first in the chain

### Removed

- Removed `RoomContextMenu` from `RoomHeader`

### Fixed

- Padding of sticky date header didn't apply to wider screens, leading to misalignment
- Fix handling of special characters in urls
- Fix padding flickering in outbox in offline edge cases
- Fixed initialization of SecretByteArrays on first start

## 3.5.1

### Added

- Knocking public rooms

### Changed

- Completely new SecretByteArray architecture, that allows to have a keychain
- Upgraded to Gradle 8.13
- Migrated build boilerplate to new conventions plugin
- Reduce height of room list elements
- Use kotlin.test for all tests
- Updated UI to use new styling system for icon buttons, floating action buttons
- Properly handle internal code deprecations

### Removed

- Kotest as test engine
- benasher44 UUID as a UUID library

### Fixed

- Forgetting the room with invite after rejection of the invitation
- Correctly use test dispatcher
- Fix tests on IOS
- Fix wrong UriCaller encoding in web
- Removed duplicate device verification dialog
- Fix iconbuttons not being visible in dark mode
- Fix room header content being greyed out

## 3.5.0

### Added

- Archived rooms support
- Add settings to forget rooms in room list and settings view
- AddMembersViewModel now has indicator when users are currently added
- New system for customizing styles of each component

### Changed

- Layout of message metadata info
- Updated UI to use new styling system for buttons, surfaces, inputarea, tooltips

### Fixed

- Fix timeline rendering bug caused by "fix-file-onsave-handling"
- Display rooms correctly the first time on join
- Set directory visibility of public rooms to public
- Don't max out the dialog height
- Styling inconsistencies
- No more flickering in timeline when writing new messages

### Security

- Sanitize Uri in Web

## 3.4.4

### Added

- Check to ensure a single running instance of the messenger in the browser

### Changed

- Change imprint and privacy information formatting to Markdown to support them in app instead of via link
- Documentation for `AccountSetupWizard`

### Fixed

- Bug in file attachment upload size
- Unrecommended HTML tags are not being sent any more
- Shared Plaintext is not interpreted as Markdown
- CloseApp should not be injected when null
- Fixed file save dialog repeatedly opening
- Fixed missing favicon in web version

## 3.4.3

### Added

- Test coverage in GitLab pipeline

### Changed

- Change imprint and privacy information formatting to Markdown to support them in app instead of via link
- Upgrade to Trixnity 4.13.3 to fix rollback issues in IndexedDB

### Fixed

- Removed useless suspense in addmembers viewmodel
- Fix Back Button for image preview

## 3.4.2

### Added

- Font and control size settings
- Loading indicator for room creation

### Changed

- Insert formula in padding of the timeline
- Increase size of avatar and change order of settings in profile view
- Check for server reachability when logout fails
- Make room notification settings collapsable and change setting design to the one used by other room settings
- Disable notification setting sub-options when notifications are disabled
- Sort users with reactions on the top of the user list on the metadata view
- Save timeline scroll state when opening sub-menu of room
- Use fastzip in CI
- Upgrade Trixnity to 4.13.2

### Fixed

- Align padding for sticky date header with dates in the timeline on mobile
- `isReplaced` should be initially false
- Date bubble is only shown when the date is changed in message metadata view
- Fix content of message replacements in history of message metadata view
- Allow to show audio messages as preview
- Scrollbar to message metadata view for long messages/long message history
- Inserting Mentions fails for multiple ones
- Don't process date for invisible elements

## 3.4.1

### Added

- Allow to wait for `SettingsHolder` to be initialized
- Introduce `MatrixMessengerWorker` and `MatrixMultiMessengerWorker`
- Introduced `ConfigureMatrixClientConfiguration` to easily extend `MatrixClientConfiguration`
- Autofill (password manager) support

### Changed

- Debounce unread message counter in timeline to avoid flickering
- Focus message input text field when starting a reply
- Use latest edit of an event for reader and isRead calculations
- Moved verification step to the last position in the setup wizard
- Don't show verification methods in self verification wizard while initial sync is running

### Fixed

- Fix Database can't be read on start up
- Update Trixnity to 4.13.1
- Delete reply on message delete
- only create new viewModel for timeline elements when the event content changes
- show correct error message colors on image details view
- TextFields behave according to their role

## 3.4.0

### Added

- Added persistent run profiles to IDEA configuration
- Message Info view

### Changed

- Refactored message bubbles to support message previews
- Refactored read receipts and reactions to be more modular
- Removed default parameter from `FileBasedRoomMessageTimelineElementView` to allow overriding it
- Moved message metadata into a unified view with a new design

### Fixed

- Only show users with sync errors in the info banner
- Fix text overflow in about licenses section
- Clear current reply when sending reply message
- The keyboard does not pop up when opening a room
- Empty reactions do not block reaction computation
- Fix possible crash when entering rooms with large messages
- Remove broken collapsed message bubbles
- Empty reactions or RedactedEventContent do not block reaction computation

## 3.3.0

### Added

- Introduced String.graphemeCount extension for counting perceived chars
- Introduced String.forEachGrapheme extension for iterating perceived chars
- Possibility to change MatrixClientServerApiClientFactory via the DI
- Introduce TextFieldViewModel for cleaner text input synchronization

### Changed

- Use password type keyboard to enter recovery key
- Refactored app view routing
- Standardized reaction when a room (chat or group) has been created

### Fixed

- Fixed stuck device verification (button did nothing)
- Make initials computation fully Unicode compliant
- Fixed redactions of message edits
- Don't render replace events even when not decryptable
- Fixed various text field and typing issues
- Fixed String.graphCount and String.forEachGraph extensions in JS implementation
- Crash on missing video thumbnail

## 3.2.0

### Added

- Add User Profile Screen

### Fixed

- Fixed cache issues in Web by upgrading Trixnity

## 3.1.1

### Changed

- Force database directory creation

### Fixed

- Fixed export directory resolution on Android
- Handle commas in url correctly
- Fixed faulty stale data cleanup on login leading to login error in Web
- Fix license entries

## 3.1.0

### Added

- Status of an edited message
- Possibility to provide an app version that is displayed in the info section
- Show users typing in room list
- Allow sending attachments with Enter (desktop)
- Check server capabilities for setting display name and avatar and prevent editing if not supported
- More conservative MatrixClient initialization failure handling
- Inform the user that additional sync methods might be available after the initial sync during the Verification Wizard
- Only show verification banner in RoomList, when the selected Account isn't verified
- Change RedoSelfVerification to Wizard Framework
- Don't start verification on startup, start it via the setup or the banner in the room list instead
- Support for refresh tokens
- Add badge with count of unread messages to scroll down button in rooms

### Changed

- Moved from Realm to Androidx Room database
- Enabled Encryption for Androidx Room database
- Image detail view UI overhaul
- Upgrade Trixnity to 4.12.0

### Removed

- Internal: removed strong skipping Compose mode as it is enabled by default now

### Fixed

- Don't show sender for own elements (including outbox elements)
- Show sender, when state event before element
- Text colors in message bubbles and input field (Desktop) adjusted for dark mode
- Removed ability to block yourself
- Fixed show SSO login option with empty identity provider list
- Don't show "Image could not be loaded" message while loading an image
- Fixed jumping timeline on fast message sending
- Fixed various issues, that unread marker is unnecessarily displayed
- Fixed unnecessary re-computations on timeline re-init
- Fixed typo in verification help text
- Timing problem in UIA flow with confirmations being accepted before the internal state machine could react

## 3.0.3

### Fixed

- Fixed various scrolling issues in timeline
- Fixed outbox loading in timeline

## 3.0.2

### Changed

- Upgraded Trixnity to 4.11.2

### Fixed

- Cleaned up timeline element loading/dropping to prevent some scroll-jump edge cases
- Fixed rendering of the date in small message bubbles
- Fixed timeline elements displayed in wrong order

## 3.0.1

### Changed

- Do not reuse an existing direct room with a user that has left the room

### Fixed

- Fix Smoketests on Web
- Fix redacted event not visible
- Fix Handling of temporary camera captures
- Fix unnecessary waiting for membership change message
- Fix displaying the outbox without timeline for a short moment
- Fix message edits not displayed immediately
- Fix unread marker is shown when sending a message

## 3.0.0

### Added

- Add ability to share plain and formatted text
- Add ability to share url with icon
- Camera capture as a file selection option

### Changed

- Color handling utils
- Changed `RoomSettingsMemberListElement` interfaces to allow more flexibility
- Updated documentation on how to use the framework from SwiftUI
- In single account mode make the profile banner open the user profile
- Device verification dialog to show the affected account name and mxid
- Use `XDG_DATA_HOME` on linux
- New extensible faster timeline architecture

### Fixed

- Format links with escaped ampersand (`&amp;`)
- Format links with semicolon (`https://abc.xyz/a;b;c`)
- Wrong device name displayed in verification success message
- Overlay sometimes won't vanish when switching accounts

## 2.4.0

### Added

- Show online-status when searching for users
- Ability to see who reacted to a message
- Make Smoketests work on Web
- Make Smoketests work on Android
- Added hook on Android to change the behaviour of the messenger on startup of the Activity
- Add download button for file overlays
- Allow configuration to not use the account setup wizard
- Support private Browser Tabs
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
- Fixed scaling on mobile devices in web
- Fix SPM: only publish when there are changes

## 2.3.7

### Added

- File save and upload dialog on web
- use KMMBridge to publish iOS Swift and Objective-C artefacts
  to [https://gitlab.com/connect2x/trixnity-messenger/spm.git]()
- Wizard for account setup after initial login
- Support for "share to" Intent on Android
- support for pasting the first file of a fileList into the clipboard

### Changed

- Upgrade Trixnity to 4.10.0
- Make `MatrixMessenger` and `MatrixMultiMessenger` `Autocloseable` and close `HttpClientEngine`
- Allow to configure `httpClientEngine` and `httpClientConfig` via `MatrixMessengerConfiguration`/
  `MatrixMultiMessengerConfiguration`
- Button to skip verification is now part of the list of available verification methods
- implement a cutoff for previews of media and thumbnails

### Fixed

- Don't show placeholder, when lastRelevantEventId not known
- Don't mark state events as relevant
- Wizard and modal dialog buttons are always shown on smaller screens
- Fix Emojis on Web
- Fix directory picker in room export
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
- Remove alpha channel in outbox messages
- Unify calculation of last relevant event in room list

### Fixed

- Show Search Results when creating Room on small screen sizes
- Fixed bad contrast due to missing root Surface
- Fixed editing a message bumps room up
- Fixed Android notifications

## 2.3.5

### Removed

- Removed edge-to-edge support until we support it in the UI

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
- Replace MacOS Keychain handling with new API (a deprecated API was used before)

### Fixed

- Editing a reply now keeps it as reply

## 2.3.2

### Changed

- Open in App Browser for SSO

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

### Removed

- `app_name` from compose-view
- reaction functionality for deleted (redacted) messages

### Fixed

- Android-Tooltips on buttons are shown again
- Desktop-Tooltips can no longer move in front of the cursor, blocking button input
- Fix device list loading set to false too early
- Fix licenses by putting them into configuration
- Fix default NotificationHandler using wrong app name and channel ID
- Fix loading of complete timeline, when event mention is wrongly found
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
- Don't show user presence in direct chat when user left room

## 2.0.2

### Fixed

- allow to invite user with MXID even if the profile cannot be found

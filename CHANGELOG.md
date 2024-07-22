# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)

## [Unreleased]

### Added

- Setting to enable E2E encryption in rooms
- upload progress to Video-, Audio- and File-Message-ViewModels
- add warning dialog to 'enable encryption' setting

### Changed

- upgraded to Kotlin 2.0
- changed JS media store to OPFS
- internal: changed gradle lock mode
- force remove of account data on logout
- improvements in ExportRoom: parallel decryption, collect decryption errors
- move gradle locks to CI
- restrict ability to change room history to WORLD_READABLE when the group is encrypted
- fixed typo in RoomEncryptionEnabledViewModel
- upgrade gradle to 8.9
- fixed Java toolchain configuration in emoji subproject

### Deprecated

### Removed

### Fixed

### Security

## 2.0.3

### Added

- mentions in formattedBody

### Fixed

- image dimensions should be null, when not determined

## 2.0.2

### Fixed

- allow to invite user with MXID even if the profile cannot be found

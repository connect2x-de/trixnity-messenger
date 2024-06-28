# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)

## [Unreleased]

### Added

- mentions in formattedBody

### Changed

- restrict ability to change room history to WORLD_READABLE when the group is encrypted

- use room alias in mention, when given
- upgrade Trixnity

### Deprecated

### Removed

### Fixed

- fix user mentions when user not found
- image dimensions should be null, when not determined
- image dimensions are computed correctly for Desktop and Android

### Security

## 2.0.2

### Fixed

- allow to invite user with MXID even if the profile cannot be found

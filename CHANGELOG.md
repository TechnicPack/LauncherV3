# Changelog

All user-visible changes to this launcher are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

When a build is promoted, the `[Unreleased]` section is renamed to
`[v4.0-<build>-<channel>] - YYYY-MM-DD` and a new empty `[Unreleased]` section
is added at the top.

## [Unreleased]

### Added
- Support for importing Prism Launcher instance zips (`mmc-pack.json`, per-component patches under `patches/`).
- Support for Prism-style version patches (uid, order, +jvmArgs, +tweakers, MMC-hint, compatibleJavaMajors).
- Orphan cleanup on modpack updates: files extracted from removed mods are now deleted between updates, tracked via `bin/extractedFiles.json`.
- ARM native library support: arch-specific classifiers (`linux-arm64`, `windows-arm64`, etc.) now resolve correctly on ARM CPUs.
- Mojang JRE component selection now reads the live JRE manifest, so newly published runtime components (e.g., a future `java-runtime-zeta`) are picked up automatically.

### Changed
- **First-run config migration:** existing `config/` directories are now moved to a timestamped `config-backup-YYYY-MM-DD-HHmmss/` folder (with a `README.txt` explaining the move) before being reset. Previously the migration overwrote prior backups.
- Atomic writes for persisted launcher state. A crash mid-save no longer truncates the settings file, saved user sessions, the Java-installs list, the runtime-constraints file (`bin/runData`), the installed-version marker (`bin/version`), or the orphan-cleanup manifest (`bin/extractedFiles.json`). Previously such a crash could silently reset settings, log the user out, drop their Java installs, trigger spurious reinstalls, or delete files that weren't actually orphaned.

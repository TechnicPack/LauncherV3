# Changelog

All user-visible changes to this launcher are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

When a build is promoted, the `[Unreleased]` section is renamed to
`[v4.0-<build>] - YYYY-MM-DD` and a new empty `[Unreleased]` section
is added at the top.

## [Unreleased]

### Fixed
- Launcher auto-updates now clean up the `launcher.exe.old` backup file that is left on disk when antivirus briefly blocks its removal, even for launchers installed outside `%APPDATA%\.technic\` (Desktop, Downloads, portable drives — the common case). Previously this cleanup only scanned `%APPDATA%\.technic\`, so most users could see a leftover `.old` file next to their launcher until they deleted it manually.
- When an auto-update is blocked by antivirus, Windows Defender, or Windows' "Controlled Folder Access" (which protects Documents, Pictures, Videos, Music, and Favorites by default), the launcher now shows a specific error dialog that names the blocked file path and walks through how to fix it (whitelist the launcher, move it out of a protected folder, or re-download from technicpack.net). Previously these blocks showed the generic "unknown I/O error occurred" message with no actionable information.

## [v4.0-1081] - 2026-04-23

### Fixed
- Launcher auto-updates on Windows now fall back to a direct file copy when the "rename first" update strategy is refused by strict antivirus configurations (some antivirus products block the rename step itself with "The process cannot access the file because it is being used by another process"). The direct copy uses different Windows file-sharing flags and can succeed where the rename could not. The auto-update retry window was also extended from 5 seconds to 20 seconds to give antivirus scans on the freshly-downloaded launcher executable more time to finish before giving up.

## [v4.0-1080] - 2026-04-22

### Fixed
- Auto-updates from older launcher builds no longer abort with an "Illegal char `<:>`" error before finishing. Older versions passed the update-target path in a URL-like form that the newer mover rejected; the mover now accepts both shapes, so users who hadn't updated in a while can now finish the update without re-downloading the launcher manually.
- Launcher auto-updates on Windows are more reliable when antivirus or Windows briefly holds the launcher executable open. The updater now renames the existing binary aside (`launcher.exe.old`) before writing the new one — Windows permits renaming a running executable even when a direct overwrite would fail with "The process cannot access the file because it is being used by another process". Any `.old` files left behind are cleaned up on the next launcher startup.

## [v4.0-1079-stable] - 2026-04-22

### Changed
- Crash and error reports are now sent to Technic's self-hosted Sentry instance (`sentry.technicpack.net`) instead of Sentry's SaaS. No change to what's reported or when.

## [v4.0-1076-stable] - 2026-04-17

### Fixed
- Orphan-file cleanup no longer reports already-removed files as deletion failures after modpack updates. Most orphans on a normal update are mod-version bumps whose old jars were already wiped by the pre-extraction cleanup step; those are now counted as "already removed" instead of "failed".

## [v4.0-1075-stable] - 2026-04-17

### Added
- Support for importing Prism Launcher instance zips (`mmc-pack.json`, per-component patches under `patches/`).
- Support for Prism-style version patches (uid, order, +jvmArgs, +tweakers, MMC-hint, compatibleJavaMajors).
- Orphan cleanup on modpack updates: files extracted from removed mods are now deleted between updates, tracked via `bin/extractedFiles.json`.
- ARM native library support: arch-specific classifiers (`linux-arm64`, `windows-arm64`, etc.) now resolve correctly on ARM CPUs.
- Mojang JRE component selection now reads the live JRE manifest, so newly published runtime components (e.g., a future `java-runtime-zeta`) are picked up automatically.

### Changed
- **First-run config migration:** existing `config/` directories are now moved to a timestamped `config-backup-YYYY-MM-DD-HHmmss/` folder (with a `README.txt` explaining the move) before being reset. Previously the migration overwrote prior backups.
- Atomic writes for persisted launcher state. A crash mid-save no longer truncates the settings file, saved user sessions, the Java-installs list, the runtime-constraints file (`bin/runData`), the installed-version marker (`bin/version`), or the orphan-cleanup manifest (`bin/extractedFiles.json`). Previously such a crash could silently reset settings, log the user out, drop their Java installs, trigger spurious reinstalls, or delete files that weren't actually orphaned.

### Removed
- **Beta update channel.** The launcher now uses a single stable channel; the channel selector has been removed from Options. Existing beta installs continue to receive updates — beta requests are server-side aliased to stable, so no reinstall is required.

<!-- historical-releases-footer -->

---

For the full list of historical stable releases (pre-dating this file), see [HISTORY.md](HISTORY.md) or the [Releases page](https://github.com/TechnicPack/LauncherV3/releases).

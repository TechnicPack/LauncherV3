# Changelog

All user-visible changes to this launcher are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

When a build is promoted, the `[Unreleased]` section is renamed to
`[v4.0-<build>] - YYYY-MM-DD` and a new empty `[Unreleased]` section
is added at the top.

## [Unreleased]

## [v4.0-1089] - 2026-05-04

### Added
- Adding a Microsoft account now opens a dialog with two sign-in options at once: the existing browser-based sign-in, and a short device code with a QR code you can scan with your phone (or any other device with a camera) to finish signing in there. Either option signs you in; whichever finishes first wins. The device code path avoids the localhost callback entirely and works in environments where the browser-based sign-in has historically hung or been blocked (antivirus intercepting localhost, Windows Firewall prompts dismissed, corporate proxies, some OneDrive configurations). The dialog shows a countdown for the device code's 15-minute window; if it expires before you finish, a "Get a new code" button fetches a fresh code without having to close and reopen the dialog.

### Changed
- Bumped bundled third-party libraries to current upstream releases: Gson 2.13.2 to 2.14.0, Guava 33.5.0 to 33.6.0, Apache Commons IO 2.21.0 to 2.22.0, Apache Commons Codec 1.21.0 to 1.22.0, Joda-Time 2.14.1 to 2.14.2 (latest tzdata), Maven Artifact 3.9.14 to 3.9.15, zstd-jni 1.5.7-7 to 1.5.7-8. Bug-fix and timezone-data refreshes only; no behaviour changes for end users.
- Bumped the Sentry crash-reporting SDK from 8.36.0 to 8.40.0 and its companion Gradle plugin from 6.2.0 to 6.6.0. Bundled bug fixes only; the launcher's reporting behaviour and self-hosted Sentry endpoint are unchanged.
- Bumped the Gradle build tool from 9.4.1 to 9.5.0 and the Shadow plugin from 9.4.0 to 9.4.1. Build-time only; no impact on the produced launcher binary.

### Fixed
- Mojang JRE component selection now recognizes the version strings Mojang actually publishes for `java-runtime-alpha` (`16.0.1.9.1`, `16.0.1.9.1_3`) and `jre-legacy` (`8u202`, `8u51-cacert462b08`). Previously these were skipped as "unrecognized version" and the launcher fell back to a hardcoded component map. The hardcoded fallback already covered the cases Mojang ships today, but any future runtime that uses the same dotted-with-build form would have been silently dropped from the live manifest. The parser now handles arbitrary-length dotted versions and the classic `<major>u<update>` shape.
- Disabled controls across the launcher now visibly render as disabled instead of looking identical to enabled ones. Styled buttons fade to reduced opacity, and the custom Width/Height fields in Options > Video now use a muted gray palette when the Default/Fullscreen window size is selected (the saved values stay legible, but the field clearly reads as non-interactive). Previously these controls ignored disabled state when drawing themselves, so any button temporarily locked (e.g., while an action was loading) or dimensions fields locked behind a non-Custom window mode gave no visual feedback about their state.
- The self-updater's progress bar no longer sits in a hard black box on top of the splash art. The bar now renders over a soft semi-transparent dark ribbon that blends with the icon above and keeps the white progress text legible over any desktop behind the translucent splash frame. Adjacent polish: the splash icon no longer flickers on cursor movement or clicks, the current-item label's descenders (g, p, y, j) are no longer clipped, and the splash frame is sized up-front to avoid clipping the icon when the asset-download phase reveals its secondary progress row.

## [v4.0-1084] - 2026-04-23

### Fixed
- When the Microsoft account sign-in folder (`%APPDATA%\.technic\oauth`) is in a damaged permission state that the launcher cannot reset automatically, the launcher now keeps running with a temporary in-memory credential store instead of crashing. A warning dialog explains which folder is affected and how to fix it manually (delete via File Explorer, with a take-ownership step if permissions are also broken). You will need to sign in to Microsoft every launch until you clean up the folder, but the rest of the launcher works as normal in the meantime.

## [v4.0-1083] - 2026-04-23

### Fixed
- Launcher auto-updates now clean up the `launcher.exe.old` backup file that is left on disk when antivirus briefly blocks its removal, even for launchers installed outside `%APPDATA%\.technic\` (Desktop, Downloads, portable drives — the common case). Previously this cleanup only scanned `%APPDATA%\.technic\`, so most users could see a leftover `.old` file next to their launcher until they deleted it manually.
- When an auto-update is blocked by antivirus, Windows Defender, or Windows' "Controlled Folder Access" (which protects Documents, Pictures, Videos, Music, and Favorites by default), the launcher now shows a specific error dialog that names the blocked file path and walks through how to fix it (whitelist the launcher, move it out of a protected folder, or re-download from technicpack.net). Previously these blocks showed the generic "unknown I/O error occurred" message with no actionable information.
- Microsoft account sign-in's recovery for corrupt permission state on the `%APPDATA%\.technic\oauth` folder now handles non-empty folders (recursive delete) and falls back to renaming the folder aside when delete is refused. When every recovery strategy fails, the resulting error message now tells the user which folder is affected and how to resolve it manually (delete via File Explorer, or take ownership via Properties > Security > Advanced). Previously the recovery only handled empty folders, so a folder with any saved sign-in state inside would crash the launcher with an opaque error.

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

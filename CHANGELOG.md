# Changelog

Notable changes to this launcher are documented here for users and contributors. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

This project uses build-based versioning (`v4.0-<build>`), not Semantic Versioning. Older release tags also include
the update channel (for example, `v4.0-1075-stable`). Curated coverage starts with that release; earlier releases
are preserved in the uncurated [release history](HISTORY.md).

When a build is promoted, the `[Unreleased]` section is renamed to `[v4.0-<build>] - YYYY-MM-DD` and a new empty
`[Unreleased]` section is added at the top.

## [Unreleased]

### Fixed

- Modpack logos now preserve their aspect ratio, centered in the artwork area without upscaling,
  instead of stretching nonstandard image dimensions.
- Modern Forge and NeoForge launch preparation now removes obsolete `bin/minecraft.jar` files left by earlier
  installations after verifying the native-launch copy, without requiring a full reinstall.

## [v4.0-1160] - 2026-09-19

### Changed

- Java compatibility and low-memory warnings now use the launcher's dark palette, bundled fonts, and rounded buttons,
  with visible keyboard focus. Native window controls, default cancellation, and expandable technical details are
  preserved.

### Fixed

- Modern mod loader installation now freshly validates locally selected Java before installing libraries, Minecraft, or
  assets, including when the launcher uses that same Java runtime. Missing or unusable executables report their path and
  recovery instructions instead of an unknown error, including if Java becomes unavailable after validation.
  Automatically installed Mojang runtimes retain their existing post-install validation; Java selections are not
  silently changed (LAUNCHER-GA).
- On Linux, modern mod loader installation warns when the selected processor Java uses zlib-ng, which can produce
  different hashes for equivalent generated JAR contents. The warning offers Cancel or Continue with reduced
  verification for that installation only, with technical details available on demand. Download and tool verification
  remain enabled, and no Java or verification settings are saved (LAUNCHER-GB).

## [v4.0-1158] - 2026-09-16

### Changed

- Routine dependency upkeep: refreshed bundled libraries (Guava, Apache Commons Codec, Google HTTP Client, SLF4J,
  zstd-jni, and the Sentry crash-reporting SDK) and build tooling. The command-line argument parser was also updated to
  JCommander's maintained successor while retaining Java 8 compatibility.

### Fixed

- When macOS cannot start a modpack's Java runtime because of an incompatible CPU architecture, the launcher now shows
  recovery instructions instead of generic Java validation advice. On Apple Silicon, the message explains that Intel
  Java requires Rosetta and provides its installation command, plus guidance for selecting a compatible Java runtime if
  Rosetta cannot be installed. Minecraft is not started, and saved Java settings are left unchanged (LAUNCHER-G2).

## [v4.0-1139] - 2026-09-15

### Fixed

- Library download progress displays the library's Maven coordinate instead of its temporary `.installer-*.tmp` staging
  filename.

## [v4.0-1138] - 2026-09-15

### Added

- Launch-time memory warnings on Windows, Linux, and macOS warn when estimated available physical memory is below the
  selected heap plus 1 GiB of advisory headroom. Estimates account for reclaimable cache where supported. Players can
  cancel or launch anyway; the warning never lowers the heap or changes saved settings. Unavailable estimates do not
  block launching.
- The development command `./gradlew previewMemoryWarning` opens the real memory warning with simulated readings,
  without generating memory pressure, changing settings, or starting Minecraft.

### Changed

- Windows Java discovery reads Unicode registry values directly instead of starting `reg.exe`, preserving vendor
  searches and both registry views. Discovered executables are still validated by running Java.

### Fixed

- macOS host architecture detection queries `hw.optional.arm64`, allowing an x64 launcher under Rosetta to select native
  Apple Silicon Mojang runtimes. Missing or failed queries retain JVM-based detection, and components without an ARM
  runtime retain the existing Intel fallback.
- Windows host architecture detection now uses native APIs instead of relying on the launcher JVM's architecture and
  inherited WOW64 environment variables. Mojang runtime selection recognizes ARM64 hosts even under emulation;
  game-native selection still follows the selected game JVM.
- OS version-range rules now use the full native Windows version, including the build number, so supported Windows
  releases select Minecraft's ZGC defaults correctly even under older launcher Java runtimes. Range minima are inclusive
  and maxima are exclusive, preventing conflicting collectors at a shared boundary; legacy OS-version regexes are
  unchanged.
- Deleting a modpack no longer crashes when no pack is selected, and clearing the pack list hides the delete action
  until a pack is selected again (LAUNCHER-FP).
- Modpacks retain their identity across metadata refreshes and deletion, preventing null-name crashes during asset
  cleanup and image loading. Entries without a usable identity are not loaded, and deletion checks its identity and
  asset path before touching installed files (LAUNCHER-FQ, LAUNCHER-FS).

## [v4.0-1137] - 2026-09-14

### Added

- Solder builds can override the automatically selected Mojang Java runtime using `java_runtime`. With **Use Mojang Java
  runtimes** enabled, the exact component takes precedence over Minecraft and version-patch metadata and is used for
  both the game and installer processors. Missing or null overrides preserve automatic selection; manually selected Java
  installations are unchanged when the option is disabled. An unavailable runtime reports an installation error instead
  of silently falling back.
- Launch attempts using a Solder Java runtime override now log the pack, build, component, actual Java version, and
  executable path before calling the game launcher, so the selected override is recorded even if launch fails.
- Optional `-platformApiUrl` and `-solderApiUrl` startup arguments allow local API testing without changing production
  defaults. A loopback-only Platform fixture script supports discovery against real local Solder builds, with
  process-local install/run statistics.

### Fixed

- Fresh library downloads no longer report a SHA-1 mismatch for their empty staging file before downloading starts.
  Downloaded content is still verified before publication, and corrupt downloads still fail installation.

## [v4.0-1136] - 2026-09-10

### Changed

- Modern Forge and NeoForge loaders are now installed before Minecraft starts, without ForgeWrapper. The launcher
  preserves the loader's native JVM arguments, runs installer processors with the selected game Java runtime, and
  verifies generated files when the installer provides hashes. Ordinary shared-library acquisition copies into a
  dedicated repository while retaining its sources. Some upstream processor recipes still require network access on
  repeat installations.
- A one-time startup migration moves canonical Maven libraries from the old mixed `cache/` into `libraries/`, deleting
  old copies only after verifying the destination. Identical copies are deduplicated; differing destinations and
  unrelated cache contents, including legacy FML libraries, are preserved. Partial I/O failures retry at the next
  startup without preventing the launcher from opening. Older launcher versions may need to redownload moved libraries.

### Fixed

- Authentication-triggered modpack refreshes now run on Swing's event thread, preventing a concurrent-modification crash
  while the modpack list is being sorted (LAUNCHER-5V).
- Downloaded Java runtimes are now validated before being selected for a pack. If the runtime cannot report its version,
  vendor, or architecture, installation stops with its executable path and available probe output instead of crashing
  during library selection with a null architecture (LAUNCHER-W). The error also explains how to select a compatible
  Java installation manually. Failed downloaded-runtime probes are reported to Sentry with the executable path, process
  startup exception or exit code, and at most the last 4,096 characters of probe output—not a full launcher log.
  Successful probes, invalid custom Java selections, and cancellation do not produce these reports.
- Repeated modpack launches can now reuse completed Forge/NeoForge processor work when the installer omits output
  hashes, including Forge 1.16.1's mappings extraction and remapping. The first successful run records SHA-256
  fingerprints; later launches recheck the recipe, runtime, inputs, and generated files before skipping. Changed or
  missing files trigger execution again. Existing files are never adopted as their own checksum baseline, and processors
  with untrackable arguments still run.
- Missing library files on a mirror (HTTP 404) no longer trigger repeated download attempts and warning stack traces
  before the installer tries the next source. Transient failures still retry, and installation still fails with the
  attempted sources if no mirror can supply the file.
- The **Discover** tab loads correctly on Java 8 again. Image requests now identify themselves as launcher traffic,
  preventing refused images from forcing the tab to show its built-in offline page.
- The **Discover** tab now shows its last successfully downloaded copy when the page cannot be fetched, instead of
  skipping straight to the built-in offline page.
- The launcher console now wraps exceptionally long unbroken text, such as the full classpath for a large modpack,
  instead of allowing a single line to grow tens of thousands of pixels wide. On Linux, lines that exceeded the XRender
  coordinate range could fold distant characters back over the visible text and appear badly garbled; the console now
  keeps the rendered view within its viewport while preserving the original text for copying.

## [v4.0-1133] - 2026-07-06

### Changed

- The launcher no longer sends its **Client ID** to modpack Solder servers by default. This per-install identifier (the
  one shown under Launcher Options) used to be attached to every Solder request; it is now omitted unless you opt in for
  a specific modpack. If you are a beta tester, or otherwise have access to builds a pack's Solder only offers to
  recognized clients, open that pack's **Modpack Options** and tick **"Send launcher client ID to this pack's Solder
  server"** to receive those builds again. The setting is off for every pack until you turn it on (including packs you
  already have installed) and is remembered per modpack. Turning it on takes effect immediately: the launcher re-checks
  that pack's Solder with the identifier attached, so newly available builds appear without a restart. As a convenience,
  when you select a Solder-backed pack that shows no available builds, the launcher offers to enable the option for that
  pack so it can look for builds that require the identifier. The public modpack listing on the Modpacks tab no longer
  sends the identifier at all.

## [v4.0-1131] - 2026-07-05

### Added

- Deleting a modpack now asks what you want to do with your world saves. If the pack has any, the confirmation offers
  **Delete everything**, **Keep world saves**, or **Cancel**: keeping them leaves the pack's `saves` folder on disk, and
  reinstalling the same pack later picks your worlds up again automatically. Packs without saves get the same yes/no
  confirmation as before. This applies to both delete buttons (the modpack page and the pack options dialog).

## [v4.0-1130] - 2026-07-05

### Changed

- Routine dependency upkeep: bundled libraries (zstd-jni and the Sentry crash-reporting SDK) and the build tooling were
  refreshed to current upstream releases. Maintenance updates only; no behaviour changes for end users.

## [v4.0-1120] - 2026-06-15

### Added

- The launcher's log console now has an **Auto-scroll** toggle in its right-click menu, enabled by default. Leave it on
  to keep following the newest line as the console always has; turn it off to scroll up and read earlier output without
  new log lines pulling you back to the bottom, then turn it back on to jump to the latest and resume following.

### Changed

- The log console is far lighter on the CPU when a modpack or the game is logging heavily. It now refreshes at a capped
  frame rate instead of repainting on every burst of output, and does less redundant work per line, cutting the
  console's processor use by roughly 40-60% under a fast log stream (on slower machines, a chatty pack no longer turns
  the console window itself into a noticeable drain). Auto-scroll was also reworked to follow the newest line directly
  through the scrollbar instead of a text-layout calculation that could occasionally fail under heavy load, so following
  the latest output is more reliable as well.
- Routine dependency upkeep: bundled libraries (zstd-jni, SLF4J, Maven Artifact, and the Sentry crash-reporting SDK) and
  the build tooling were refreshed to current upstream releases, and the launcher dropped its Joda-Time dependency in
  favour of the JDK's built-in `java.time`. Maintenance and bug-fix updates only; no behaviour changes for end users.

### Fixed

- Searching for a modpack by name no longer crashes when a locally installed pack has no online listing. Local-only
  packs are excluded from name-search results; online and default packs remain searchable.

## [v4.0-1098] - 2026-05-10

### Fixed

- Modpacks hosted on third-party Solders that don't set a recommended or latest build no longer require manual
  intervention to install. When the modpack has exactly one build available, the launcher now falls back to that build
  automatically; when there are multiple builds and the Solder hasn't picked a default, the launcher shows a clear "open
  the modpack settings and pick a specific build" error from the install dialog. Previously these modpacks failed at
  install time with no automatic resolution path (and in build 1090 specifically, the failure was a silent
  install-thread crash before the graceful error path was restored in 1097).
- Mod resources with characters in their filenames or paths that are technically illegal in URLs per RFC 3986 (most
  commonly square brackets in Pixelmon-style filenames like `[solder]ThePixelmonOST.zip.zip`, but also spaces and other
  reserved characters) now download via a strict-URI parser that percent-encodes the path before fetching. Previously
  these URLs only worked because of leniency in Java's deprecated URL parser; the launcher now handles them via the
  strict path while still accepting the upstream URL as published. URLs that are already strictly valid pass through
  unchanged, and URLs that arrive partially or fully percent-encoded are preserved without double-encoding.
- Opening a modpack's settings dialog no longer crashes with a `NullPointerException` when the modpack's persisted build
  is null (most commonly after a partial install or manual edit of `installedPacks.json`). The dialog now treats a null
  build as the default "Recommended" selection and writes that back to `installedPacks.json` immediately, self-healing
  the corrupt state.

## [v4.0-1097] - 2026-05-09

### Fixed

- A class of installs that began crashing in build 1090 with a `NullPointerException` deep inside `URLEncoder.encode`
  now fails gracefully with a "build can't be loaded" error from the install dialog. The crash occurred when the
  launcher tried to install a modpack with no selected build (most often after a modpack maintainer removed the
  launcher's previously-cached build name from their Solder, leaving the launcher with a stale reference). The null
  build name reached the URL-encoding helpers introduced in 1090 and threw `NullPointerException` before the request was
  even made, terminating the install thread silently. The launcher now refuses null inputs at the URL-construction
  helpers and surfaces a recognizable "build inaccessible" error from the Solder modpack API surface, so the install
  dialog reports a meaningful failure instead of stalling. The upstream cause of the null build name itself is being
  addressed separately.

## [v4.0-1090] - 2026-05-04

### Fixed

- Modpacks hosted on third-party Solder instances whose **build identifiers contain spaces** (commonly seen on
  community-run Pixelmon Solders, where build names like `Pixelmon 9.3.14` or `1.16.5 - Oficial` are typical) now
  install reliably. The launcher was concatenating these build names directly into URL paths without percent-encoding
  the space, which depended on the upstream Solder's HTTP router being lenient enough to match the malformed path.
  Routers that strictly conform to RFC 3986 would refuse the request. The launcher now percent-encodes path segments at
  every API call site, so spaces (and other reserved characters) round-trip correctly regardless of router strictness.

## [v4.0-1089] - 2026-05-04

### Added

- Adding a Microsoft account now opens a dialog with two sign-in options at once: the existing browser-based sign-in,
  and a short device code with a QR code you can scan with your phone (or any other device with a camera) to finish
  signing in there. Either option signs you in; whichever finishes first wins. The device code path avoids the localhost
  callback entirely and works in environments where the browser-based sign-in has historically hung or been blocked
  (antivirus intercepting localhost, Windows Firewall prompts dismissed, corporate proxies, some OneDrive
  configurations). The dialog shows a countdown for the device code's 15-minute window; if it expires before you finish,
  a "Get a new code" button fetches a fresh code without having to close and reopen the dialog.

### Changed

- Routine dependency upkeep: bundled libraries (Gson, Guava, Apache Commons IO/Codec, Joda-Time with the latest tzdata,
  Maven Artifact, zstd-jni), the Sentry crash-reporting SDK, and the build tooling were refreshed to current upstream
  releases. Bug-fix and timezone-data updates only; no behaviour changes for end users.

### Fixed

- Mojang JRE component selection now recognizes the version strings Mojang actually publishes for `java-runtime-alpha`
  (`16.0.1.9.1`, `16.0.1.9.1_3`) and `jre-legacy` (`8u202`, `8u51-cacert462b08`). Previously these were skipped as
  "unrecognized version" and the launcher fell back to a hardcoded component map. The hardcoded fallback already covered
  the cases Mojang ships today, but any future runtime that uses the same dotted-with-build form would have been
  silently dropped from the live manifest. The parser now handles arbitrary-length dotted versions and the classic
  `<major>u<update>` shape.
- Disabled controls across the launcher now visibly render as disabled instead of looking identical to enabled ones.
  Styled buttons fade to reduced opacity, and the custom Width/Height fields in Options > Video now use a muted gray
  palette when the Default/Fullscreen window size is selected (the saved values stay legible, but the field clearly
  reads as non-interactive). Previously these controls ignored disabled state when drawing themselves, so any button
  temporarily locked (e.g., while an action was loading) or dimensions fields locked behind a non-Custom window mode
  gave no visual feedback about their state.
- The self-updater's progress bar no longer sits in a hard black box on top of the splash art. The bar now renders over
  a soft semi-transparent dark ribbon that blends with the icon above and keeps the white progress text legible over any
  desktop behind the translucent splash frame. Adjacent polish: the splash icon no longer flickers on cursor movement or
  clicks, the current-item label's descenders (g, p, y, j) are no longer clipped, and the splash frame is sized up-front
  to avoid clipping the icon when the asset-download phase reveals its secondary progress row.

## [v4.0-1084] - 2026-04-23

### Fixed

- When the Microsoft account sign-in folder (`%APPDATA%\.technic\oauth`) is in a damaged permission state that the
  launcher cannot reset automatically, the launcher now keeps running with a temporary in-memory credential store
  instead of crashing. A warning dialog explains which folder is affected and how to fix it manually (delete via File
  Explorer, with a take-ownership step if permissions are also broken). You will need to sign in to Microsoft every
  launch until you clean up the folder, but the rest of the launcher works as normal in the meantime.

## [v4.0-1083] - 2026-04-23

### Fixed

- Launcher auto-updates now clean up the `launcher.exe.old` backup file that is left on disk when antivirus briefly
  blocks its removal, even for launchers installed outside `%APPDATA%\.technic\` (Desktop, Downloads, portable drives —
  the common case). Previously this cleanup only scanned `%APPDATA%\.technic\`, so most users could see a leftover
  `.old` file next to their launcher until they deleted it manually.
- When an auto-update is blocked by antivirus, Windows Defender, or Windows' "Controlled Folder Access" (which protects
  Documents, Pictures, Videos, Music, and Favorites by default), the launcher now shows a specific error dialog that
  names the blocked file path and walks through how to fix it (whitelist the launcher, move it out of a protected
  folder, or re-download from technicpack.net). Previously these blocks showed the generic "unknown I/O error occurred"
  message with no actionable information.
- Microsoft account sign-in's recovery for corrupt permission state on the `%APPDATA%\.technic\oauth` folder now handles
  non-empty folders (recursive delete) and falls back to renaming the folder aside when delete is refused. When every
  recovery strategy fails, the resulting error message now tells the user which folder is affected and how to resolve it
  manually (delete via File Explorer, or take ownership via Properties > Security > Advanced). Previously the recovery
  only handled empty folders, so a folder with any saved sign-in state inside would crash the launcher with an opaque
  error.

## [v4.0-1081] - 2026-04-23

### Fixed

- Launcher auto-updates on Windows now fall back to a direct file copy when the "rename first" update strategy is
  refused by strict antivirus configurations (some antivirus products block the rename step itself with "The process
  cannot access the file because it is being used by another process"). The direct copy uses different Windows
  file-sharing flags and can succeed where the rename could not. The auto-update retry window was also extended from 5
  seconds to 20 seconds to give antivirus scans on the freshly-downloaded launcher executable more time to finish before
  giving up.

## [v4.0-1080] - 2026-04-22

### Fixed

- Auto-updates from older launcher builds no longer abort with an "Illegal char `<:>`" error before finishing. Older
  versions passed the update-target path in a URL-like form that the newer mover rejected; the mover now accepts both
  shapes, so users who hadn't updated in a while can now finish the update without re-downloading the launcher manually.
- Launcher auto-updates on Windows are more reliable when antivirus or Windows briefly holds the launcher executable
  open. The updater now renames the existing binary aside (`launcher.exe.old`) before writing the new one — Windows
  permits renaming a running executable even when a direct overwrite would fail with "The process cannot access the file
  because it is being used by another process". Any `.old` files left behind are cleaned up on the next launcher
  startup.

## [v4.0-1079-stable] - 2026-04-22

### Changed

- Crash and error reports are now sent to Technic's self-hosted Sentry instance (`sentry.technicpack.net`) instead of
  Sentry's SaaS. No change to what's reported or when.

## [v4.0-1076-stable] - 2026-04-17

### Fixed

- Orphan-file cleanup no longer reports already-removed files as deletion failures after modpack updates. Most orphans
  on a normal update are mod-version bumps whose old jars were already wiped by the pre-extraction cleanup step; those
  are now counted as "already removed" instead of "failed".

## [v4.0-1075-stable] - 2026-04-17

### Added

- Support for importing Prism Launcher instance zips (`mmc-pack.json`, per-component patches under `patches/`).
- Support for Prism-style version patches (uid, order, +jvmArgs, +tweakers, MMC-hint, compatibleJavaMajors).
- Orphan cleanup on modpack updates: files extracted from removed mods are now deleted between updates, tracked via
  `bin/extractedFiles.json`.
- ARM native library support: arch-specific classifiers (`linux-arm64`, `windows-arm64`, etc.) now resolve correctly on
  ARM CPUs.
- Mojang JRE component selection now reads the live JRE manifest, so newly published runtime components (e.g., a future
  `java-runtime-zeta`) are picked up automatically.

### Changed

- **First-run config migration:** existing `config/` directories are now moved to a timestamped
  `config-backup-YYYY-MM-DD-HHmmss/` folder (with a `README.txt` explaining the move) before being reset. Previously the
  migration overwrote prior backups.
- Atomic writes for persisted launcher state. A crash mid-save no longer truncates the settings file, saved user
  sessions, the Java-installs list, the runtime-constraints file (`bin/runData`), the installed-version marker
  (`bin/version`), or the orphan-cleanup manifest (`bin/extractedFiles.json`). Previously such a crash could silently
  reset settings, log the user out, drop their Java installs, trigger spurious reinstalls, or delete files that weren't
  actually orphaned.

### Removed

- **Beta update channel.** The launcher now uses a single stable channel; the channel selector has been removed from
  Options. Existing beta installs continue to receive updates — beta requests are server-side aliased to stable, so no
  reinstall is required.

<!-- historical-releases-footer -->

---

For the full list of historical stable releases (pre-dating this file), see [HISTORY.md](HISTORY.md) or the
[Releases page](https://github.com/TechnicPack/LauncherV3/releases).

[Unreleased]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1160...HEAD
[v4.0-1160]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1158...v4.0-1160
[v4.0-1158]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1139...v4.0-1158
[v4.0-1139]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1138...v4.0-1139
[v4.0-1138]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1137...v4.0-1138
[v4.0-1137]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1136...v4.0-1137
[v4.0-1136]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1133...v4.0-1136
[v4.0-1133]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1131...v4.0-1133
[v4.0-1131]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1130...v4.0-1131
[v4.0-1130]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1120...v4.0-1130
[v4.0-1120]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1098...v4.0-1120
[v4.0-1098]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1097...v4.0-1098
[v4.0-1097]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1090...v4.0-1097
[v4.0-1090]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1089...v4.0-1090
[v4.0-1089]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1084...v4.0-1089
[v4.0-1084]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1083...v4.0-1084
[v4.0-1083]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1081...v4.0-1083
[v4.0-1081]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1080...v4.0-1081
[v4.0-1080]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1079-stable...v4.0-1080
[v4.0-1079-stable]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1076-stable...v4.0-1079-stable
[v4.0-1076-stable]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1075-stable...v4.0-1076-stable
[v4.0-1075-stable]: https://github.com/TechnicPack/LauncherV3/releases/tag/v4.0-1075-stable

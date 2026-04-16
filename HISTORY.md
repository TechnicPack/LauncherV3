# Release History

Auto-generated log of every stable release, reverse-chronological.
Sourced from the Technic Jenkins build server (https://jenkins.technicpack.net) and
GitHub Releases (TechnicPack/LauncherV3). Commits are listed as recorded in git —
**this is not a curated changelog** (see [CHANGELOG.md](CHANGELOG.md) for that).

Per-release pages with downloadable artifacts (where available) are on the
[Releases page](https://github.com/TechnicPack/LauncherV3/releases).

---

## [v4.0-1072-stable] - 2026-04-01

### Changes since v4.0-1065-stable

- fix(ci): correct dependabot ecosystems (6bb94c5)
- build(deps): lock file maintenance (474c62c)
- fix(ui): correct relative time showing "A Minute Ago" for all same-day posts (7ca473c)
- feat(ci): create GitHub releases when promoting builds (31a6115)
- fix: add bounds check in isLegacyVersion for single-segment versions (ef53d47)
- refactor: use JavaVersionComparator in RunData.isJavaVersionAtLeast (cfb0c3b)
- fix: handle short legacy Java versions in JavaVersionComparator (7e44f69)
- feat(ci): include commit hashes in release changelog (deace54)
- fix(ci): stop swallowing errors in release changelog generation (75610a8)
- feat(ci): use channel-specific release tags and auto-bump beta on stable promotion (0036df2)
- fix(ci): handle old-format beta tags in auto-bump build number parsing (ff505c0)

## [v4.0-1065-stable] - 2026-03-31

### Changes since v4.0-1033-stable

_No commits recorded between previous stable and this build._

## [v4.0-1033-stable] - 2026-01-27

### Changes since v4.0-1031-stable

- build(deps): update actions/upload-artifact action to v6 (2197c6d)
- build(deps): update dependency org.apache.maven:maven-artifact to v3.9.12 (74a13cc)
- fix: add workaround for window dragging in Cosmic DE (95e6637)

## [v4.0-1031-stable] - 2025-12-16

### Changes since v4.0-1030-stable

- fix: provide a helpful error message when the OS blocks a process (0a8ace5)

## [v4.0-1030-stable] - 2025-12-12

### Changes since v4.0-1028-stable

- fix: fix console fallback font name (09493c1)
- refactor: remove unused things in ConsoleFrame (333ff1d)
- refactor: reword return description (86395b6)
- fix: rewrite file logging to be async (ff3a98f)
- fix: fix message order when switching to new log file (4c678d4)

## [v4.0-1028-stable] - 2025-12-04

### Changes since v4.0-1014-stable

- build(deps): update dependency commons-io:commons-io to v2.21.0 (5bc94a1)
- chore: add changelog URL for commons-codec package (9fd9a42)
- build(deps): update dependency commons-codec:commons-codec to v1.20.0 (3d8666a)
- build(deps): update dependency io.sentry:sentry to v8.25.0 (8c623e8)
- build(deps): update dependency io.sentry:sentry to v8.26.0 (c7e3749)
- build(deps): update dependency org.apache.maven.plugins:maven-jar-plugin to v3.5.0 (3dc3d76)
- build(deps): update actions/checkout digest to 93cb6ef (5eebbb1)
- build(deps): update dependency org.tukaani:xz to v1.11 (b7a16be)
- chore: add changelog URL for commons-lang3 package (d302d44)
- build(deps): update dependency org.apache.commons:commons-lang3 to v3.20.0 (f03efcb)
- ci: re-enable Dependabot updates (981d59a)
- build(deps): update dependency io.sentry:sentry-maven-plugin to v0.10.0 (320d046)
- build(deps): update actions/checkout action to v6 (498c0c9)
- build(deps): update dependency io.sentry:sentry to v8.27.0 (86a59fd)
- ci: add minimumReleaseAge to Renovate configuration (ac038e3)
- ci: actually set minimumReleaseAge (09e82de)
- build(deps): update dependency io.sentry:sentry to v8.27.1 (b4b259f)
- feat: update the certificate store with a full one from OpenJDK (9ccf647)
- build(deps): update dependency org.apache.maven.plugins:maven-assembly-plugin to v3.8.0 (ecd53f4)
- build(deps): update actions/checkout digest to 8e8c483 (77270b4)
- build(deps): update actions/stale digest to 9971854 (ab4fa1c)
- ci: set internalChecksFilter (1385420)
- fix: synchronize InstalledPackStore operations (a49e984)
- chore(deps): bump actions/setup-java from 5.0.0 to 5.1.0 (dead8b0)
- fix: ensure that installedPacks is flushed before moving (a9a1985)

## [v4.0-1014-stable] - 2025-10-28

### Changes since v4.0-1004-stable

- build(deps): update actions/stale action to v10 (067f9e2)
- build(deps): update dependency org.jetbrains:annotations to v26.0.2-1 (8844311)
- feat: add cached response for the Mojang JRE index (9e16096)
- build(deps): update dependency io.sentry:sentry to v8.24.0 (1114510)
- build(deps): update dependency org.apache.commons:commons-compress to v1.28.0 (389d361)
- build(deps): update actions/checkout action to v5 (2cc06e8)
- build(deps): update dependency com.google.code.gson:gson to v2.13.2 (ada8258)
- build(deps): update actions/setup-java action to v5 (f9e158b)
- build(deps): update dependency io.sentry:sentry-maven-plugin to v0.9.0 (3b7d1fd)
- build(deps): update dependency org.apache.maven.plugins:maven-shade-plugin to v3.6.1 (7bdaa87)
- build(deps): update dependency com.google.guava:guava to v33.5.0-jre (2b3a544)
- build(deps): update actions/upload-artifact action to v5 (c22eef3)
- build(deps): update dependency org.apache.commons:commons-lang3 to v3.19.0 (408c09b)
- build(deps): update dependency org.apache.maven.plugins:maven-enforcer-plugin to v3.6.2 (b1040f1)
- build(deps): update dependency com.github.luben:zstd-jni to v1.5.7-6 (3e5320a)
- build(deps): update actions/stale digest to 5f858e3 (b5c7167)
- build(deps): update dependency org.apache.maven.plugins:maven-compiler-plugin to v3.14.1 (5b05ee2)
- build(deps): update google-http-client to v2 (95a3c05)

## [v4.0-1004-stable] - 2025-08-11

### Changes since v4.0-1003-stable

- fix: disable automatic autoscroll disabling for now (943af61)

## [v4.0-1003-stable] - 2025-08-11

### Changes since v4.0-1002-stable

- fix: fix modpack cache not saving correctly (921cdc5)

## [v4.0-1002-stable] - 2025-08-10

### Changes since v4.0-999-stable

- feat: write .version file for Mojang JREs (c6b7780)
- revert: "feat: write .version file for Mojang JREs" (b78b624)
- fix: fix file logging not working (ed2dfd0)

## [v4.0-999-stable] - 2025-08-10

### Changes since v4.0-998-stable

- fix: fix link resolution logic for Mojang JREs (0697ded)

## [v4.0-998-stable] - 2025-08-10

### Changes since v4.0-985-stable

- fix: use interface List instead of implementation ArrayList (8640a43)
- refactor: mark Resource.getMd5() as nullable (aac985b)
- refactor: mark PackInfo.getDiscordId as nullable (a7dee92)
- build(deps): update dependency commons-codec:commons-codec to v1.19.0 (328e7e8)
- fix: change JSON parsing to read directly from file (3383c07)
- refactor: remove IUserStore interface (649665e)
- refactor: use java.nio for UserStore (f05ef51)
- refactor: remove LauncherDirectories interface (107a049)
- refactor: replace usage of Serializable with warning suppression (ac3eaa0)
- refactor: read process output in chunks instead of line-by-line (c1c4fcb)
- refactor: remove IInstalledPackRepository interface (41478d3)
- fix: remove random launcher -Xmx property (8da9c0a)
- fix: remove "final" from JSON data classes (047cdde)
- refactor: fix indentation on CompleteVersionV21 (0d772e6)
- refactor: simplify Minecraft version.json processing (eec6900)
- style: reformat ArgumentList (b3e6363)
- fix: clean up relaunch arguments (6118106)
- refactor: ensure that launchAction is never null (756a495)
- refactor: de-duplicate Platform pack version logic (4a5dc4c)
- refactor: clarify RunDataDialog result enums (ad2ea02)
- refactor: misc cleanup (35ba17c)
- refactor: fix Minecraft version.json processing (d31ad68)
- refactor: fix missed overrides on InstalledPackStore (457bde8)
- refactor: tiny improvement in isPortable (985d207)
- build(deps): update dependency org.apache.commons:commons-text to v1.14.0 (2804178)
- build(deps): update dependency commons-io:commons-io to v2.20.0 (696591b)
- build(deps): update dependency io.sentry:sentry-maven-plugin to v0.7.1 (602dbdf)
- build(deps): update dependency org.apache.maven.plugins:maven-enforcer-plugin to v3.6.1 (a7f410d)
- build(deps): update dependency org.apache.maven:maven-artifact to v3.9.11 (f4a895d)
- refactor: de-duplicate strings in Rule (4ac9fba)
- refactor: add explanation to ArgumentListAdapter (3330909)
- refactor: throw JsonSyntaxException instead of the more generic one (0d5f72e)
- refactor: rewrite MoveLauncherPackage (54d2496)
- fix: remove usage of deprecated URL constructor (59f4eaa)
- refactor: migrate LauncherFileSystem to use Path (bde61df)
- fix: fix Gson exception handling order (a25d665)
- fix: add workaround for "can't find file" first run warnings (001e983)
- refactor: simplify exceptions to "e" (bb33257)
- refactor: remove useless Sentry exception log (de83813)
- fix: fix some threading issues (b55d014)
- fix: fix font loading (413fb8d)
- fix: use correct Swing EDT functions (41833ac)
- refactor: reorganize QueryUpdateStream (58ceefd)
- refactor: optimize imports on the entire codebase (c272c67)
- fix: fix some threading issues on Installer (0e31fa1)
- fix: remove usage of deprecated Locale constructor (2cc4d9c)
- revert: "fix: remove usage of deprecated URL constructor" (57f8c13)
- fix: fix IllegalArgumentException with Forge (2a97def)
- fix: fix race condition in game process exit handler (572b181)
- fix: don't serialize install_profile into MinecraftVersionInfo (8410c6d)
- revert: "fix: set up play button without refreshing all packs" (a003ec7)
- perf: improve TaskGroup.getTaskDescription speed (e416c87)
- feat: improve console performance under load (052a090)

## [v4.0-985-stable] - 2025-07-15

### Changes since v4.0-983-stable

- build(deps): update dependency io.sentry:sentry-maven-plugin to v0.7.0 (a95235f)
- fix: fix NPE in install when a non-empty folder is selected (2b0308f)

## [v4.0-983-stable] - 2025-07-13

### Changes since v4.0-978-stable

- build(deps): update dependency org.apache.maven.plugins:maven-enforcer-plugin to v3.6.0 (7812d01)
- build(deps): update dependency com.github.luben:zstd-jni to v1.5.7-4 (83663e1)
- build(deps): update dependency io.sentry:sentry-maven-plugin to v0.6.0 (9d5974a)
- build(deps): update dependency org.apache.commons:commons-lang3 to v3.18.0 [security] (ae869af)
- build(deps): update dependency io.sentry:sentry to v8.17.0 (5437f92)

## [v4.0-978-stable] - 2025-07-01

### Changes since v4.0-975-stable

- fix: set up play button without refreshing all packs (aa82937)
- refactor: make shouldRequestInstall more readable (c52fb37)
- refactor: rename Version to ModpackVersion (f157d09)
- fix: fix possible NPE after loading pack Solder cache (5f26899)
- refactor: fix NPE warning (d7fcafe)
- fix: re-do console log handler so it formats messages properly (1d14f9e)
- fix: fix last task in the queue not having any progress (c1974c9)
- fix: prevent exception with the console context menu (2077fa7)
- feat: hack in progress for Minecraft jar manipulation (7b06bb4)

## [v4.0-975-stable] - 2025-06-30

### Changes since v4.0-973-stable

- fix: that timeout call is in the wrong place (a235e31)
- perf: speed up downloads and reduce system calls (d84dcb8)

## [v4.0-973-stable] - 2025-06-30

### Changes since v4.0-971-stable

- fix: some processes don't require env var cleanup (01a7b40)
- fix: protect against null replacement strings (4d407de)
- fix: fix "null" exception when pack info isn't available (7db0cbd)
- fix: catch exception when Minecraft jar creation is cancelled (170a44d)
- fix: simplify task queue progress, which probably fixes a few bugs (8650a6b)

## [v4.0-971-stable] - 2025-06-29

### Changes since v4.0-970-stable

- feat: add launcher path to Sentry debug info (481c9e3)

## [v4.0-970-stable] - 2025-06-28

### Changes since v4.0-969-stable

- fix: try to obtain more information about strange os.arch bug (c5d7773)

## [v4.0-969-stable] - 2025-06-28

### Changes since v4.0-968-stable

- fix: ignore AWT exceptions when initializing the properties (bcfdc44)

## [v4.0-968-stable] - 2025-06-28

### Changes since v4.0-967-stable

- fix: guard against null recommended Java string (73aa33b)

## [v4.0-967-stable] - 2025-06-28

### Changes since v4.0-966-stable

- feat: add updateStream to Sentry information (8fccf12)
- fix: fix NPE on BasicScrollBarUI due to the buttons (b6d37c6)

## [v4.0-966-stable] - 2025-06-28

### Changes since v4.0-965-stable

- fix: fix "Extraction directory must be set" error (cdaacbb)

## [v4.0-965-stable] - 2025-06-28

### Changes since v4.0-948-stable

- fix: fix Minecraft jar not being properly stripped (439e3a4)
- fix: the examine java queue isn't ready for parallelism yet (2b543d0)
- fix: add workaround for oauth datastore access denied exception (7fff970)
- refactor: add comment explaining that code segment (49144a8)
- fix: fix position of early default Java runtime declaration (de6de3c)
- refactor: capture strange exception (2457bf6)
- feat(sentry): add build number as Sentry tag (39ea48c)
- fix: add some more monitoring to the strange datastore exceptions (327f216)
- fix: remove invalid classpath in launch4j config (c6b2b35)
- fix: ensure that AWT desktop properties initialize properly on Linux (894e73e)
- fix: ensure that launchCompleted only runs once (9d6a699)
- refactor: move installer thread to an inner class (16fc99b)
- fix: use a more readable font for the launcher options (d2d0579)
- fix: fix use of deprecated function in WatermarkTextField (77bf7de)
- refactor: idk what happened there (c1997f6)
- refactor: move buildTasksQueue and createVersionBuilder into InstallerThread (ecb794c)
- fix: improve cache deletion exceptions in modpack installer (36a5026)
- fix: fix potential stack overflow with regex (96a5c5a)
- refactor: remove strange generic from `installPack` (e4879f8)
- fix: ensure that longer modpack names don't break the layout (0af646f)
- refactor: reduce variables in InstallerThread (f7d21a9)
- fix: move some pack cleanup operations to the task (36e1972)
- refactor: move fml libs logic to a task (9a7dd1f)
- feat: add breadcrumbs for task queues (75941d3)
- fix: parallel task groups break the task queue (35a2e76)
- fix: clean up error handling code in Installer (1b65dfc)
- fix: synchronize all queue operations in ParallelTaskGroup (cc3d280)
- refactor: split up MC launcher code for readability (cf53b3d)
- fix: small fixes in ResourceLoader (feb8e9b)
- refactor: split up some larger tasks (aaff68d)
- fix: random fixes (0db0d30)
- build(deps): update dependency org.jetbrains:annotations to v26 (a4d766b)
- build(deps): update dependency io.sentry:sentry to v8.16.0 (f208c24)
- build(deps): update dependency com.coderplus.maven.plugins:copy-rename-maven-plugin to v1.0.1 (621f465)
- build(deps): update google-http-client to v1.47.1 (84cd08d)
- build(deps): update advanced-security/maven-dependency-submission-action digest to df268dd (566be8a)
- build(deps): update dependency org.apache.maven:maven-artifact to v3.9.10 (c33ccb4)
- feat: add Sentry generic user information (115b4ea)

## [v4.0-948-stable] - 2025-06-22

### Changes since v4.0-946-stable

- fix: actually copy the English lang file to the root one (0519f1e)
- fix: fix possible threading bug in TaskGroup (17faa12)
- refactor: canonically call default Java runtime (c5dd1ab)
- refactor: remove duplicate variable in buildCommands (c19daee)
- fix: validate osArch as well in FileBasedJavaRuntime (547d87a)
- refactor: set the default Java runtime at an earlier point (5d65fa6)
- feat: add Sentry (3f3ff87)

## [v4.0-946-stable] - 2025-06-21

### Changes since v4.0-919-stable

- build(deps): update google-http-client to v1.47.0 (7af6074)
- build(deps): update dependency com.github.luben:zstd-jni to v1.5.7-2 (1c70c8f)
- build(deps): update dependency com.google.guava:guava to v33.4.8-jre (8e2d2df)
- build(deps): update slf4j monorepo to v2.0.17 (d408233)
- build(deps): update dependency commons-codec:commons-codec to v1.18.0 (a3d29ca)
- build(deps): update actions/setup-java digest to c5195ef (e2422d7)
- build(deps): update advanced-security/maven-dependency-submission-action digest to 4bf8a28 (423862d)
- build(deps): update dependency com.akathist.maven.plugins.launch4j:launch4j-maven-plugin to v2.6.0 (809c4d1)
- build(deps): update dependency com.google.code.gson:gson to v2.13.1 (b2d4a48)
- build(deps): update google-oauth-client to v1.39.0 (2ace25b)
- build(deps): update dependency org.apache.maven.plugins:maven-compiler-plugin to v3.14.0 (41bd948)
- build(deps): update dependency joda-time:joda-time to v2.14.0 (98792b2)
- ci(renovate): fix commons-io changelog (ce994d2)
- build(deps): update dependency org.apache.commons:commons-text to v1.13.1 (d478c9b)
- build(deps): update dependency commons-io:commons-io to v2.19.0 (94eb474)
- build(deps): update dependency com.github.luben:zstd-jni to v1.5.7-3 (3e88123)
- build(deps): update advanced-security/maven-dependency-submission-action digest to fe8d4d6 (c17e564)
- refactor: remove outdated try-catch and comment (3d4d1fc)
- feat: completely rewrite JRE handling to allow vendor (9ab650d)
- fix: fix possible bug in `Utils.getProcessOutput` (3b16fa5)
- fix: fix up FileBasedJavaRuntime for Gson usage (fadd7c3)
- perf: improve FileJavaSource loading and saving (9842266)
- fix: fix Java runtimes list serialization and deserialization (a7d8b14)
- style: remove unused imports (aa6f821)
- refactor: misc cleanup (a2f6fa4)
- perf: run oxipng on png images (a9ea014)
- refactor: remove unused background images (41844f5)
- fix: improve alignment of discover fallback error (c508845)
- refactor: clean up code (1ae034d)
- feat: add asset name to the "verifying assets" message (23bbd97)
- perf: increase download decompression buffer size to 64 KiB (0a0b045)
- fix: set log cleanup thread as a background task (da01c34)
- refactor: more cleaning up (e3c42d1)
- feat: darken progress bar a bit (5e66dac)
- feat: add download speed and download time logging (35bbfd1)
- refactor: more cleanup (03b9a23)
- fix: properly hook up Mojang JREs (4d6c1b5)
- refactor: more cleanup (ac4762e)
- refactor: cleaning up more code (536c938)
- fix: exclude console window from modal-related blocking (7274035)
- refactor: more cleaning up (9f295d2)
- build: automatically clone root bundle from the English one (cc467fe)
- feat: move MSA Java check strings to the lang file (ade985a)
- fix: update Java installation instructions (55f4c60)
- fix: handle exceptions in getCertificateFingerprint (f6a9192)
- build: remove redundant resource operation (7d94741)
- feat!: remove beta-specific update logic and add skipUpdate argument (2f121da)
- fix: show console earlier in the startup process (2e561a7)
- refactor: fix raw use of generic class in ImageRepository (46091ea)
- fix: fix scrollbars not being themed properly (dc5208f)
- fix: update Discord guild endpoint and refactor code (0d1d8f7)
- fix: ensure that all processes have safe environment variables (2019954)
- chore: add .editorconfig file (bee614f)
- fix: fix logic issue in `DownloadFileTask.setDecompressor` (64c1793)
- fix: add error handling during launcher startup (25cedc2)
- fix: properly check decompressor validity (3ac899e)
- fix: use the correct build number source (9ae2829)
- feat: add headless mode check (9c649f9)
- refactor: simplify getRunningPath (f761935)
- fix: speed up startup DNS debug (2932971)
- fix: speed up Minecraft assets verification (52cc857)
- refactor: move colors to a new class (132fdd9)
- refactor: simplify user serialization (e9a8d02)
- fix: speed up Mojang JRE file checks (1d41814)
- refactor: misc cleanup (c0ccb3f)
- refactor: fix raw use of parameterized classes (caf90f5)
- refactor: misc cleanup (f8387b0)
- fix: fix busy loop during downloads (5ced555)
- revert: build: remove redundant resource operation (9ae45dd)
- refactor: merge both Relauncher classes into one (1941eb0)
- fix: correct locale used for locale-insensitive operations (e9db409)
- fix: improve alignment of modpack tags text (c36e7a6)
- fix: fix slow update of download progress (5f21cb4)
- fix: fix assembly of updater launch arguments (3b429f6)

## [v4.0-919-stable] - 2025-04-17

### Changes since v4.0-917-stable

- feat(msa): open "edit profile" page when user has no Minecraft profile (cd470b3)

## [v4.0-917-stable] - 2025-04-17

### Changes since v4.0-910-stable

- build(deps): update actions/upload-artifact digest to ea165f8 (1d8a7d4)
- build(deps): update actions/stale digest to 5bef64f (787ab7c)
- build(deps): update actions/setup-java digest to 3a4f6e1 (70ee35b)
- chore(msa): yagni (7a12470)
- refactor(msa): move request logging into factory (0af95e9)
- fix(msa): use player UUID to store credentials (b44d34d)
- fix(msa): refresh Minecraft profile when refreshing the session (bbb034f)
- feat(msa): check entitlements (game ownership) (4f9d645)
- feat(msa): show specific message when account has no Minecraft profile (4d30c07)
- build: fix default goal so it doesn't pollute the Maven cache (d31d5e1)
- build(osx): fix executable bit on JavaAppLauncher (8bb1704)
- build: always build the Windows executable (b852171)
- fix(msa): update "no profile" error message to mention minecraft.net (baf7f40)

## [v4.0-910-stable] - 2025-02-26

### Changes since v4.0-900-stable

- chore(renovate): group Google packages (d0854cd)
- build(deps): update google-http-client to v1.45.2 (bec19fa)
- feat: support compressed launcher resources and use sha256 (e72879a)
- fix: add missing constructor (52a985f)
- feat: use LZMA downloads for Mojang JREs (73ce917)
- fix: prevent negative progress percentage when content-length is missing (3ec31aa)
- feat: display more precise progress percentage (1d3cd28)
- fix: catch exceptions when parsing runData memory value (58440a4)
- build(deps): update google-http-client to v1.45.3 (3efedd6)
- build(deps): update google-oauth-client to v1.37.0 (631abb4)
- build(deps): update dependency com.google.guava:guava to v33.4.0-jre (79bcd9a)
- build(deps): update dependency org.apache.commons:commons-text to v1.13.0 (272c77d)
- build(deps): update actions/upload-artifact digest to 6f51ac0 (9a190ca)
- build(deps): update actions/setup-java digest to 7a6d8a8 (c369b44)
- ci: ignore non-project files in build workflow (19b4b6e)
- fix: de-duplicate all libraries, ignoring versions (2f625c5)

## [v4.0-900-stable] - 2024-11-24

### Changes since v4.0-895-stable

- build(deps): update dependency com.google.http-client:google-http-client-apache-v5 to v1.45.1 (ad9775a)
- build(deps): update dependency com.google.http-client:google-http-client-gson to v1.45.1 (49d25d4)
- ci(renovate): add common-io changelog link (51776a8)
- build(deps): update dependency commons-io:commons-io to v2.18.0 (c54738b)
- fix: fix broken Minecraft Forge dependency downloads (77f67d6)

## [v4.0-895-stable] - 2024-10-28

### Changes since v4.0-831-stable

- chore(dependabot): bump PR limit (ebc368e)
- chore(dependabot): group google dependencies (e9e697a)
- ci: set up github action (dd39d70)
- build(deps): bump the google-oauth-client group with 3 updates (05d753e)
- build(deps): bump the google-http-client group with 3 updates (092b5f7)
- build(deps): bump joda-time:joda-time from 2.12.5 to 2.12.7 (4340cf4)
- build(deps): bump commons-codec:commons-codec from 1.16.0 to 1.17.0 (5beb31d)
- build(deps): bump org.codehaus.mojo:exec-maven-plugin (72b7c53)
- build(deps): bump org.apache.maven.plugins:maven-compiler-plugin (3c27fdc)
- build(deps): bump org.apache.maven.plugins:maven-jar-plugin (24c6327)
- build(deps): bump org.apache.maven.plugins:maven-assembly-plugin (0b746f3)
- build(deps): bump com.akathist.maven.plugins.launch4j:launch4j-maven-plugin (c9d3e85)
- build(deps): ignore flying-saucer-core 9.5.0+ for now (1cb2123)
- chore(dependabot): forgot a character (69e83fe)
- build(deps): bump org.xhtmlrenderer:flying-saucer-core (0189472)
- chore(stale): update version (21d0bac)
- build(deps): bump the google-http-client group with 3 updates (ace419a)
- feat: add more memory options in dialog box (#300) (c81bb0f)
- Return maximum memory option instead of returning null (#306) (eba59fb)
- Make runData labels related to Java downloads clickable (#158) (1751c9c)
- build(deps): bump org.apache.commons:commons-compress from 1.26.1 to 1.26.2 (#384) (2ef33a8)
- build(deps): bump com.google.code.gson:gson from 2.10.1 to 2.11.0 (a3595e5)
- build(deps): bump org.codehaus.mojo:exec-maven-plugin from 3.2.0 to 3.3.0 (#383) (b099280)
- build(deps): bump org.apache.maven:maven-artifact from 3.9.6 to 3.9.7 (f8e1474)
- build(deps): bump com.akathist.maven.plugins.launch4j:launch4j-maven-plugin (8b46330)
- fix: update cert store for Java older than 8u141 (45e45bd)
- ci: actually build (a98318e)
- ci: fix build number (b2234cd)
- ci: disable maven progress spam (3c4dd24)
- feat!: remove Mojang auth code (6abefed)
- chore: remove 2 unused classes (2419add)
- feat: add offline mode for MSA auth (6e42e34)
- fix: delete MSA OAuth data store if it becomes corrupted (c6ae655)
- build(deps): bump google-http-client group (bfef261)
- build(deps): bump commons-codec:commons-codec from 1.17.0 to 1.17.1 (f3e608f)
- build: enforce Maven version (518e0a9)
- fix: remove usage of deprecated ZipFile constructor (43c0da7)
- build(deps): bump org.apache.commons:commons-compress (2fe8ac2)
- build(deps): bump org.apache.commons:commons-lang3 from 3.14.0 to 3.17.0 (f53260c)
- build(deps): bump com.google.guava:guava from 33.2.0-jre to 33.3.0-jre (c4cc6ae)
- build(deps): bump org.codehaus.mojo:native2ascii-maven-plugin (8c69532)
- build(deps): bump org.apache.maven.plugins:maven-jar-plugin (15dac73)
- build(deps): bump org.codehaus.mojo:exec-maven-plugin (2c58be4)
- build(deps): bump org.apache.maven:maven-artifact from 3.9.7 to 3.9.9 (e46f2fb)
- build(deps): bump org.apache.maven.plugins:maven-shade-plugin (e442bc7)
- chore: Configure Renovate (ad1e064)
- build(deps): pin dependencies (251cc85)
- build(deps): update advanced-security/maven-dependency-submission-action digest to 4f64dda (ffa77dd)
- build(deps): update actions/setup-java digest to 2dfa201 (dfef081)
- refactor: misc code cleanup (84fc579)
- fix: remove expired root certificate DST Root CA X3 (167bebd)
- fix: import root certificate ISRG Root X2 (015ee2a)
- build(deps): update dependency joda-time:joda-time to v2.13.0 (9b2b24d)
- build(deps): update dependency org.apache.maven.plugins:maven-jarsigner-plugin to v3.1.0 (7e4292c)
- Disable Dependabot version updates (74286f2)
- build(deps): update dependency commons-io:commons-io to v2.17.0 (0ae6574)
- build(deps): update dependency com.google.guava:guava to v33.3.1-jre (15b244f)
- build(deps): update actions/setup-java digest to b36c23c (8409065)
- build(deps): update advanced-security/maven-dependency-submission-action digest to 49866fe (4ee188e)
- ci(renovate): fix changelog for launch4j-maven-plugin (edf00bd)
- build(deps): update dependency com.akathist.maven.plugins.launch4j:launch4j-maven-plugin to v2.5.2 (8409e25)
- ci(renovate): ignore flying-saucer-core for now (c03763f)
- build(deps): update actions/checkout digest to eef6144 (7be4740)
- build(deps): update actions/upload-artifact digest to 604373d (57323fd)
- build(deps): update actions/upload-artifact digest to 8448086 (527247e)
- build(deps): update actions/upload-artifact digest to 604373d (fb79608)
- build(deps): update actions/upload-artifact digest to b4b15b8 (0d6529e)
- build(deps): update actions/checkout digest to 11bd719 (68e45cf)
- build(deps): update dependency org.codehaus.mojo:exec-maven-plugin to v3.5.0 (40b1e58)
- build(deps): update actions/setup-java digest to 8df1039 (c2e7db6)
- fix: fail if launcher folders can't be created or accessed (c568522)
- fix: missed a file when patching (2d7ea40)

## [v4.0-831-stable] - 2024-05-08

### Changes since v4.0-829-stable

- feat: unify mcforge and neoforge handling (49b190a)
- fix: de-duplicate libraries in simple versions (ee60869)

## [v4.0-829-stable] - 2024-05-08

### Changes since v4.0-828-stable

- fix: fix mcforge 49.0.4+, optimize mcforge and neoforge handling (f18321c)

## [v4.0-828-stable] - 2024-05-07

### Changes since v4.0-827-stable

- fix: remove defunct auth server from startup debug (db43e51)
- fix: properly check for duplicate libraries (1cb743d)

## [v4.0-827-stable] - 2024-05-04

### Changes since v4.0-822-stable

- fix: fix support for recent Forge installers (1211034)
- feat: add WIP support for NeoForge (3338cab)
- fix: remove duplicate libraries (0fa1c7e)
- build(deps): bump org.apache.commons:commons-compress (39bd939)
- build(deps): bump com.google.guava:guava from 32.1.2-jre to 33.2.0-jre (923a3c9)
- build(deps): bump com.google.oauth-client:google-oauth-client-java6 (d8be1f2)
- build(deps): bump com.google.oauth-client:google-oauth-client-jetty (29150a8)
- build(deps): bump commons-io:commons-io from 2.13.0 to 2.16.1 (e4c718f)
- build(deps): bump org.apache.maven:maven-artifact from 3.9.3 to 3.9.6 (7a0b9e0)
- build(deps): bump org.apache.commons:commons-text from 1.10.0 to 1.12.0 (50366f3)
- build(deps): bump org.apache.maven.plugins:maven-shade-plugin (7406ee1)
- build(deps): bump org.apache.commons:commons-lang3 from 3.12.0 to 3.14.0 (ae08b31)

## [v4.0-822-stable] - 2023-10-17

### Changes since v4.0-819-stable

- fix: sanitize mod cache filenames (838a1bd)
- fix: prevent reinstall for offline modpacks (f122050)
- feat: close modpack options after triggering reinstall (359548f)
- feat: detect duplicate cache files when installing (56f5f31)

## [v4.0-819-stable] - 2023-09-02

### Changes since v4.0-812-stable

- fix: log exception when ZIP validation fails (bba077e)
- refactor: use try-with-resources in ZIP verifier (e3f0bfd)
- refactor: use possessive qualifier in MC version regex (c22f8e5)
- ci: switch to actions/stable (eb651ef)
- ci: update CodeQL config (c91d5f1)
- ci: remove custom CodeQL workflow (01b6dc3)
- ci(stale): import exempt issue labels from previous config (b995368)
- fix: disable outdated Universal Analytics code (123ad98)

## [v4.0-812-stable] - 2023-08-16

### Changes since v4.0-811-stable

- fix: show the URL in an input dialog rather than as uncopiable text (fbd7c17)
- feat: prompt user to fix the hosts file if localhost is detected during auth (1508695)
- refactor: DATA_STORE_FACTORY is not actually static (086c26f)

## [v4.0-811-stable] - 2023-08-14

### Changes since v4.0-807-stable

- fix: remove extra saves to the pack store (0b6e25e)
- fix: log stacktraces in the launcher console (b3068fd)
- fix: properly log Solder exceptions (b98c0b0)
- feat: increase console size to 75% of the launcher (4290114)
- fix: fix login button logic (7c0b263)
- fix: download FML libs for Forge 1.3.2 (62d0d9a)

## [v4.0-807-stable] - 2023-08-05

### Changes since v4.0-805-stable

- refactor: join variable declaration and assignments (9ae1b36)
- fix: remove use of deprecated ApacheHttpTransport class (82ec397)
- build: add missing dependencies (f2b2dae)
- feat: add support for NeoForge (e2df1b7)

## [v4.0-805-stable] - 2023-08-03

### Changes since v4.0-803-stable

- build(deps): bump com.google.guava:guava from 32.1.1-jre to 32.1.2-jre (669da9d)
- chore: remove unnecessary verbose flag (628b2d4)
- fix: remove outdated Mojang S3 code (9d40b14)
- fix: regenerate minecraft.jar when the modpack build changes (f4ab84b)

## [v4.0-803-stable] - 2023-07-16

### Changes since v4.0-779-stable

- chore: fix weird indentation after if statement (28909bc)
- Bump launch4j-maven-plugin from 2.1.2 to 2.4.1 (cfcdc41)
- chore: update launch4j config for the new version (2381cfc)
- Bump guava from 31.1-jre to 32.1.1-jre (26b261e)
- Bump joda-time from 2.10.14 to 2.12.5 (6df5177)
- Bump gson from 2.9.1 to 2.10.1 (76de149)
- Bump maven-assembly-plugin from 3.3.0 to 3.6.0 (f7a75ca)
- Bump maven-shade-plugin from 3.2.4 to 3.5.0 (a427790)
- Bump google-http-client from 1.42.2 to 1.43.3 (b48743e)
- Bump maven-artifact from 3.8.6 to 3.9.3 (ad09e56)
- Bump google-http-client-gson from 1.42.2 to 1.43.3 (0f5b269)
- Bump maven-jar-plugin from 3.2.2 to 3.3.0 (39cff54)
- Bump commons-compress from 1.21 to 1.23.0 (239788d)
- Bump commons-io from 2.11.0 to 2.13.0 (7879ab8)
- Bump commons-codec from 1.15 to 1.16.0 (835eb62)
- Bump maven-compiler-plugin from 3.10.1 to 3.11.0 (d0e76ec)
- chore: `${basedir}` is deprecated (52054c4)
- Fix typos in German localization (e71a3b3)
- Fix encoding by loading strings from properties files as UTF-8 instead of ISO-8859-1 (83eca28)
- fix: automatically escape UTF-8 sequences in language files (8e18d9f)
- fix: fixes Java version parsing for cert injection with OpenJDK 8 builds (02dd122)
- refactor: rename runningThread to installerThread (2724145)
- fix: prevent infinite loop if launching the game fails for any reason (2c5b466)
- build: specify version for exec-maven-plugin (27b795d)
- fix: regenerate minecraft.jar whenever necessary (915b20c)
- fix: prevent resizing of the launcher windows (9dd03ed)
- feat: show error message if default browser isn't set (6b22c25)

## [v4.0-779-stable] - 2023-07-11

### Changes since v4.0-778-stable

- fix: properly save the current selection if no old selection exists (77b9950)

## [v4.0-778-stable] - 2023-07-10

### Changes since v4.0-777-stable

- fix: avoid saving installedPacks if no changes were made (a09c034)

## [v4.0-777-stable] - 2023-06-29

### Changes since v4.0-774-stable

- fix: transform browseUrl into a SwingWorker so it doesn't freeze the UI (8d16461)
- fix: rewrite MSA login into a SwingWorker so it doesn't freeze the UI (c56c5ca)
- feat: allow cancelling MSA login (6e1c298)
- feat: add piston-meta.mojang.com to dns debug (38c9c2e)
- fix: remove erroneously committed changes (2ea377b)

## [v4.0-774-stable] - 2023-06-08

### Changes since v4.0-773-stable

- Only calculate PermGen if necessary (a164f1d)
- Refactor rule features parsing to handle quick play and unknown features (725f01b)

## [v4.0-773-stable] - 2023-05-10

### Changes since v4.0-772-stable

- Improve download error messages (00bf68a)
- Don't add a new line if the logged message already ends with one (44b705b)

## [v4.0-772-stable] - 2023-04-24

### Changes since v4.0-771-stable

- Add launchermeta.mojang.com to startup debug (4573eed)

## [v4.0-771-stable] - 2023-02-23

### Changes since v4.0-769-stable

- Centralize mod cache filename generation (c4909e3)
- Disallow path separators in mod cache filenames (1abaa62)

## [v4.0-769-stable] - 2023-02-16

### Changes since v4.0-768-stable

- Take into account empty mod version strings in Solder packs (6c7d9b8)

## [v4.0-768-stable] - 2022-11-12

### Changes since v4.0-767-stable

- Default legacy profile to false (051465a)
- Fix runData RAM/JRE settings not being applied with Mojang JREs enabled (7d05c4e)

## [v4.0-767-stable] - 2022-10-26

### Changes since v4.0-766-stable

- Fix log4j patch for upcoming releases (bf396b7)
- Tighten restrictions on reserved/restricted modpack paths (eef21dc)
- Apparently this should be final (29741cf)

## [v4.0-766-stable] - 2022-10-13

### Changes since v4.0-765-stable

- Bump commons-text from 1.9 to 1.10.0 (b151032)

## [v4.0-765-stable] - 2022-10-05

### Changes since v4.0-764-stable

- Fix bug with 64-bit JRE detection when only the system JRE exists (5a70b4c)

## [v4.0-764-stable] - 2022-10-03

### Changes since v4.0-757-stable

- Improve 64-bit JRE detection (ef9e516)
- Scan for Eclipse Foundation and Eclipse Adoptium JREs (4854009)

## [v4.0-757-stable] - 2022-07-06

### Changes since v4.0-756-stable

- Fix access token censoring (bfa0d11)

## [v4.0-756-stable] - 2022-06-24

### Changes since v4.0-753-stable

- Validate Mojang JRE paths when installing (dc1d6d8)
- Don't leave debug code in (b0ebed2)
- Downloads are installs (d873222)

## [v4.0-753-stable] - 2022-06-12

### Changes since v4.0-752-stable

- Properly add support for the Mac ARM64 Mojang JREs (330cca0)
- Fix exception when on Mac and natives don't exist (d4e0097)
- Add warning about disabling Mojang JREs (ef5e12b)

## [v4.0-752-stable] - 2022-06-11

### Changes since v4.0-749-stable

- Revert "Add untested support for the Mac ARM64 Mojang JRE" (2e3562a)

## [v4.0-749-stable] - 2022-06-10

### Changes since v4.0-747-stable

- Fix potential bug with library name reprocessing (b107b64)
- Add untested support for the Mac ARM64 Mojang JRE (23fba86)
- Improve fixing of OSX natives (dccc8f0)

## [v4.0-747-stable] - 2022-05-18

### Changes since v4.0-745-stable

- The lib repo is a constant (03e36d9)

## [v4.0-745-stable] - 2022-05-17

### Changes since v4.0-744-stable

- Add support for Forge 1.17+ (af413c7)

## [v4.0-744-stable] - 2022-04-26

### Changes since v4.0-743-stable

- Fix isLegacyVersion failing with version IDs that don't match the vanilla or Forge format (ef98a9d)

## [v4.0-743-stable] - 2022-04-25

### Changes since v4.0-742-stable

- Fix local modpacks not launching (f908d25)

## [v4.0-742-stable] - 2022-04-24

### Changes since v4.0-725-stable

- Bump guava from 31.0.1-jre to 31.1-jre (11a62af)
- Bump google-http-client from 1.41.4 to 1.41.5 (7c9505d)
- Bump google-http-client-gson from 1.41.4 to 1.41.5 (33f99ab)
- Bump maven-shade-plugin from 3.2.4 to 3.3.0 (fa305af)
- Revert "Bump maven-shade-plugin from 3.2.4 to 3.3.0" (457e79f)
- Properly chain JVM arguments (64e261c)
- Add MSA hosts to DNS debug (29edafe)

## [v4.0-725-stable] - 2022-03-21

### Changes since v4.0-723-stable

- Hotfix for Java version detection in MSA login (bd56c56)

## [v4.0-723-stable] - 2022-03-20

### Changes since v4.0-721-stable

- Correct proper value for ephemeral port (958daec)
- Add Java update info for old JREs and MSA login (8d950bb)

## [v4.0-721-stable] - 2022-03-17

### Changes since v4.0-720-stable

- Use new MSA endpoint (de44c70)
- Switch to new client ID (7222cb5)

## [v4.0-720-stable] - 2022-03-07

### Changes since v4.0-709-stable

- Work around blocking Microsoft Login on Linux (fixes #245) (e0c05ff)

## [v4.0-709-stable] - 2021-12-25

### Changes since v4.0-708-stable

- Show Microsoft login button before the Mojang login button (d4c680d)

## [v4.0-708-stable] - 2021-12-17

### Changes since v4.0-707-stable

- Update log4j patch to 2.16.0 (CVE-2021-45046) (5f3588c)

## [v4.0-707-stable] - 2021-12-13

### Changes since v4.0-706-stable

- Fix log4j vulnerability (4a4b4da)

## [v4.0-706-stable] - 2021-12-10

### Changes since v4.0-705-stable

- Another null check (53a67d9)

## [v4.0-705-stable] - 2021-12-10

### Changes since v4.0-704-stable

- Forgot a null check (f27bef3)

## [v4.0-704-stable] - 2021-12-10

### Changes since v4.0-703-stable

- Improve handling of default JVM flags (6e58c33)

## [v4.0-703-stable] - 2021-12-10

### Changes since v4.0-701-stable

- Add JVM flag to handle log4j zero-day (089d3df)

## [v4.0-701-stable] - 2021-11-23

### Changes since v4.0-695-stable

- Bump guava from 30.1.1-jre to 31.0.1-jre (c8420c4)
- Remove bin/modpack.jar on modpack reinstall/upgrade (39a8015)

## [v4.0-695-stable] - 2021-09-29

### Changes since v4.0-693-stable

- Add some debugging for physical memory failures (8330383)

## [v4.0-693-stable] - 2021-09-07

### Changes since v4.0-683-stable

- Fix downloads larger than Int.MAX_VALUE going into negative percentage (0b5e377)

## [v4.0-683-stable] - 2021-08-23

### Changes since v4.0-682-stable

- Fix infinite loop with broken wrapper command (f1c80bd)

## [v4.0-682-stable] - 2021-08-21

### Changes since v4.0-681-stable

- Add null check for MS credential on startup auth (8955d9f)

## [v4.0-681-stable] - 2021-08-18

### Changes since v4.0-680-stable

- Remove "force directory change" (294de5f)

## [v4.0-680-stable] - 2021-07-30

### Changes since v4.0-679-stable

- Revert "Take download filesizes into account for progress bar" (691907f)

## [v4.0-679-stable] - 2021-07-29

### Changes since v4.0-678-stable

- Change Mojang JRE settings verbiage (8203a8b)

## [v4.0-678-stable] - 2021-07-24

### Changes since v4.0-677-stable

- Update ForgeWrapper to 1.5.1 (6775928)

## [v4.0-677-stable] - 2021-07-22

### Changes since v4.0-671-stable

- Take download filesizes into account for progress bar (60f98c5)
- Update ForgeWrapper to 1.5.0 (ddaac21)

## [v4.0-671-stable] - 2021-07-10

### Changes since v4.0-664-stable

- Fix MacOS runtime path (034fa31)

## [v4.0-664-stable] - 2021-07-07

### Changes since v4.0-663-stable

- Show error message for AuthenticationException in new Mojang logins (9aeaa6f)

## [v4.0-663-stable] - 2021-07-04

### Changes since v4.0-656-stable

- Replace our GC JVM args with the default vanilla ones (a4107ff)
- Show auth error message on startup auth (f67bb27)
- Remove unnecessary user model add in MojangUser (87f002f)
- Fix credential refresh in MS auth (dcfde4d)
- Fix custom JVM args (c982dc0)
- Add Mojang Java runtimes (2d93efd)
- Improve Java detection on Mac (cab43a4)
- Forgot a null check (b03111e)
- Delete the runData file during updates/reinstall (eb3b8af)
- Fix Mac Java 8 Mojang JRE (5f30d66)
- Show console by default (b8e3b2b)

## [v4.0-656-stable] - 2021-07-01

### Changes since v4.0-653-stable

- Hook up custom Java arguments to the launch code (c6a945c)
- Add support for wrapper commands (b356113)
- List Oracle Java runtimes as such (de77c7c)
- Add arm64/aarch64 as a 64-bit platform (f8baff5)
- Document JavaUtils (f19177b)
- Add support for more Java runtimes on Windows (725bfd3)
- Remember to null check (e3e8eb7)

## [v4.0-653-stable] - 2021-06-29

### Changes since v4.0-652-stable

- Automatically clean up broken Java runtimes (ceb5d1c)
- Fix browser not opening in some Linux distros (77f2f22)

## [v4.0-652-stable] - 2021-06-05

### Changes since v4.0-651-stable

- Fix XSTS (2069a01)
- Add JBoss logging lib to fix the ClassNotFoundException (dae40b3)

## [v4.0-651-stable] - 2021-06-05

### Changes since v4.0-648-stable

- Revert "Add commons-logging" (7fdc646)
- Revert "Fix XSTS" (dba48ed)

## [v4.0-648-stable] - 2021-06-05

### Changes since v4.0-645-stable

- Change wording on underage Xbox accounts (7512132)
- Fix XSTS (b9aba6d)

## [v4.0-645-stable] - 2021-06-02

### Changes since v4.0-644-stable

- Remove useless launcher.css (0002c31)

## [v4.0-644-stable] - 2021-06-02

### Changes since v4.0-643-stable

- Fix globe logic in LanguageCellRenderer (a618301)
- Highlight the currently selected language (ed5a0bf)

## [v4.0-643-stable] - 2021-05-26

### Changes since v4.0-642-stable

- Removed detailed logging now that we have enough bug reports (25929b0)

## [v4.0-642-stable] - 2021-05-23

### Changes since v4.0-641-stable

- Add full request debug logging (394fc83)

## [v4.0-641-stable] - 2021-05-23

### Changes since v4.0-637-stable

- Moved boot login auth to share the same login flow as a regular login (b9f650f)
- Temp session error fix (421530a)
- Fixed exception flow for refresh logins (bb26d70)
- Fixed exception flow for new logins (935a739)

## [v4.0-637-stable] - 2021-05-23

### Changes since v4.0-638-stable

_No commits recorded between previous stable and this build._

## [v4.0-638-stable] - 2021-05-23

### Changes since v4.0-636-stable

- Fixed error handling for no minecraft account purchased (9805a6f)
- Moved boot login auth to share the same login flow as a regular login (b9f650f)

## [v4.0-636-stable] - 2021-05-23

### Changes since v4.0-634-stable

- Turned off throwing on non-200 response for MC profile (5f4a1d0)

## [v4.0-634-stable] - 2021-05-23

### Changes since v4.0-633-stable

- Added XSTS response logging too (4094ec7)

## [v4.0-633-stable] - 2021-05-23

### Changes since v4.0-632-stable

- Added even more debug info for XSTS logins (eea8843)

## [v4.0-632-stable] - 2021-05-23

### Changes since v4.0-631-stable

- Added log stack trace debugging to auth (da93cb2)

## [v4.0-631-stable] - 2021-05-23

### Changes since v4.0-625-stable

- Added prototype implementation of Microsoft auth login (1e80b94)
- Added major UI pass (24efaf9)
- Put random client UUID back and Changed OAuth Port (f91defc)
- Adding exception and edge case handling flow (4e0467a)
- Fixed issues with login saving/refreshing (8e9a1ba)
- Fix user_type and user_properties for MSA (7f0da3a)
- Switched user.json microsoft user type symbol (50f05f6)
- Fix issue where string wasn't updated (2160527)

## [v4.0-625-stable] - 2021-03-21

### Changes since v4.0-617-stable

- Bump launch4j-maven-plugin from 1.7.25 to 2.0.1 (9a4c716)

## [v4.0-617-stable] - 2021-02-12

### Changes since v4.0-615-stable

- Fix second --tweakClass getting removed with Forge + LiteLoader (8d257d7)

## [v4.0-615-stable] - 2021-02-06

### Changes since v4.0-611-stable

_No commits recorded between previous stable and this build._

## [v4.0-611-stable] - 2021-01-11

### Changes since v4.0-608-stable

- Bump joda-time from 2.10.8 to 2.10.9 (33e35d5)

## [v4.0-608-stable] - 2020-12-02

### Changes since v4.0-607-stable

- These are unused (326efa7)
- Add Akliz referral (b3e5ecf)

## [v4.0-607-stable] - 2020-11-17

### Changes since v4.0-603-stable

- Try to fix installedPacks empty file issue (2485e3c)
- Prevent special modpack files from being overwritten (f4322a6)

## [v4.0-603-stable] - 2020-11-05

### Changes since v4.0-602-stable

- I hate race conditions (eacc849)
- Move debug messages and root cert injection before launching (3294729)

## [v4.0-602-stable] - 2020-11-05

### Changes since v4.0-601-stable

- More HTTPS (ce8daf1)

## [v4.0-601-stable] - 2020-10-26

### Changes since v4.0-591-stable

- Bump joda-time from 2.10.6 to 2.10.7 (d1ab35b)
- Bump joda-time from 2.10.7 to 2.10.8 (c075616)

## [v4.0-591-stable] - 2020-08-21

### Changes since v4.0-590-stable

- Fix modpack build manual selection NPE (4de1024)

## [v4.0-590-stable] - 2020-07-28

### Changes since v4.0-588-stable

- Update ForgeWrapper to 1.4.2 (2ecdc03)
- Fix typos in ChainVersionBuilder (c2136ed)
- Bump commons-text from 1.8 to 1.9 (91b2b7d)

## [v4.0-588-stable] - 2020-07-23

### Changes since v4.0-587-stable

- Bring back the FML arguments so previous jars still work (618327f)

## [v4.0-587-stable] - 2020-07-23

### Changes since v4.0-583-stable

- More Solder-related fixes (a8e6e37)
- Revert minecraft.jar creation changes so Forge stops complaining (9f001fa)

## [v4.0-583-stable] - 2020-07-09

### Changes since v4.0-582-stable

- Fix image downloads randomly failing (f573b98)

## [v4.0-582-stable] - 2020-07-09

### Changes since v4.0-580-stable

- Fix minecraft.jar Forge crash (5ee396e)

## [v4.0-580-stable] - 2020-07-09

### Changes since v4.0-568-stable

- Clean up usage of UserModel (1dc784b)
- Validate asset indexes using their SHA-1 hash (9b687ee)
- Remove unused code in utils (978e2b0)
- Improve minecraft.jar creation (b59c38f)
- Apparently FieldCanBeLocal is redundant (a983bf7)

## [v4.0-568-stable] - 2020-06-30

### Changes since v4.0-566-stable

- Rename InstallModpackTask to CleanupAndExtractModpackTask (4d96154)
- Refactor ForgeWrapper-related code (6720d0b)
- Add SHA-1 validation for libraries (347696d)
- Clean up some old client download code (9dc541c)
- Add SHA-1 validation to the Minecraft JAR (c62ad84)
- Deprecate old S3 Minecraft bucket (697311b)

## [v4.0-566-stable] - 2020-06-26

### Changes since v4.0-564-stable

- Properly clean up the bin folder (61cf5de)

## [v4.0-564-stable] - 2020-06-25

### Changes since v4.0-563-stable

- Pass the right assets path if map_to_resources is enabled (30026c6)

## [v4.0-563-stable] - 2020-06-25

### Changes since v4.0-562-stable

- Fix sounds on pre-1.6 Minecraft (15352a3)

## [v4.0-562-stable] - 2020-06-25

### Changes since v4.0-560-stable

- Fix game arguments logic for older MC versions (f79b96c)

## [v4.0-560-stable] - 2020-06-23

### Changes since v4.0-559-stable

- Improve game arguments generation (f4ce866)

## [v4.0-559-stable] - 2020-06-22

### Changes since v4.0-554-stable

- Add our Forge mirror as a fallback for artifact URLs (16a8518)

## [v4.0-554-stable] - 2020-06-16

### Changes since v4.0-546-stable

- Fix Guava version (0212c34)

## [v4.0-546-stable] - 2020-05-30

### Changes since v4.0-543-stable

- Inject new root certificates (a708aea)

## [v4.0-543-stable] - 2020-05-29

### Changes since v4.0-538-stable

- Remove Apex branding (fdb74f4)

## [v4.0-538-stable] - 2020-05-24

### Changes since v4.0-534-stable

- Log the exception when pinging an URL fails (709fa92)

## [v4.0-534-stable] - 2020-05-13

### Changes since v4.0-529-stable

- That was not supposed to have been removed (91805db)

## [v4.0-529-stable] - 2020-05-12

### Changes since v4.0-528-stable

- Forgot to take that out (5e6fd97)

## [v4.0-528-stable] - 2020-05-12

### Changes since v4.0-525-stable

- Add support for Forge 1.12.2 builds with the new installer (bfffd16)

## [v4.0-525-stable] - 2020-05-02

### Changes since v4.0-510-stable

- That should be parseInt (9ce0745)
- Use HEAD requests for URL checks (6df5fa0)
- Redact access token (2ee36eb)

## [v4.0-510-stable] - 2020-03-22

### Changes since v4.0-505-stable

_No commits recorded between previous stable and this build._

## [v4.0-505-stable] - 2020-02-19

### Changes since v4.0-503-stable

_No commits recorded between previous stable and this build._

## [v4.0-503-stable] - 2020-01-28

### Changes since v4.0-498-stable

- Use presence count for Discord guilds (7f46851)

## [v4.0-498-stable] - 2019-12-10

### Changes since v4.0-493-stable

_No commits recorded between previous stable and this build._

## [v4.0-493-stable] - 2019-10-31

### Changes since v4.0-491-stable

_No commits recorded between previous stable and this build._

## [v4.0-491-stable] - 2019-10-10

### Changes since v4.0-490-stable

_No commits recorded between previous stable and this build._

## [v4.0-490-stable] - 2019-10-09

### Changes since v4.0-489-stable

_No commits recorded between previous stable and this build._

## [v4.0-489-stable] - 2019-10-08

### Changes since v4.0-488-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-488-stable] - 2019-10-07

### Changes since v4.0-472-stable

- Change version to the previous format (c8a9a60)

## [v4.0-472-stable] - 2019-09-23

### Changes since v4.0-470-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-470-stable] - 2019-09-07

### Changes since v4.0-467-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-467-stable] - 2019-09-02

### Changes since v4.0-465-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-465-stable] - 2019-09-01

### Changes since v4.0-458-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-458-stable] - 2019-09-01

### Changes since v4.0-456-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-456-stable] - 2019-09-01

### Changes since v4.0-455-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-455-stable] - 2019-08-29

### Changes since v4.0-453-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-453-stable] - 2019-08-26

### Changes since v4.0-450-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-450-stable] - 2019-08-23

### Changes since v4.0-449-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-449-stable] - 2019-08-23

### Changes since v4.0-448-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-448-stable] - 2019-08-22

### Changes since v4.0-407-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-407-stable] - 2019-05-31

### Changes since v4.0-402-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-402-stable] - 2018-12-21

### Changes since v4.0-401-stable

- Fix Slovak language typos (#88) (7defc9e)

## [v4.0-401-stable] - 2018-11-21

### Changes since v4.0-397-stable

- Hook up Slovak language (ed2d4ab)

## [v4.0-397-stable] - 2018-11-03

### Changes since v4.0-396-stable

_No commits recorded between previous stable and this build._

## [v4.0-396-stable] - 2018-11-02

### Changes since v4.0-395-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-395-stable] - 2018-10-28

### Changes since v4.0-394-stable

_No commits recorded between previous stable and this build._

## [v4.0-394-stable] - 2018-10-28

### Changes since v4.0-393-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-393-stable] - 2018-10-28

### Changes since v4.0-392-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-392-stable] - 2018-10-28

### Changes since v4.0-391-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-391-stable] - 2018-10-28

### Changes since v4.0-388-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-388-stable] - 2018-10-28

### Changes since v4.0-373-stable

_No commits recorded between previous stable and this build._

## [v4.0-373-stable] - 2018-10-20

### Changes since v4.0-372-stable

_No commits recorded between previous stable and this build._

## [v4.0-372-stable] - 2018-10-20

### Changes since v4.0-369-stable

- Warn user if using java > 8 (f30b49f)

## [v4.0-369-stable] - 2018-10-19

### Changes since v4.0-360-stable

_No commits recorded between previous stable and this build._

## [v4.0-360-stable] - 2018-03-31

### Changes since v4.0-359-stable

- That shouldn't be there (233d511)

## [v4.0-359-stable] - 2018-03-30

### Changes since v4.0-355-stable

- Update website URLs to use HTTPS (538e3f7)

## [v4.0-355-stable] - 2018-01-14

### Changes since v4.0-353-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-353-stable] - 2017-09-15

### Changes since v4.0-349-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-349-stable] - 2017-01-31

### Changes since v4.0-347-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-347-stable] - 2016-01-26

### Changes since v4.0-339-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-339-stable] - 2015-12-18

### Changes since v4.0-328-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-328-stable] - 2015-09-24

### Changes since v4.0-327-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-327-stable] - 2015-09-24

### Changes since v4.0-326-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-326-stable] - 2015-09-24

### Changes since v4.0-325-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-325-stable] - 2015-09-24

### Changes since v4.0-322-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-322-stable] - 2015-09-24

### Changes since v4.0-319-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-319-stable] - 2015-09-14

### Changes since v4.0-317-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-317-stable] - 2015-09-10

### Changes since v4.0-313-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-313-stable] - 2015-08-07

### Changes since v4.0-312-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-312-stable] - 2015-08-07

### Changes since v4.0-311-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-311-stable] - 2015-08-07

### Changes since v4.0-310-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-310-stable] - 2015-08-07

### Changes since v4.0-308-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-308-stable] - 2015-07-10

### Changes since v4.0-304-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-304-stable] - 2015-07-06

### Changes since v4.0-303-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-303-stable] - 2015-07-06

### Changes since v4.0-301-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-301-stable] - 2015-04-25

### Changes since v4.0-300-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-300-stable] - 2015-04-24

### Changes since v4.0-283-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-283-stable] - 2015-03-06

### Changes since v4.0-282-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-282-stable] - 2015-02-25

### Changes since v4.0-272-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-272-stable] - 2015-02-11

### Changes since v4.0-265-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-265-stable] - 2015-02-04

### Changes since v4.0-256-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-256-stable] - 2015-01-28

### Changes since v4.0-238-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-238-stable] - 2015-01-20

### Changes since v4.0-234-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-234-stable] - 2015-01-17

### Changes since v4.0-231-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-231-stable] - 2015-01-15

### Changes since v4.0-211-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-211-stable] - 2015-01-03

### Changes since v4.0-210-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-210-stable] - 2015-01-03

### Changes since v4.0-209-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-209-stable] - 2014-12-31

### Changes since v4.0-208-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-208-stable] - 2014-12-30

### Changes since v4.0-207-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-207-stable] - 2014-12-30

### Changes since v4.0-205-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-205-stable] - 2014-12-29

### Changes since v4.0-204-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-204-stable] - 2014-12-27

### Changes since v4.0-203-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-203-stable] - 2014-12-27

### Changes since v4.0-201-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-201-stable] - 2014-12-27

### Changes since v4.0-199-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-199-stable] - 2014-12-26

### Changes since v4.0-192-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-192-stable] - 2014-12-26

### Changes since v4.0-191-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-191-stable] - 2014-12-25

### Changes since v4.0-185-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-185-stable] - 2014-12-25

### Changes since v4.0-181-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-181-stable] - 2014-12-25

### Changes since v4.0-172-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-172-stable] - 2014-12-25

### Changes since v4.0-171-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-171-stable] - 2014-12-25

### Changes since v4.0-170-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-170-stable] - 2014-12-25

### Changes since v4.0-169-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-169-stable] - 2014-12-25

### Changes since v4.0-168-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-168-stable] - 2014-12-25

### Changes since v4.0-167-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-167-stable] - 2014-12-25

### Changes since v4.0-166-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-166-stable] - 2014-12-25

### Changes since v4.0-165-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-165-stable] - 2014-12-19

### Changes since v4.0-163-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-163-stable] - 2014-12-18

### Changes since v4.0-162-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-162-stable] - 2014-12-16

### Changes since v4.0-161-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-161-stable] - 2014-12-08

### Changes since v4.0-160-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-160-stable] - 2014-12-08

### Changes since v4.0-159-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-159-stable] - 2014-12-06

### Changes since v4.0-158-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-158-stable] - 2014-12-05

### Changes since v4.0-156-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-156-stable] - 2014-12-05

### Changes since v4.0-155-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-155-stable] - 2014-12-05

### Changes since v4.0-154-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-154-stable] - 2014-12-05

### Changes since v4.0-153-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-153-stable] - 2014-12-05

### Changes since v4.0-152-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-152-stable] - 2014-12-05

### Changes since v4.0-151-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-151-stable] - 2014-12-02

### Changes since v4.0-150-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-150-stable] - 2014-12-02

### Changes since v4.0-149-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-149-stable] - 2014-11-15

### Changes since v4.0-148-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-148-stable] - 2014-11-12

### Changes since v4.0-147-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-147-stable] - 2014-11-12

### Changes since v4.0-146-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-146-stable] - 2014-11-12

### Changes since v4.0-145-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-145-stable] - 2014-11-12

### Changes since v4.0-143-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-143-stable] - 2014-11-12

### Changes since v4.0-142-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-142-stable] - 2014-11-12

### Changes since v4.0-141-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-141-stable] - 2014-11-12

### Changes since v4.0-138-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-138-stable] - 2014-11-12

### Changes since v4.0-137-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-137-stable] - 2014-11-12

### Changes since v4.0-136-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-136-stable] - 2014-11-04

### Changes since v4.0-135-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-135-stable] - 2014-11-04

### Changes since v4.0-133-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-133-stable] - 2014-11-04

### Changes since v4.0-132-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-132-stable] - 2014-10-31

### Changes since v4.0-130-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-130-stable] - 2014-10-30

### Changes since v4.0-129-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-129-stable] - 2014-10-29

### Changes since v4.0-128-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-128-stable] - 2014-10-26

### Changes since v4.0-127-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-127-stable] - 2014-10-26

### Changes since v4.0-126-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-126-stable] - 2014-10-26

### Changes since v4.0-125-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-125-stable] - 2014-10-26

### Changes since v4.0-124-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-124-stable] - 2014-10-26

### Changes since v4.0-123-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-123-stable] - 2014-10-23

### Changes since v4.0-119-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-119-stable] - 2014-10-22

### Changes since v4.0-118-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-118-stable] - 2014-10-19

### Changes since v4.0-117-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-117-stable] - 2014-10-17

### Changes since v4.0-116-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-116-stable] - 2014-10-16

### Changes since v4.0-115-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-115-stable] - 2014-10-14

### Changes since v4.0-114-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-114-stable] - 2014-10-14

### Changes since v4.0-113-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-113-stable] - 2014-10-11

### Changes since v4.0-112-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-112-stable] - 2014-10-10

### Changes since v4.0-111-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-111-stable] - 2014-10-09

### Changes since v4.0-110-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-110-stable] - 2014-10-09

### Changes since v4.0-109-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-109-stable] - 2014-10-09

### Changes since v4.0-108-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-108-stable] - 2014-10-08

### Changes since v4.0-106-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-106-stable] - 2014-10-08

### Changes since v4.0-105-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-105-stable] - 2014-10-08

### Changes since v4.0-103-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-103-stable] - 2014-10-08

### Changes since v4.0-102-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-102-stable] - 2014-10-08

### Changes since v4.0-101-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-101-stable] - 2014-10-08

### Changes since v4.0-99-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-99-stable] - 2014-10-08

### Changes since v4.0-98-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-98-stable] - 2014-10-08

### Changes since v4.0-96-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-96-stable] - 2014-10-08

### Changes since v4.0-94-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-94-stable] - 2014-10-07

### Changes since v4.0-92-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-92-stable] - 2014-10-07

### Changes since v4.0-91-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-91-stable] - 2014-10-07

### Changes since v4.0-90-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-90-stable] - 2014-10-06

### Changes since v4.0-89-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-89-stable] - 2014-10-06

### Changes since v4.0-88-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-88-stable] - 2014-10-06

### Changes since v4.0-87-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-87-stable] - 2014-10-06

### Changes since v4.0-86-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-86-stable] - 2014-10-06

### Changes since v4.0-85-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-85-stable] - 2014-10-05

### Changes since v4.0-84-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-84-stable] - 2014-10-05

### Changes since v4.0-82-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-82-stable] - 2014-10-05

### Changes since v4.0-80-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-80-stable] - 2014-10-05

### Changes since v4.0-79-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-79-stable] - 2014-10-05

### Changes since v4.0-78-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-78-stable] - 2014-10-05

### Changes since v4.0-77-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-77-stable] - 2014-10-05

### Changes since v4.0-76-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-76-stable] - 2014-10-04

### Changes since v4.0-73-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-73-stable] - 2014-10-04

### Changes since v4.0-72-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-72-stable] - 2014-10-03

### Changes since v4.0-71-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-71-stable] - 2014-10-03

### Changes since v4.0-69-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-69-stable] - 2014-10-03

### Changes since v4.0-67-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-67-stable] - 2014-10-03

### Changes since v4.0-64-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-64-stable] - 2014-10-03

### Changes since v4.0-63-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-63-stable] - 2014-10-03

### Changes since v4.0-62-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-62-stable] - 2014-10-03

### Changes since v4.0-61-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-61-stable] - 2014-10-02

### Changes since v4.0-58-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-58-stable] - 2014-10-01

### Changes since v4.0-56-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-56-stable] - 2014-09-30

### Changes since v4.0-55-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-55-stable] - 2014-09-28

### Changes since v4.0-54-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-54-stable] - 2014-09-28

### Changes since v4.0-53-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-53-stable] - 2014-09-28

### Changes since v4.0-51-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-51-stable] - 2014-09-28

### Changes since v4.0-50-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-50-stable] - 2014-09-27

### Changes since v4.0-49-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-49-stable] - 2014-09-20

### Changes since v4.0-48-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-48-stable] - 2014-09-20

### Changes since v4.0-47-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-47-stable] - 2014-09-20

### Changes since v4.0-46-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-46-stable] - 2014-09-20

### Changes since v4.0-45-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-45-stable] - 2014-09-20

### Changes since v4.0-35-stable

_Build data purged from CI; changelog unrecoverable._

## [v4.0-35-stable] - 2014-09-19

Initial tracked stable release (v4.0-35-stable).

